package com.rfaelxs.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.time.LocalDate;

/** Instância Gson compartilhada com suporte a {@link LocalDate}. */
public final class GsonConfig {

  private GsonConfig() {
  }

  /** Gson configurado para serializar/desserializar {@link LocalDate} como ISO-8601. */
  public static final Gson GSON = new GsonBuilder()
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
}
