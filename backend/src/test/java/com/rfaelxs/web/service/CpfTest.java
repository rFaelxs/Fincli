package com.rfaelxs.web.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class CpfTest {

  @Test
  void aceitaCpfComMascaraEDevolveSoOsDigitos() {
    assertThat(Cpf.normalizar("529.982.247-25")).isEqualTo("52998224725");
  }

  @Test
  void aceitaCpfSemMascara() {
    assertThat(Cpf.normalizar("12345678909")).isEqualTo("12345678909");
  }

  @ParameterizedTest
  @ValueSource(strings = {"111.111.111-11", "529.982.247-24", "5299822472", "529982247250"})
  void recusaCpfInvalido(String cpf) {
    assertThatThrownBy(() -> Cpf.normalizar(cpf))
        .isInstanceOf(ValorInvalidoException.class)
        .hasMessage("CPF inválido.");
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  void recusaCpfAusente(String cpf) {
    assertThatThrownBy(() -> Cpf.normalizar(cpf))
        .isInstanceOf(ValorInvalidoException.class)
        .hasMessage("CPF é obrigatório.");
  }

  @Test
  void digitosNaoValidaENaoQuebraComNulo() {
    assertThat(Cpf.digitos("abc.123")).isEqualTo("123");
    assertThat(Cpf.digitos(null)).isEmpty();
  }
}
