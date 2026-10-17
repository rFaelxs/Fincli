package com.rfaelxs.web.service.comando;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rfaelxs.web.config.RelogioConfig;
import com.rfaelxs.web.domain.ContaPrevista;
import com.rfaelxs.web.domain.Reserva;
import com.rfaelxs.web.domain.TipoTransacao;
import com.rfaelxs.web.domain.Transacao;
import com.rfaelxs.web.domain.Usuario;
import com.rfaelxs.web.service.ComandoNaoEntendidoException;
import com.rfaelxs.web.service.ContaPrevistaService;
import com.rfaelxs.web.service.DadosTransacao;
import com.rfaelxs.web.service.ReservaService;
import com.rfaelxs.web.service.TransacaoService;
import com.rfaelxs.web.service.ValorInvalidoException;
import com.rfaelxs.web.service.comando.ComandoService.Resultado;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ComandoServiceTest {

  private static final Long DONO = 7L;

  // Dia 5 de setembro de 2026, no fuso da aplicação.
  private static final Clock RELOGIO =
      Clock.fixed(Instant.parse("2026-09-05T15:00:00Z"), RelogioConfig.FUSO);

  @Mock
  private TransacaoService transacaoService;

  @Mock
  private ReservaService reservaService;

  @Mock
  private ContaPrevistaService contaPrevistaService;

  private ComandoService service;
  private final Usuario usuario = new Usuario("Rafael", "52998224725", "hash");
  private final Reserva emergencia = Reserva.emergenciaDe(usuario);

  @BeforeEach
  void setUp() {
    service = new ComandoService(transacaoService, reservaService, contaPrevistaService, RELOGIO);
  }

  // CA-CMD-01
  @Test
  void saidaViraTransacaoComOQueOParserEntendeu() {
    Transacao criada = new Transacao(usuario, new BigDecimal("120.00"), "Alimentação", "Mercado",
        LocalDate.of(2026, 9, 4), TipoTransacao.SAIDA, true);
    when(transacaoService.criar(eq(DONO), any())).thenReturn(criada);

    Resultado r = service.executar(DONO, "mercado 120 ontem");

    verify(transacaoService).criar(DONO, new DadosTransacao(new BigDecimal("120.00"),
        "Alimentação", "Mercado", LocalDate.of(2026, 9, 4), TipoTransacao.SAIDA, true));
    assertThat(r.tipo()).isEqualTo(ComandoParser.Tipo.SAIDA);
    assertThat(r.id()).isEqualTo(criada.getPublicId());
    assertThat(r.reservaId()).isNull();
  }

  // CA-CMD-10
  @Test
  void textoSemNumeroResponde422() {
    assertThatThrownBy(() -> service.executar(DONO, "mercado sem numero"))
        .isInstanceOf(ComandoNaoEntendidoException.class)
        .hasMessageStartingWith("Não entendi \"mercado sem numero\"");
  }

  // CA-CMD-18
  @Test
  void ecoDoTextoTemNoMaximo40Caracteres() {
    String texto = "a".repeat(60);

    assertThatThrownBy(() -> service.executar(DONO, texto))
        .hasMessage("Não entendi \"%s…\" — tente algo como `mercado 120 ontem`."
            .formatted("a".repeat(40)));
  }

  // CA-CMD-14
  @Test
  void aporteSemNomeVaiParaAEmergencia() {
    Reserva viagem = reserva("Viagem Japão");
    when(reservaService.listar(DONO)).thenReturn(List.of(emergencia, viagem));

    Resultado r = service.executar(DONO, "guardar 300");

    verify(reservaService).alocar(DONO, emergencia.getPublicId(), new BigDecimal("300.00"));
    assertThat(r.id()).isNull();
    assertThat(r.reservaId()).isEqualTo(emergencia.getPublicId());
    assertThat(r.descricao()).isEqualTo("Reserva de Emergência");
  }

  // CA-CMD-15 e item 14
  @Test
  void aporteEscolheAReservaPorTrechoDoNomeSemAcento() {
    Reserva japao = reserva("Viagem Japão");
    when(reservaService.listar(DONO)).thenReturn(List.of(emergencia, japao));

    service.executar(DONO, "guardar 300 japao");

    verify(reservaService).alocar(DONO, japao.getPublicId(), new BigDecimal("300.00"));
  }

  // CA-CMD-16 e item 34
  @Test
  void nomeAmbiguoListaAsReservas() {
    when(reservaService.listar(DONO)).thenReturn(List.of(emergencia,
        reserva("Viagem Chile"), reserva("Viagem Japão")));

    assertThatThrownBy(() -> service.executar(DONO, "guardar 300 viagem"))
        .isInstanceOf(ComandoNaoEntendidoException.class)
        .hasMessage("\"Viagem\" casa com mais de uma reserva: Viagem Chile, Viagem Japão. "
            + "Escreva o nome inteiro.");
    verify(reservaService, never()).alocar(any(), any(), any());
  }

  // Item 33
  @Test
  void nomeQueNaoCasaResponde422() {
    when(reservaService.listar(DONO)).thenReturn(List.of(emergencia));

    assertThatThrownBy(() -> service.executar(DONO, "guardar 300 viagem"))
        .isInstanceOf(ComandoNaoEntendidoException.class)
        .hasMessage("Não achei a reserva \"Viagem\". Crie a reserva antes de guardar nela.");
  }

  // Item 32
  @Test
  void aporteSemNenhumaReservaResponde422() {
    when(reservaService.listar(DONO)).thenReturn(List.of());

    assertThatThrownBy(() -> service.executar(DONO, "guardar 300"))
        .isInstanceOf(ComandoNaoEntendidoException.class)
        .hasMessage("Você ainda não tem reservas para guardar dinheiro.");
  }

  @Test
  void nomeSoDePontuacaoVaiParaAEmergencia() {
    when(reservaService.listar(DONO)).thenReturn(List.of(emergencia, reserva("Viagem")));

    service.executar(DONO, "guardar 300 !!!");

    verify(reservaService).alocar(DONO, emergencia.getPublicId(), new BigDecimal("300.00"));
  }

  // CA-CMD-17: a regra de saldo é a do módulo de reservas
  @Test
  void saldoInsuficienteSobeComo400() {
    when(reservaService.listar(DONO)).thenReturn(List.of(emergencia));
    when(reservaService.alocar(DONO, emergencia.getPublicId(), new BigDecimal("300.00")))
        .thenThrow(new ValorInvalidoException("Saldo insuficiente para esta alocação."));

    assertThatThrownBy(() -> service.executar(DONO, "guardar 300"))
        .isInstanceOf(ValorInvalidoException.class)
        .hasMessage("Saldo insuficiente para esta alocação.");
  }

  // CA-CMD-11
  @Test
  void contaRecorrenteVaiSemMes() {
    ContaPrevista conta = new ContaPrevista(usuario, "Energia", new BigDecimal("210.00"), 22, null);
    when(contaPrevistaService.criar(DONO, "Energia", new BigDecimal("210.00"), 22, null))
        .thenReturn(conta);

    Resultado r = service.executar(DONO, "prever energia 210 dia 22");

    assertThat(r.id()).isEqualTo(conta.getPublicId());
    assertThat(r.categoria()).isEqualTo("Conta");
  }

  // CA-CMD-12 e CA-CMD-13
  @Test
  void contaAvulsaLevaOMesDoComandoESemDiaVenceHoje() {
    YearMonth setembro = YearMonth.of(2026, 9);
    when(contaPrevistaService.criar(DONO, "Internet", new BigDecimal("99.00"), 5, setembro))
        .thenReturn(new ContaPrevista(usuario, "Internet", new BigDecimal("99.00"), 5, setembro));

    service.executar(DONO, "prever internet 99 uma vez");

    verify(contaPrevistaService).criar(DONO, "Internet", new BigDecimal("99.00"), 5, setembro);
  }

  private Reserva reserva(String nome) {
    return new Reserva(usuario, nome, BigDecimal.ZERO, false);
  }
}
