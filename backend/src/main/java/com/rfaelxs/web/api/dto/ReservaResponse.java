package com.rfaelxs.web.api.dto;

import com.rfaelxs.web.domain.Reserva;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Reserva como o front a consome.
 *
 * @param progresso percentual da meta, com uma casa; 0,0 sem meta
 */
public record ReservaResponse(
    UUID id,
    String nome,
    BigDecimal metaValor,
    BigDecimal saldoAtual,
    BigDecimal progresso,
    boolean emergencia) {

  /** Converte a entidade, expondo o id público. */
  public static ReservaResponse de(Reserva r) {
    return new ReservaResponse(r.getPublicId(), r.getNome(), r.getMetaValor(),
        r.getSaldoAtual(), r.progresso(), r.isEmergencia());
  }
}
