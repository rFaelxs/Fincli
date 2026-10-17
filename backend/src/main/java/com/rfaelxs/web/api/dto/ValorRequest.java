package com.rfaelxs.web.api.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Corpo das operações de um valor só: alocar, sacar e trocar a meta. O sinal é validado no
 * serviço, porque a mensagem muda conforme a operação.
 *
 * @param valor valor em reais
 */
public record ValorRequest(@NotNull(message = "Informe o valor.") BigDecimal valor) {
}
