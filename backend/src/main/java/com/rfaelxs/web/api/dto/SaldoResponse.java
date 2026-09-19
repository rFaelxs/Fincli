package com.rfaelxs.web.api.dto;

import java.math.BigDecimal;

/**
 * Saldo que ainda pode ir para uma reserva.
 *
 * @param saldoDisponivel entradas − saídas − total já alocado em reservas; pode ser negativo
 */
public record SaldoResponse(BigDecimal saldoDisponivel) {
}
