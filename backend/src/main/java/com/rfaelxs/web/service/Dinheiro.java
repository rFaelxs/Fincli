package com.rfaelxs.web.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Normalização de valores monetários (ADR 0008).
 *
 * <p>Toda entrada é fixada em duas casas antes de qualquer comparação ou conta, para que
 * {@code 10.005} e {@code 10.00} não produzam saldos diferentes do que o usuário vê.
 */
public final class Dinheiro {

  public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

  private static final Locale PT_BR = Locale.of("pt", "BR");

  private Dinheiro() {
  }

  /** Fixa o valor em duas casas decimais, arredondando pela metade para cima. */
  public static BigDecimal normalizar(BigDecimal valor) {
    return valor.setScale(2, RoundingMode.HALF_UP);
  }

  /**
   * O valor em reais, como {@code R$ 1.234,56}, com espaço comum depois do símbolo.
   *
   * <p>Não usa o formato de moeda do Java: no pt-BR ele põe um espaço inseparável (U+00A0) entre
   * "R$" e o número, e a frase deixa de bater com a que a spec e o front esperam.
   */
  public static String emReais(BigDecimal valor) {
    DecimalFormat formato =
        new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(PT_BR));
    return "R$ " + formato.format(normalizar(valor));
  }
}
