package com.rfaelxs.web.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;

class ContaPrevistaTest {

  private final Usuario usuario = new Usuario("Rafael", "52998224725", "hash");

  // Item 24 e CA-GESTAO-22
  @Test
  void dia31EmFevereiroComumCaiNoDia28() {
    assertThat(conta(31, null).vencimentoEm(YearMonth.of(2026, 2)))
        .isEqualTo(LocalDate.of(2026, 2, 28));
  }

  @Test
  void dia31EmFevereiroBissextoCaiNoDia29() {
    assertThat(conta(31, null).vencimentoEm(YearMonth.of(2028, 2)))
        .isEqualTo(LocalDate.of(2028, 2, 29));
  }

  @Test
  void diaQueCabeNoMesNaoMuda() {
    assertThat(conta(22, null).vencimentoEm(YearMonth.of(2026, 9)))
        .isEqualTo(LocalDate.of(2026, 9, 22));
  }

  @Test
  void contaSemMesEhRecorrenteEComMesEhAvulsa() {
    assertThat(conta(22, null).isRecorrente()).isTrue();
    assertThat(conta(22, null).getMesReferencia()).isNull();

    ContaPrevista avulsa = conta(22, YearMonth.of(2026, 9));
    assertThat(avulsa.isRecorrente()).isFalse();
    assertThat(avulsa.getMesReferencia()).isEqualTo(YearMonth.of(2026, 9));
  }

  private ContaPrevista conta(int dia, YearMonth mes) {
    return new ContaPrevista(usuario, "Energia", new BigDecimal("210.00"), dia, mes);
  }
}
