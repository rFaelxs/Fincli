package com.rfaelxs.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Fábrica de conexões JDBC lendo {@code DATABASE_URL} do ambiente. */
public final class DatabaseConfig {

  private DatabaseConfig() {
  }

  /**
   * Cria uma nova conexão JDBC a partir da variável de ambiente {@code DATABASE_URL}.
   *
   * @return conexão aberta com o banco
   * @throws SQLException se a conexão falhar
   */
  public static Connection getConnection() throws SQLException {
    String url = System.getenv("DATABASE_URL");
    if (url == null || url.isEmpty()) {
      throw new IllegalStateException("DATABASE_URL não configurada");
    }
    return DriverManager.getConnection(url);
  }
}
