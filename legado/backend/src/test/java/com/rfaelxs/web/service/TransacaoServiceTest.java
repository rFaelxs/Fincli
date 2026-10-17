package com.rfaelxs.web.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.rfaelxs.web.domain.TipoTransacao;
import com.rfaelxs.web.domain.Usuario;
import com.rfaelxs.web.repository.ReservaRepository;
import com.rfaelxs.web.repository.TransacaoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Regras de transação portadas do CLI, agora sobre BigDecimal e sem estado no serviço. */
@ExtendWith(MockitoExtension.class)
class TransacaoServiceTest {

  @Mock private TransacaoRepository transacaoRepository;
  @Mock private ReservaRepository reservaRepository;

  private TransacaoService service;
  private Usuario usuario;

  @BeforeEach
  void setUp() {
    service = new TransacaoService(transacaoRepository, reservaRepository);
    usuario = new Usuario("Teste", "11144477735", "hash");
  }

  @Test
  void deveRecusarValorNegativo() {
    assertThrows(ValorInvalidoException.class, () ->
        service.adicionar(
            usuario, new BigDecimal("-10.00"), "Lazer", "Cinema",
            LocalDate.now(), TipoTransacao.SAIDA, false));
  }

  @Test
  void deveAceitarValorZero() {
    when(transacaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    assertEquals(
        new BigDecimal("0.00"),
        service.adicionar(
            usuario, BigDecimal.ZERO, "Ajuste", null,
            LocalDate.now(), TipoTransacao.ENTRADA, false).getValor());
  }

  @Test
  void deveCalcularSaldoDescontandoReservas() {
    when(transacaoRepository.somarPorTipo(any(), eq(TipoTransacao.ENTRADA)))
        .thenReturn(new BigDecimal("1000.00"));
    when(transacaoRepository.somarPorTipo(any(), eq(TipoTransacao.SAIDA)))
        .thenReturn(new BigDecimal("200.00"));
    when(reservaRepository.somarSaldoAlocado(any())).thenReturn(new BigDecimal("300.00"));

    assertEquals(new BigDecimal("500.00"), service.calcularSaldoDisponivel(usuario));
  }

  @Test
  void deveNormalizarSaldoEmDuasCasas() {
    when(transacaoRepository.somarPorTipo(any(), eq(TipoTransacao.ENTRADA)))
        .thenReturn(new BigDecimal("0.1"));
    when(transacaoRepository.somarPorTipo(any(), eq(TipoTransacao.SAIDA)))
        .thenReturn(BigDecimal.ZERO);
    when(reservaRepository.somarSaldoAlocado(any())).thenReturn(BigDecimal.ZERO);

    assertEquals(new BigDecimal("0.10"), service.calcularSaldoDisponivel(usuario));
  }

  @Test
  void deveSomarEntradasDoMesPeloPeriodoCorreto() {
    YearMonth agosto = YearMonth.of(2026, 8);
    when(transacaoRepository.somarPorTipoNoPeriodo(
            any(),
            eq(TipoTransacao.ENTRADA),
            eq(LocalDate.of(2026, 8, 1)),
            eq(LocalDate.of(2026, 8, 31))))
        .thenReturn(new BigDecimal("3500.00"));

    assertEquals(new BigDecimal("3500.00"), service.totalEntradasMes(usuario, agosto));
  }

  @Test
  void deveFalharAoEditarTransacaoDeOutroUsuario() {
    when(transacaoRepository.findByPublicIdAndUsuarioId(any(), any()))
        .thenReturn(java.util.Optional.empty());

    assertThrows(RecursoNaoEncontradoException.class, () ->
        service.editar(
            usuario, java.util.UUID.randomUUID(), BigDecimal.TEN, "X", null,
            LocalDate.now(), TipoTransacao.SAIDA, false));
  }
}
