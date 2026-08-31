package com.rfaelxs.web.service;

/**
 * Recurso inexistente <strong>ou</strong> pertencente a outro usuário. Mapeada para HTTP 404.
 *
 * <p>Os dois casos devolvem a mesma resposta de propósito: distinguir "não existe" de "não é
 * seu" revelaria a existência de recursos alheios.
 */
public class RecursoNaoEncontradoException extends RuntimeException {

  public RecursoNaoEncontradoException(String mensagem) {
    super(mensagem);
  }
}
