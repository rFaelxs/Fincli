package com.rfaelxs.web.api.dto;

import com.rfaelxs.web.service.ResumoService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Tela Hoje.
 *
 * @param livre          o número grande: sobra do mês já descontadas as contas a vencer; pode
 *                       ser negativo, e nesse caso a tela muda de cor e de texto
 * @param porDia         quanto dá para gastar por dia até o fim do mês
 * @param diasRestantes  dias entre hoje e o último dia do mês; zero no último dia
 * @param fimDoMes       usado no rótulo "até 30 de setembro"
 */
public record HojeResponse(
    LocalDate hoje,
    String mes,
    LocalDate fimDoMes,
    int diasRestantes,
    BigDecimal livre,
    BigDecimal porDia,
    BigDecimal entradas,
    BigDecimal saidas,
    BigDecimal essenciais,
    BigDecimal superfluas,
    BigDecimal aportes,
    BigDecimal sobra,
    BigDecimal previstasTotal,
    List<LancamentoResponse> ultimosLancamentos,
    List<ContaPrevistaResponse> previstas) {

  public static HojeResponse de(ResumoService.Hoje h) {
    return new HojeResponse(
        h.hoje(),
        h.mes().toString(),
        h.mes().atEndOfMonth(),
        h.diasRestantes(),
        h.livre(),
        h.porDia(),
        h.totais().entradas(),
        h.totais().saidas(),
        h.totais().essenciais(),
        h.totais().superfluas(),
        h.totais().aportes(),
        h.totais().sobra(),
        h.previstasTotal(),
        h.ultimosLancamentos().stream().map(LancamentoResponse::de).toList(),
        h.previstas().stream().map(c -> ContaPrevistaResponse.de(c, h.mes())).toList());
  }
}
