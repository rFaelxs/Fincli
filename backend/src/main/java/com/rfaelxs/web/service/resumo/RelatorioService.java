package com.rfaelxs.web.service.resumo;

import com.rfaelxs.web.domain.Reserva;
import com.rfaelxs.web.domain.TipoMovimentacaoReserva;
import com.rfaelxs.web.domain.Usuario;
import com.rfaelxs.web.repository.MovimentacaoReservaRepository;
import com.rfaelxs.web.repository.TotalCategoria;
import com.rfaelxs.web.repository.TotalPorReserva;
import com.rfaelxs.web.repository.TransacaoRepository;
import com.rfaelxs.web.service.ContaPrevistaService;
import com.rfaelxs.web.service.Dinheiro;
import com.rfaelxs.web.service.ReservaService;
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

/**
 * Números da tela Relatório: fluxo do mês, histórico de sobra, categorias, ritmo das reservas e
 * projeção de fechamento.
 */
@Service
public class RelatorioService {

  /** Quantos meses o gráfico "sobra dos últimos meses" mostra. */
  private static final int MESES_HISTORICO = 6;

  /** Janela usada para estimar o aporte médio de cada reserva. */
  private static final int MESES_RITMO = 3;

  /** Quantas categorias aparecem em "o que puxou meu mês pra baixo". */
  private static final int TOP_CATEGORIAS = 4;

  private final CalculadoraDeTotais calculadora;
  private final TransacaoRepository transacaoRepository;
  private final MovimentacaoReservaRepository movimentacaoRepository;
  private final ReservaService reservaService;
  private final ContaPrevistaService contaPrevistaService;
  private final Clock relogio;

  public RelatorioService(
      CalculadoraDeTotais calculadora,
      TransacaoRepository transacaoRepository,
      MovimentacaoReservaRepository movimentacaoRepository,
      ReservaService reservaService,
      ContaPrevistaService contaPrevistaService,
      Clock relogio) {
    this.calculadora = calculadora;
    this.transacaoRepository = transacaoRepository;
    this.movimentacaoRepository = movimentacaoRepository;
    this.reservaService = reservaService;
    this.contaPrevistaService = contaPrevistaService;
    this.relogio = relogio;
  }

  /**
   * Monta o Relatório do mês informado.
   *
   * @param usuario dono dos dados
   * @param mes     mês de referência
   * @return fluxo do mês, histórico de sobra, categorias, ritmo das reservas e projeção
   */
  @Transactional(readOnly = true)
  public Relatorio montar(Usuario usuario, YearMonth mes) {
    Map<YearMonth, Totais> porMes =
        calculadora.porMes(usuario, mes.minusMonths(MESES_HISTORICO - 1L), mes);

    List<SobraMensal> historico = new ArrayList<>();
    for (int i = MESES_HISTORICO - 1; i >= 0; i--) {
      YearMonth m = mes.minusMonths(i);
      historico.add(new SobraMensal(m, porMes.getOrDefault(m, Totais.zero()).sobra()));
    }

    Totais totais = porMes.getOrDefault(mes, Totais.zero());

    List<TotalCategoria> topCategorias =
        transacaoRepository
            .somarSaidasPorCategoria(usuario.getId(), mes.atDay(1), mes.atEndOfMonth())
            .stream()
            .limit(TOP_CATEGORIAS)
            .map(c -> new TotalCategoria(c.categoria(), Dinheiro.normalizar(c.total())))
            .toList();

    // A projeção só faz sentido no mês corrente. Num mês já fechado nada mais vence, e a sobra
    // prevista coincide com a sobra realizada.
    LocalDate hoje = LocalDate.now(relogio);
    LocalDate corte = YearMonth.from(hoje).equals(mes) ? hoje : mes.atEndOfMonth().plusDays(1);
    BigDecimal aindaVence =
        ContaPrevistaService.somar(contaPrevistaService.aVencer(usuario, mes, corte));

    return new Relatorio(
        mes,
        totais,
        historico,
        topCategorias,
        ritmoDasReservas(usuario, mes),
        new Projecao(
            totais.saidas(),
            aindaVence,
            Dinheiro.normalizar(totais.sobra().subtract(aindaVence))));
  }

  /**
   * Ritmo de cada reserva: quanto entrou por mês em média e quando a meta fecha nesse passo.
   *
   * @param mes mês de referência — a janela de {@value #MESES_RITMO} meses olha para trás dele
   */
  private List<RitmoReserva> ritmoDasReservas(Usuario usuario, YearMonth mes) {
    YearMonth de = mes.minusMonths(MESES_RITMO - 1L);
    Map<Long, BigDecimal> liquidoPorReserva = new HashMap<>();
    for (TotalPorReserva t :
        movimentacaoRepository.somarPorReserva(
            usuario.getId(), de.atDay(1), mes.atEndOfMonth())) {
      liquidoPorReserva.merge(
          t.reservaId(),
          t.tipo() == TipoMovimentacaoReserva.ALOCACAO ? t.total() : t.total().negate(),
          BigDecimal::add);
    }

    List<RitmoReserva> ritmos = new ArrayList<>();
    for (Reserva r : reservaService.listar(usuario)) {
      BigDecimal medio =
          Dinheiro.normalizar(
              liquidoPorReserva
                  .getOrDefault(r.getId(), BigDecimal.ZERO)
                  .divide(BigDecimal.valueOf(MESES_RITMO), 2, RoundingMode.HALF_UP));
      ritmos.add(ritmo(r, medio, mes));
    }
    return ritmos;
  }

  private static RitmoReserva ritmo(Reserva r, BigDecimal aporteMedio, YearMonth mes) {
    BigDecimal meta = r.getMetaValor();
    BigDecimal saldo = r.getSaldoAtual();
    BigDecimal progresso =
        meta.signum() == 0
            ? BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP)
            : saldo.multiply(BigDecimal.valueOf(100)).divide(meta, 1, RoundingMode.HALF_UP);

    // Sem meta não há o que projetar; a reserva aparece na lista, fora da contagem "N de M".
    if (meta.signum() == 0) {
      return new RitmoReserva(
          r.getPublicId(), r.getNome(), saldo, meta, progresso, aporteMedio, null, false);
    }
    if (saldo.compareTo(meta) >= 0) {
      return new RitmoReserva(
          r.getPublicId(), r.getNome(), saldo, meta, progresso, aporteMedio, mes, true);
    }
    // Reserva parada ou encolhendo: qualquer previsão aqui seria invenção.
    if (aporteMedio.signum() <= 0) {
      return new RitmoReserva(
          r.getPublicId(), r.getNome(), saldo, meta, progresso, aporteMedio, null, false);
    }

    long meses = meta.subtract(saldo).divide(aporteMedio, 0, RoundingMode.CEILING).longValue();
    return new RitmoReserva(
        r.getPublicId(),
        r.getNome(),
        saldo,
        meta,
        progresso,
        aporteMedio,
        mes.plusMonths(meses),
        true);
  }
}
