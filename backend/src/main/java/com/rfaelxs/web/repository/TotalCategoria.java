package com.rfaelxs.web.repository;

import java.math.BigDecimal;

/** Soma das saídas de uma categoria no período: "o que puxou meu mês pra baixo". */
public record TotalCategoria(String categoria, BigDecimal total) {
}
