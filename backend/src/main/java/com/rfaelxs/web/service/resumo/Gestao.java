package com.rfaelxs.web.service.resumo;

import com.rfaelxs.web.domain.ContaPrevista;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Números da Gestão, sempre do mês corrente.
 *
 * @param livre          sobra menos as contas a vencer; pode ser negativo
 * @param porDia         livre dividido pelos dias restantes, com piso 1 no divisor
 * @param diasRestantes  último dia do mês menos hoje; zero no último dia
 * @param previstasTotal soma das contas ainda por vencer
 * @param previstas      contas ainda por vencer, da primeira para a última
 */
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
