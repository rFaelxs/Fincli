package com.rfaelxs.web.repository;

import com.rfaelxs.web.domain.TipoMovimentacaoReserva;
import java.math.BigDecimal;

/**
 * Soma de movimentações de reserva agrupada por mês e tipo.
 *
 * <p>O aporte de um mês é a alocação menos o saque: quem tira dinheiro da reserva não guardou
 * nada naquele mês.
 */
public record TotalMovimentacaoMensal(
    Integer ano, Integer mes, TipoMovimentacaoReserva tipo, BigDecimal total) {
}
