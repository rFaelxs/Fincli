package com.rfaelxs.web.api.dto;

import com.rfaelxs.web.service.ItemExtrato;
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

  public static ExtratoItemResponse de(ItemExtrato i) {
    return new ExtratoItemResponse(
        i.origem(), i.data(), i.valor(), i.tipo(), i.categoria(), i.descricao());
  }
}
