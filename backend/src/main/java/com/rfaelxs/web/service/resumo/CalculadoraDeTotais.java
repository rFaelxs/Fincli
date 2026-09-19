package com.rfaelxs.web.service.resumo;

import com.rfaelxs.web.domain.TipoMovimentacaoReserva;
import com.rfaelxs.web.domain.TipoTransacao;
import com.rfaelxs.web.domain.Usuario;
import com.rfaelxs.web.repository.MovimentacaoReservaRepository;
import com.rfaelxs.web.repository.TotalMensal;
import com.rfaelxs.web.repository.TotalMovimentacaoMensal;
import com.rfaelxs.web.repository.TransacaoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Totais de um mês, a única conta que as telas Gestão e Relatório compartilham.
 *
 * <p>Implementa as fórmulas do handoff de design:
 *
 * <pre>
 *   essenciais = soma das saídas marcadas essencial
 *   superfluas = saidas - essenciais
 *   aportes    = alocações - saques do mês
 *   sobra      = entradas - saidas - aportes
 * </pre>
 *
 * <p>Tudo em {@code BigDecimal} escala 2 HALF_UP (ADR 0008). Nenhuma soma percorre a lista
 * inteira de transações em memória: as agregações vêm do banco.
 */
@Component
public class CalculadoraDeTotais {

  private final TransacaoRepository transacaoRepository;
  private final MovimentacaoReservaRepository movimentacaoRepository;

  public CalculadoraDeTotais(
      TransacaoRepository transacaoRepository,
      MovimentacaoReservaRepository movimentacaoRepository) {
    this.transacaoRepository = transacaoRepository;
    this.movimentacaoRepository = movimentacaoRepository;
  }

  /** @return os totais de um único mês */
  Totais doMes(Usuario usuario, YearMonth mes) {
    return porMes(usuario, mes, mes).getOrDefault(mes, Totais.zero());
  }

  /**
   * Totais de cada mês do intervalo, em duas consultas agregadas.
   *
   * @return mapa por mês; meses sem movimento simplesmente não aparecem
   */
  Map<YearMonth, Totais> porMes(Usuario usuario, YearMonth de, YearMonth ate) {
    LocalDate inicio = de.atDay(1);
    LocalDate fim = ate.atEndOfMonth();

    // Por mês: [0] entradas, [1] saídas, [2] essenciais, [3] aportes.
    Map<YearMonth, BigDecimal[]> acumulado = new HashMap<>();
    for (TotalMensal t : transacaoRepository.somarPorMes(usuario.getId(), inicio, fim)) {
      BigDecimal[] linha = linha(acumulado, YearMonth.of(t.ano(), t.mes()));
      if (t.tipo() == TipoTransacao.ENTRADA) {
        linha[0] = linha[0].add(t.total());
      } else {
        linha[1] = linha[1].add(t.total());
        if (Boolean.TRUE.equals(t.essencial())) {
          linha[2] = linha[2].add(t.total());
        }
      }
    }
    for (TotalMovimentacaoMensal m :
        movimentacaoRepository.somarPorMes(usuario.getId(), inicio, fim)) {
      BigDecimal[] linha = linha(acumulado, YearMonth.of(m.ano(), m.mes()));
      linha[3] =
          m.tipo() == TipoMovimentacaoReserva.ALOCACAO
              ? linha[3].add(m.total())
              : linha[3].subtract(m.total());
    }

    Map<YearMonth, Totais> totais = new HashMap<>();
    acumulado.forEach((mes, l) -> totais.put(mes, Totais.de(l[0], l[1], l[2], l[3])));
    return totais;
  }

  private static BigDecimal[] linha(Map<YearMonth, BigDecimal[]> mapa, YearMonth mes) {
    return mapa.computeIfAbsent(
        mes,
        chave ->
            new BigDecimal[] {
              BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
            });
  }
}
