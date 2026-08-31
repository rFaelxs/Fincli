package com.rfaelxs.web.api.dto;

import com.rfaelxs.web.domain.Usuario;
import java.util.UUID;

/**
 * Usuário da sessão.
 *
 * @param id   identificador público
 * @param nome nome de exibição
 * @param cpf  CPF mascarado — o valor completo nunca sai da aplicação (escopo-web.md §1.5)
 */
public record UsuarioResponse(UUID id, String nome, String cpf) {

  public static UsuarioResponse de(Usuario usuario) {
    return new UsuarioResponse(usuario.getPublicId(), usuario.getNome(), mascarar(usuario.getCpf()));
  }

  /** Devolve apenas os três últimos dígitos, no formato {@code ***.***.***-NN}. */
  private static String mascarar(String cpf) {
    return "***.***.**" + cpf.substring(8);
  }
}
