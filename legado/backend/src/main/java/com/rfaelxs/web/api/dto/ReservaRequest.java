package com.rfaelxs.web.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Criação de uma reserva financeira. */
public record ReservaRequest(
    @NotBlank(message = "Informe o nome da reserva.")
    @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres.")
    String nome,

    @NotNull(message = "Informe a meta.")
    @PositiveOrZero(message = "A meta não pode ser negativa.")
    BigDecimal metaValor) {
}
