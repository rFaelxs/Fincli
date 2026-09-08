package com.rfaelxs.web.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Texto cru da barra de comando.
 *
 * <p>Só o texto trafega: tipo, valor e categoria são deduzidos no servidor. Se o cliente
 * mandasse o resultado do próprio parse, escolheria o que gravar sem passar pelas regras.
 */
public record ComandoRequest(
    @NotBlank(message = "Escreva o que você quer lançar.")
    @Size(max = 255, message = "O comando deve ter no máximo 255 caracteres.")
    String texto) {
}
