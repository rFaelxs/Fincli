package com.rfaelxs.web.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Normalização de valores monetários (escopo-web.md §1.4).
 *
 * <p>Toda entrada é fixada em duas casas antes de qualquer comparação ou aritmética, para que
 * {@code 10.005} e {@code 10.00} não produzam saldos que divergem do que o usuário vê.
 */
public final class Dinheiro {

  public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

  private Dinheiro() {
  }

  /**
   * Fixa o valor em duas casas decimais, arredondando pela metade para cima.
   *
   * @param valor valor a normalizar
   * @return o valor com escala 2
   */
  public static BigDecimal normalizar(BigDecimal valor) {
    return valor.setScale(2, RoundingMode.HALF_UP);
  }
}
