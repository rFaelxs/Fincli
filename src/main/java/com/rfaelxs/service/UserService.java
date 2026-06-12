package com.rfaelxs.service;

import com.rfaelxs.model.DadosUsuario;
import com.rfaelxs.model.Reserva;
import com.rfaelxs.model.User;
import com.rfaelxs.repository.IUsuarioRepository;
import java.util.UUID;

/**
 * Regras de negócio para cadastro e autenticação de usuários.
 * No cadastro, cria automaticamente a Reserva de Emergência.
 */
public class UserService {

  private final IUsuarioRepository repository;

  /**
   * Inicializa o serviço com o repositório unificado de dados de usuário.
   *
   * @param repository repositório responsável pela persistência
   */
  public UserService(IUsuarioRepository repository) {
    this.repository = repository;
  }

  /**
   * Cadastra um novo usuário com UUID gerado automaticamente.
   * Cria o arquivo de dados do usuário com a Reserva de Emergência já incluída.
   * Retorna {@code null} se o CPF já estiver em uso.
   *
   * @param nome nome do usuário
   * @param cpf  CPF do usuário, usado como identificador de login
   * @return o {@link User} criado, ou {@code null} se o CPF já existir
   */
  public User cadastrar(String nome, String cpf) {
    if (repository.existeCpf(cpf)) {
      return null;
    }
    User novoUser = new User(cpf, UUID.randomUUID(), nome);
    DadosUsuario dados = new DadosUsuario(novoUser);
    dados.getReservas().add(
        new Reserva(ReservaService.ID_EMERGENCIA, "Reserva de Emergência", 0.0, true));
    repository.salvarPerfil(novoUser);
    repository.salvarDados(novoUser.getIdUsuario(), dados);
    return novoUser;
  }

  /**
   * Autentica um usuário pelo CPF.
   *
   * @param cpf CPF informado na tela de login
   * @return o {@link User} encontrado, ou {@code null} se não existir
   */
  public User login(String cpf) {
    return repository.buscarPorCpf(cpf);
  }
}
