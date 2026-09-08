package com.rfaelxs.web.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * Criação de uma conta prevista.
 *
 * @param mesReferencia {@code yyyy-MM} para conta avulsa; nulo torna a conta recorrente
 */
public record ContaPrevistaRequest(
    @NotBlank(message = "Informe o nome da conta.")
    @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres.")
    String nome,

    @NotNull(message = "Informe o valor.")
    @Positive(message = "O valor deve ser positivo.")
    BigDecimal valor,

    @Min(value = 1, message = "O dia de vencimento deve estar entre 1 e 31.")
    @Max(value = 31, message = "O dia de vencimento deve estar entre 1 e 31.")
    int diaVencimento,

    YearMonth mesReferencia) {
}
