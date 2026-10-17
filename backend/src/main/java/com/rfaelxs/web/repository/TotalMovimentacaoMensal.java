package com.rfaelxs.web.repository;

import com.rfaelxs.web.domain.TipoMovimentacaoReserva;
import java.math.BigDecimal;

/** Soma de movimentações de reserva agrupada por mês e tipo. */
public record TotalMovimentacaoMensal(
    Integer ano, Integer mes, TipoMovimentacaoReserva tipo, BigDecimal total) {
}
