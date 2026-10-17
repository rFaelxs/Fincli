package com.rfaelxs.web.integracao;

import static org.assertj.core.api.Assertions.assertThat;

import com.rfaelxs.web.domain.Reserva;
import com.rfaelxs.web.domain.TipoTransacao;
import com.rfaelxs.web.service.DadosTransacao;
import com.rfaelxs.web.service.ExtratoService;
import com.rfaelxs.web.service.ItemExtrato;
import com.rfaelxs.web.service.ReservaService;
import com.rfaelxs.web.service.SaldoService;
import com.rfaelxs.web.service.TransacaoService;
import com.rfaelxs.web.service.ValorInvalidoException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** O lock por usuário e a chave estrangeira das movimentações, contra o banco de verdade. */
class ReservasIT extends BancoReal {

  @Autowired
  private ReservaService reservas;

  @Autowired
  private TransacaoService transacoes;

  @Autowired
  private SaldoService saldo;

  @Autowired
  private ExtratoService extrato;

  @Autowired
  private JdbcTemplate jdbc;

  @Autowired
  private Clock relogio;

  // CA-RES-25, no caso que o lock só da reserva deixava passar: reservas diferentes
  @Test
  void duasAlocacoesSimultaneasEmReservasDiferentesNaoPassamDoDisponivel() throws Exception {
    Long u = novoUsuario();
    entrada(u, "500");
    UUID viagem = reservas.criar(u, "Viagem", BigDecimal.ZERO).getPublicId();
    UUID carro = reservas.criar(u, "Carro", BigDecimal.ZERO).getPublicId();

    List<Throwable> falhas = alocarAoMesmoTempo(u, List.of(viagem, carro), "300");

    assertThat(falhas).hasSize(1);
    assertThat(falhas.get(0)).isInstanceOf(ValorInvalidoException.class)
        .hasMessage("Saldo insuficiente para esta alocação.");
    assertThat(saldo.disponivel(u)).isEqualByComparingTo("200.00");
  }

  // CA-RES-25, como a spec descreve: a mesma reserva
  @Test
  void duasAlocacoesSimultaneasNaMesmaReservaNaoPassamDoDisponivel() throws Exception {
    Long u = novoUsuario();
    entrada(u, "500");
    UUID viagem = reservas.criar(u, "Viagem", BigDecimal.ZERO).getPublicId();

    List<Throwable> falhas = alocarAoMesmoTempo(u, List.of(viagem, viagem), "300");

    assertThat(falhas).singleElement().isInstanceOf(ValorInvalidoException.class);
    assertThat(reservas.buscar(u, viagem).getSaldoAtual()).isEqualByComparingTo("300.00");
  }

  // ADR 0008: a movimentação sobrevive à exclusão da reserva
  @Test
  void excluirReservaQueJaTeveMovimentoMantemOExtrato() {
    Long u = novoUsuario();
    entrada(u, "500");
    Reserva viagem = reservas.criar(u, "Viagem", BigDecimal.ZERO);
    reservas.alocar(u, viagem.getPublicId(), new BigDecimal("100"));
    reservas.sacar(u, viagem.getPublicId(), new BigDecimal("100"));

    reservas.excluir(u, viagem.getPublicId());

    assertThat(extrato.listar(u)).filteredOn(i -> i.origem() == ItemExtrato.Origem.RESERVA)
        .extracting(ItemExtrato::descricao).containsExactly("Viagem", "Viagem");
    Integer orfas = jdbc.queryForObject(
        "select count(*) from movimentacao_reserva where usuario_id = ? and reserva_id is null",
        Integer.class, u);
    assertThat(orfas).isEqualTo(2);
  }

  /** Dispara as alocações juntas, cada uma na sua thread e na sua transação. */
  private List<Throwable> alocarAoMesmoTempo(Long u, List<UUID> destinos, String valor)
      throws InterruptedException {
    ExecutorService threads = Executors.newFixedThreadPool(destinos.size());
    CountDownLatch largada = new CountDownLatch(1);
    List<Future<?>> tarefas = new ArrayList<>();
    for (UUID destino : destinos) {
      tarefas.add(threads.submit(() -> {
        largada.await();
        return reservas.alocar(u, destino, new BigDecimal(valor));
      }));
    }
    largada.countDown();

    List<Throwable> falhas = new ArrayList<>();
    for (Future<?> tarefa : tarefas) {
      try {
        tarefa.get();
      } catch (ExecutionException e) {
        falhas.add(e.getCause());
      }
    }
    threads.shutdown();
    return falhas;
  }

  private void entrada(Long u, String valor) {
    transacoes.criar(u, new DadosTransacao(new BigDecimal(valor), "Trabalho", null,
        LocalDate.now(relogio), TipoTransacao.ENTRADA, false));
  }
}
