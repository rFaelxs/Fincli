package com.rfaelxs.web.repository;

import com.rfaelxs.web.domain.TipoTransacao;
import java.math.BigDecimal;

/**
 * Soma de transações agrupada por mês, tipo e natureza. Uma consulta cobre de uma vez quantos
 * meses a tela precisar.
 *
 * @param essencial só faz sentido em saída
 */
public record TotalMensal(
    Integer ano, Integer mes, TipoTransacao tipo, Boolean essencial, BigDecimal total) {
}
