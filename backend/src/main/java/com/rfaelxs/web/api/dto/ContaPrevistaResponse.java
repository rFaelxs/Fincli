package com.rfaelxs.web.api.dto;

import com.rfaelxs.web.domain.ContaPrevista;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

/**
 * Conta prevista dentro de um mês.
 *
 * @param vencimento data já ajustada ao tamanho do mês: dia 31 em fevereiro vira o último dia
 * @param recorrente o "desfazer" usa para decidir se manda {@code mesReferencia}
 */
public record ContaPrevistaResponse(
    UUID id,
    String nome,
    BigDecimal valor,
    int diaVencimento,
    LocalDate vencimento,
    boolean recorrente) {

  /** Converte a conta, calculando o vencimento no mês informado. */
  public static ContaPrevistaResponse de(ContaPrevista c, YearMonth mes) {
    return new ContaPrevistaResponse(c.getPublicId(), c.getNome(), c.getValor(),
        c.getDiaVencimento(), c.vencimentoEm(mes), c.isRecorrente());
  }
}
