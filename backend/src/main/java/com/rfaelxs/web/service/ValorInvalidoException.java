package com.rfaelxs.web.service;

/** Entrada rejeitada pelas regras de negócio. Mapeada para HTTP 400. */
public class ValorInvalidoException extends RuntimeException {

  public ValorInvalidoException(String mensagem) {
    super(mensagem);
  }
}
