package com.rfaelxs.web.api;

import com.rfaelxs.web.api.dto.DashboardResponse;
import com.rfaelxs.web.domain.Usuario;
import com.rfaelxs.web.service.DashboardService;
import java.time.YearMonth;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Dashboard de Saúde Financeira. */
@RestController
@RequestMapping("/api")
public class DashboardController {

  private final DashboardService dashboardService;

  public DashboardController(DashboardService dashboardService) {
    this.dashboardService = dashboardService;
  }

  /**
   * Números do Dashboard de Saúde Financeira.
   *
   * @param mes mês de referência em {@code yyyy-MM}; ausente significa o mês corrente
   */
  @GetMapping("/dashboard")
  public DashboardResponse dashboard(
      @UsuarioAtual Usuario usuario,
      @RequestParam(required = false)
      @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes) {
    YearMonth referencia = mes != null ? mes : YearMonth.now();
    return DashboardResponse.de(referencia.toString(), dashboardService.montar(usuario, referencia));
  }
}
