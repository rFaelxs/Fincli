package com.rfaelxs.repository;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;

/**
 * Consulta a taxa Selic atual via API pública do Banco Central do Brasil.
 * Em caso de falha de rede, retorna {@code null} sem lançar exceção.
 */
public class SelicRepository {

  private static final String URL_BCB =
      "https://api.bcb.gov.br/dados/serie/bcdata.sgs.11/dados/ultimos/1?formato=json";
  private static final int TIMEOUT_MS = 5000;

  /**
   * Retorna a taxa Selic anual mais recente.
   *
   * @return valor percentual (ex: 10.5) ou {@code null} se a API estiver indisponível
   */
  public Double obterTaxaAtual() {
    try {
      HttpURLConnection conexao = (HttpURLConnection) URI.create(URL_BCB).toURL().openConnection();
      conexao.setConnectTimeout(TIMEOUT_MS);
      conexao.setReadTimeout(TIMEOUT_MS);
      conexao.setRequestMethod("GET");

      if (conexao.getResponseCode() != HttpURLConnection.HTTP_OK) {
        return null;
      }

      try (InputStreamReader reader = new InputStreamReader(conexao.getInputStream())) {
        JsonArray array = JsonParser.parseReader(reader).getAsJsonArray();
        String valor = array.get(0).getAsJsonObject().get("valor").getAsString();
        return Double.parseDouble(valor);
      }
    } catch (Exception e) {
      return null;
    }
  }
}
