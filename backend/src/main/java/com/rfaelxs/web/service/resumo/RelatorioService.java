package com.rfaelxs.web.service.resumo;

import com.rfaelxs.web.domain.Reserva;
import com.rfaelxs.web.domain.TipoMovimentacaoReserva;
import com.rfaelxs.web.repository.MovimentacaoReservaRepository;
import com.rfaelxs.web.repository.ReservaRepository;
import com.rfaelxs.web.repository.TotalCategoria;
import com.rfaelxs.web.repository.TotalPorReserva;
import com.rfaelxs.web.repository.TransacaoRepository;
import com.rfaelxs.web.service.ContaPrevistaService;
import com.rfaelxs.web.service.Dinheiro;
import com.rfaelxs.web.service.resumo.Relatorio.Projecao;
import com.rfaelxs.web.service.resumo.Relatorio.RitmoReserva;
import com.rfaelxs.web.service.resumo.Relatorio.SobraMensal;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** O que {@code GET /api/relatorio} devolve (spec 05). */
@Service
public class RelatorioService {

  /** Quantos meses o gráfico "sobra dos últimos meses" mostra. */
  static final int MESES_HISTORICO = 6;

  /** Janela do aporte médio de cada reserva. */
  static final int MESES_RITMO = 3;

  /** Quantas categorias aparecem em "o que puxou meu mês pra baixo". */
  static final int TOP_CATEGORIAS = 4;

  private final CalculadoraDeTotais calculadora;
  private final ContaPrevistaService contaPrevistaService;
  private final TransacaoRepository transacaoRepository;
  private final MovimentacaoReservaRepository movimentacaoRepository;
  private final ReservaRepository reservaRepository;
  private final Clock relogio;

  /** Cria o serviço com as fontes de dados e o relógio da aplicação. */
  public RelatorioService(
      CalculadoraDeTotais calculadora,
      ContaPrevistaService contaPrevistaService,
      TransacaoRepository transacaoRepository,
      MovimentacaoReservaRepository movimentacaoRepository,
      ReservaRepository reservaRepository,
      Clock relogio) {
    this.calculadora = calculadora;
    this.contaPrevistaService = contaPrevistaService;
    this.transacaoRepository = transacaoRepository;
    this.movimentacaoRepository = movimentacaoRepository;
    this.reservaRepository = reservaRepository;
    this.relogio = relogio;
  }

  /** Relatório do mês corrente. */
  @Transactional(readOnly = true)
  public Relatorio doMesCorrente(Long usuarioId) {
    return doMes(usuarioId, YearMonth.now(relogio));
  }

  /** Relatório do mês informado: fluxo, histórico, categorias, ritmo e projeção. */
  @Transactional(readOnly = true)
  public Relatorio doMes(Long usuarioId, YearMonth mes) {
    YearMonth inicioHistorico = mes.minusMonths(MESES_HISTORICO - 1L);
    Map<YearMonth, Totais> porMes = calculadora.porMes(usuarioId, inicioHistorico, mes);

    List<SobraMensal> historico = new ArrayList<>();
    for (YearMonth m = inicioHistorico; !m.isAfter(mes); m = m.plusMonths(1)) {
      historico.add(new SobraMensal(m, porMes.getOrDefault(m, Totais.zero()).sobra()));
    }
    Totais totais = porMes.getOrDefault(mes, Totais.zero());

    List<TotalCategoria> topCategorias = transacaoRepository
        .somarSaidasPorCategoria(usuarioId, mes.atDay(1), mes.atEndOfMonth()).stream()
        .limit(TOP_CATEGORIAS)
        .map(c -> new TotalCategoria(c.categoria(), Dinheiro.normalizar(c.total())))
        .toList();

    BigDecimal aindaVence = ContaPrevistaService.somar(
        contaPrevistaService.aindaVencem(usuarioId, mes, corteDasContas(mes)));
    Projecao projecao = new Projecao(totais.saidas(), aindaVence,
        Dinheiro.normalizar(totais.sobra().subtract(aindaVence)));

    return new Relatorio(mes, totais, historico, topCategorias, ritmos(usuarioId, mes),
        projecao);
  }

  /**
   * A partir de quando uma conta do mês ainda "vence". No mês corrente, hoje. Num mês fechado,
   * nada mais vence. Num mês futuro, todas as contas do mês ainda vão vencer.
   */
  private LocalDate corteDasContas(YearMonth mes) {
    LocalDate hoje = LocalDate.now(relogio);
    YearMonth corrente = YearMonth.from(hoje);
    if (mes.equals(corrente)) {
      return hoje;
    }
    return mes.isBefore(corrente) ? mes.atEndOfMonth().plusDays(1) : mes.atDay(1);
  }

  /**
   * Ritmo de cada reserva na janela de {@value #MESES_RITMO} meses até o de referência. Saldo e
   * meta são os atuais, mesmo num mês passado.
   */
  private List<RitmoReserva> ritmos(Long usuarioId, YearMonth mes) {
    YearMonth inicioJanela = mes.minusMonths(MESES_RITMO - 1L);
    Map<Long, BigDecimal> liquidoPorReserva = new HashMap<>();
    for (TotalPorReserva t : movimentacaoRepository.somarPorReserva(
        usuarioId, inicioJanela.atDay(1), mes.atEndOfMonth())) {
      BigDecimal sinal = t.tipo() == TipoMovimentacaoReserva.ALOCACAO
          ? t.total() : t.total().negate();
      liquidoPorReserva.merge(t.reservaId(), sinal, BigDecimal::add);
    }

    return reservaRepository.findByUsuarioIdOrderByEmergenciaDescNomeAsc(usuarioId).stream()
        .map(r -> ritmo(r, liquidoPorReserva.getOrDefault(r.getId(), BigDecimal.ZERO)
            .divide(BigDecimal.valueOf(MESES_RITMO), 2, RoundingMode.HALF_UP), mes))
        .toList();
  }

  /** Previsão de quando a meta fecha no ritmo do aporte médio. */
  static RitmoReserva ritmo(Reserva r, BigDecimal aporteMedio, YearMonth mes) {
    BigDecimal meta = r.getMetaValor();
    BigDecimal saldo = r.getSaldoAtual();
    YearMonth previsao = null;
    boolean noRitmo = false;

    if (meta.signum() > 0 && saldo.compareTo(meta) >= 0) {
      // Meta batida: fecha no próprio mês de referência.
      previsao = mes;
      noRitmo = true;
    } else if (meta.signum() > 0 && aporteMedio.signum() > 0) {
      long meses = meta.subtract(saldo).divide(aporteMedio, 0, RoundingMode.CEILING).longValue();
      previsao = mes.plusMonths(meses);
      noRitmo = true;
    }
    // Sem meta, ou com a reserva parada ou encolhendo, qualquer previsão seria invenção.

    return new RitmoReserva(r.getPublicId(), r.getNome(), saldo, meta, r.progresso(),
        aporteMedio, previsao, noRitmo);
  }
}
