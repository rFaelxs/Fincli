package com.rfaelxs.web.repository;

import com.rfaelxs.web.domain.TipoTransacao;
import java.math.BigDecimal;

/**
 * Soma de transações agrupada por mês, tipo e natureza.
 *
 * <p>Uma consulta só cobre o mês corrente e o histórico de seis meses do Relatório — somar mês
 * a mês em chamadas separadas custaria uma ida ao banco por mês e por tipo.
 *
 * @param essencial só faz sentido em saídas; em entradas vem sempre falso
 */
public record TotalMensal(
    Integer ano, Integer mes, TipoTransacao tipo, Boolean essencial, BigDecimal total) {
}
