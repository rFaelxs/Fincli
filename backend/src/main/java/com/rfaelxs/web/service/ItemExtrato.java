package com.rfaelxs.web.service;

import com.rfaelxs.web.domain.MovimentacaoReserva;
import com.rfaelxs.web.domain.Transacao;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Linha do extrato — transações e movimentações de reserva na mesma lista cronológica.
 *
 * @param origem    {@code TRANSACAO} ou {@code RESERVA}
 * @param tipo      ENTRADA/SAIDA para transações, ALOCACAO/SAQUE para reservas
 * @param descricao texto livre da transação, ou o nome da reserva no momento do movimento
 */
public record ItemExtrato(
    String origem,
    LocalDate data,
    BigDecimal valor,
    String tipo,
    String categoria,
    String descricao) {

  static ItemExtrato de(Transacao t) {
    return new ItemExtrato(
        "TRANSACAO",
        t.getDataTransacao(),
        t.getValor(),
        t.getTipo().name(),
        t.getCategoria(),
        t.getDescricao());
  }

  static ItemExtrato de(MovimentacaoReserva m) {
    return new ItemExtrato(
        "RESERVA",
        m.getDataMovimento(),
        m.getValor(),
        m.getTipo().name(),
        "Reserva",
        m.getReservaNome());
  }
}
