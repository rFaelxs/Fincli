package com.rfaelxs.web.api.dto;

import java.math.BigDecimal;

/**
 * Saldo disponível para alocar.
 *
 * @param saldoDisponivel entradas menos saídas menos o guardado em reservas; pode ser negativo
 */
public record SaldoResponse(BigDecimal saldoDisponivel) {
}
