package com.rfaelxs.web.service;

import com.rfaelxs.web.domain.ContaPrevista;
import com.rfaelxs.web.domain.MovimentacaoReserva;
import com.rfaelxs.web.domain.Reserva;
import com.rfaelxs.web.domain.TipoMovimentacaoReserva;
import com.rfaelxs.web.domain.TipoTransacao;
import com.rfaelxs.web.domain.Transacao;
import com.rfaelxs.web.domain.Usuario;
import com.rfaelxs.web.repository.MovimentacaoReservaRepository;
import com.rfaelxs.web.repository.TotalCategoria;
import com.rfaelxs.web.repository.TotalMensal;
import com.rfaelxs.web.repository.TotalMovimentacaoMensal;
import com.rfaelxs.web.repository.TotalPorReserva;
import com.rfaelxs.web.repository.TransacaoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Números das telas Hoje e Relatório.
 *
 * <p>Implementa as fórmulas do handoff de design:
 *
 * <pre>
 *   essenciais = soma das saídas marcadas essencial
 *   superfluas = saidas - essenciais
 *   aportes    = alocações - saques do mês
 *   sobra      = entradas - saidas - aportes
 *   previstas  = soma das contas ainda por vencer no mês
 *   livre      = sobra - previstas          (o número grande da tela Hoje)
 *   porDia     = livre / dias que faltam para o fim do mês
 * </pre>
 *
 * <p>Tudo em {@code BigDecimal} escala 2 HALF_UP (escopo-web.md §1.4). Nenhuma soma percorre a
 * lista inteira de transações em memória: as agregações vêm do banco.
 */
@Service
public class ResumoService {

  /** Quantos meses o gráfico "sobra dos últimos meses" mostra. */
  private static final int MESES_HISTORICO = 6;

  /** Janela usada para estimar o aporte médio de cada reserva. */
  private static final int MESES_RITMO = 3;

  /** Quantas categorias aparecem em "o que puxou meu mês pra baixo". */
  private static final int TOP_CATEGORIAS = 4;

  /** Quantos lançamentos a tela Hoje mostra. */
  private static final int ULTIMOS_LANCAMENTOS = 6;

  private final TransacaoRepository transacaoRepository;
  private final MovimentacaoReservaRepository movimentacaoRepository;
  private final ReservaService reservaService;
  private final ContaPrevistaService contaPrevistaService;
  private final Clock relogio;

  public ResumoService(
      TransacaoRepository transacaoRepository,
      MovimentacaoReservaRepository movimentacaoRepository,
      ReservaService reservaService,
      ContaPrevistaService contaPrevistaService,
      Clock relogio) {
    this.transacaoRepository = transacaoRepository;
    this.movimentacaoRepository = movimentacaoRepository;
    this.reservaService = reservaService;
    this.contaPrevistaService = contaPrevistaService;
    this.relogio = relogio;
  }

  /**
   * Monta a tela Hoje, sempre do mês corrente.
   *
   * @param usuario dono dos dados
   * @return o número livre, o ritmo diário, os totais do mês e as listas de apoio
   */
  @Transactional(readOnly = true)
  public Hoje hoje(Usuario usuario) {
    LocalDate hoje = LocalDate.now(relogio);
    YearMonth mes = YearMonth.from(hoje);

    Totais totais = totaisDoMes(usuario, mes);
    List<ContaPrevista> previstas = contaPrevistaService.aVencer(usuario, mes, hoje);
    BigDecimal previstasTotal = ContaPrevistaService.somar(previstas);
    BigDecimal livre = Dinheiro.normalizar(totais.sobra().subtract(previstasTotal));

    // O README define os dias restantes como último dia menos hoje. No último dia do mês isso
    // dá zero, e dividir por zero derrubaria a tela: o divisor tem piso 1 e o rótulo mostra o
    // número real de dias.
    int diasRestantes = mes.lengthOfMonth() - hoje.getDayOfMonth();
    BigDecimal porDia =
        livre.divide(BigDecimal.valueOf(Math.max(diasRestantes, 1)), 2, RoundingMode.HALF_UP);

    return new Hoje(
        hoje,
        mes,
        livre,
        porDia,
        diasRestantes,
        totais,
        previstasTotal,
        ultimosLancamentos(usuario, mes),
        previstas);
  }

  /**
   * Monta o Relatório do mês informado.
   *
   * @param usuario dono dos dados
   * @param mes     mês de referência
   * @return fluxo do mês, histórico de sobra, categorias, ritmo das reservas e projeção
   */
  @Transactional(readOnly = true)
  public Relatorio relatorio(Usuario usuario, YearMonth mes) {
    Map<YearMonth, Totais> porMes =
        totaisPorMes(usuario, mes.minusMonths(MESES_HISTORICO - 1L), mes);

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

  /** @return os totais de um único mês */
  private Totais totaisDoMes(Usuario usuario, YearMonth mes) {
    return totaisPorMes(usuario, mes, mes).getOrDefault(mes, Totais.zero());
  }

  /**
   * Totais de cada mês do intervalo, em duas consultas agregadas.
   *
   * @return mapa por mês; meses sem movimento simplesmente não aparecem
   */
  private Map<YearMonth, Totais> totaisPorMes(Usuario usuario, YearMonth de, YearMonth ate) {
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

  /** @return transações e movimentações do mês na mesma lista, as mais recentes primeiro */
  private List<Lancamento> ultimosLancamentos(Usuario usuario, YearMonth mes) {
    LocalDate inicio = mes.atDay(1);
    LocalDate fim = mes.atEndOfMonth();

    Stream<Lancamento> transacoes =
        transacaoRepository
            .findByUsuarioIdAndDataTransacaoBetweenOrderByDataTransacaoDescIdDesc(
                usuario.getId(), inicio, fim)
            .stream()
            .map(ResumoService::lancamento);

    Stream<Lancamento> movimentos =
        movimentacaoRepository
            .findByUsuarioIdAndDataMovimentoBetweenOrderByDataMovimentoDescIdDesc(
                usuario.getId(), inicio, fim)
            .stream()
            .map(ResumoService::lancamento);

    return Stream.concat(transacoes, movimentos)
        .sorted(Comparator.comparing(Lancamento::data).reversed())
        .limit(ULTIMOS_LANCAMENTOS)
        .toList();
  }

  private static Lancamento lancamento(Transacao t) {
    boolean temDescricao = t.getDescricao() != null && !t.getDescricao().isBlank();
    return new Lancamento(
        t.getPublicId(),
        t.getDataTransacao(),
        temDescricao ? t.getDescricao() : t.getCategoria(),
        t.getCategoria(),
        t.isEssencial(),
        t.getTipo() == TipoTransacao.ENTRADA ? TipoLancamento.ENTRADA : TipoLancamento.SAIDA,
        t.getValor());
  }

  private static Lancamento lancamento(MovimentacaoReserva m) {
    return new Lancamento(
        null,
        m.getDataMovimento(),
        m.getReservaNome(),
        "Reserva",
        false,
        m.getTipo() == TipoMovimentacaoReserva.ALOCACAO
            ? TipoLancamento.APORTE
            : TipoLancamento.SAQUE,
        m.getValor());
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

  /** Como um lançamento aparece na lista da tela Hoje. */
  public enum TipoLancamento {
    ENTRADA,
    SAIDA,
    APORTE,
    SAQUE
  }

  /**
   * Totais de um mês.
   *
   * @param aportes alocações menos saques; negativo quando o usuário sacou mais do que guardou
   */
  public record Totais(
      BigDecimal entradas,
      BigDecimal saidas,
      BigDecimal essenciais,
      BigDecimal superfluas,
      BigDecimal aportes,
      BigDecimal sobra) {

    static Totais de(
        BigDecimal entradas, BigDecimal saidas, BigDecimal essenciais, BigDecimal aportes) {
      return new Totais(
          Dinheiro.normalizar(entradas),
          Dinheiro.normalizar(saidas),
          Dinheiro.normalizar(essenciais),
          Dinheiro.normalizar(saidas.subtract(essenciais)),
          Dinheiro.normalizar(aportes),
          Dinheiro.normalizar(entradas.subtract(saidas).subtract(aportes)));
    }

    static Totais zero() {
      return de(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }
  }

  /**
   * Uma linha da lista de lançamentos.
   *
   * @param id nulo em movimentação de reserva, que não tem id público
   */
  public record Lancamento(
      UUID id,
      LocalDate data,
      String descricao,
      String categoria,
      boolean essencial,
      TipoLancamento tipo,
      BigDecimal valor) {
  }

  /** Números da tela Hoje. */
  public record Hoje(
      LocalDate hoje,
      YearMonth mes,
      BigDecimal livre,
      BigDecimal porDia,
      int diasRestantes,
      Totais totais,
      BigDecimal previstasTotal,
      List<Lancamento> ultimosLancamentos,
      List<ContaPrevista> previstas) {
  }

  /** Sobra de um mês do histórico. */
  public record SobraMensal(YearMonth mes, BigDecimal sobra) {
  }

  /**
   * Ritmo de uma reserva.
   *
   * @param previsao mês em que a meta fecha no ritmo atual; nulo quando não há o que projetar
   * @param noRitmo  falso para reserva sem meta ou sem aporte na janela olhada
   */
  public record RitmoReserva(
      UUID id,
      String nome,
      BigDecimal saldo,
      BigDecimal meta,
      BigDecimal progresso,
      BigDecimal aporteMedio,
      YearMonth previsao,
      boolean noRitmo) {
  }

  /** Fechamento projetado do mês. */
  public record Projecao(BigDecimal jaSaiu, BigDecimal aindaVence, BigDecimal sobraPrevista) {
  }

  /** Números da tela Relatório. */
  public record Relatorio(
      YearMonth mes,
      Totais totais,
      List<SobraMensal> historico,
      List<TotalCategoria> topCategorias,
      List<RitmoReserva> reservas,
      Projecao projecao) {
  }
}
