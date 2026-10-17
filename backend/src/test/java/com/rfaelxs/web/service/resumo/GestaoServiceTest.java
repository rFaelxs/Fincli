package com.rfaelxs.web.service.resumo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.rfaelxs.web.config.RelogioConfig;
import com.rfaelxs.web.domain.ContaPrevista;
import com.rfaelxs.web.domain.MovimentacaoReserva;
import com.rfaelxs.web.domain.Reserva;
import com.rfaelxs.web.domain.TipoMovimentacaoReserva;
import com.rfaelxs.web.domain.TipoTransacao;
import com.rfaelxs.web.domain.Transacao;
import com.rfaelxs.web.domain.Usuario;
import com.rfaelxs.web.repository.MovimentacaoReservaRepository;
import com.rfaelxs.web.repository.TransacaoRepository;
import com.rfaelxs.web.service.ContaPrevistaService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GestaoServiceTest {

  private static final Long DONO = 7L;
  private static final YearMonth SETEMBRO = YearMonth.of(2026, 9);

  @Mock
  private CalculadoraDeTotais calculadora;

  @Mock
  private ContaPrevistaService contaPrevistaService;

  @Mock
  private TransacaoRepository transacaoRepository;

  @Mock
  private MovimentacaoReservaRepository movimentacaoRepository;

  private final Usuario usuario = new Usuario("Rafael", "52998224725", "hash");
  private final Reserva viagem = new Reserva(usuario, "Viagem", BigDecimal.TEN, false);

  // RN-05 a RN-07: livre = sobra − previstas; porDia = livre / dias restantes
  @Test
  void livreDescontaAsContasAVencerEDivideOsDiasRestantes() {
    LocalDate hoje = LocalDate.of(2026, 9, 18);
    totais("5000.00", "2500.00", "300.00");
    previstas(hoje, conta("Energia", "210.00"), conta("Internet", "190.00"));

    Gestao g = servico(hoje).hoje(DONO);

    assertThat(g.mes()).isEqualTo(SETEMBRO);
    assertThat(g.totais().sobra()).isEqualByComparingTo("2200.00");
    assertThat(g.previstasTotal()).isEqualByComparingTo("400.00");
    assertThat(g.livre()).isEqualByComparingTo("1800.00");
    assertThat(g.diasRestantes()).isEqualTo(12);
    assertThat(g.porDia()).isEqualByComparingTo("150.00");
    assertThat(g.previstas()).extracting(ContaPrevista::getNome)
        .containsExactly("Energia", "Internet");
  }

  // CA-GESTAO-21
  @Test
  void noUltimoDiaDoMesPorDiaIgualaLivre() {
    LocalDate hoje = LocalDate.of(2026, 9, 30);
    totais("1000.00", "0", "0");
    previstas(hoje);

    Gestao g = servico(hoje).hoje(DONO);

    assertThat(g.diasRestantes()).isZero();
    assertThat(g.porDia()).isEqualByComparingTo(g.livre());
  }

  // CA-GESTAO-04
  @Test
  void livreNegativoTambemDivide() {
    LocalDate hoje = LocalDate.of(2026, 9, 28);
    totais("0", "300.00", "0");
    previstas(hoje);

    Gestao g = servico(hoje).hoje(DONO);

    assertThat(g.livre()).isEqualByComparingTo("-300.00");
    assertThat(g.porDia()).isEqualByComparingTo("-150.00");
  }

  // CA-GESTAO-12: com 9 lançamentos no mês, vêm 6, na ordem declarada
  @Test
  void ultimosLancamentosVemOrdenadosELimitadosA6() {
    LocalDate hoje = LocalDate.of(2026, 9, 18);
    totais("0", "0", "0");
    previstas(hoje);
    List<Transacao> transacoes = new ArrayList<>();
    for (long id = 6; id >= 1; id--) {
      transacoes.add(transacao(id, LocalDate.of(2026, 9, (int) id + 10)));
    }
    when(transacaoRepository.findTop6ByUsuarioIdAndDataBetweenOrderByDataDescIdDesc(
        DONO, SETEMBRO.atDay(1), SETEMBRO.atEndOfMonth())).thenReturn(transacoes);
    when(movimentacaoRepository.findTop6ByUsuarioIdAndDataBetweenOrderByDataDescIdDesc(
        DONO, SETEMBRO.atDay(1), SETEMBRO.atEndOfMonth())).thenReturn(List.of(
            movimentacao(1L, LocalDate.of(2026, 9, 16)),
            movimentacao(2L, LocalDate.of(2026, 9, 2)),
            movimentacao(3L, LocalDate.of(2026, 9, 1))));

    List<Lancamento> ultimos = servico(hoje).hoje(DONO).ultimosLancamentos();

    // Dia 16 tem uma transação e uma movimentação: a transação vem antes.
    assertThat(ultimos).hasSize(6).extracting(Lancamento::data).containsExactly(
        LocalDate.of(2026, 9, 16), LocalDate.of(2026, 9, 16), LocalDate.of(2026, 9, 15),
        LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 13), LocalDate.of(2026, 9, 12));
    assertThat(ultimos.get(0).tipo()).isEqualTo(Lancamento.Tipo.SAIDA);
    assertThat(ultimos.get(1).tipo()).isEqualTo(Lancamento.Tipo.APORTE);
  }

  // CA-GESTAO-10
  @Test
  void movimentacaoViraAporteSemIdComONomeDaReserva() {
    Lancamento l = Lancamento.de(movimentacao(1L, LocalDate.of(2026, 9, 16)));

    assertThat(l.id()).isNull();
    assertThat(l.tipo()).isEqualTo(Lancamento.Tipo.APORTE);
    assertThat(l.descricao()).isEqualTo("Viagem");
    assertThat(l.categoria()).isEqualTo("Reserva");
  }

  @Test
  void transacaoSemDescricaoMostraACategoria() {
    Transacao t = new Transacao(usuario, BigDecimal.TEN, "Alimentação", null,
        LocalDate.of(2026, 9, 16), TipoTransacao.SAIDA, true);

    Lancamento l = Lancamento.de(t);

    assertThat(l.descricao()).isEqualTo("Alimentação");
    assertThat(l.essencial()).isTrue();
    assertThat(l.id()).isEqualTo(t.getPublicId());
  }

  private GestaoService servico(LocalDate hoje) {
    Clock relogio = Clock.fixed(hoje.atTime(12, 0).atZone(RelogioConfig.FUSO).toInstant(),
        RelogioConfig.FUSO);
    return new GestaoService(calculadora, contaPrevistaService, transacaoRepository,
        movimentacaoRepository, relogio);
  }

  private void totais(String entradas, String saidas, String aportes) {
    when(calculadora.doMes(eq(DONO), any())).thenReturn(Totais.de(new BigDecimal(entradas),
        new BigDecimal(saidas), BigDecimal.ZERO, new BigDecimal(aportes)));
  }

  private void previstas(LocalDate hoje, ContaPrevista... contas) {
    when(contaPrevistaService.aindaVencem(DONO, YearMonth.from(hoje), hoje))
        .thenReturn(List.of(contas));
  }

  private ContaPrevista conta(String nome, String valor) {
    return new ContaPrevista(usuario, nome, new BigDecimal(valor), 22, null);
  }

  private Transacao transacao(Long id, LocalDate data) {
    Transacao t = new Transacao(usuario, BigDecimal.TEN, "Mercado", "Compra", data,
        TipoTransacao.SAIDA, false);
    ReflectionTestUtils.setField(t, "id", id);
    return t;
  }

  private MovimentacaoReserva movimentacao(Long id, LocalDate data) {
    MovimentacaoReserva m = new MovimentacaoReserva(usuario, viagem, BigDecimal.TEN,
        TipoMovimentacaoReserva.ALOCACAO, data);
    ReflectionTestUtils.setField(m, "id", id);
    return m;
  }
}
