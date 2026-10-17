package com.rfaelxs.web.integracao;

import static org.assertj.core.api.Assertions.assertThat;

import com.rfaelxs.web.domain.ContaPrevista;
import com.rfaelxs.web.domain.Reserva;
import com.rfaelxs.web.domain.TipoTransacao;
import com.rfaelxs.web.repository.TotalCategoria;
import com.rfaelxs.web.service.ContaPrevistaService;
import com.rfaelxs.web.service.DadosTransacao;
import com.rfaelxs.web.service.ExtratoService;
import com.rfaelxs.web.service.ItemExtrato;
import com.rfaelxs.web.service.ReservaService;
import com.rfaelxs.web.service.SaldoService;
import com.rfaelxs.web.service.TransacaoService;
import com.rfaelxs.web.service.comando.ComandoService;
import com.rfaelxs.web.service.resumo.Gestao;
import com.rfaelxs.web.service.resumo.GestaoService;
import com.rfaelxs.web.service.resumo.Lancamento;
import com.rfaelxs.web.service.resumo.Relatorio;
import com.rfaelxs.web.service.resumo.RelatorioService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Um mês de uso de ponta a ponta. Cada número conferido aqui sai de uma consulta real: as somas
 * agrupadas por mês, por categoria e por reserva, os {@code findTop6} e o filtro de contas do mês.
 */
class FluxoDoMesIT extends BancoReal {

  @Autowired
  private TransacaoService transacoes;

  @Autowired
  private ReservaService reservas;

  @Autowired
  private SaldoService saldo;

  @Autowired
  private ExtratoService extrato;

  @Autowired
  private GestaoService gestao;

  @Autowired
  private RelatorioService relatorio;

  @Autowired
  private ContaPrevistaService previstas;

  @Autowired
  private ComandoService comando;

  @Autowired
  private JdbcTemplate jdbc;

  @Autowired
  private Clock relogio;

  @Test
  void asMigrationsForamAplicadas() {
    Integer aplicadas = jdbc.queryForObject(
        "select count(*) from flyway_schema_history where success = 1", Integer.class);

    assertThat(aplicadas).isGreaterThanOrEqualTo(4);
  }

  @Test
  void cadastroCriaAReservaDeEmergencia() {
    Long u = novoUsuario();

    assertThat(reservas.listar(u)).singleElement()
        .satisfies(r -> assertThat(r.isEmergencia()).isTrue());
  }

  @Test
  void numerosDoMesBatemEmTodasAsTelas() {
    Long u = novoUsuario();
    LocalDate hoje = LocalDate.now(relogio);
    transacao(u, "5000", "Trabalho", "Salário", TipoTransacao.ENTRADA, false, hoje);
    transacao(u, "2000", "Moradia", "Aluguel", TipoTransacao.SAIDA, true, hoje);
    transacao(u, "500", "Lazer", "Show", TipoTransacao.SAIDA, false, hoje);
    Reserva viagem = reservas.criar(u, "Viagem", new BigDecimal("10000"));
    reservas.alocar(u, viagem.getPublicId(), new BigDecimal("1000"));

    // RN-09: entradas − saídas − guardado
    assertThat(saldo.disponivel(u)).isEqualByComparingTo("1500.00");

    Gestao g = gestao.hoje(u);
    assertThat(g.totais().entradas()).isEqualByComparingTo("5000.00");
    assertThat(g.totais().saidas()).isEqualByComparingTo("2500.00");
    assertThat(g.totais().essenciais()).isEqualByComparingTo("2000.00");
    assertThat(g.totais().superfluas()).isEqualByComparingTo("500.00");
    assertThat(g.totais().aportes()).isEqualByComparingTo("1000.00");
    assertThat(g.totais().sobra()).isEqualByComparingTo("1500.00");
    // Tudo no mesmo dia: transações antes da movimentação, a última lançada primeiro.
    assertThat(g.ultimosLancamentos()).extracting(Lancamento::descricao)
        .containsExactly("Show", "Aluguel", "Salário", "Viagem");

    Relatorio r = relatorio.doMes(u, YearMonth.from(hoje));
    assertThat(r.topCategorias()).extracting(TotalCategoria::categoria)
        .containsExactly("Moradia", "Lazer");
    assertThat(r.historico()).hasSize(6);
    assertThat(r.historico().get(5).sobra()).isEqualByComparingTo("1500.00");
    assertThat(r.reservas()).filteredOn(x -> x.nome().equals("Viagem")).singleElement()
        .satisfies(x -> assertThat(x.aporteMedio()).isEqualByComparingTo("333.33"));

    assertThat(extrato.listar(u)).extracting(ItemExtrato::descricao)
        .containsExactly("Show", "Aluguel", "Salário", "Viagem");
  }

  @Test
  void ultimosLancamentosParamEm6() {
    Long u = novoUsuario();
    LocalDate hoje = LocalDate.now(relogio);
    for (int i = 1; i <= 9; i++) {
      transacao(u, "10", "Outros", "Gasto " + i, TipoTransacao.SAIDA, false, hoje);
    }

    assertThat(gestao.hoje(u).ultimosLancamentos()).extracting(Lancamento::descricao)
        .containsExactly("Gasto 9", "Gasto 8", "Gasto 7", "Gasto 6", "Gasto 5", "Gasto 4");
  }

  // CA-EXT-10
  @Test
  void umUsuarioNaoVeOsDadosDoOutro() {
    Long primeiro = novoUsuario();
    Long segundo = novoUsuario();
    transacao(segundo, "100", "Outros", "Do segundo", TipoTransacao.SAIDA, false,
        LocalDate.now(relogio));

    assertThat(extrato.listar(primeiro)).isEmpty();
    assertThat(transacoes.listar(primeiro)).isEmpty();
    assertThat(gestao.hoje(primeiro).totais().saidas()).isEqualByComparingTo("0.00");
  }

  // CA-GESTAO-22: o filtro do mês roda no banco, e o dia 31 é ajustado depois
  @Test
  void contasDoMesFiltramAvulsasDeOutrosMeses() {
    Long u = novoUsuario();
    YearMonth fevereiro = YearMonth.of(2026, 2);
    previstas.criar(u, "Aluguel", new BigDecimal("1500"), 31, null);
    previstas.criar(u, "IPVA", new BigDecimal("800"), 10, fevereiro);
    previstas.criar(u, "Seguro", new BigDecimal("300"), 10, YearMonth.of(2026, 3));

    List<ContaPrevista> contas = previstas.aindaVencem(u, fevereiro, fevereiro.atDay(1));

    assertThat(contas).extracting(ContaPrevista::getNome).containsExactly("IPVA", "Aluguel");
    assertThat(contas.get(1).vencimentoEm(fevereiro)).isEqualTo(LocalDate.of(2026, 2, 28));
  }

  @Test
  void barraDeComandoGravaPelosServicosDeCadaModulo() {
    Long u = novoUsuario();

    comando.executar(u, "freela 450");
    comando.executar(u, "mercado 120");
    comando.executar(u, "guardar 50");

    assertThat(saldo.disponivel(u)).isEqualByComparingTo("280.00");
    assertThat(reservas.listar(u).get(0).getSaldoAtual()).isEqualByComparingTo("50.00");
  }

  private void transacao(Long u, String valor, String categoria, String descricao,
      TipoTransacao tipo, boolean essencial, LocalDate data) {
    transacoes.criar(u, new DadosTransacao(new BigDecimal(valor), categoria, descricao, data,
        tipo, essencial));
  }
}
