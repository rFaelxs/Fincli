package com.rfaelxs.web.service.comando;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * O que o usuário quis lançar, como o {@link ComandoParser} entendeu.
 *
 * @param diaVencimento dia do mês; preenchido só em conta prevista
 * @param recorrente    se a conta prevista se repete todo mês; falso nos demais tipos
 */
public record Comando(
    ComandoParser.Tipo tipo,
    BigDecimal valor,
    String descricao,
    String categoria,
    LocalDate data,
    boolean essencial,
    Integer diaVencimento,
    boolean recorrente) {
}
