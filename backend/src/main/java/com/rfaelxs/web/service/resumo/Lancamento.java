package com.rfaelxs.web.service.resumo;

import com.rfaelxs.web.domain.MovimentacaoReserva;
import com.rfaelxs.web.domain.TipoMovimentacaoReserva;
import com.rfaelxs.web.domain.TipoTransacao;
import com.rfaelxs.web.domain.Transacao;
import com.rfaelxs.web.service.ItemExtrato;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.UUID;

/**
 * Uma linha de "últimos lançamentos" da Gestão.
 *
 * @param id        id público da transação; nulo em movimentação de reserva
 * @param descricao a descrição, ou a categoria quando não há descrição; o nome da reserva em
 *                  movimentação
 * @param tipo      define o sinal e o chip na tela
 * @param origem    só ordena; não sai na API
 * @param idInterno só desempata a ordem; não sai na API
 */
public record Lancamento(
    UUID id,
    LocalDate data,
    String descricao,
    String categoria,
    boolean essencial,
    Tipo tipo,
    BigDecimal valor,
    ItemExtrato.Origem origem,
    Long idInterno) {

  /**
   * A mesma ordem do extrato: data mais recente primeiro; no mesmo dia, transações antes de
   * movimentações; dentro de cada origem, o lançado por último primeiro.
   */
  public static final Comparator<Lancamento> ORDEM =
      Comparator.comparing(Lancamento::data, Comparator.reverseOrder())
          .thenComparing(Lancamento::origem)
          .thenComparing(Lancamento::idInterno, Comparator.reverseOrder());

  /** Como o lançamento aparece na tela. */
  public enum Tipo {
    ENTRADA,
    SAIDA,
    APORTE,
    SAQUE
  }

  /** Linha de uma transação. */
  public static Lancamento de(Transacao t) {
    String descricao = t.getDescricao() == null || t.getDescricao().isBlank()
        ? t.getCategoria() : t.getDescricao();
    return new Lancamento(t.getPublicId(), t.getData(), descricao, t.getCategoria(),
        t.isEssencial(), t.getTipo() == TipoTransacao.ENTRADA ? Tipo.ENTRADA : Tipo.SAIDA,
        t.getValor(), ItemExtrato.Origem.TRANSACAO, t.getId());
  }

  /** Linha de uma movimentação, com o nome da reserva gravado no movimento. */
  public static Lancamento de(MovimentacaoReserva m) {
    return new Lancamento(null, m.getData(), m.getReservaNome(), ItemExtrato.CATEGORIA_RESERVA,
        false, m.getTipo() == TipoMovimentacaoReserva.ALOCACAO ? Tipo.APORTE : Tipo.SAQUE,
        m.getValor(), ItemExtrato.Origem.RESERVA, m.getId());
  }
}
