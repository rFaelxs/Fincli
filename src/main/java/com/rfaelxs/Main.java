package com.rfaelxs;

import com.rfaelxs.api.ApiServer;
import com.rfaelxs.command.CommandHandler;
import com.rfaelxs.model.User;
import com.rfaelxs.repository.IUsuarioRepository;
import com.rfaelxs.repository.SelicRepository;
import com.rfaelxs.repository.UsuarioRepository;
import com.rfaelxs.repository.UsuarioRepositoryDb;
import com.rfaelxs.service.DashboardService;
import com.rfaelxs.service.ReservaService;
import com.rfaelxs.service.TransacaoService;
import com.rfaelxs.service.UserService;
import java.util.Arrays;
import java.util.Scanner;

/** Ponto de entrada do FinCLI. Exibe o fluxo de autenticação antes do menu principal. */
public class Main {

  public static void main(String[] args) {
    boolean apiMode = "true".equalsIgnoreCase(System.getenv("API_MODE"))
        || Arrays.asList(args).contains("--api");

    if (apiMode) {
      new ApiServer(new UsuarioRepositoryDb()).start();
      return;
    }

    IUsuarioRepository usuarioRepository = new UsuarioRepository();
    UserService userService = new UserService(usuarioRepository);
    Scanner scanner = new Scanner(System.in);

    System.out.println("╔═══════════════════════════════════════════╗");
    System.out.println("║            Bem-vindo ao FinCLI            ║");
    System.out.println("╚═══════════════════════════════════════════╝");

    User usuarioLogado = null;
    while (usuarioLogado == null) {
      System.out.println("\n[1] Login  [2] Cadastro  [0] Sair");
      System.out.print("Escolha: ");
      String opcao = scanner.nextLine().trim();

      switch (opcao) {
        case "1" -> {
          System.out.print("CPF: ");
          String cpf = scanner.nextLine().trim();
          usuarioLogado = userService.login(cpf);
          if (usuarioLogado == null) {
            System.out.println("CPF não encontrado. Tente novamente.");
          } else {
            System.out.println("Login realizado. Olá, " + usuarioLogado.getNmUsuario() + "!");
          }
        }
        case "2" -> {
          System.out.print("Nome: ");
          String nome = scanner.nextLine().trim();
          System.out.print("CPF: ");
          String cpf = scanner.nextLine().trim();
          usuarioLogado = userService.cadastrar(nome, cpf);
          if (usuarioLogado == null) {
            System.out.println("CPF já cadastrado. Faça login.");
          } else {
            System.out.println("Cadastro realizado. Bem-vindo, " + usuarioLogado.getNmUsuario()
                + "!");
          }
        }
        case "0" -> {
          System.out.println("Até logo!");
          return;
        }
        default -> System.out.println("Opção inválida.");
      }
    }

    TransacaoService transacaoService =
        new TransacaoService(usuarioRepository, usuarioLogado.getIdUsuario());
    ReservaService reservaService =
        new ReservaService(usuarioRepository, usuarioLogado.getIdUsuario());
    DashboardService dashboardService =
        new DashboardService(transacaoService, reservaService, new SelicRepository());

    new CommandHandler(usuarioLogado, transacaoService, reservaService, dashboardService)
        .menuPrincipal();
  }
}
