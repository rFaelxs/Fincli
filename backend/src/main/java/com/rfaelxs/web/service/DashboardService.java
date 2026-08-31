package com.rfaelxs.web.service;

import com.rfaelxs.web.domain.Reserva;
import com.rfaelxs.web.domain.Usuario;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Agrega os números do Dashboard de Saúde Financeira. */
@Service
public class DashboardService {

  private final TransacaoService transacaoService;
  private final ReservaService reservaService;
  private final SelicService selicService;

  public DashboardService(
      TransacaoService transacaoService,
      ReservaService reservaService,
      SelicService selicService) {
    this.transacaoService = transacaoService;
    this.reservaService = reservaService;
    this.selicService = selicService;
  }

  /**
   * Monta o dashboard do mês informado.
   *
   * @param usuario dono dos dados
   * @param mes     mês de referência dos totais
   * @return os números agregados; {@code selic} vem nulo se a API do BCB estiver fora
   */
  @Transactional(readOnly = true)
  public Dashboard montar(Usuario usuario, YearMonth mes) {
    List<Reserva> reservas = reservaService.listar(usuario);
    Reserva emergencia = reservas.stream().filter(Reserva::isEmergencia).findFirst().orElse(null);

    return new Dashboard(
        transacaoService.calcularSaldoDisponivel(usuario),
        transacaoService.totalEntradasMes(usuario, mes),
        transacaoService.totalSaidasMes(usuario, mes),
        progresso(emergencia),
        selicService.obterMetaAnual(),
        reservas);
  }

  /**
   * Percentual atingido da Reserva de Emergência.
   *
   * @return 0 se não houver reserva ou a meta ainda não estiver definida; pode passar de 100
   */
  private BigDecimal progresso(Reserva emergencia) {
    if (emergencia == null || emergencia.getMetaValor().signum() == 0) {
      return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
    }
    return emergencia
        .getSaldoAtual()
        .multiply(BigDecimal.valueOf(100))
        .divide(emergencia.getMetaValor(), 1, RoundingMode.HALF_UP);
  }

  /** Números agregados do dashboard. */
  public record Dashboard(
      BigDecimal saldoDisponivel,
      BigDecimal entradasMes,
      BigDecimal saidasMes,
      BigDecimal progressoEmergencia,
      BigDecimal selicMetaAnual,
      List<Reserva> reservas) {
  }
}
