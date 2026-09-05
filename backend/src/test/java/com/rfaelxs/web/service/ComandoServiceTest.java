package com.rfaelxs.web.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rfaelxs.web.domain.ContaPrevista;
import com.rfaelxs.web.domain.Reserva;
import com.rfaelxs.web.domain.TipoTransacao;
import com.rfaelxs.web.domain.Transacao;
import com.rfaelxs.web.domain.Usuario;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Execução do comando: o que cada tipo grava e o que acontece quando o texto não fecha.
 *
 * <p>O parse em si tem testes próprios em {@link ComandoParserTest}; aqui interessa o efeito.
 */
@ExtendWith(MockitoExtension.class)
class ComandoServiceTest {

  private static final LocalDate HOJE = LocalDate.of(2026, 9, 16);

  @Mock private TransacaoService transacaoService;
  @Mock private ReservaService reservaService;
  @Mock private ContaPrevistaService contaPrevistaService;

  private ComandoService service;
  private Usuario usuario;

  @BeforeEach
  void setUp() {
    Clock relogio = Clock.fixed(
        HOJE.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
    service = new ComandoService(
        transacaoService, reservaService, contaPrevistaService, relogio);
    usuario = new Usuario("Rafael", "11144477735", "hash");
  }

  @Test
  void saidaGravaTransacaoComOQueOParserDeduziu() {
    when(transacaoService.adicionar(any(), any(), any(), any(), any(), any(), anyBoolean()))
        .thenAnswer(inv -> new Transacao(
            usuario, inv.getArgument(1), inv.getArgument(2), inv.getArgument(3),
            inv.getArgument(4), inv.getArgument(5), inv.getArgument(6)));

    ComandoService.Resultado r = service.executar(usuario, "mercado 120 ontem essencial");

    verify(transacaoService).adicionar(
        eq(usuario), eq(new BigDecimal("120.00")), eq("Alimentação"), eq("Mercado"),
        eq(LocalDate.of(2026, 9, 15)), eq(TipoTransacao.SAIDA), eq(true));
    assertEquals("Alimentação", r.categoria());
    assertNull(r.reservaId());
  }

  @Test
  void aporteVaiParaAReservaQueONomeIndica() {
    Reserva emergencia = reserva("Reserva de Emergência", true);
    Reserva viagem = reserva("Viagem Chile", false);
    when(reservaService.listar(usuario)).thenReturn(List.of(emergencia, viagem));

    ComandoService.Resultado r = service.executar(usuario, "guardar 300 viagem");

    verify(reservaService).alocar(usuario, viagem.getPublicId(), new BigDecimal("300.00"));
    assertEquals(viagem.getPublicId(), r.reservaId());
    assertEquals("Viagem Chile", r.descricao());
  }

  @Test
  void aporteSemNomeVaiParaAEmergencia() {
    Reserva emergencia = reserva("Reserva de Emergência", true);
    when(reservaService.listar(usuario)).thenReturn(
        List.of(emergencia, reserva("Viagem Chile", false)));

    service.executar(usuario, "guardar 300");

    verify(reservaService).alocar(usuario, emergencia.getPublicId(), new BigDecimal("300.00"));
  }

  @Test
  void nomeAmbiguoNaoEscolheNoChute() {
    when(reservaService.listar(usuario)).thenReturn(List.of(
        reserva("Viagem Chile", false), reserva("Viagem Japão", false)));

    ComandoNaoEntendidoException e = assertThrows(ComandoNaoEntendidoException.class, () ->
        service.executar(usuario, "guardar 300 viagem"));

    assertTrue(e.getMessage().contains("Viagem Chile"));
    assertTrue(e.getMessage().contains("Viagem Japão"));
    verify(reservaService, never()).alocar(any(), any(), any());
  }

  @Test
  void reservaInexistenteNaoCriaNada() {
    when(reservaService.listar(usuario)).thenReturn(List.of(reserva("Notebook", false)));

    assertThrows(ComandoNaoEntendidoException.class, () ->
        service.executar(usuario, "guardar 300 viagem"));
    verify(reservaService, never()).alocar(any(), any(), any());
  }

  @Test
  void preverCriaContaRecorrenteSemMesDeReferencia() {
    ArgumentCaptor<YearMonth> mes = ArgumentCaptor.forClass(YearMonth.class);
    contaCriada();

    service.executar(usuario, "prever energia 210 dia 22");

    verify(contaPrevistaService).criar(
        eq(usuario), eq("Energia"), eq(new BigDecimal("210.00")), eq(22), mes.capture());
    assertNull(mes.getValue());
  }

  @Test
  void umaVezCriaContaDoMesCorrente() {
    ArgumentCaptor<YearMonth> mes = ArgumentCaptor.forClass(YearMonth.class);
    contaCriada();

    service.executar(usuario, "prever ipva 840 dia 15 uma vez");

    verify(contaPrevistaService).criar(
        eq(usuario), eq("Ipva"), eq(new BigDecimal("840.00")), eq(15), mes.capture());
    assertEquals(YearMonth.of(2026, 9), mes.getValue());
  }

  @Test
  void textoSemNumeroNaoGravaNada() {
    ComandoNaoEntendidoException e = assertThrows(ComandoNaoEntendidoException.class, () ->
        service.executar(usuario, "blablabla"));

    assertTrue(e.getMessage().contains("blablabla"));
    verify(transacaoService, never())
        .adicionar(any(), any(), any(), any(), any(), any(), anyBoolean());
    verify(reservaService, never()).alocar(any(), any(), any());
    verify(contaPrevistaService, never()).criar(any(), any(), any(), anyInt(), any());
  }

  @Test
  void mensagemDeErroNaoEcoaUmTextoGigante() {
    String gigante = "x".repeat(500);

    ComandoNaoEntendidoException e = assertThrows(ComandoNaoEntendidoException.class, () ->
        service.executar(usuario, gigante));

    assertTrue(e.getMessage().length() < 120, e.getMessage());
  }

  private void contaCriada() {
    when(contaPrevistaService.criar(any(), any(), any(), anyInt(), any()))
        .thenAnswer(inv -> new ContaPrevista(
            usuario, inv.getArgument(1), inv.getArgument(2), inv.getArgument(3),
            inv.getArgument(4)));
  }

  private Reserva reserva(String nome, boolean emergencia) {
    return new Reserva(usuario, nome, new BigDecimal("1000.00"), emergencia);
  }
}
