package com.rfaelxs.web.service;

/**
 * Normalização e validação de CPF.
 *
 * <p>O CLI aceitava qualquer texto como CPF. Como aqui o CPF é a credencial de login
 * (escopo-web.md §1.1), ele passa a ser validado pelos dígitos verificadores.
 */
public final class Cpf {

  private Cpf() {
  }

  /**
   * Remove máscara e valida os dígitos verificadores.
   *
   * @param entrada CPF com ou sem pontuação
   * @return os 11 dígitos, sem máscara
   * @throws ValorInvalidoException se o CPF for inválido
   */
  public static String normalizar(String entrada) {
    if (entrada == null) {
      throw new ValorInvalidoException("CPF é obrigatório.");
    }
    String digitos = entrada.replaceAll("\\D", "");
    if (!ehValido(digitos)) {
      throw new ValorInvalidoException("CPF inválido.");
    }
    return digitos;
  }

  private static boolean ehValido(String cpf) {
    if (cpf.length() != 11 || cpf.chars().distinct().count() == 1) {
      return false;
    }
    return digitoVerificador(cpf, 9) == cpf.charAt(9) - '0'
        && digitoVerificador(cpf, 10) == cpf.charAt(10) - '0';
  }

  /** Calcula o dígito verificador da posição informada pelo módulo 11. */
  private static int digitoVerificador(String cpf, int posicao) {
    int soma = 0;
    for (int i = 0; i < posicao; i++) {
      soma += (cpf.charAt(i) - '0') * (posicao + 1 - i);
    }
    int resto = soma % 11;
    return resto < 2 ? 0 : 11 - resto;
  }
}
