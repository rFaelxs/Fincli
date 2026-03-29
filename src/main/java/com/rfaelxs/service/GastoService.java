package com.rfaelxs.service;

import com.rfaelxs.model.Gasto;
import com.rfaelxs.repository.GastoRepository;

import java.time.LocalDate;
import java.util.List;

public class GastoService {

    private GastoRepository repository;
    private List<Gasto> gastos;

//    public void carregarRepositorio(GastoRepository repository, List<Gasto> gastos) {
//        repository.carregarTodos(gastos);
//    }

    public void adicionarGasto(double valor, String categoria, String descricao, LocalDate data) {
        if (valor >= 0) {
            Gasto gasto = new Gasto(valor, categoria, descricao, data);
            gastos.add(gasto);
            return salvarTudo(gasto);
        }

    }

}
