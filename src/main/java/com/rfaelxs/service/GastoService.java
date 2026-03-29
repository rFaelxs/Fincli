package com.rfaelxs.service;

import com.rfaelxs.model.Gasto;
import com.rfaelxs.repository.GastoRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class GastoService {

    private GastoRepository repository;
    private List<Gasto> gastos;


    public GastoService(GastoRepository repository) {
        this.repository = repository;
        this.gastos = repository.carregarTodos();

    }

    public void adicionarGasto(double valor, String categoria, String descricao, LocalDate data) {
        if (valor >= 0) {
            Gasto gasto = new Gasto(valor, categoria, descricao, data);
            gastos.add(gasto);
            repository.salvarGasto(gastos);
        }
    }

    public List<Gasto> listarGastos() {
        return gastos;
    }

    public void removerGasto(UUID id) {
        this.gastos.removeIf(g -> g.getId().equals(id));
        this.repository.salvarGasto(gastos);

    }

    public Map<String, Double> resumoPorCategoria() {
        return gastos.stream().collect(Collectors.groupingBy(Gasto::getCategoria, Collectors.summingDouble(Gasto::getValorGasto)));
    }


}


