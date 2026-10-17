package com.rfaelxs.web.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Criação de uma reserva.
 *
 * <p>A mensagem de meta negativa é a mesma do serviço, para que a frase da spec chegue à tela
 * por qualquer um dos dois caminhos.
 *
 * @param nome      nome de exibição
 * @param metaValor valor-alvo; zero quando não há meta
 */
public record ReservaRequest(
    @NotBlank(message = "Informe o nome da reserva.")
    @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres.")
    String nome,

    @NotNull(message = "Informe a meta.")
    @PositiveOrZero(message = "A meta da reserva não pode ser negativa.")
    BigDecimal metaValor) {
}
