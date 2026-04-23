package com.rfaelxs.exception;

/**
 * Lançada quando um valor monetário inválido é fornecido.
 * Exemplo: valor negativo ao criar uma transação.
 */
public class ValorInvalidoException extends RuntimeException {

    /**
     * @param message descrição do motivo pelo qual o valor é inválido
     */
    public ValorInvalidoException(String message) {
        super(message);
    }

}
