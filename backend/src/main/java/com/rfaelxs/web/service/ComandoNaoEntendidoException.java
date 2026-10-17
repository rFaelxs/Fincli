package com.rfaelxs.web.service;

/**
 * O texto da barra de comando não virou lançamento. Vira 422.
 *
 * <p>Separada de {@link ValorInvalidoException} porque a resposta é outra: a requisição está bem
 * formada, o servidor é que não conseguiu interpretar o conteúdo. A mensagem vai direto para a
 * tela, então diz o que fazer em vez de só recusar.
 */
public class ComandoNaoEntendidoException extends RuntimeException {

  public ComandoNaoEntendidoException(String mensagem) {
    super(mensagem);
  }
}
