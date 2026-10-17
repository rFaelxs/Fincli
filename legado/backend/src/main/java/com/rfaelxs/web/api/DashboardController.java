package com.rfaelxs.web.api;

import com.rfaelxs.web.api.dto.DashboardResponse;
import com.rfaelxs.web.api.dto.ExtratoItemResponse;
import com.rfaelxs.web.domain.Usuario;
import com.rfaelxs.web.service.DashboardService;
import com.rfaelxs.web.service.ReservaService;
import com.rfaelxs.web.service.TransacaoService;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Dashboard e extrato consolidado. */
@RestController
@RequestMapping("/api")
public class DashboardController {

  private final DashboardService dashboardService;
  private final TransacaoService transacaoService;
  private final ReservaService reservaService;

  public DashboardController(
      DashboardService dashboardService,
      TransacaoService transacaoService,
      ReservaService reservaService) {
    this.dashboardService = dashboardService;
    this.transacaoService = transacaoService;
    this.reservaService = reservaService;
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

  /**
   * Extrato cronológico: transações e movimentações de reserva na mesma lista.
   *
   * @return itens do mais recente para o mais antigo
   */
  @GetMapping("/extrato")
  public List<ExtratoItemResponse> extrato(@UsuarioAtual Usuario usuario) {
    return Stream.concat(
            transacaoService.listar(usuario).stream().map(ExtratoItemResponse::de),
            reservaService.listarMovimentacoes(usuario).stream().map(ExtratoItemResponse::de))
        .sorted(Comparator.comparing(ExtratoItemResponse::data).reversed())
        .toList();
  }
}
