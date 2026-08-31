package com.rfaelxs.service;

import com.rfaelxs.model.Reserva;
import com.rfaelxs.repository.SelicRepository;
import java.time.YearMonth;
import java.util.List;

/**
 * Agrega os dados para exibição do Dashboard de Saúde Financeira.
 * A meta Selic é obtida uma única vez por sessão (cache em memória).
 */
public class DashboardService {

  private final TransacaoService transacaoService;
  private final ReservaService reservaService;
  private final SelicRepository selicRepository;

  private Double selicCached;

  /**
   * Cria o serviço de dashboard com os serviços de transação, reserva e o repositório Selic.
   *
   * @param transacaoService serviço de transações do usuário logado
   * @param reservaService   serviço de reservas do usuário logado
   * @param selicRepository  repositório de consulta à API do BCB
   */
  public DashboardService(
      TransacaoService transacaoService,
      ReservaService reservaService,
      SelicRepository selicRepository) {
    this.transacaoService = transacaoService;
    this.reservaService = reservaService;
    this.selicRepository = selicRepository;
  }

  /**
   * Calcula o saldo disponível do usuário.
   *
   * @return saldo = sum(ENTRADA) - sum(SAIDA) - sum(saldoAtual das reservas)
   */
  public double obterSaldo() {
    return transacaoService.calcularSaldo(reservaService.listarReservas());
  }

  /**
   * Soma as entradas do mês informado.
   *
   * @param mes mês de referência
   * @return total de entradas no mês
   */
  public double totalEntradasMes(YearMonth mes) {
    return transacaoService.totalEntradasMes(mes);
  }

  /**
   * Soma as saídas do mês informado.
   *
   * @param mes mês de referência
   * @return total de saídas no mês
   */
  public double totalSaidasMes(YearMonth mes) {
    return transacaoService.totalSaidasMes(mes);
  }

  /**
   * Calcula o progresso percentual da Reserva de Emergência.
   *
   * @return percentual entre 0 e 100 (ou além de 100 se a meta foi superada);
   *         retorna 0 se a meta ainda não foi definida
   */
  public double progressoEmergencia() {
    return reservaService.listarReservas().stream()
        .filter(r -> ReservaService.ID_EMERGENCIA.equals(r.getId()))
        .findFirst()
        .map(r -> r.getMetaValor() == 0 ? 0.0 : r.getSaldoAtual() / r.getMetaValor() * 100)
        .orElse(0.0);
  }

  /**
   * Retorna a meta Selic anual vigente, usando cache de sessão.
   * Retorna {@code null} se a API do BCB estiver indisponível.
   *
   * @return meta Selic em % ao ano ou {@code null}
   */
  public Double obterSelic() {
    if (selicCached == null) {
      selicCached = selicRepository.obterTaxaAtual();
    }
    return selicCached;
  }

  /**
   * Retorna a lista de reservas com suas metas e saldos para o dashboard.
   *
   * @return lista de reservas
   */
  public List<Reserva> obterReservas() {
    return reservaService.listarReservas();
  }
}
