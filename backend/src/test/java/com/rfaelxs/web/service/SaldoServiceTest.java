package com.rfaelxs.web.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.rfaelxs.web.domain.TipoTransacao;
import com.rfaelxs.web.repository.ReservaRepository;
import com.rfaelxs.web.repository.TransacaoRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SaldoServiceTest {

  @Mock
  private TransacaoRepository transacaoRepository;

  @Mock
  private ReservaRepository reservaRepository;

  @InjectMocks
  private SaldoService service;

  // CA-RES-24
  @Test
  void entradasMenosSaidasMenosOGuardado() {
    somas("5000.00", "2000.00", "1000.00");

    assertThat(service.disponivel(7L)).isEqualByComparingTo("2000.00").hasScaleOf(2);
  }

  @Test
  void podeSerNegativo() {
    somas("100.00", "300.00", "0");

    assertThat(service.disponivel(7L)).isEqualByComparingTo("-200.00");
  }

  private void somas(String entradas, String saidas, String guardado) {
    when(transacaoRepository.somarPorTipo(7L, TipoTransacao.ENTRADA))
        .thenReturn(new BigDecimal(entradas));
    when(transacaoRepository.somarPorTipo(7L, TipoTransacao.SAIDA))
        .thenReturn(new BigDecimal(saidas));
    when(reservaRepository.somarSaldo(7L)).thenReturn(new BigDecimal(guardado));
  }
}
