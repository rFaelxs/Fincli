package com.rfaelxs.repository;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.rfaelxs.model.Gasto;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;


public class GastoRepository {


    private static final String ARQUIVO = "gasto.json";
    Gson gson = new Gson();

    public void salvarGasto(List<Gasto> gastos) {
        String json = gson.toJson(gastos);
        try (FileWriter writer = new FileWriter(ARQUIVO)) {
            writer.write(json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<Gasto> carregarTodos() {
        //Parte 1: Verifica se arquivo existe
        File arquivo = new File(ARQUIVO);
        if (!arquivo.exists()) {
            return new ArrayList<>();
        }

        //Parte 2: Leitura e conversão
        try (FileReader reader = new FileReader(ARQUIVO)) {
            Type tipo = new TypeToken<List<Gasto>>() {
            }.getType();
            return gson.fromJson(reader, tipo);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
