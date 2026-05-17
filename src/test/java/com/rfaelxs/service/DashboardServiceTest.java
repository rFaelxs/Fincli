package com.rfaelxs.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rfaelxs.model.Reserva;
import com.rfaelxs.repository.SelicRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

  @Mock
  private TransacaoService transacaoService;

  @Mock
  private ReservaService reservaService;

  @Mock
  private SelicRepository selicRepository;

  private DashboardService criarService() {
    return new DashboardService(transacaoService, reservaService, selicRepository);
  }

  @Test
  void deveCalcularProgressoEmergencia() {
    Reserva emergencia = new Reserva(ReservaService.ID_EMERGENCIA, "Emergência", 10000.0, true);
    emergencia.setSaldoAtual(2500.0);
    when(reservaService.listarReservas()).thenReturn(List.of(emergencia));

    double progresso = criarService().progressoEmergencia();

    assertEquals(25.0, progresso, 0.001);
  }

  @Test
  void deveRetornarZeroParaMetaZero() {
    Reserva emergencia = new Reserva(ReservaService.ID_EMERGENCIA, "Emergência", 0.0, true);
    when(reservaService.listarReservas()).thenReturn(List.of(emergencia));

    double progresso = criarService().progressoEmergencia();

    assertEquals(0.0, progresso, 0.001);
  }

  @Test
  void deveCachearSelic() {
    when(selicRepository.obterTaxaAtual()).thenReturn(10.75);
    DashboardService dashboard = criarService();

    dashboard.obterSelic();
    dashboard.obterSelic();

    verify(selicRepository, times(1)).obterTaxaAtual();
  }

  @Test
  void deveRetornarNullQuandoSelicIndisponivel() {
    when(selicRepository.obterTaxaAtual()).thenReturn(null);

    assertNull(criarService().obterSelic());
  }
}
