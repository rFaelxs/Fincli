package com.rfaelxs.web.service.resumo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Uma linha da lista de lançamentos.
 *
 * @param id nulo em movimentação de reserva, que não tem id público
 */
public record Lancamento(
    UUID id,
    LocalDate data,
    String descricao,
    String categoria,
    boolean essencial,
    TipoLancamento tipo,
    BigDecimal valor) {
}
