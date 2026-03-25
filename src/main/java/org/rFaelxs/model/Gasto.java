package org.rFaelxs.model;

import java.time.LocalDate;
import java.util.UUID;

public class Gasto {

    UUID cd_gasto;
    double vlr_gasto;
    int tp_gasto;
    String ds_gasto;
    LocalDate dt_gasto;

    public UUID getRelGasto() {
        return cd_gasto;
    }

    public void setRelgasto(UUID cd_gasto) {
        this.cd_gasto = cd_gasto;
    }

    public double getValorGasto() {
        return vlr_gasto;
    }

    public void setValoGasto(double vlr_gasto) {
        this.vlr_gasto = vlr_gasto;
    }

    public int getTipoGasto() {
        return tp_gasto;
    }

    public void setTipoGasto(int tp_gasto) {
        this.tp_gasto = tp_gasto;
    }

    public LocalDate getDataGasto() {
        return dt_gasto;
    }

    public void setDataGasto(LocalDate dt_gasto) {
        this.dt_gasto = dt_gasto;
    }

    public String getDescGasto() {
        return ds_gasto;
    }

    public void setDescGasto(String ds_gasto) {
        this.ds_gasto = ds_gasto;
    }

    @Override
    public String toString() {
        return "Gasto{" +
                "Código do valor Gasto: " + cd_gasto +
                ", Valor do Gasto: " + vlr_gasto +
                ", Tipo do Gasto: " + tp_gasto +
                ", Descrição do Gasto: " + ds_gasto + '\'' +
                ", Data do Gasto: " + dt_gasto +
                '}';
    }
}
