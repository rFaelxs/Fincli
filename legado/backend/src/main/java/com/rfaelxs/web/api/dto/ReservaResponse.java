package com.rfaelxs.web.api.dto;

import com.rfaelxs.web.domain.Reserva;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/** Reserva como o SPA a consome, já com o progresso calculado. */
public record ReservaResponse(
    UUID id,
    String nome,
    BigDecimal metaValor,
    BigDecimal saldoAtual,
    BigDecimal progresso,
    boolean emergencia) {

  public static ReservaResponse de(Reserva r) {
    return new ReservaResponse(
        r.getPublicId(),
        r.getNome(),
        r.getMetaValor(),
        r.getSaldoAtual(),
        progresso(r),
        r.isEmergencia());
  }

  /** @return percentual da meta atingido; 0 se a meta não foi definida */
  private static BigDecimal progresso(Reserva r) {
    if (r.getMetaValor().signum() == 0) {
      return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
    }
    return r.getSaldoAtual()
        .multiply(BigDecimal.valueOf(100))
        .divide(r.getMetaValor(), 1, RoundingMode.HALF_UP);
  }
}
