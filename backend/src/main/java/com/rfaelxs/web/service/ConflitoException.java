package com.rfaelxs.web.service;

/** A operação colide com o estado atual, como um CPF já cadastrado. Vira 409. */
public class ConflitoException extends RuntimeException {

  public ConflitoException(String mensagem) {
    super(mensagem);
  }
}
