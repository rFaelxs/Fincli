package com.rfaelxs.web.api.dto;

import com.rfaelxs.web.service.ComandoService;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * O que o comando criou, e como desfazer.
 *
 * @param id        recurso criado: transação em ENTRADA/SAIDA, conta em PREVISTA; nulo em
 *                  APORTE
 * @param reservaId só em APORTE — desfazer é um saque de mesmo valor nessa reserva, que fica
 *                  registrado no extrato em vez de sumir sem rastro
 * @param mensagem  texto pronto para a faixa de confirmação
 */
public record ComandoResponse(
    String tipo,
    UUID id,
    UUID reservaId,
    BigDecimal valor,
    String descricao,
    String categoria,
    String mensagem) {

  public static ComandoResponse de(ComandoService.Resultado r, String mensagem) {
    return new ComandoResponse(
        r.tipo().name(), r.id(), r.reservaId(), r.valor(), r.descricao(), r.categoria(), mensagem);
  }
}
