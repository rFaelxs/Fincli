package com.rfaelxs.web.service.resumo;

import com.rfaelxs.web.domain.ContaPrevista;
import com.rfaelxs.web.domain.MovimentacaoReserva;
import com.rfaelxs.web.domain.TipoMovimentacaoReserva;
import com.rfaelxs.web.domain.TipoTransacao;
import com.rfaelxs.web.domain.Transacao;
import com.rfaelxs.web.domain.Usuario;
import com.rfaelxs.web.repository.MovimentacaoReservaRepository;
import com.rfaelxs.web.repository.TransacaoRepository;
import com.rfaelxs.web.service.ContaPrevistaService;
import com.rfaelxs.web.service.Dinheiro;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Números da tela Gestão, sempre do mês corrente.
 *
 * <p>Sobre os totais da {@link CalculadoraDeTotais}:
 *
 * <pre>
 *   previstas  = soma das contas ainda por vencer no mês
 *   livre      = sobra - previstas          (o número grande da tela Gestão)
 *   porDia     = livre / dias que faltam para o fim do mês
 * </pre>
 */
@Service
public class GestaoService {

  /** Quantos lançamentos a tela Gestão mostra. */
  private static final int ULTIMOS_LANCAMENTOS = 6;

  private final CalculadoraDeTotais calculadora;
  private final TransacaoRepository transacaoRepository;
  private final MovimentacaoReservaRepository movimentacaoRepository;
  private final ContaPrevistaService contaPrevistaService;
  private final Clock relogio;

  public GestaoService(
      CalculadoraDeTotais calculadora,
      TransacaoRepository transacaoRepository,
      MovimentacaoReservaRepository movimentacaoRepository,
      ContaPrevistaService contaPrevistaService,
      Clock relogio) {
    this.calculadora = calculadora;
    this.transacaoRepository = transacaoRepository;
    this.movimentacaoRepository = movimentacaoRepository;
    this.contaPrevistaService = contaPrevistaService;
    this.relogio = relogio;
  }

  /**
   * Monta a tela Gestão, sempre do mês corrente.
   *
   * @param usuario dono dos dados
   * @return o número livre, o ritmo diário, os totais do mês e as listas de apoio
   */
  @Transactional(readOnly = true)
  public Gestao montar(Usuario usuario) {
    LocalDate hoje = LocalDate.now(relogio);
    YearMonth mes = YearMonth.from(hoje);

    Totais totais = calculadora.doMes(usuario, mes);
    List<ContaPrevista> previstas = contaPrevistaService.aVencer(usuario, mes, hoje);
    BigDecimal previstasTotal = ContaPrevistaService.somar(previstas);
    BigDecimal livre = Dinheiro.normalizar(totais.sobra().subtract(previstasTotal));

    // O README define os dias restantes como último dia menos hoje. No último dia do mês isso
    // dá zero, e dividir por zero derrubaria a tela: o divisor tem piso 1 e o rótulo mostra o
    // número real de dias.
    int diasRestantes = mes.lengthOfMonth() - hoje.getDayOfMonth();
    BigDecimal porDia =
        livre.divide(BigDecimal.valueOf(Math.max(diasRestantes, 1)), 2, RoundingMode.HALF_UP);

    return new Gestao(
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

  /** @return transações e movimentações do mês na mesma lista, as mais recentes primeiro */
  private List<Lancamento> ultimosLancamentos(Usuario usuario, YearMonth mes) {
    LocalDate inicio = mes.atDay(1);
    LocalDate fim = mes.atEndOfMonth();

    Stream<Lancamento> transacoes =
        transacaoRepository
            .findByUsuarioIdAndDataTransacaoBetweenOrderByDataTransacaoDescIdDesc(
                usuario.getId(), inicio, fim)
            .stream()
            .map(GestaoService::lancamento);

    Stream<Lancamento> movimentos =
        movimentacaoRepository
            .findByUsuarioIdAndDataMovimentoBetweenOrderByDataMovimentoDescIdDesc(
                usuario.getId(), inicio, fim)
            .stream()
            .map(GestaoService::lancamento);

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
}
