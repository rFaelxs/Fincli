package com.rfaelxs.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.rfaelxs.exception.ValorInvalidoException;
import com.rfaelxs.model.DadosUsuario;
import com.rfaelxs.model.Reserva;
import com.rfaelxs.model.TipoTransacao;
import com.rfaelxs.model.User;
import com.rfaelxs.repository.UsuarioRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransacaoServiceTest {

  @Mock
  private UsuarioRepository repository;

  private UUID idUsuario;
  private DadosUsuario dados;
  private TransacaoService service;

  @BeforeEach
  void setUp() {
    idUsuario = UUID.randomUUID();
    dados = new DadosUsuario(new User("000.000.000-00", idUsuario, "Teste"));
    service = new TransacaoService(repository, idUsuario, dados);
  }

  @Test
  void deveAdicionarTransacaoValida() {
    service.adicionarTransacao(
        100.0, "Alimentação", "Mercado", LocalDate.now(), TipoTransacao.SAIDA, true);

    assertEquals(1, service.listarTransacoes().size());
    verify(repository).salvarDados(eq(idUsuario), any());
  }

  @Test
  void deveLancarExcecaoParaValorNegativo() {
    assertThrows(ValorInvalidoException.class, () ->
        service.adicionarTransacao(
            -10.0, "Alimentação", "Mercado", LocalDate.now(), TipoTransacao.SAIDA, true));
  }

  @Test
  void deveCalcularSaldoCorretamente() {
    service.adicionarTransacao(
        1000.0, "Salário", "Salário maio", LocalDate.now(), TipoTransacao.ENTRADA, false);
    service.adicionarTransacao(
        200.0, "Aluguel", "Aluguel maio", LocalDate.now(), TipoTransacao.SAIDA, true);

    Reserva reserva = new Reserva("r1", "Viagem", 500.0, false);
    reserva.setSaldoAtual(300.0);

    double saldo = service.calcularSaldo(List.of(reserva));

    assertEquals(500.0, saldo, 0.001);
  }

  @Test
  void deveEditarTransacaoExistente() {
    service.adicionarTransacao(
        100.0, "Lazer", "Cinema", LocalDate.now(), TipoTransacao.SAIDA, false);
    UUID id = service.listarTransacoes().get(0).getId();

    service.editarTransacao(
        id, 150.0, "Lazer", "Teatro", LocalDate.now(), TipoTransacao.SAIDA, true);

    assertEquals(150.0, service.listarTransacoes().get(0).getValorTransacao(), 0.001);
    assertEquals("Teatro", service.listarTransacoes().get(0).getDescTransacao());
  }

  @Test
  void deveTotalEntradasMes() {
    YearMonth maio = YearMonth.of(2026, 5);
    service.adicionarTransacao(
        3000.0, "Salário", "Salário", LocalDate.of(2026, 5, 1), TipoTransacao.ENTRADA, false);
    service.adicionarTransacao(
        500.0, "Freelance", "Projeto", LocalDate.of(2026, 5, 15), TipoTransacao.ENTRADA, false);
    service.adicionarTransacao(
        200.0, "Alimentação", "Mercado", LocalDate.of(2026, 5, 10), TipoTransacao.SAIDA, true);

    assertEquals(3500.0, service.totalEntradasMes(maio), 0.001);
  }
}
