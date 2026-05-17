package com.rfaelxs.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.rfaelxs.exception.ValorInvalidoException;
import com.rfaelxs.model.DadosUsuario;
import com.rfaelxs.model.Reserva;
import com.rfaelxs.model.User;
import com.rfaelxs.repository.UsuarioRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

  @Mock
  private UsuarioRepository repository;

  private UUID idUsuario;
  private DadosUsuario dados;
  private ReservaService service;

  @BeforeEach
  void setUp() {
    idUsuario = UUID.randomUUID();
    dados = new DadosUsuario(new User("000.000.000-00", idUsuario, "Teste"));
    dados.getReservas().add(
        new Reserva(ReservaService.ID_EMERGENCIA, "Reserva de Emergência", 5000.0, true));
    when(repository.carregarDados(idUsuario)).thenReturn(dados);
    service = new ReservaService(repository, idUsuario);
  }

  @Test
  void deveAlocarSaldoComSucesso() {
    service.criarReserva("Viagem", 2000.0);
    String idReserva = service.listarReservas().stream()
        .filter(r -> !r.isEmergencia()).findFirst().get().getId();

    service.alocarSaldo(idReserva, 500.0, 1000.0);

    double saldo = service.listarReservas().stream()
        .filter(r -> r.getId().equals(idReserva)).findFirst().get().getSaldoAtual();
    assertEquals(500.0, saldo, 0.001);
  }

  @Test
  void deveLancarExcecaoParaSaldoInsuficiente() {
    service.criarReserva("Carro", 10000.0);
    String idReserva = service.listarReservas().stream()
        .filter(r -> !r.isEmergencia()).findFirst().get().getId();

    assertThrows(ValorInvalidoException.class,
        () -> service.alocarSaldo(idReserva, 600.0, 500.0));
  }

  @Test
  void deveSacarReservaComSucesso() {
    service.criarReserva("Carro", 10000.0);
    String idReserva = service.listarReservas().stream()
        .filter(r -> !r.isEmergencia()).findFirst().get().getId();

    service.alocarSaldo(idReserva, 400.0, 1000.0);
    service.sacarReserva(idReserva, 100.0);

    double saldo = service.listarReservas().stream()
        .filter(r -> r.getId().equals(idReserva)).findFirst().get().getSaldoAtual();
    assertEquals(300.0, saldo, 0.001);
  }

  @Test
  void naoDeveExcluirReservaDeEmergencia() {
    assertThrows(IllegalStateException.class,
        () -> service.excluirReserva(ReservaService.ID_EMERGENCIA));
  }

  @Test
  void deveAtualizarMetaEmergencia() {
    service.atualizarMetaEmergencia(10000.0);

    double meta = service.listarReservas().stream()
        .filter(Reserva::isEmergencia).findFirst().get().getMetaValor();
    assertEquals(10000.0, meta, 0.001);
  }
}
