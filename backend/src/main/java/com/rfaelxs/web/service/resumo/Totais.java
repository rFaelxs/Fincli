package com.rfaelxs.web.service.resumo;

import com.rfaelxs.web.service.Dinheiro;
import java.math.BigDecimal;

/**
 * Totais de um mês, todos em duas casas.
 *
 * <pre>
 *   essenciais = soma das saídas marcadas essencial
 *   superfluas = saidas − essenciais
 *   aportes    = alocações − saques do mês
 *   sobra      = entradas − saidas − aportes
 * </pre>
 *
 * @param aportes negativo quando o usuário sacou mais do que guardou no mês
 */
public record Totais(
    BigDecimal entradas,
    BigDecimal saidas,
    BigDecimal essenciais,
    BigDecimal superfluas,
    BigDecimal aportes,
    BigDecimal sobra) {

  /** Deriva supérfluas e sobra a partir das quatro somas que vêm do banco. */
  public static Totais de(BigDecimal entradas, BigDecimal saidas, BigDecimal essenciais,
      BigDecimal aportes) {
    return new Totais(
        Dinheiro.normalizar(entradas),
        Dinheiro.normalizar(saidas),
        Dinheiro.normalizar(essenciais),
        Dinheiro.normalizar(saidas.subtract(essenciais)),
        Dinheiro.normalizar(aportes),
        Dinheiro.normalizar(entradas.subtract(saidas).subtract(aportes)));
  }

  /** Mês sem nenhum lançamento. */
  public static Totais zero() {
    return de(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
  }
}
