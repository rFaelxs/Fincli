package com.rfaelxs.web.service;

import com.rfaelxs.web.domain.TipoTransacao;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Campos editáveis de uma transação, como chegam do cliente.
 *
 * @param valor     não negativo; normalizado para duas casas
 * @param categoria aparada
 * @param descricao aparada; em branco vira nula
 * @param data      dia do lançamento
 * @param tipo      entrada ou saída
 * @param essencial só tem efeito em saída
 */
public record DadosTransacao(
    BigDecimal valor,
    String categoria,
    String descricao,
    LocalDate data,
    TipoTransacao tipo,
    boolean essencial) {
}
