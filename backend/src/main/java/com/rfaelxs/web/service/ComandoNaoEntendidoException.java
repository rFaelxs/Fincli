package com.rfaelxs.web.service;

/**
 * O texto da barra de comando não virou um lançamento.
 *
 * <p>Separada de {@link ValorInvalidoException} porque a resposta é outra: aqui a requisição
 * está bem formada, o servidor é que não conseguiu interpretar o conteúdo — 422, não 400. A
 * mensagem vai direto para a tela, então diz o que fazer em vez de só recusar.
 */
public class ComandoNaoEntendidoException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public ComandoNaoEntendidoException(String mensagem) {
    super(mensagem);
  }
}
