package com.rfaelxs.web.service.resumo;

import com.rfaelxs.web.repository.TotalCategoria;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * Números do Relatório de um mês.
 *
 * @param historico     seis meses até o de referência, do mais antigo ao mais recente
 * @param topCategorias até quatro categorias de saída, da maior para a menor
 */
public record Relatorio(
    YearMonth mes,
    Totais totais,
    List<SobraMensal> historico,
    List<TotalCategoria> topCategorias,
    List<RitmoReserva> reservas,
    Projecao projecao) {

  /** Sobra de um mês do histórico; zero quando o mês não teve movimento. */
  public record SobraMensal(YearMonth mes, BigDecimal sobra) {
  }

  /**
   * Ritmo de uma reserva.
   *
   * @param aporteMedio alocações menos saques da janela de 3 meses, dividido por 3
   * @param previsao    mês em que a meta fecha nesse ritmo; nulo sem meta ou sem aporte
   * @param noRitmo     falso para reserva sem meta ou sem aporte na janela
   */
  public record RitmoReserva(
      UUID id,
      String nome,
      BigDecimal saldo,
      BigDecimal meta,
      BigDecimal progresso,
      BigDecimal aporteMedio,
      YearMonth previsao,
      boolean noRitmo) {
  }

  /**
   * Fechamento projetado do mês.
   *
   * @param jaSaiu        total de saídas do mês
   * @param aindaVence    contas ainda por vencer; zero em mês fechado
   * @param sobraPrevista sobra menos o que ainda vence
   */
  public record Projecao(BigDecimal jaSaiu, BigDecimal aindaVence, BigDecimal sobraPrevista) {
  }
}
