package com.rfaelxs.web.service.resumo;

import com.rfaelxs.web.domain.ContaPrevista;
import com.rfaelxs.web.repository.MovimentacaoReservaRepository;
import com.rfaelxs.web.repository.TransacaoRepository;
import com.rfaelxs.web.service.ContaPrevistaService;
import com.rfaelxs.web.service.Dinheiro;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * O que {@code GET /api/hoje} devolve (spec 03).
 *
 * <pre>
 *   previstas = soma das contas ainda por vencer no mês
 *   livre     = sobra − previstas
 *   porDia    = livre / máx(diasRestantes, 1)
 * </pre>
 */
@Service
public class GestaoService {

  /** Quantos lançamentos a Gestão mostra. */
  static final int ULTIMOS_LANCAMENTOS = 6;

  private final CalculadoraDeTotais calculadora;
  private final ContaPrevistaService contaPrevistaService;
  private final TransacaoRepository transacaoRepository;
  private final MovimentacaoReservaRepository movimentacaoRepository;
  private final Clock relogio;

  /** Cria o serviço com as fontes de dados e o relógio da aplicação. */
  public GestaoService(
      CalculadoraDeTotais calculadora,
      ContaPrevistaService contaPrevistaService,
      TransacaoRepository transacaoRepository,
      MovimentacaoReservaRepository movimentacaoRepository,
      Clock relogio) {
    this.calculadora = calculadora;
    this.contaPrevistaService = contaPrevistaService;
    this.transacaoRepository = transacaoRepository;
    this.movimentacaoRepository = movimentacaoRepository;
    this.relogio = relogio;
  }

  /** Números do mês corrente. Não aceita outro mês, por decisão de produto. */
  @Transactional(readOnly = true)
  public Gestao hoje(Long usuarioId) {
    LocalDate hoje = LocalDate.now(relogio);
    YearMonth mes = YearMonth.from(hoje);

    Totais totais = calculadora.doMes(usuarioId, mes);
    List<ContaPrevista> previstas = contaPrevistaService.aindaVencem(usuarioId, mes, hoje);
    BigDecimal previstasTotal = ContaPrevistaService.somar(previstas);
    BigDecimal livre = Dinheiro.normalizar(totais.sobra().subtract(previstasTotal));

    // No último dia do mês os dias restantes são zero. O divisor tem piso 1 para não dividir por
    // zero; a tela mostra o número real de dias.
    int diasRestantes = mes.lengthOfMonth() - hoje.getDayOfMonth();
    BigDecimal porDia =
        livre.divide(BigDecimal.valueOf(Math.max(diasRestantes, 1)), 2, RoundingMode.HALF_UP);

    return new Gestao(hoje, mes, livre, porDia, diasRestantes, totais, previstasTotal,
        ultimosLancamentos(usuarioId, mes), previstas);
  }

  /**
   * Os 6 lançamentos mais recentes do mês, das duas origens. Cada origem já vem limitada a 6
   * pelo banco: os 6 primeiros da junção estão sempre entre eles.
   */
  private List<Lancamento> ultimosLancamentos(Long usuarioId, YearMonth mes) {
    Stream<Lancamento> transacoes = transacaoRepository
        .findTop6ByUsuarioIdAndDataBetweenOrderByDataDescIdDesc(
            usuarioId, mes.atDay(1), mes.atEndOfMonth())
        .stream().map(Lancamento::de);
    Stream<Lancamento> movimentacoes = movimentacaoRepository
        .findTop6ByUsuarioIdAndDataBetweenOrderByDataDescIdDesc(
            usuarioId, mes.atDay(1), mes.atEndOfMonth())
        .stream().map(Lancamento::de);
    return Stream.concat(transacoes, movimentacoes)
        .sorted(Lancamento.ORDEM)
        .limit(ULTIMOS_LANCAMENTOS)
        .toList();
  }
}
