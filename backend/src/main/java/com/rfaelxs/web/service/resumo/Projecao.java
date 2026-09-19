package com.rfaelxs.web.service.resumo;

import java.math.BigDecimal;

/** Fechamento projetado do mês. */
public record Projecao(BigDecimal jaSaiu, BigDecimal aindaVence, BigDecimal sobraPrevista) {
}
