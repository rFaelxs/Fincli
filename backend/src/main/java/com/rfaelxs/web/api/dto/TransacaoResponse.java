package com.rfaelxs.web.api.dto;

import com.rfaelxs.web.domain.TipoTransacao;
import com.rfaelxs.web.domain.Transacao;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Transação como o SPA a consome. */
public record TransacaoResponse(
    UUID id,
    BigDecimal valor,
    String categoria,
    String descricao,
    LocalDate data,
    TipoTransacao tipo,
    boolean essencial) {

  public static TransacaoResponse de(Transacao t) {
    return new TransacaoResponse(
        t.getPublicId(),
        t.getValor(),
        t.getCategoria(),
        t.getDescricao(),
        t.getDataTransacao(),
        t.getTipo(),
        t.isEssencial());
  }
}
