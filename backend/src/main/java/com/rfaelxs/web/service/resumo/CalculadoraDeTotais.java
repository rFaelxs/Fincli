package com.rfaelxs.web.service.resumo;

import com.rfaelxs.web.domain.TipoMovimentacaoReserva;
import com.rfaelxs.web.domain.TipoTransacao;
import com.rfaelxs.web.repository.MovimentacaoReservaRepository;
import com.rfaelxs.web.repository.TotalMensal;
import com.rfaelxs.web.repository.TotalMovimentacaoMensal;
import com.rfaelxs.web.repository.TransacaoRepository;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Totais por mês, a única peça que a Gestão e o Relatório compartilham (ADR 0005).
 *
 * <p>Duas consultas agregadas cobrem o intervalo inteiro: nenhuma soma percorre a lista de
 * transações em memória (ADR 0008).
 */
@Component
public class CalculadoraDeTotais {

  private final TransacaoRepository transacaoRepository;
  private final MovimentacaoReservaRepository movimentacaoRepository;

  public CalculadoraDeTotais(TransacaoRepository transacaoRepository,
      MovimentacaoReservaRepository movimentacaoRepository) {
    this.transacaoRepository = transacaoRepository;
    this.movimentacaoRepository = movimentacaoRepository;
  }

  /** Totais de um mês só; zerados quando não houve movimento. */
  @Transactional(readOnly = true)
  public Totais doMes(Long usuarioId, YearMonth mes) {
    return porMes(usuarioId, mes, mes).getOrDefault(mes, Totais.zero());
  }

  /**
   * Totais de cada mês do intervalo, inclusive nas pontas.
   *
   * @return mapa por mês; um mês sem movimento não aparece
   */
  @Transactional(readOnly = true)
  public Map<YearMonth, Totais> porMes(Long usuarioId, YearMonth de, YearMonth ate) {
    Map<YearMonth, Somas> somas = new HashMap<>();

    for (TotalMensal t : transacaoRepository.somarPorMes(usuarioId, de.atDay(1),
        ate.atEndOfMonth())) {
      Somas s = somas.computeIfAbsent(YearMonth.of(t.ano(), t.mes()), m -> new Somas());
      if (t.tipo() == TipoTransacao.ENTRADA) {
        s.entradas = s.entradas.add(t.total());
      } else {
        s.saidas = s.saidas.add(t.total());
        if (Boolean.TRUE.equals(t.essencial())) {
          s.essenciais = s.essenciais.add(t.total());
        }
      }
    }

    // O aporte do mês é o que foi alocado menos o que foi sacado.
    for (TotalMovimentacaoMensal m : movimentacaoRepository.somarPorMes(usuarioId,
        de.atDay(1), ate.atEndOfMonth())) {
      Somas s = somas.computeIfAbsent(YearMonth.of(m.ano(), m.mes()), mes -> new Somas());
      s.aportes = m.tipo() == TipoMovimentacaoReserva.ALOCACAO
          ? s.aportes.add(m.total())
          : s.aportes.subtract(m.total());
    }

    Map<YearMonth, Totais> totais = new HashMap<>();
    somas.forEach((mes, s) -> totais.put(mes,
        Totais.de(s.entradas, s.saidas, s.essenciais, s.aportes)));
    return totais;
  }

  /** Acumulador de um mês enquanto as linhas agregadas são lidas. */
  private static final class Somas {
    private BigDecimal entradas = BigDecimal.ZERO;
    private BigDecimal saidas = BigDecimal.ZERO;
    private BigDecimal essenciais = BigDecimal.ZERO;
    private BigDecimal aportes = BigDecimal.ZERO;
  }
}
