package com.rfaelxs.web.api.dto;

import com.rfaelxs.web.domain.Usuario;
import java.util.UUID;

/**
 * Usuário da sessão.
 *
 * <p>Sem o CPF: nenhuma tela o mostra, e dado que não sai do servidor não vaza (ADR 0009).
 *
 * @param id   identificador público
 * @param nome nome de exibição
 */
public record UsuarioResponse(UUID id, String nome) {

  public static UsuarioResponse de(Usuario usuario) {
    return new UsuarioResponse(usuario.getPublicId(), usuario.getNome());
  }
}
