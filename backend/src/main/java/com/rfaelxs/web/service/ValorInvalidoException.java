package com.rfaelxs.web.service;

/** Regra de negócio violada por um valor enviado pelo cliente. Vira 400. */
public class ValorInvalidoException extends RuntimeException {

  public ValorInvalidoException(String mensagem) {
    super(mensagem);
  }
}
