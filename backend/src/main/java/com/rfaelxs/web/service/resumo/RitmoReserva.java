package com.rfaelxs.web.service.resumo;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;

/**
 * Ritmo de uma reserva.
 *
 * @param previsao mês em que a meta fecha no ritmo atual; nulo quando não há o que projetar
 * @param noRitmo  falso para reserva sem meta ou sem aporte na janela olhada
 */
public record RitmoReserva(
    UUID id,
    String nome,
    BigDecimal saldo,
    BigDecimal meta,
    BigDecimal progresso,
    BigDecimal aporteMedio,
    YearMonth previsao,
    boolean noRitmo) {
}
