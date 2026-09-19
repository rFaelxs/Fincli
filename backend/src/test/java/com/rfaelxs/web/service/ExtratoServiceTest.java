package com.rfaelxs.web.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.rfaelxs.web.domain.MovimentacaoReserva;
import com.rfaelxs.web.domain.Reserva;
import com.rfaelxs.web.domain.TipoMovimentacaoReserva;
import com.rfaelxs.web.domain.TipoTransacao;
import com.rfaelxs.web.domain.Transacao;
import com.rfaelxs.web.domain.Usuario;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Extrato: transações e movimentações de reserva numa lista só.
 *
 * <p>Escrito antes de a composição sair do controller, contra o comportamento que ela tinha lá
 * — é a rede da mudança de lugar.
 */
@ExtendWith(MockitoExtension.class)
class ExtratoServiceTest {

  private static final LocalDate DIA_16 = LocalDate.of(2026, 9, 16);
  private static final LocalDate DIA_17 = LocalDate.of(2026, 9, 17);
  private static final LocalDate DIA_18 = LocalDate.of(2026, 9, 18);

  @Mock private TransacaoService transacaoService;
  @Mock private ReservaService reservaService;

  private ExtratoService service;
  private Usuario usuario;
  private Reserva viagem;

  @BeforeEach
  void setUp() {
    service = new ExtratoService(transacaoService, reservaService);
    usuario = new Usuario("Teste", "11144477735", "hash");
    viagem = new Reserva(usuario, "Viagem", new BigDecimal("8000.00"), false);
  }

  @Test
  void deveJuntarTransacoesEMovimentacoesDoMaisRecenteParaOMaisAntigo() {
    when(transacaoService.listar(usuario)).thenReturn(List.of(
        saida("Mercado", DIA_17), entrada("Freela", DIA_16)));
    when(reservaService.listarMovimentacoes(usuario)).thenReturn(List.of(
        movimento(TipoMovimentacaoReserva.ALOCACAO, DIA_18)));

    List<ItemExtrato> itens = service.listar(usuario);

    assertEquals(List.of(DIA_18, DIA_17, DIA_16), itens.stream().map(ItemExtrato::data).toList());
    assertEquals(
        List.of("RESERVA", "TRANSACAO", "TRANSACAO"),
        itens.stream().map(ItemExtrato::origem).toList());
  }

  /** A ordenação é estável: no mesmo dia, as transações vêm antes das movimentações. */
  @Test
  void deveManterTransacoesAntesDasMovimentacoesNoMesmoDia() {
    when(transacaoService.listar(usuario)).thenReturn(List.of(
        saida("Mercado", DIA_16), entrada("Freela", DIA_16)));
    when(reservaService.listarMovimentacoes(usuario)).thenReturn(List.of(
        movimento(TipoMovimentacaoReserva.ALOCACAO, DIA_16)));

    List<ItemExtrato> itens = service.listar(usuario);

    assertEquals(
        List.of("Mercado", "Freela", "Viagem"),
        itens.stream().map(ItemExtrato::descricao).toList());
  }

  @Test
  void deveDescreverTransacaoComTipoCategoriaEDescricao() {
    when(transacaoService.listar(usuario)).thenReturn(List.of(saida("Mercado", DIA_16)));
    when(reservaService.listarMovimentacoes(usuario)).thenReturn(List.of());

    ItemExtrato item = service.listar(usuario).get(0);

    assertEquals(
        new ItemExtrato(
            "TRANSACAO", DIA_16, new BigDecimal("120.00"), "SAIDA", "Alimentação", "Mercado"),
        item);
  }

  /** Movimentação leva o nome da reserva no momento do movimento e a categoria "Reserva". */
  @Test
  void deveDescreverMovimentacaoPeloNomeDaReserva() {
    when(transacaoService.listar(usuario)).thenReturn(List.of());
    when(reservaService.listarMovimentacoes(usuario)).thenReturn(List.of(
        movimento(TipoMovimentacaoReserva.SAQUE, DIA_16)));
    viagem.setNome("Viagem Japão");

    ItemExtrato item = service.listar(usuario).get(0);

    assertEquals(
        new ItemExtrato("RESERVA", DIA_16, new BigDecimal("300.00"), "SAQUE", "Reserva", "Viagem"),
        item);
  }

  @Test
  void deveDevolverListaVaziaSemLancamentos() {
    when(transacaoService.listar(usuario)).thenReturn(List.of());
    when(reservaService.listarMovimentacoes(usuario)).thenReturn(List.of());

    assertTrue(service.listar(usuario).isEmpty());
  }

  private Transacao saida(String descricao, LocalDate data) {
    return new Transacao(
        usuario, new BigDecimal("120.00"), "Alimentação", descricao, data,
        TipoTransacao.SAIDA, true);
  }

  private Transacao entrada(String descricao, LocalDate data) {
    return new Transacao(
        usuario, new BigDecimal("450.00"), "Trabalho", descricao, data,
        TipoTransacao.ENTRADA, false);
  }

  private MovimentacaoReserva movimento(TipoMovimentacaoReserva tipo, LocalDate data) {
    return new MovimentacaoReserva(usuario, viagem, new BigDecimal("300.00"), tipo, data);
  }
}
