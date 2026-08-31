package com.rfaelxs.web.api.dto;

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
public record ExtratoItemResponse(
    String origem,
    LocalDate data,
    BigDecimal valor,
    String tipo,
    String categoria,
    String descricao) {

  public static ExtratoItemResponse de(Transacao t) {
    return new ExtratoItemResponse(
        "TRANSACAO",
        t.getDataTransacao(),
        t.getValor(),
        t.getTipo().name(),
        t.getCategoria(),
        t.getDescricao());
  }

  public static ExtratoItemResponse de(MovimentacaoReserva m) {
    return new ExtratoItemResponse(
        "RESERVA",
        m.getDataMovimento(),
        m.getValor(),
        m.getTipo().name(),
        "Reserva",
        m.getReservaNome());
  }
}
