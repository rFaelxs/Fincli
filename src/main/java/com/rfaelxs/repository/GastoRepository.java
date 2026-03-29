package com.rfaelxs.repository;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.rfaelxs.model.Gasto;


public class GastoRepository {

    private static final String ARQUIVO = "gasto.json";
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, new TypeAdapter<LocalDate>() {
                @Override
                public void write(JsonWriter out, LocalDate value) throws IOException {
                    out.value(value.toString());
                }

                @Override
                public LocalDate read(JsonReader in) throws IOException {
                    return LocalDate.parse(in.nextString());
                }
            })
            .create();

    public void salvarGasto(List<Gasto> gastos) {
        String json = gson.toJson(gastos);
        try (FileWriter writer = new FileWriter(ARQUIVO)) {
            writer.write(json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<Gasto> carregarTodos() {
        File arquivo = new File(ARQUIVO);
        if (!arquivo.exists()) {
            return new ArrayList<>();
        }

        try (FileReader reader = new FileReader(ARQUIVO)) {
            Type tipo = new TypeToken<List<Gasto>>() {
            }.getType();
            return gson.fromJson(reader, tipo);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
