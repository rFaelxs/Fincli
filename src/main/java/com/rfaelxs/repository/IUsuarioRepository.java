package com.rfaelxs.repository;

import com.rfaelxs.model.DadosUsuario;
import com.rfaelxs.model.User;
import java.util.UUID;

/** Contrato de persistência para dados de usuário. */
public interface IUsuarioRepository {

  /**
   * Verifica se já existe um usuário com o CPF informado.
   *
   * @param cpf CPF a verificar
   * @return {@code true} se o CPF já está cadastrado
   */
  boolean existeCpf(String cpf);

  /**
   * Salva ou atualiza o perfil do usuário.
   *
   * @param usuario usuário a persistir
   */
  void salvarPerfil(User usuario);

  /**
   * Busca um usuário pelo CPF.
   *
   * @param cpf CPF de login
   * @return o {@link User} encontrado ou {@code null} se não existir
   */
  User buscarPorCpf(String cpf);

  /**
   * Carrega os dados completos de um usuário.
   *
   * @param idUsuario UUID do usuário
   * @return {@link DadosUsuario} ou {@code null} se não existir
   */
  DadosUsuario carregarDados(UUID idUsuario);

  /**
   * Persiste os dados completos de um usuário.
   *
   * @param idUsuario UUID do usuário
   * @param dados     dados a persistir
   */
  void salvarDados(UUID idUsuario, DadosUsuario dados);
}
