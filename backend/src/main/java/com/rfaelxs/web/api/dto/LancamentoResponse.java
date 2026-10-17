package com.rfaelxs.web.api.dto;

import com.rfaelxs.web.service.resumo.Lancamento;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Uma linha de "últimos lançamentos".
 *
 * @param id   nulo em movimentação de reserva
 * @param tipo ENTRADA, SAIDA, APORTE ou SAQUE; define o sinal e o chip na tela
 */
public record LancamentoResponse(
    UUID id,
    LocalDate data,
    String descricao,
    String categoria,
    boolean essencial,
    Lancamento.Tipo tipo,
    BigDecimal valor) {

  public static LancamentoResponse de(Lancamento l) {
    return new LancamentoResponse(
        l.id(), l.data(), l.descricao(), l.categoria(), l.essencial(), l.tipo(), l.valor());
  }
}
