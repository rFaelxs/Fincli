package com.rfaelxs.web.service;

import com.rfaelxs.web.domain.MovimentacaoReserva;
import com.rfaelxs.web.domain.Transacao;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;

/**
 * Uma linha do extrato, vinda de uma transação ou de uma movimentação de reserva.
 *
 * @param origem    de qual tabela a linha veio
 * @param data      dia do lançamento ou do movimento
 * @param valor     sempre positivo; o sentido vem do tipo
 * @param tipo      ENTRADA ou SAIDA em transação; ALOCACAO ou SAQUE em movimentação
 * @param categoria categoria da transação; "Reserva" em movimentação
 * @param descricao texto livre da transação, ou o nome da reserva no momento do movimento
 * @param id        id interno na tabela de origem; só desempata a ordem e não sai na API
 */
public record ItemExtrato(
    Origem origem,
    LocalDate data,
    BigDecimal valor,
    String tipo,
    String categoria,
    String descricao,
    Long id) {

  /** Categoria fixa das movimentações de reserva. */
  public static final String CATEGORIA_RESERVA = "Reserva";

  /**
   * Ordem do extrato, declarada em vez de depender da ordem da junção: a data mais recente
   * primeiro; no mesmo dia, as transações antes das movimentações; dentro de cada origem, a
   * lançada por último primeiro.
   */
  public static final Comparator<ItemExtrato> ORDEM =
      Comparator.comparing(ItemExtrato::data, Comparator.reverseOrder())
          .thenComparing(ItemExtrato::origem)
          .thenComparing(ItemExtrato::id, Comparator.reverseOrder());

  /** De onde a linha veio. A ordem de declaração é a ordem dentro do mesmo dia. */
  public enum Origem {
    TRANSACAO,
    RESERVA
  }

  /** Linha de uma transação. */
  public static ItemExtrato de(Transacao t) {
    return new ItemExtrato(Origem.TRANSACAO, t.getData(), t.getValor(), t.getTipo().name(),
        t.getCategoria(), t.getDescricao(), t.getId());
  }

  /** Linha de uma movimentação, com o nome da reserva gravado no movimento. */
  public static ItemExtrato de(MovimentacaoReserva m) {
    return new ItemExtrato(Origem.RESERVA, m.getData(), m.getValor(), m.getTipo().name(),
        CATEGORIA_RESERVA, m.getReservaNome(), m.getId());
  }
}
