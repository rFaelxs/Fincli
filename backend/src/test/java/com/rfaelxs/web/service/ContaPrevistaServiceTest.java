package com.rfaelxs.web.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.rfaelxs.web.domain.ContaPrevista;
import com.rfaelxs.web.domain.Usuario;
import com.rfaelxs.web.repository.ContaPrevistaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Contas previstas: validação, recorrência e o corte por data de vencimento. */
@ExtendWith(MockitoExtension.class)
class ContaPrevistaServiceTest {

  @Mock private ContaPrevistaRepository repository;

  private ContaPrevistaService service;
  private Usuario usuario;

  @BeforeEach
  void setUp() {
    service = new ContaPrevistaService(repository);
    usuario = new Usuario("Teste", "11144477735", "hash");
  }

  @Test
  void deveRecusarValorNaoPositivo() {
    assertThrows(ValorInvalidoException.class, () ->
        service.criar(usuario, "Energia", BigDecimal.ZERO, 10, null));
  }

  @Test
  void deveRecusarDiaForaDoIntervalo() {
    assertThrows(ValorInvalidoException.class, () ->
        service.criar(usuario, "Energia", new BigDecimal("210.00"), 32, null));
  }

  @Test
  void deveNormalizarValorEmDuasCasas() {
    when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    ContaPrevista conta =
        service.criar(usuario, "Energia", new BigDecimal("210.005"), 22, null);

    assertEquals(new BigDecimal("210.01"), conta.getValor());
  }

  @Test
  void contaSemMesDeReferenciaEhRecorrente() {
    when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    ContaPrevista conta = service.criar(usuario, "Aluguel", new BigDecimal("1150"), 5, null);

    assertTrue(conta.isRecorrente());
    assertTrue(conta.valeNoMes(YearMonth.of(2026, 1)));
    assertTrue(conta.valeNoMes(YearMonth.of(2026, 9)));
  }

  @Test
  void contaAvulsaValeSoNoMesGravado() {
    when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    ContaPrevista conta =
        service.criar(usuario, "IPVA", new BigDecimal("840"), 15, YearMonth.of(2026, 3));

    assertFalse(conta.isRecorrente());
    assertTrue(conta.valeNoMes(YearMonth.of(2026, 3)));
    assertFalse(conta.valeNoMes(YearMonth.of(2026, 4)));
  }

  @Test
  void diaAlemDoTamanhoDoMesCaiNoUltimoDia() {
    when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    ContaPrevista conta = service.criar(usuario, "Cartão", new BigDecimal("980"), 31, null);

    assertEquals(LocalDate.of(2026, 2, 28), conta.vencimentoEm(YearMonth.of(2026, 2)));
    assertEquals(LocalDate.of(2026, 3, 31), conta.vencimentoEm(YearMonth.of(2026, 3)));
  }

  @Test
  void aVencerIgnoraContasJaVencidasMasMantemAsDeHoje() {
    YearMonth setembro = YearMonth.of(2026, 9);
    when(repository.doMes(any(), any())).thenReturn(List.of(
        conta("Aluguel", "1150", 5),
        conta("Cartão", "980", 16),
        conta("Energia", "210", 22)));

    List<ContaPrevista> aVencer =
        service.aVencer(usuario, setembro, LocalDate.of(2026, 9, 16));

    assertEquals(List.of("Cartão", "Energia"), aVencer.stream().map(ContaPrevista::getNome).toList());
    assertEquals(new BigDecimal("1190.00"), ContaPrevistaService.somar(aVencer));
  }

  @Test
  void somarSemContasDaZeroComDuasCasas() {
    assertEquals(new BigDecimal("0.00"), ContaPrevistaService.somar(List.of()));
  }

  private ContaPrevista conta(String nome, String valor, int dia) {
    return new ContaPrevista(usuario, nome, new BigDecimal(valor).setScale(2), dia, null);
  }
}
