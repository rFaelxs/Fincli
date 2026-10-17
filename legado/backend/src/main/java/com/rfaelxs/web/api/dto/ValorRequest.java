package com.rfaelxs.web.api.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** Corpo de operações que carregam um único valor monetário: alocar, sacar, ajustar meta. */
public record ValorRequest(@NotNull(message = "Informe o valor.") BigDecimal valor) {
}
