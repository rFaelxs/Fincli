package com.rfaelxs.model;

import java.time.LocalDate;
import java.util.UUID;

public class Gasto {

    UUID id;
    double valorGasto;
    String categoria;
    String descGasto;
    LocalDate dataGasto;

    public UUID getId() {
        return id;
    }

    public double getValorGasto() {
        return valorGasto;
    }

    public void setValorGasto(double valorGasto) {
        this.valorGasto = valorGasto;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public LocalDate getDataGasto() {
        return dataGasto;
    }

    public void setDataGasto(LocalDate dataGasto) {
        this.dataGasto = dataGasto;
    }

    public String getDescGasto() {
        return descGasto;
    }

    public void setDescGasto(String descGasto) {
        this.descGasto = descGasto;
    }

    public Gasto(double valorGasto, String categoria, String descGasto, LocalDate dataGasto ){
        this.id = UUID.randomUUID();
        this.valorGasto = valorGasto;
        this.categoria = categoria;
        this.descGasto = descGasto;
        this.dataGasto = dataGasto;

    }

    @Override
    public String toString() {
        return "Gasto{" +
                "Código do valor Gasto: " + id +
                ", Valor do Gasto: " + valorGasto +
                ", Tipo do Gasto: " + categoria +
                ", Descrição do Gasto: " + descGasto + '\'' +
                ", Data do Gasto: " + dataGasto +
                '}';
    }
}
