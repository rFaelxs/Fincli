package com.rfaelxs.web.api.dto;

import com.rfaelxs.web.service.resumo.Gestao;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Tela Gestão. Datas em {@code yyyy-MM-dd}, mês em {@code yyyy-MM}, valores em duas casas.
 *
 * @param livre         o número grande; negativo põe a tela no vermelho
 * @param porDia        quanto dá para gastar por dia até o fim do mês
 * @param diasRestantes zero no último dia do mês
 * @param fimDoMes      usado no rótulo "até 30 de setembro"
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

  /** Achata os totais e converte as listas. */
  public static HojeResponse de(Gestao g) {
    return new HojeResponse(
        g.hoje(),
        g.mes().toString(),
        g.mes().atEndOfMonth(),
        g.diasRestantes(),
        g.livre(),
        g.porDia(),
        g.totais().entradas(),
        g.totais().saidas(),
        g.totais().essenciais(),
        g.totais().superfluas(),
        g.totais().aportes(),
        g.totais().sobra(),
        g.previstasTotal(),
        g.ultimosLancamentos().stream().map(LancamentoResponse::de).toList(),
        g.previstas().stream().map(c -> ContaPrevistaResponse.de(c, g.mes())).toList());
  }
}
