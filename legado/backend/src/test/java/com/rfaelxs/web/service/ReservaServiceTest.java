package com.rfaelxs.web.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rfaelxs.web.domain.Reserva;
import com.rfaelxs.web.domain.Usuario;
import com.rfaelxs.web.repository.MovimentacaoReservaRepository;
import com.rfaelxs.web.repository.ReservaRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Regras de reserva portadas do CLI, incluindo a proteção da Reserva de Emergência. */
@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

  @Mock private ReservaRepository reservaRepository;
  @Mock private MovimentacaoReservaRepository movimentacaoRepository;
  @Mock private TransacaoService transacaoService;

  private ReservaService service;
  private Usuario usuario;
  private Reserva emergencia;
  private Reserva viagem;
  private UUID idEmergencia;
  private UUID idViagem;

  @BeforeEach
  void setUp() {
    service = new ReservaService(reservaRepository, movimentacaoRepository, transacaoService);
    usuario = new Usuario("Teste", "11144477735", "hash");

    emergencia = new Reserva(usuario, ReservaService.NOME_EMERGENCIA, new BigDecimal("5000.00"), true);
    viagem = new Reserva(usuario, "Viagem", new BigDecimal("2000.00"), false);
    idEmergencia = emergencia.getPublicId();
    idViagem = viagem.getPublicId();
  }

  @Test
  void deveAlocarQuandoHaSaldo() {
    when(reservaRepository.findParaAtualizacao(any(), any())).thenReturn(Optional.of(viagem));
    when(transacaoService.calcularSaldoDisponivel(usuario)).thenReturn(new BigDecimal("1000.00"));

    service.alocar(usuario, idViagem, new BigDecimal("500.00"));

    assertEquals(new BigDecimal("500.00"), viagem.getSaldoAtual());
    verify(movimentacaoRepository).save(any());
  }

  @Test
  void deveRecusarAlocacaoAcimaDoSaldoDisponivel() {
    when(reservaRepository.findParaAtualizacao(any(), any())).thenReturn(Optional.of(viagem));
    when(transacaoService.calcularSaldoDisponivel(usuario)).thenReturn(new BigDecimal("500.00"));

    assertThrows(ValorInvalidoException.class, () ->
        service.alocar(usuario, idViagem, new BigDecimal("600.00")));

    assertEquals(new BigDecimal("0.00"), viagem.getSaldoAtual().setScale(2));
    verify(movimentacaoRepository, never()).save(any());
  }

  @Test
  void deveRecusarAlocacaoDeValorNaoPositivo() {
    assertThrows(ValorInvalidoException.class, () ->
        service.alocar(usuario, idViagem, BigDecimal.ZERO));
  }

  @Test
  void deveSacarAteOSaldoDaReserva() {
    viagem.setSaldoAtual(new BigDecimal("400.00"));
    when(reservaRepository.findParaAtualizacao(any(), any())).thenReturn(Optional.of(viagem));

    service.sacar(usuario, idViagem, new BigDecimal("100.00"));

    assertEquals(new BigDecimal("300.00"), viagem.getSaldoAtual());
  }

  @Test
  void deveRecusarSaqueAcimaDoSaldoDaReserva() {
    viagem.setSaldoAtual(new BigDecimal("100.00"));
    when(reservaRepository.findParaAtualizacao(any(), any())).thenReturn(Optional.of(viagem));

    assertThrows(ValorInvalidoException.class, () ->
        service.sacar(usuario, idViagem, new BigDecimal("200.00")));
  }

  @Test
  void naoDeveExcluirReservaDeEmergencia() {
    when(reservaRepository.findByPublicIdAndUsuarioId(any(), any()))
        .thenReturn(Optional.of(emergencia));

    assertThrows(ConflitoException.class, () -> service.excluir(usuario, idEmergencia));
    verify(reservaRepository, never()).delete(any());
  }

  @Test
  void naoDeveExcluirReservaComSaldo() {
    viagem.setSaldoAtual(new BigDecimal("10.00"));
    when(reservaRepository.findByPublicIdAndUsuarioId(any(), any())).thenReturn(Optional.of(viagem));

    assertThrows(ConflitoException.class, () -> service.excluir(usuario, idViagem));
  }

  @Test
  void deveExcluirReservaComumZerada() {
    when(reservaRepository.findByPublicIdAndUsuarioId(any(), any())).thenReturn(Optional.of(viagem));

    service.excluir(usuario, idViagem);

    verify(reservaRepository).delete(viagem);
  }

  @Test
  void deveFalharAoAcessarReservaDeOutroUsuario() {
    when(reservaRepository.findByPublicIdAndUsuarioId(any(), any())).thenReturn(Optional.empty());

    assertThrows(RecursoNaoEncontradoException.class, () ->
        service.excluir(usuario, UUID.randomUUID()));
  }

  @Test
  void deveRecusarMetaNegativa() {
    assertThrows(ValorInvalidoException.class, () ->
        service.criar(usuario, "Carro", new BigDecimal("-1.00")));
  }
}
