package com.rfaelxs.web.service.resumo;

import java.math.BigDecimal;
import java.time.YearMonth;

/** Sobra de um mês do histórico. */
public record SobraMensal(YearMonth mes, BigDecimal sobra) {
}
