package com.rfaelxs.web.service;

/**
 * Recurso inexistente <strong>ou</strong> de outro usuário. Vira 404.
 *
 * <p>Os dois casos têm a mesma resposta de propósito: distinguir "não existe" de "não é seu"
 * revelaria que o recurso existe (ADR 0006).
 */
public class RecursoNaoEncontradoException extends RuntimeException {

  public RecursoNaoEncontradoException(String mensagem) {
    super(mensagem);
  }
}
