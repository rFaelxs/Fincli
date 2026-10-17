package com.rfaelxs.web.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** O CLI aceitava qualquer texto como CPF; aqui ele é credencial e precisa ser válido. */
class CpfTest {

  @Test
  void deveRemoverMascara() {
    assertEquals("11144477735", Cpf.normalizar("111.444.777-35"));
  }

  @Test
  void deveAceitarSemMascara() {
    assertEquals("11144477735", Cpf.normalizar("11144477735"));
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "12345678900",   // dígitos verificadores errados
      "111444777",     // curto demais
      "111444777351",  // longo demais
      "11111111111",   // todos os dígitos iguais
      "00000000000",
      ""
  })
  void deveRecusarCpfInvalido(String entrada) {
    assertThrows(ValorInvalidoException.class, () -> Cpf.normalizar(entrada));
  }

  @Test
  void deveRecusarNulo() {
    assertThrows(ValorInvalidoException.class, () -> Cpf.normalizar(null));
  }
}
