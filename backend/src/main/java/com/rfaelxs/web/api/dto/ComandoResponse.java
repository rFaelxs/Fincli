package com.rfaelxs.web.api.dto;

import com.rfaelxs.web.service.Dinheiro;
import com.rfaelxs.web.service.comando.ComandoParser;
import com.rfaelxs.web.service.comando.ComandoService;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * O que o comando criou, e como desfazer.
 *
 * @param id        transação em ENTRADA e SAIDA, conta em PREVISTA; nulo em APORTE
 * @param reservaId só em APORTE: desfazer é um saque de mesmo valor nessa reserva, que fica no
 *                  extrato em vez de sumir sem rastro
 * @param mensagem  texto pronto para a faixa de confirmação
 */
public record ComandoResponse(
    ComandoParser.Tipo tipo,
    UUID id,
    UUID reservaId,
    BigDecimal valor,
    String descricao,
    String categoria,
    String mensagem) {

  /** Converte o resultado, montando a frase no tempo verbal de cada tipo. */
  public static ComandoResponse de(ComandoService.Resultado r) {
    String verbo = switch (r.tipo()) {
      case ENTRADA -> "Recebido";
      case SAIDA -> "Lançado";
      case APORTE -> "Guardado";
      case PREVISTA -> "Previsto";
    };
    String mensagem = "%s %s · %s".formatted(verbo, Dinheiro.emReais(r.valor()), r.descricao());
    return new ComandoResponse(r.tipo(), r.id(), r.reservaId(), r.valor(), r.descricao(),
        r.categoria(), mensagem);
  }
}
