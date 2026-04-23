package com.rfaelxs.comand;

import com.rfaelxs.model.TipoTransacao;
import com.rfaelxs.model.Transacao;
import com.rfaelxs.service.GastoService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

/**
 * Gerencia a interface de linha de comando (CLI) do sistema.
 * Exibe o menu principal, lê as entradas do usuário e delega
 * as operações ao {@link GastoService}.
 */
public class CommandHandler {

    private final GastoService gastoService;
    private final Scanner scanner;

    /**
     * @param service serviço de transações usado para executar as operações do menu
     */
    public CommandHandler(GastoService service) {
        this.gastoService = service;
        this.scanner = new Scanner(System.in);
    }

    /**
     * Inicia o loop principal do menu.
     * Permanece em execução até o usuário escolher a opção 0 (sair).
     * Opções disponíveis:
     * <ul>
     *   <li>1 - Adicionar transação</li>
     *   <li>2 - Listar transações</li>
     *   <li>3 - Remover transação por ID</li>
     *   <li>4 - Resumo por categoria</li>
     *   <li>0 - Encerrar</li>
     * </ul>
     */
    public void menuExecucao() {
        int opcao = -1;

        while (opcao != 0) {
            System.out.println("\n=== MENU PRINCIPAL ===");
            System.out.println("1 - Adicionar nova transação (add)");
            System.out.println("2 - Ver lista de transações (list)");
            System.out.println("3 - Remover transação por ID (remove)");
            System.out.println("4 - Resumo por Categoria (summary)");
            System.out.println("0 - Encerrar (exit)");
            System.out.print("Escolha uma opção: ");

            opcao = scanner.nextInt();
            scanner.nextLine();

            switch (opcao) {
                case 1:
                    System.out.println("\n-> Ação: Adicionando nova transação...");

                    System.out.print("Tipo (ENTRADA/SAIDA): ");
                    TipoTransacao tipo = TipoTransacao.valueOf(scanner.nextLine().toUpperCase());

                    System.out.print("Digite o valor: ");
                    double valor = scanner.nextDouble();
                    scanner.nextLine();

                    System.out.print("Digite a categoria: ");
                    String categoria = scanner.nextLine();

                    System.out.print("Digite a descrição: ");
                    String descricao = scanner.nextLine();

                    System.out.print("Digite a data (ex: 2020-01-10 ou 2020/01/10 ou 10/01/2020): ");
                    String dataTexto = scanner.nextLine();
                    LocalDate data;
                    if (dataTexto.contains("/")) {
                        String[] parts = dataTexto.split("/");
                        if (parts.length == 3 && parts[0].length() == 4) {
                            data = LocalDate.parse(dataTexto, DateTimeFormatter.ofPattern("yyyy/MM/dd"));
                        } else {
                            data = LocalDate.parse(dataTexto, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                        }
                    } else {
                        data = LocalDate.parse(dataTexto);
                    }

                    System.out.print("É essencial? (s/n): ");
                    boolean essencial = scanner.nextLine().trim().equalsIgnoreCase("s");

                    gastoService.adicionarTransacao(valor, categoria, descricao, data, tipo, essencial);
                    System.out.println("\n✔ Transação adicionada com sucesso!");
                    System.out.println("  " + tipo + " | R$ " + String.format("%.2f", valor) + " | " + categoria + " | " + descricao);
                    break;

                case 2:
                    List<Transacao> lista = gastoService.listarTransacoes();
                    if (lista.isEmpty()) {
                        System.out.println("\n  Nenhuma transação registrada.");
                    } else {
                        System.out.println("\n===== LISTA DE TRANSAÇÕES (" + lista.size() + " registro(s)) =====");
                        lista.forEach(System.out::println);
                    }
                    break;

                case 3:
                    System.out.println("\n-> Ação: Remover transação...");
                    System.out.print("Digite o ID da transação: ");

                    String idTexto = scanner.nextLine();
                    UUID idFormatado = UUID.fromString(idTexto);
                    gastoService.removerTransacao(idFormatado);
                    System.out.println("Transação removida com sucesso!");
                    break;

                case 4:
                    var resumo = gastoService.resumoPorCategoria();
                    if (resumo.isEmpty()) {
                        System.out.println("\n  Nenhuma transação registrada.");
                    } else {
                        System.out.println("\n===== RESUMO POR CATEGORIA =====");
                        resumo.forEach((cat, total) ->
                            System.out.printf("  %-20s R$ %,.2f%n", cat, total));
                        double totalGeral = resumo.values().stream().mapToDouble(Double::doubleValue).sum();
                        System.out.println("--------------------------------");
                        System.out.printf("  %-20s R$ %,.2f%n", "TOTAL", totalGeral);
                        System.out.println("================================");
                    }
                    break;

                case 0:
                    System.out.println("\n-> Encerrando o sistema. Até logo!");
                    break;

                default:
                    System.out.println("\n-> Opção inválida! Por favor, digite um número de 0 a 4.");
                    break;
            }
        }
    }
}
