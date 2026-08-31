package com.rfaelxs.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import com.rfaelxs.config.GsonConfig;
import com.rfaelxs.model.DadosUsuario;
import com.rfaelxs.model.Reserva;
import com.rfaelxs.model.TipoTransacao;
import com.rfaelxs.model.User;
import com.rfaelxs.repository.UsuarioRepository;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Regressão: {@link TransacaoService} e {@link ReservaService} devem operar sobre a mesma
 * instância de {@link DadosUsuario}.
 *
 * <p>Cada mutação regrava o documento inteiro do usuário. Enquanto cada serviço carregava a
 * própria cópia no construtor, a última escrita apagava silenciosamente as alterações feitas
 * pelo outro serviço — alocar em uma reserva e registrar qualquer transação em seguida
 * devolvia o dinheiro ao saldo disponível e sumia com a movimentação do extrato.
 */
class PersistenciaCompartilhadaTest {

  @Test
  void alocacaoDeveSobreviverATransacaoRegistradaDepois() {
    UsuarioRepository repository = mock(UsuarioRepository.class);
    UUID idUsuario = UUID.randomUUID();

    DadosUsuario dados = new DadosUsuario(new User("000.000.000-00", idUsuario, "Teste"));
    dados.getReservas().add(
        new Reserva(ReservaService.ID_EMERGENCIA, "Reserva de Emergência", 5000.0, true));

    // Captura o que de fato iria para o disco, serializado como no repositório real.
    String[] persistido = new String[1];
    doAnswer(invocacao -> {
      persistido[0] = GsonConfig.GSON.toJson(invocacao.getArgument(1, DadosUsuario.class));
      return null;
    }).when(repository).salvarDados(any(), any());

    TransacaoService transacaoService = new TransacaoService(repository, idUsuario, dados);
    ReservaService reservaService = new ReservaService(repository, idUsuario, dados);

    transacaoService.adicionarTransacao(
        1000.0, "Salário", "Salário", LocalDate.now(), TipoTransacao.ENTRADA, false);
    reservaService.alocarSaldo(ReservaService.ID_EMERGENCIA, 500.0, 1000.0);
    transacaoService.adicionarTransacao(
        50.0, "Alimentação", "Mercado", LocalDate.now(), TipoTransacao.SAIDA, true);

    DadosUsuario doDisco = GsonConfig.GSON.fromJson(persistido[0], DadosUsuario.class);

    assertEquals(500.0, doDisco.getReservas().get(0).getSaldoAtual(), 0.001,
        "a alocação na reserva foi apagada pela transação registrada depois");
    assertEquals(1, doDisco.getMovimentacoesReserva().size(),
        "a movimentação da reserva sumiu do extrato");
    assertEquals(2, doDisco.getTransacoes().size());
  }
}
