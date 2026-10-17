package com.rfaelxs.web.service;

/** Operação válida em forma, mas impedida pelo estado atual. Mapeada para HTTP 409. */
public class ConflitoException extends RuntimeException {

  public ConflitoException(String mensagem) {
    super(mensagem);
  }
}
