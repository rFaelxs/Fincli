package com.rfaelxs.web.service.resumo;

import com.rfaelxs.web.domain.ContaPrevista;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/** Números da tela Gestão. */
public record Gestao(
    LocalDate hoje,
    YearMonth mes,
    BigDecimal livre,
    BigDecimal porDia,
    int diasRestantes,
    Totais totais,
    BigDecimal previstasTotal,
    List<Lancamento> ultimosLancamentos,
    List<ContaPrevista> previstas) {
}
