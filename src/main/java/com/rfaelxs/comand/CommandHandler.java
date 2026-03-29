package com.rfaelxs.comand;

import com.rfaelxs.model.Gasto;
import com.rfaelxs.service.GastoService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

public class CommandHandler {

    private final GastoService gastoService;
    private final Scanner scanner;

    public CommandHandler(GastoService service){
        this.gastoService = service;
        this.scanner = new Scanner(System.in);
    }

    public void menuExecucao(){
        int opcao = -1;

        while(opcao != 0){
            System.out.println("\n=== MENU PRINCIPAL ===");
            System.out.println("1 - Adicionar novo gasto (add)");
            System.out.println("2 - Ver lista de gastos (list)");
            System.out.println("3 - Remover gasto por ID (remove)");
            System.out.println("4 - Resumo por Categoria (summary)");
            System.out.println("0 - Encerrar (exit)");
            System.out.print("Escolha uma opção: ");

            opcao = scanner.nextInt();

            scanner.nextLine();

            switch (opcao) {
                case 1:
                    System.out.println("\n-> Ação: Adicionando novo gasto...");

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

                    gastoService.adicionarGasto(valor, categoria, descricao, data);
                    System.out.println("\n✔ Gasto adicionado com sucesso!");
                    System.out.println("  R$ " + String.format("%.2f", valor) + " | " + categoria + " | " + descricao);
                    break;

                case 2:
                    List<Gasto> lista = gastoService.listarGastos();
                    if (lista.isEmpty()) {
                        System.out.println("\n  Nenhum gasto registrado.");
                    } else {
                        System.out.println("\n===== LISTA DE GASTOS (" + lista.size() + " registro(s)) =====");
                        lista.forEach(System.out::println);
                    }
                    break;

                case 3:
                    System.out.println("\n-> Ação: Remover gasto...");
                    System.out.print("Digite o ID do gasto: ");

                    String idTexto = scanner.nextLine();
                    UUID idFormatado = UUID.fromString(idTexto);
                    gastoService.removerGasto(idFormatado);
                    System.out.println("Gasto removido com sucesso!");
                    break;

                case 4:
                    var resumo = gastoService.resumoPorCategoria();
                    if (resumo.isEmpty()) {
                        System.out.println("\n  Nenhum gasto registrado.");
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
