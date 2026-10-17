package com.rfaelxs.web.repository;

import com.rfaelxs.web.domain.TipoMovimentacaoReserva;
import java.math.BigDecimal;

/** Soma movimentada numa reserva no período, separada por tipo. */
public record TotalPorReserva(Long reservaId, TipoMovimentacaoReserva tipo, BigDecimal total) {
}
