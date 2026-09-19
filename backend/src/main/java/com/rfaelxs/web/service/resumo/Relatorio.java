package com.rfaelxs.web.service.resumo;

import com.rfaelxs.web.repository.TotalCategoria;
import java.time.YearMonth;
import java.util.List;

/** Números da tela Relatório. */
public record Relatorio(
    YearMonth mes,
    Totais totais,
    List<SobraMensal> historico,
    List<TotalCategoria> topCategorias,
    List<RitmoReserva> reservas,
    Projecao projecao) {
}
