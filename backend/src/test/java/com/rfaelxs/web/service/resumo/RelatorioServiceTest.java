package com.rfaelxs.web.service.resumo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rfaelxs.web.config.RelogioConfig;
import com.rfaelxs.web.domain.Reserva;
import com.rfaelxs.web.domain.TipoMovimentacaoReserva;
import com.rfaelxs.web.domain.Usuario;
import com.rfaelxs.web.repository.MovimentacaoReservaRepository;
import com.rfaelxs.web.repository.ReservaRepository;
import com.rfaelxs.web.repository.TotalCategoria;
import com.rfaelxs.web.repository.TotalPorReserva;
import com.rfaelxs.web.repository.TransacaoRepository;
import com.rfaelxs.web.service.ContaPrevistaService;
import com.rfaelxs.web.service.resumo.Relatorio.RitmoReserva;
import com.rfaelxs.web.service.resumo.Relatorio.SobraMensal;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RelatorioServiceTest {

  private static final Long DONO = 7L;
  private static final YearMonth SETEMBRO = YearMonth.of(2026, 9);

  // Hoje é 18/09/2026.
  private static final Clock RELOGIO =
      Clock.fixed(Instant.parse("2026-09-18T15:00:00Z"), RelogioConfig.FUSO);

  @Mock
  private CalculadoraDeTotais calculadora;

  @Mock
  private ContaPrevistaService contaPrevistaService;

  @Mock
  private TransacaoRepository transacaoRepository;

  @Mock
  private MovimentacaoReservaRepository movimentacaoRepository;

  @Mock
  private ReservaRepository reservaRepository;

  private RelatorioService service;
  private final Usuario usuario = new Usuario("Rafael", "52998224725", "hash");

  @BeforeEach
  void setUp() {
    service = new RelatorioService(calculadora, contaPrevistaService, transacaoRepository,
        movimentacaoRepository, reservaRepository, RELOGIO);
    when(calculadora.porMes(eq(DONO), any(), any())).thenReturn(Map.of());
    when(contaPrevistaService.aindaVencem(eq(DONO), any(), any())).thenReturn(List.of());
  }

  // RN-REL-01 e CA-REL-13
  @Test
  void historicoTemSeisMesesDoMaisAntigoAoMaisRecenteComOsVaziosZerados() {
    when(calculadora.porMes(DONO, YearMonth.of(2026, 4), SETEMBRO)).thenReturn(Map.of(
        YearMonth.of(2026, 6), totais("900.00", "0")));

    Relatorio r = service.doMes(DONO, SETEMBRO);

    assertThat(r.historico()).extracting(SobraMensal::mes).containsExactly(
        YearMonth.of(2026, 4), YearMonth.of(2026, 5), YearMonth.of(2026, 6),
        YearMonth.of(2026, 7), YearMonth.of(2026, 8), SETEMBRO);
    assertThat(r.historico()).extracting(SobraMensal::sobra).map(BigDecimal::toPlainString)
        .containsExactly("0.00", "0.00", "900.00", "0.00", "0.00", "0.00");
    assertThat(r.totais()).isEqualTo(Totais.zero());
  }

  // CA-REL-18
  @Test
  void topCategoriasTrazAsQuatroMaiores() {
    when(transacaoRepository.somarSaidasPorCategoria(DONO, SETEMBRO.atDay(1),
        SETEMBRO.atEndOfMonth())).thenReturn(List.of(categoria("Moradia", "1500"),
            categoria("Mercado", "800"), categoria("Lazer", "300"), categoria("Saúde", "200"),
            categoria("Transporte", "100")));

    assertThat(service.doMes(DONO, SETEMBRO).topCategorias())
        .extracting(TotalCategoria::categoria)
        .containsExactly("Moradia", "Mercado", "Lazer", "Saúde");
  }

  // RN-REL-03 e CA-REL-25: no mês corrente, vence o que é de hoje em diante
  @Test
  void projecaoDoMesCorrenteDescontaOQueAindaVence() {
    when(calculadora.porMes(eq(DONO), any(), any())).thenReturn(Map.of(
        SETEMBRO, totais("3210.00", "2500.00")));
    when(contaPrevistaService.aindaVencem(DONO, SETEMBRO, LocalDate.of(2026, 9, 18)))
        .thenReturn(List.of(new com.rfaelxs.web.domain.ContaPrevista(usuario, "Energia",
            new BigDecimal("210.00"), 22, null)));

    Relatorio.Projecao p = service.doMes(DONO, SETEMBRO).projecao();

    assertThat(p.jaSaiu()).isEqualByComparingTo("2500.00");
    assertThat(p.aindaVence()).isEqualByComparingTo("210.00");
    assertThat(p.sobraPrevista()).isEqualByComparingTo("500.00");
  }

  // CA-REL-27: em mês fechado nada mais vence
  @Test
  void mesFechadoUsaCorteDepoisDoFimDoMes() {
    YearMonth julho = YearMonth.of(2026, 7);

    service.doMes(DONO, julho);

    verify(contaPrevistaService).aindaVencem(DONO, julho, LocalDate.of(2026, 8, 1));
  }

  @Test
  void mesFuturoContaTodasAsContasDoMes() {
    YearMonth dezembro = YearMonth.of(2026, 12);

    service.doMes(DONO, dezembro);

    verify(contaPrevistaService).aindaVencem(DONO, dezembro, LocalDate.of(2026, 12, 1));
  }

  @Test
  void semMesUsaOCorrenteDoRelogio() {
    assertThat(service.doMesCorrente(DONO).mes()).isEqualTo(SETEMBRO);
  }

  // RN-REL-02: alocações menos saques da janela, divididos por 3
  @Test
  void aporteMedioUsaAJanelaDeTresMeses() {
    Reserva viagem = reserva("Viagem", "10000.00", "4000.00", 3L);
    when(reservaRepository.findByUsuarioIdOrderByEmergenciaDescNomeAsc(DONO))
        .thenReturn(List.of(viagem));
    when(movimentacaoRepository.somarPorReserva(DONO, LocalDate.of(2026, 7, 1),
        LocalDate.of(2026, 9, 30))).thenReturn(List.of(
            new TotalPorReserva(3L, TipoMovimentacaoReserva.ALOCACAO, new BigDecimal("1800")),
            new TotalPorReserva(3L, TipoMovimentacaoReserva.SAQUE, new BigDecimal("300"))));

    RitmoReserva ritmo = service.doMes(DONO, SETEMBRO).reservas().get(0);

    assertThat(ritmo.aporteMedio()).isEqualByComparingTo("500.00");
    assertThat(ritmo.previsao()).isEqualTo(YearMonth.of(2027, 9));
  }

  // CA-REL-21
  @Test
  void previsaoArredondaParaCimaOsMesesQueFaltam() {
    RitmoReserva r = RelatorioService.ritmo(
        reserva("Viagem", "10000.00", "4000.00", 1L), new BigDecimal("500.00"), SETEMBRO);

    assertThat(r.previsao()).isEqualTo(YearMonth.of(2027, 9));
    assertThat(r.noRitmo()).isTrue();
  }

  // 1000 / 300 = 3,33 meses: o quarto mês ainda é preciso para fechar a meta.
  @Test
  void mesesQuebradosArredondamParaCima() {
    RitmoReserva r = RelatorioService.ritmo(
        reserva("Carro", "1000.00", "0", 1L), new BigDecimal("300.00"), SETEMBRO);

    assertThat(r.previsao()).isEqualTo(YearMonth.of(2027, 1));
  }

  // CA-REL-22
  @Test
  void reservaParadaFicaSemRitmo() {
    RitmoReserva r = RelatorioService.ritmo(
        reserva("Viagem", "5000.00", "1000.00", 1L), new BigDecimal("0.00"), SETEMBRO);

    assertThat(r.previsao()).isNull();
    assertThat(r.noRitmo()).isFalse();
  }

  // CA-REL-23
  @Test
  void reservaSemMetaFicaSemPrevisao() {
    RitmoReserva r = RelatorioService.ritmo(
        reserva("Emergência", "0", "500.00", 1L), new BigDecimal("100.00"), SETEMBRO);

    assertThat(r.previsao()).isNull();
    assertThat(r.noRitmo()).isFalse();
    assertThat(r.progresso()).isEqualByComparingTo("0.0");
  }

  // CA-REL-24
  @Test
  void metaBatidaFechaNoProprioMes() {
    RitmoReserva r = RelatorioService.ritmo(
        reserva("Viagem", "5000.00", "5000.00", 1L), new BigDecimal("0.00"), SETEMBRO);

    assertThat(r.previsao()).isEqualTo(SETEMBRO);
    assertThat(r.noRitmo()).isTrue();
  }

  private static Totais totais(String entradas, String saidas) {
    return Totais.de(new BigDecimal(entradas), new BigDecimal(saidas), BigDecimal.ZERO,
        BigDecimal.ZERO);
  }

  private static TotalCategoria categoria(String nome, String total) {
    return new TotalCategoria(nome, new BigDecimal(total));
  }

  private Reserva reserva(String nome, String meta, String saldo, Long id) {
    Reserva r = new Reserva(usuario, nome, new BigDecimal(meta), false);
    r.alocar(new BigDecimal(saldo));
    ReflectionTestUtils.setField(r, "id", id);
    return r;
  }
}
