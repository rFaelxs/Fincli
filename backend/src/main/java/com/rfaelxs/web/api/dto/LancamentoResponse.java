package com.rfaelxs.web.api.dto;

import com.rfaelxs.web.service.ResumoService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Uma linha de "últimos lançamentos".
 *
 * @param id   nulo em movimentação de reserva, que não tem id público — a tela não oferece
 *             excluir nessas linhas
 * @param tipo ENTRADA, SAIDA, APORTE ou SAQUE; define o sinal e a cor na tela
 */
public record LancamentoResponse(
    UUID id,
    LocalDate data,
    String descricao,
    String categoria,
    boolean essencial,
    String tipo,
    BigDecimal valor) {

  public static LancamentoResponse de(ResumoService.Lancamento l) {
    return new LancamentoResponse(
        l.id(), l.data(), l.descricao(), l.categoria(), l.essencial(), l.tipo().name(), l.valor());
  }
}
