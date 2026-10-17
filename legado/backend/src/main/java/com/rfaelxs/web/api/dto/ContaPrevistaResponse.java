package com.rfaelxs.web.api.dto;

import com.rfaelxs.web.domain.ContaPrevista;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

/**
 * Conta que ainda vai vencer.
 *
 * @param vencimento data já ajustada ao tamanho do mês — dia 31 em fevereiro vira o dia 28
 */
public record ContaPrevistaResponse(
    UUID id,
    String nome,
    BigDecimal valor,
    int diaVencimento,
    LocalDate vencimento,
    boolean recorrente) {

  public static ContaPrevistaResponse de(ContaPrevista c, YearMonth mes) {
    return new ContaPrevistaResponse(
        c.getPublicId(),
        c.getNome(),
        c.getValor(),
        c.getDiaVencimento(),
        c.vencimentoEm(mes),
        c.isRecorrente());
  }
}
