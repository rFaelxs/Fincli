package com.rfaelxs.web.service.resumo;

import com.rfaelxs.web.service.Dinheiro;
import java.math.BigDecimal;

/**
 * Totais de um mês.
 *
 * @param aportes alocações menos saques; negativo quando o usuário sacou mais do que guardou
 */
public record Totais(
    BigDecimal entradas,
    BigDecimal saidas,
    BigDecimal essenciais,
    BigDecimal superfluas,
    BigDecimal aportes,
    BigDecimal sobra) {

  static Totais de(
      BigDecimal entradas, BigDecimal saidas, BigDecimal essenciais, BigDecimal aportes) {
    return new Totais(
        Dinheiro.normalizar(entradas),
        Dinheiro.normalizar(saidas),
        Dinheiro.normalizar(essenciais),
        Dinheiro.normalizar(saidas.subtract(essenciais)),
        Dinheiro.normalizar(aportes),
        Dinheiro.normalizar(entradas.subtract(saidas).subtract(aportes)));
  }

  static Totais zero() {
    return de(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
  }
}
