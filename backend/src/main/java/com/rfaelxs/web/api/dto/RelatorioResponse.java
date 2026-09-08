package com.rfaelxs.web.api.dto;

import com.rfaelxs.web.service.ResumoService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Tela Relatório.
 *
 * @param historico     seis meses, do mais antigo para o mais recente, incluindo os vazios
 * @param topCategorias até quatro categorias de saída, da maior para a menor
 */
public record RelatorioResponse(
    String mes,
    BigDecimal entradas,
    BigDecimal saidas,
    BigDecimal essenciais,
    BigDecimal superfluas,
    BigDecimal aportes,
    BigDecimal sobra,
    List<SobraMes> historico,
    List<Categoria> topCategorias,
    List<Reserva> reservas,
    Projecao projecao) {

  /** Barra de altura proporcional no gráfico "sobra dos últimos 6 meses". */
  public record SobraMes(String mes, BigDecimal sobra) {
  }

  /** Uma linha de "o que puxou meu mês pra baixo". */
  public record Categoria(String nome, BigDecimal total) {
  }

  /**
   * Uma linha de "estou no ritmo das minhas metas?".
   *
   * @param previsao mês em que a meta fecha no ritmo atual, em {@code yyyy-MM}; nulo quando a
   *                 reserva está parada ou não tem meta
   */
  public record Reserva(
      UUID id,
      String nome,
      BigDecimal saldo,
      BigDecimal meta,
      BigDecimal progresso,
      BigDecimal aporteMedio,
      String previsao,
      boolean noRitmo) {
  }

  /** Os três números de "como fecho o mês se seguir assim?". */
  public record Projecao(BigDecimal jaSaiu, BigDecimal aindaVence, BigDecimal sobraPrevista) {
  }

  public static RelatorioResponse de(ResumoService.Relatorio r) {
    return new RelatorioResponse(
        r.mes().toString(),
        r.totais().entradas(),
        r.totais().saidas(),
        r.totais().essenciais(),
        r.totais().superfluas(),
        r.totais().aportes(),
        r.totais().sobra(),
        r.historico().stream()
            .map(h -> new SobraMes(h.mes().toString(), h.sobra()))
            .toList(),
        r.topCategorias().stream()
            .map(c -> new Categoria(c.categoria(), c.total()))
            .toList(),
        r.reservas().stream()
            .map(res -> new Reserva(
                res.id(),
                res.nome(),
                res.saldo(),
                res.meta(),
                res.progresso(),
                res.aporteMedio(),
                res.previsao() != null ? res.previsao().toString() : null,
                res.noRitmo()))
            .toList(),
        new Projecao(
            r.projecao().jaSaiu(), r.projecao().aindaVence(), r.projecao().sobraPrevista()));
  }
}
