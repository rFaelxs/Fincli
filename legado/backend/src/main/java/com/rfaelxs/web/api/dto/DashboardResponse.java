package com.rfaelxs.web.api.dto;

import com.rfaelxs.web.service.DashboardService;
import java.math.BigDecimal;
import java.util.List;

/**
 * Dashboard de Saúde Financeira.
 *
 * @param mes                 mês de referência, em ISO {@code yyyy-MM}
 * @param selicMetaAnual      meta Selic em % ao ano; {@code null} se a API do BCB estiver fora,
 *                            caso em que o SPA deve exibir "Indisponível"
 */
public record DashboardResponse(
    String mes,
    BigDecimal saldoDisponivel,
    BigDecimal entradasMes,
    BigDecimal saidasMes,
    BigDecimal progressoEmergencia,
    BigDecimal selicMetaAnual,
    List<ReservaResponse> reservas) {

  public static DashboardResponse de(String mes, DashboardService.Dashboard d) {
    return new DashboardResponse(
        mes,
        d.saldoDisponivel(),
        d.entradasMes(),
        d.saidasMes(),
        d.progressoEmergencia(),
        d.selicMetaAnual(),
        d.reservas().stream().map(ReservaResponse::de).toList());
  }
}
