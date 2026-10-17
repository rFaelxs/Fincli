package com.rfaelxs.web.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ReservaTest {

  private final Usuario usuario = new Usuario("Rafael", "52998224725", "hash");

  // CA-RES-03
  @Test
  void progressoComUmaCasa() {
    Reserva reserva = new Reserva(usuario, "Viagem", new BigDecimal("10000.00"), false);
    reserva.alocar(new BigDecimal("1250.00"));

    assertThat(reserva.progresso()).isEqualTo(new BigDecimal("12.5"));
  }

  // CA-RES-04
  @Test
  void progressoPassaDeCem() {
    Reserva reserva = new Reserva(usuario, "Viagem", new BigDecimal("10000.00"), false);
    reserva.alocar(new BigDecimal("12000.00"));

    assertThat(reserva.progresso()).isEqualTo(new BigDecimal("120.0"));
  }

  // CA-RES-05
  @Test
  void progressoZeroSemMeta() {
    Reserva reserva = Reserva.emergenciaDe(usuario);
    reserva.alocar(new BigDecimal("500.00"));

    assertThat(reserva.progresso()).isEqualTo(new BigDecimal("0.0"));
  }
}
