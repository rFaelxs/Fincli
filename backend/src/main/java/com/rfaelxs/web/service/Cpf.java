package com.rfaelxs.web.service;

/**
 * Normalização e validação de CPF.
 *
 * <p>O CPF é metade da credencial de login (ADR 0007), então é validado pelos dígitos
 * verificadores e gravado só com dígitos.
 */
public final class Cpf {

  private Cpf() {
  }

  /** Remove tudo que não é dígito. Não valida. */
  public static String digitos(String entrada) {
    return entrada == null ? "" : entrada.replaceAll("\\D", "");
  }

  /**
   * Remove a máscara e valida os dígitos verificadores.
   *
   * @param entrada CPF com ou sem pontuação
   * @return os 11 dígitos, sem máscara
   * @throws ValorInvalidoException se o CPF estiver ausente ou for inválido
   */
  public static String normalizar(String entrada) {
    if (entrada == null || entrada.isBlank()) {
      throw new ValorInvalidoException("CPF é obrigatório.");
    }
    String digitos = digitos(entrada);
    if (!ehValido(digitos)) {
      throw new ValorInvalidoException("CPF inválido.");
    }
    return digitos;
  }

  private static boolean ehValido(String cpf) {
    // Sequências repetidas, como 111.111.111-11, passam no cálculo mas não são CPFs emitidos.
    if (cpf.length() != 11 || cpf.chars().distinct().count() == 1) {
      return false;
    }
    return digitoVerificador(cpf, 9) == cpf.charAt(9) - '0'
        && digitoVerificador(cpf, 10) == cpf.charAt(10) - '0';
  }

  /** Calcula pelo módulo 11 o dígito verificador da posição informada. */
  private static int digitoVerificador(String cpf, int posicao) {
    int soma = 0;
    for (int i = 0; i < posicao; i++) {
      soma += (cpf.charAt(i) - '0') * (posicao + 1 - i);
    }
    int resto = soma % 11;
    return resto < 2 ? 0 : 11 - resto;
  }
}
