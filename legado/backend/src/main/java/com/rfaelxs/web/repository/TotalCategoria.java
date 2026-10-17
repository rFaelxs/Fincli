package com.rfaelxs.web.repository;

import java.math.BigDecimal;

/** Soma de saídas de uma categoria no período — alimenta o "o que puxou meu mês pra baixo". */
public record TotalCategoria(String categoria, BigDecimal total) {
}
