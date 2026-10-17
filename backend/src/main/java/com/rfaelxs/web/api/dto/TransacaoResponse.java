package com.rfaelxs.web.api.dto;

import com.rfaelxs.web.domain.TipoTransacao;
import com.rfaelxs.web.domain.Transacao;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Transação como o front a consome. {@code data} sai em {@code yyyy-MM-dd}. */
public record TransacaoResponse(
    UUID id,
    BigDecimal valor,
    String categoria,
    String descricao,
    LocalDate data,
    TipoTransacao tipo,
    boolean essencial) {

  /** Converte a entidade, expondo o id público. */
  public static TransacaoResponse de(Transacao t) {
    return new TransacaoResponse(t.getPublicId(), t.getValor(), t.getCategoria(),
        t.getDescricao(), t.getData(), t.getTipo(), t.isEssencial());
  }
}
