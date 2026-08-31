package com.rfaelxs.command;

import com.rfaelxs.model.MovimentacaoReserva;
import com.rfaelxs.model.Reserva;
import com.rfaelxs.model.TipoTransacao;
import com.rfaelxs.model.Transacao;
import com.rfaelxs.model.User;
import com.rfaelxs.service.DashboardService;
import com.rfaelxs.service.ReservaService;
import com.rfaelxs.service.TransacaoService;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

/**
 * Gerencia os menus CLI do FinCLI para o usuário autenticado.
 * Chama apenas Services — nunca Repositories diretamente.
 */
public class CommandHandler {

  private static final String LINHA = "─────────────────────────────────────────────";
  private static final List<DateTimeFormatter> FORMATOS_DATA = List.of(
      DateTimeFormatter.ofPattern("yyyy-MM-dd"),
      DateTimeFormatter.ofPattern("yyyy/MM/dd"),
      DateTimeFormatter.ofPattern("dd/MM/yyyy")
  );

  private final User usuario;
  private final TransacaoService transacaoService;
  private final ReservaService reservaService;
  private final DashboardService dashboardService;
  private final Scanner scanner;

  /**
   * Cria o handler para o usuário autenticado com os serviços já inicializados.
   *
   * @param usuario          usuário logado
   * @param transacaoService serviço de transações
   * @param reservaService   serviço de reservas
   * @param dashboardService serviço de dashboard
   */
  public CommandHandler(
      User usuario,
      TransacaoService transacaoService,
      ReservaService reservaService,
      DashboardService dashboardService) {
    this.usuario = usuario;
    this.transacaoService = transacaoService;
    this.reservaService = reservaService;
    this.dashboardService = dashboardService;
    this.scanner = new Scanner(System.in);
  }

  /** Loop principal do menu após o login. */
  public void menuPrincipal() {
    boolean executando = true;
    while (executando) {
      System.out.println("\n┌" + LINHA);
      System.out.println("│  FinCLI · Olá, " + usuario.getNmUsuario());
      System.out.println("├" + LINHA);
      System.out.println("│  [1] Dashboard");
      System.out.println("│  [2] Transações");
      System.out.println("│  [3] Reservas");
      System.out.println("│  [4] Extrato");
      System.out.println("│  [0] Sair");
      System.out.println("└" + LINHA);
      int opcao = lerInt("Escolha: ");
      switch (opcao) {
        case 1 -> exibirDashboard();
        case 2 -> menuTransacoes();
        case 3 -> menuReservas();
        case 4 -> exibirExtrato();
        case 0 -> executando = false;
        default -> System.out.println("Opção inválida.");
      }
    }
    System.out.println("Até logo!");
  }

  // ── Dashboard ────────────────────────────────────────────────────────────

  private void exibirDashboard() {
    YearMonth mesAtual = YearMonth.now();
    double saldo = dashboardService.obterSaldo();
    double entradas = dashboardService.totalEntradasMes(mesAtual);
    double saidas = dashboardService.totalSaidasMes(mesAtual);
    double progresso = dashboardService.progressoEmergencia();
    Double selic = dashboardService.obterSelic();
    String selicTexto = selic != null ? String.format("%.2f%% a.a.", selic) : "Indisponível";

    System.out.println("\n┌" + LINHA);
    System.out.println("│  DASHBOARD — " + mesAtual);
    System.out.println("├" + LINHA);
    System.out.printf("│  Saldo disponível : R$ %.2f%n", saldo);
    System.out.printf("│  Entradas no mês  : R$ %.2f%n", entradas);
    System.out.printf("│  Saídas no mês    : R$ %.2f%n", saidas);
    System.out.printf("│  Reserva emergência: %.1f%%%n", progresso);
    System.out.printf("│  Meta Selic       : %s%n", selicTexto);

    List<Reserva> reservas = dashboardService.obterReservas();
    if (!reservas.isEmpty()) {
      System.out.println("├" + LINHA);
      System.out.println("│  Reservas:");
      for (Reserva r : reservas) {
        double pct = r.getMetaValor() == 0 ? 0 : r.getSaldoAtual() / r.getMetaValor() * 100;
        System.out.printf("│   %-20s R$ %8.2f / R$ %8.2f (%.1f%%)%n",
            r.getNome(), r.getSaldoAtual(), r.getMetaValor(), pct);
      }
    }
    System.out.println("└" + LINHA);
  }

  // ── Transações ────────────────────────────────────────────────────────────

  private void menuTransacoes() {
    boolean executando = true;
    while (executando) {
      System.out.println("\n│  TRANSAÇÕES");
      System.out.println("│  [1] Adicionar  [2] Listar  [3] Editar  [4] Remover  [5] Resumo  [0] Voltar");
      int opcao = lerInt("Escolha: ");
      switch (opcao) {
        case 1 -> adicionarTransacao();
        case 2 -> listarTransacoes();
        case 3 -> editarTransacao();
        case 4 -> removerTransacao();
        case 5 -> resumoPorCategoria();
        case 0 -> executando = false;
        default -> System.out.println("Opção inválida.");
      }
    }
  }

  private void adicionarTransacao() {
    TipoTransacao tipo = lerTipoTransacao();
    double valor = lerDouble("Valor (R$): ");
    String categoria = lerString("Categoria: ");
    String descricao = lerString("Descrição: ");
    LocalDate data = lerData("Data (dd/MM/yyyy): ");
    boolean essencial = tipo == TipoTransacao.SAIDA && lerBoolean("É essencial? (s/n): ");
    try {
      transacaoService.adicionarTransacao(valor, categoria, descricao, data, tipo, essencial);
      System.out.println("Transação registrada com sucesso.");
    } catch (Exception e) {
      System.out.println("Erro: " + e.getMessage());
    }
  }

  private void listarTransacoes() {
    List<Transacao> lista = transacaoService.listarTransacoes();
    if (lista.isEmpty()) {
      System.out.println("Nenhuma transação registrada.");
      return;
    }
    lista.forEach(System.out::println);
  }

  private void editarTransacao() {
    UUID id = lerUuid("ID da transação: ");
    System.out.println("Preencha os novos dados:");
    TipoTransacao tipo = lerTipoTransacao();
    double valor = lerDouble("Valor (R$): ");
    String categoria = lerString("Categoria: ");
    String descricao = lerString("Descrição: ");
    LocalDate data = lerData("Data (dd/MM/yyyy): ");
    boolean essencial = tipo == TipoTransacao.SAIDA && lerBoolean("É essencial? (s/n): ");
    try {
      transacaoService.editarTransacao(id, valor, categoria, descricao, data, tipo, essencial);
      System.out.println("Transação atualizada com sucesso.");
    } catch (Exception e) {
      System.out.println("Erro: " + e.getMessage());
    }
  }

  private void removerTransacao() {
    UUID id = lerUuid("ID da transação a remover: ");
    transacaoService.removerTransacao(id);
    System.out.println("Transação removida.");
  }

  private void resumoPorCategoria() {
    List<Transacao> lista = transacaoService.listarTransacoes();
    if (lista.isEmpty()) {
      System.out.println("Nenhuma transação registrada.");
      return;
    }
    System.out.println("\n┌" + LINHA);
    System.out.println("│  RESUMO POR CATEGORIA");
    System.out.println("├" + LINHA);
    lista.stream()
        .collect(java.util.stream.Collectors.groupingBy(
            Transacao::getCategoria,
            java.util.stream.Collectors.summingDouble(Transacao::getValorTransacao)))
        .forEach((cat, total) ->
            System.out.printf("│  %-20s R$ %.2f%n", cat, total));
    System.out.println("└" + LINHA);
  }

  // ── Reservas ─────────────────────────────────────────────────────────────

  private void menuReservas() {
    boolean executando = true;
    while (executando) {
      System.out.println("\n│  RESERVAS");
      System.out.println(
          "│  [1] Criar  [2] Listar  [3] Alocar saldo  [4] Sacar  "
          + "[5] Meta emergência  [6] Excluir  [0] Voltar");
      int opcao = lerInt("Escolha: ");
      switch (opcao) {
        case 1 -> criarReserva();
        case 2 -> listarReservas();
        case 3 -> alocarSaldo();
        case 4 -> sacarReserva();
        case 5 -> atualizarMetaEmergencia();
        case 6 -> excluirReserva();
        case 0 -> executando = false;
        default -> System.out.println("Opção inválida.");
      }
    }
  }

  private void criarReserva() {
    String nome = lerString("Nome da reserva: ");
    double meta = lerDouble("Meta (R$): ");
    try {
      Reserva r = reservaService.criarReserva(nome, meta);
      System.out.println("Reserva criada. ID: " + r.getId());
    } catch (Exception e) {
      System.out.println("Erro: " + e.getMessage());
    }
  }

  private void listarReservas() {
    List<Reserva> lista = reservaService.listarReservas();
    if (lista.isEmpty()) {
      System.out.println("Nenhuma reserva encontrada.");
      return;
    }
    System.out.println("\n┌" + LINHA);
    for (Reserva r : lista) {
      double pct = r.getMetaValor() == 0 ? 0 : r.getSaldoAtual() / r.getMetaValor() * 100;
      System.out.printf("│  [%s]%n│   Nome : %s%n│   Saldo: R$ %.2f / R$ %.2f (%.1f%%)%n",
          r.getId(), r.getNome(), r.getSaldoAtual(), r.getMetaValor(), pct);
      System.out.println("├" + LINHA);
    }
    System.out.println("└" + LINHA);
  }

  private void alocarSaldo() {
    String idReserva = lerString("ID da reserva: ");
    double valor = lerDouble("Valor a alocar (R$): ");
    double saldo = dashboardService.obterSaldo();
    try {
      reservaService.alocarSaldo(idReserva, valor, saldo);
      System.out.println("Saldo alocado com sucesso.");
    } catch (Exception e) {
      System.out.println("Erro: " + e.getMessage());
    }
  }

  private void sacarReserva() {
    String idReserva = lerString("ID da reserva: ");
    double valor = lerDouble("Valor a sacar (R$): ");
    try {
      reservaService.sacarReserva(idReserva, valor);
      System.out.println("Saque realizado com sucesso.");
    } catch (Exception e) {
      System.out.println("Erro: " + e.getMessage());
    }
  }

  private void atualizarMetaEmergencia() {
    double novaMeta = lerDouble("Nova meta da Reserva de Emergência (R$): ");
    try {
      reservaService.atualizarMetaEmergencia(novaMeta);
      System.out.println("Meta atualizada com sucesso.");
    } catch (Exception e) {
      System.out.println("Erro: " + e.getMessage());
    }
  }

  private void excluirReserva() {
    String idReserva = lerString("ID da reserva a excluir: ");
    try {
      reservaService.excluirReserva(idReserva);
      System.out.println("Reserva excluída.");
    } catch (Exception e) {
      System.out.println("Erro: " + e.getMessage());
    }
  }

  // ── Extrato ───────────────────────────────────────────────────────────────

  private void exibirExtrato() {
    record EntradaExtrato(LocalDate data, String descricao) {}
    List<EntradaExtrato> linhas = new ArrayList<>();

    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    for (Transacao t : transacaoService.listarTransacoes()) {
      String desc = String.format("%-8s R$ %8.2f  %-15s %s",
          t.getTipo(), t.getValorTransacao(), t.getCategoria(), t.getDescTransacao());
      linhas.add(new EntradaExtrato(t.getDataTransacao(), desc));
    }

    for (MovimentacaoReserva m : reservaService.listarMovimentacoes()) {
      String desc = String.format("%-8s R$ %8.2f  Reserva: %s",
          m.getTipo(), m.getValor(), m.getNomeReserva());
      linhas.add(new EntradaExtrato(m.getData(), desc));
    }

    linhas.sort(Comparator.comparing(EntradaExtrato::data).reversed());

    System.out.println("\n┌" + LINHA);
    System.out.println("│  EXTRATO DE MOVIMENTAÇÕES");
    System.out.println("├" + LINHA);
    if (linhas.isEmpty()) {
      System.out.println("│  Nenhuma movimentação registrada.");
    } else {
      for (EntradaExtrato e : linhas) {
        System.out.printf("│  %s  %s%n", e.data().format(fmt), e.descricao());
      }
    }
    System.out.println("└" + LINHA);
  }

  // ── Helpers de leitura ────────────────────────────────────────────────────

  private int lerInt(String prompt) {
    while (true) {
      System.out.print(prompt);
      try {
        return Integer.parseInt(scanner.nextLine().trim());
      } catch (NumberFormatException e) {
        System.out.println("Digite um número inteiro válido.");
      }
    }
  }

  private double lerDouble(String prompt) {
    while (true) {
      System.out.print(prompt);
      try {
        return Double.parseDouble(scanner.nextLine().trim().replace(",", "."));
      } catch (NumberFormatException e) {
        System.out.println("Digite um valor numérico válido.");
      }
    }
  }

  private String lerString(String prompt) {
    String valor;
    do {
      System.out.print(prompt);
      valor = scanner.nextLine().trim();
      if (valor.isEmpty()) {
        System.out.println("O campo não pode ser vazio.");
      }
    } while (valor.isEmpty());
    return valor;
  }

  private boolean lerBoolean(String prompt) {
    System.out.print(prompt);
    return scanner.nextLine().trim().equalsIgnoreCase("s");
  }

  private LocalDate lerData(String prompt) {
    while (true) {
      System.out.print(prompt);
      String entrada = scanner.nextLine().trim();
      for (DateTimeFormatter fmt : FORMATOS_DATA) {
        try {
          return LocalDate.parse(entrada, fmt);
        } catch (DateTimeParseException ignored) {
          // tenta próximo formato
        }
      }
      System.out.println("Formato inválido. Use dd/MM/yyyy, yyyy-MM-dd ou yyyy/MM/dd.");
    }
  }

  private UUID lerUuid(String prompt) {
    while (true) {
      System.out.print(prompt);
      try {
        return UUID.fromString(scanner.nextLine().trim());
      } catch (IllegalArgumentException e) {
        System.out.println("UUID inválido. Tente novamente.");
      }
    }
  }

  private TipoTransacao lerTipoTransacao() {
    while (true) {
      System.out.print("Tipo ([E]ntrada / [S]aída): ");
      String entrada = scanner.nextLine().trim().toUpperCase();
      if (entrada.equals("E") || entrada.equals("ENTRADA")) {
        return TipoTransacao.ENTRADA;
      }
      if (entrada.equals("S") || entrada.equals("SAIDA") || entrada.equals("SAÍDA")) {
        return TipoTransacao.SAIDA;
      }
      System.out.println("Digite E para Entrada ou S para Saída.");
    }
  }
}
