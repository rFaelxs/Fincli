package com.rfaelxs.web.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.rfaelxs.web.domain.ContaPrevista;
import com.rfaelxs.web.domain.Reserva;
import com.rfaelxs.web.domain.TipoMovimentacaoReserva;
import com.rfaelxs.web.domain.TipoTransacao;
import com.rfaelxs.web.domain.Usuario;
import com.rfaelxs.web.repository.MovimentacaoReservaRepository;
import com.rfaelxs.web.repository.TotalCategoria;
import com.rfaelxs.web.repository.TotalMensal;
import com.rfaelxs.web.repository.TotalMovimentacaoMensal;
import com.rfaelxs.web.repository.TotalPorReserva;
import com.rfaelxs.web.repository.TransacaoRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Fórmulas das telas Hoje e Relatório.
 *
 * <p>O relógio é fixo em 16/09/2026 — o mesmo dia do protótipo de design — para que os dias
 * restantes e o valor por dia sejam verificáveis.
 */
@ExtendWith(MockitoExtension.class)
class ResumoServiceTest {

  private static final LocalDate HOJE = LocalDate.of(2026, 9, 16);
  private static final YearMonth SETEMBRO = YearMonth.of(2026, 9);

  @Mock private TransacaoRepository transacaoRepository;
  @Mock private MovimentacaoReservaRepository movimentacaoRepository;
  @Mock private ReservaService reservaService;
  @Mock private ContaPrevistaService contaPrevistaService;

  private ResumoService service;
  private Usuario usuario;

  @BeforeEach
  void setUp() {
    Clock relogio = Clock.fixed(HOJE.atStartOfDay(ZoneId.systemDefault()).toInstant(),
        ZoneId.systemDefault());
    service = new ResumoService(
        transacaoRepository, movimentacaoRepository, reservaService, contaPrevistaService,
        relogio);
    usuario = new Usuario("Teste", "11144477735", "hash");

    lenient().when(transacaoRepository
        .findByUsuarioIdAndDataTransacaoBetweenOrderByDataTransacaoDescIdDesc(any(), any(), any()))
        .thenReturn(List.of());
    lenient().when(movimentacaoRepository
        .findByUsuarioIdAndDataMovimentoBetweenOrderByDataMovimentoDescIdDesc(any(), any(), any()))
        .thenReturn(List.of());
  }

  @Test
  void livreDescontaSaidasAportesEContasAVencer() {
    transacoes(
        entrada(SETEMBRO, "7480.00"),
        saida(SETEMBRO, "2000.00", true),
        saida(SETEMBRO, "813.40", false));
    movimentacoes(alocacao(SETEMBRO, "1100.00"));
    previstas(conta("Cartão", "980.00", 20), conta("Energia", "210.00", 22));

    ResumoService.Hoje hoje = service.hoje(usuario);

    // sobra = 7480 − 2813,40 − 1100 = 3566,60 ; livre = 3566,60 − 1190 = 2376,60
    assertEquals(new BigDecimal("3566.60"), hoje.totais().sobra());
    assertEquals(new BigDecimal("1190.00"), hoje.previstasTotal());
    assertEquals(new BigDecimal("2376.60"), hoje.livre());
  }

  @Test
  void superfluasSaoSaidasMenosEssenciais() {
    transacoes(
        entrada(SETEMBRO, "7480.00"),
        saida(SETEMBRO, "2000.00", true),
        saida(SETEMBRO, "813.40", false));
    movimentacoes();
    previstas();

    ResumoService.Totais totais = service.hoje(usuario).totais();

    assertEquals(new BigDecimal("2813.40"), totais.saidas());
    assertEquals(new BigDecimal("2000.00"), totais.essenciais());
    assertEquals(new BigDecimal("813.40"), totais.superfluas());
  }

  @Test
  void aporteDoMesEhAlocacaoMenosSaque() {
    transacoes(entrada(SETEMBRO, "1000.00"));
    movimentacoes(alocacao(SETEMBRO, "800.00"), saque(SETEMBRO, "300.00"));
    previstas();

    ResumoService.Totais totais = service.hoje(usuario).totais();

    assertEquals(new BigDecimal("500.00"), totais.aportes());
    assertEquals(new BigDecimal("500.00"), totais.sobra());
  }

  @Test
  void porDiaDivideOLivrePelosDiasQueFaltam() {
    transacoes(entrada(SETEMBRO, "1400.00"));
    movimentacoes();
    previstas();

    ResumoService.Hoje hoje = service.hoje(usuario);

    // 30 − 16 = 14 dias; 1400 / 14 = 100,00
    assertEquals(14, hoje.diasRestantes());
    assertEquals(new BigDecimal("100.00"), hoje.porDia());
  }

  @Test
  void livrePodeSerNegativo() {
    transacoes(entrada(SETEMBRO, "1000.00"), saida(SETEMBRO, "1500.00", true));
    movimentacoes();
    previstas(conta("Cartão", "200.00", 20));

    assertEquals(new BigDecimal("-700.00"), service.hoje(usuario).livre());
  }

  @Test
  void mesSemMovimentoDaZeroEmVezDeQuebrar() {
    transacoes();
    movimentacoes();
    previstas();

    ResumoService.Hoje hoje = service.hoje(usuario);

    assertEquals(new BigDecimal("0.00"), hoje.livre());
    assertEquals(new BigDecimal("0.00"), hoje.totais().entradas());
    assertTrue(hoje.ultimosLancamentos().isEmpty());
  }

  @Test
  void historicoTrazSeisMesesMesmoSemMovimento() {
    transacoes(entrada(SETEMBRO, "1000.00"), entrada(YearMonth.of(2026, 7), "900.00"));
    movimentacoes();
    previstas();
    when(transacaoRepository.somarSaidasPorCategoria(any(), any(), any())).thenReturn(List.of());
    when(reservaService.listar(usuario)).thenReturn(List.of());
    when(movimentacaoRepository.somarPorReserva(any(), any(), any())).thenReturn(List.of());

    List<ResumoService.SobraMensal> historico = service.relatorio(usuario, SETEMBRO).historico();

    assertEquals(6, historico.size());
    assertEquals(YearMonth.of(2026, 4), historico.get(0).mes());
    assertEquals(SETEMBRO, historico.get(5).mes());
    assertEquals(new BigDecimal("0.00"), historico.get(1).sobra());
    assertEquals(new BigDecimal("900.00"), historico.get(3).sobra());
    assertEquals(new BigDecimal("1000.00"), historico.get(5).sobra());
  }

  @Test
  void topCategoriasFicaNasQuatroMaiores() {
    transacoes();
    movimentacoes();
    previstas();
    when(transacaoRepository.somarSaidasPorCategoria(any(), any(), any())).thenReturn(List.of(
        new TotalCategoria("Moradia", new BigDecimal("1269.90")),
        new TotalCategoria("Alimentação", new BigDecimal("1010.58")),
        new TotalCategoria("Lazer", new BigDecimal("545.00")),
        new TotalCategoria("Saúde", new BigDecimal("301.42")),
        new TotalCategoria("Transporte", new BigDecimal("120.00"))));
    when(reservaService.listar(usuario)).thenReturn(List.of());
    when(movimentacaoRepository.somarPorReserva(any(), any(), any())).thenReturn(List.of());

    List<TotalCategoria> top = service.relatorio(usuario, SETEMBRO).topCategorias();

    assertEquals(4, top.size());
    assertEquals("Moradia", top.get(0).categoria());
    assertEquals("Saúde", top.get(3).categoria());
  }

  @Test
  void previsaoDaReservaUsaOAporteMedioDosUltimosTresMeses() {
    transacoes();
    movimentacoes();
    previstas();
    when(transacaoRepository.somarSaidasPorCategoria(any(), any(), any())).thenReturn(List.of());
    Reserva viagem = reserva("Viagem Chile", "6000.00", "3000.00");
    when(reservaService.listar(usuario)).thenReturn(List.of(viagem));
    // 900 alocados na janela de 3 meses → média 300/mês; faltam 3000 → 10 meses.
    when(movimentacaoRepository.somarPorReserva(any(), any(), any())).thenReturn(List.of(
        new TotalPorReserva(null, TipoMovimentacaoReserva.ALOCACAO, new BigDecimal("900.00"))));

    ResumoService.RitmoReserva ritmo = service.relatorio(usuario, SETEMBRO).reservas().get(0);

    assertEquals(new BigDecimal("300.00"), ritmo.aporteMedio());
    assertEquals(YearMonth.of(2027, 7), ritmo.previsao());
    assertTrue(ritmo.noRitmo());
  }

  @Test
  void reservaParadaNaoRecebePrevisao() {
    transacoes();
    movimentacoes();
    previstas();
    when(transacaoRepository.somarSaidasPorCategoria(any(), any(), any())).thenReturn(List.of());
    when(reservaService.listar(usuario)).thenReturn(List.of(reserva("Curso", "2000.00", "640.00")));
    when(movimentacaoRepository.somarPorReserva(any(), any(), any())).thenReturn(List.of());

    ResumoService.RitmoReserva ritmo = service.relatorio(usuario, SETEMBRO).reservas().get(0);

    assertNull(ritmo.previsao());
    assertFalse(ritmo.noRitmo());
    assertEquals(new BigDecimal("32.0"), ritmo.progresso());
  }

  @Test
  void projecaoSeparaOQueJaSaiuDoQueAindaVence() {
    transacoes(entrada(SETEMBRO, "7480.00"), saida(SETEMBRO, "2813.40", true));
    movimentacoes();
    previstas(conta("Cartão", "980.00", 20));
    when(transacaoRepository.somarSaidasPorCategoria(any(), any(), any())).thenReturn(List.of());
    when(reservaService.listar(usuario)).thenReturn(List.of());
    when(movimentacaoRepository.somarPorReserva(any(), any(), any())).thenReturn(List.of());

    ResumoService.Projecao p = service.relatorio(usuario, SETEMBRO).projecao();

    assertEquals(new BigDecimal("2813.40"), p.jaSaiu());
    assertEquals(new BigDecimal("980.00"), p.aindaVence());
    assertEquals(new BigDecimal("3686.60"), p.sobraPrevista());
  }

  // ── apoio ──

  private void transacoes(TotalMensal... totais) {
    when(transacaoRepository.somarPorMes(any(), any(), any())).thenReturn(List.of(totais));
  }

  private void movimentacoes(TotalMovimentacaoMensal... totais) {
    when(movimentacaoRepository.somarPorMes(any(), any(), any())).thenReturn(List.of(totais));
  }

  private void previstas(ContaPrevista... contas) {
    when(contaPrevistaService.aVencer(any(), any(), any())).thenReturn(List.of(contas));
    lenient().when(contaPrevistaService.doMes(any(), any())).thenReturn(List.of(contas));
  }

  private static TotalMensal entrada(YearMonth mes, String valor) {
    return new TotalMensal(
        mes.getYear(), mes.getMonthValue(), TipoTransacao.ENTRADA, false, new BigDecimal(valor));
  }

  private static TotalMensal saida(YearMonth mes, String valor, boolean essencial) {
    return new TotalMensal(
        mes.getYear(), mes.getMonthValue(), TipoTransacao.SAIDA, essencial,
        new BigDecimal(valor));
  }

  private static TotalMovimentacaoMensal alocacao(YearMonth mes, String valor) {
    return new TotalMovimentacaoMensal(
        mes.getYear(), mes.getMonthValue(), TipoMovimentacaoReserva.ALOCACAO,
        new BigDecimal(valor));
  }

  private static TotalMovimentacaoMensal saque(YearMonth mes, String valor) {
    return new TotalMovimentacaoMensal(
        mes.getYear(), mes.getMonthValue(), TipoMovimentacaoReserva.SAQUE, new BigDecimal(valor));
  }

  private ContaPrevista conta(String nome, String valor, int dia) {
    return new ContaPrevista(usuario, nome, new BigDecimal(valor), dia, null);
  }

  private Reserva reserva(String nome, String meta, String saldo) {
    Reserva r = new Reserva(usuario, nome, new BigDecimal(meta), false);
    r.setSaldoAtual(new BigDecimal(saldo));
    return r;
  }
}
