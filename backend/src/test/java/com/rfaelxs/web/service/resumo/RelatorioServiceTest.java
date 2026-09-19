package com.rfaelxs.web.service.resumo;

import static com.rfaelxs.web.service.resumo.DadosDeResumo.SETEMBRO;
import static com.rfaelxs.web.service.resumo.DadosDeResumo.entrada;
import static com.rfaelxs.web.service.resumo.DadosDeResumo.saida;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.rfaelxs.web.domain.ContaPrevista;
import com.rfaelxs.web.domain.Reserva;
import com.rfaelxs.web.domain.TipoMovimentacaoReserva;
import com.rfaelxs.web.domain.Usuario;
import com.rfaelxs.web.repository.MovimentacaoReservaRepository;
import com.rfaelxs.web.repository.TotalCategoria;
import com.rfaelxs.web.repository.TotalMensal;
import com.rfaelxs.web.repository.TotalMovimentacaoMensal;
import com.rfaelxs.web.repository.TotalPorReserva;
import com.rfaelxs.web.repository.TransacaoRepository;
import com.rfaelxs.web.service.ContaPrevistaService;
import com.rfaelxs.web.service.ReservaService;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Histórico, categorias, ritmo das reservas e projeção da tela Relatório.
 *
 * <p>A calculadora de totais é real, sobre os repositórios mockados: o que se verifica é a
 * conta, não a chamada.
 */
@ExtendWith(MockitoExtension.class)
class RelatorioServiceTest {

  @Mock private TransacaoRepository transacaoRepository;
  @Mock private MovimentacaoReservaRepository movimentacaoRepository;
  @Mock private ReservaService reservaService;
  @Mock private ContaPrevistaService contaPrevistaService;

  private RelatorioService service;
  private Usuario usuario;

  @BeforeEach
  void setUp() {
    service = new RelatorioService(
        new CalculadoraDeTotais(transacaoRepository, movimentacaoRepository),
        transacaoRepository, movimentacaoRepository, reservaService, contaPrevistaService,
        DadosDeResumo.relogio());
    usuario = new Usuario("Teste", "11144477735", "hash");
  }

  @Test
  void historicoTrazSeisMesesMesmoSemMovimento() {
    transacoes(entrada(SETEMBRO, "1000.00"), entrada(YearMonth.of(2026, 7), "900.00"));
    movimentacoes();
    previstas();
    when(transacaoRepository.somarSaidasPorCategoria(any(), any(), any())).thenReturn(List.of());
    when(reservaService.listar(usuario)).thenReturn(List.of());
    when(movimentacaoRepository.somarPorReserva(any(), any(), any())).thenReturn(List.of());

    List<SobraMensal> historico = service.montar(usuario, SETEMBRO).historico();

    assertEquals(6, historico.size());
    assertEquals(YearMonth.of(2026, 4), historico.get(0).mes());
    assertEquals(SETEMBRO, historico.get(5).mes());
    assertEquals(new BigDecimal("0.00"), historico.get(1).sobra());
    assertEquals(new BigDecimal("900.00"), historico.get(3).sobra());
    assertEquals(new BigDecimal("1000.00"), historico.get(5).sobra());
  }

  @Test
  void topCategoriasFicaNasQuatroMaiores() {
    transacoes();
    movimentacoes();
    previstas();
    when(transacaoRepository.somarSaidasPorCategoria(any(), any(), any())).thenReturn(List.of(
        new TotalCategoria("Moradia", new BigDecimal("1269.90")),
        new TotalCategoria("Alimentação", new BigDecimal("1010.58")),
        new TotalCategoria("Lazer", new BigDecimal("545.00")),
        new TotalCategoria("Saúde", new BigDecimal("301.42")),
        new TotalCategoria("Transporte", new BigDecimal("120.00"))));
    when(reservaService.listar(usuario)).thenReturn(List.of());
    when(movimentacaoRepository.somarPorReserva(any(), any(), any())).thenReturn(List.of());

    List<TotalCategoria> top = service.montar(usuario, SETEMBRO).topCategorias();

    assertEquals(4, top.size());
    assertEquals("Moradia", top.get(0).categoria());
    assertEquals("Saúde", top.get(3).categoria());
  }

  @Test
  void previsaoDaReservaUsaOAporteMedioDosUltimosTresMeses() {
    transacoes();
    movimentacoes();
    previstas();
    when(transacaoRepository.somarSaidasPorCategoria(any(), any(), any())).thenReturn(List.of());
    Reserva viagem = reserva("Viagem Chile", "6000.00", "3000.00");
    when(reservaService.listar(usuario)).thenReturn(List.of(viagem));
    // 900 alocados na janela de 3 meses → média 300/mês; faltam 3000 → 10 meses.
    when(movimentacaoRepository.somarPorReserva(any(), any(), any())).thenReturn(List.of(
        new TotalPorReserva(null, TipoMovimentacaoReserva.ALOCACAO, new BigDecimal("900.00"))));

    RitmoReserva ritmo = service.montar(usuario, SETEMBRO).reservas().get(0);

    assertEquals(new BigDecimal("300.00"), ritmo.aporteMedio());
    assertEquals(YearMonth.of(2027, 7), ritmo.previsao());
    assertTrue(ritmo.noRitmo());
  }

  @Test
  void reservaParadaNaoRecebePrevisao() {
    transacoes();
    movimentacoes();
    previstas();
    when(transacaoRepository.somarSaidasPorCategoria(any(), any(), any())).thenReturn(List.of());
    when(reservaService.listar(usuario)).thenReturn(List.of(reserva("Curso", "2000.00", "640.00")));
    when(movimentacaoRepository.somarPorReserva(any(), any(), any())).thenReturn(List.of());

    RitmoReserva ritmo = service.montar(usuario, SETEMBRO).reservas().get(0);

    assertNull(ritmo.previsao());
    assertFalse(ritmo.noRitmo());
    assertEquals(new BigDecimal("32.0"), ritmo.progresso());
  }

  @Test
  void projecaoSeparaOQueJaSaiuDoQueAindaVence() {
    transacoes(entrada(SETEMBRO, "7480.00"), saida(SETEMBRO, "2813.40", true));
    movimentacoes();
    previstas(DadosDeResumo.conta(usuario, "Cartão", "980.00", 20));
    when(transacaoRepository.somarSaidasPorCategoria(any(), any(), any())).thenReturn(List.of());
    when(reservaService.listar(usuario)).thenReturn(List.of());
    when(movimentacaoRepository.somarPorReserva(any(), any(), any())).thenReturn(List.of());

    Projecao p = service.montar(usuario, SETEMBRO).projecao();

    assertEquals(new BigDecimal("2813.40"), p.jaSaiu());
    assertEquals(new BigDecimal("980.00"), p.aindaVence());
    assertEquals(new BigDecimal("3686.60"), p.sobraPrevista());
  }

  // ── apoio ──

  private void transacoes(TotalMensal... totais) {
    when(transacaoRepository.somarPorMes(any(), any(), any())).thenReturn(List.of(totais));
  }

  private void movimentacoes(TotalMovimentacaoMensal... totais) {
    when(movimentacaoRepository.somarPorMes(any(), any(), any())).thenReturn(List.of(totais));
  }

  private void previstas(ContaPrevista... contas) {
    when(contaPrevistaService.aVencer(any(), any(), any())).thenReturn(List.of(contas));
  }

  private Reserva reserva(String nome, String meta, String saldo) {
    return DadosDeResumo.reserva(usuario, nome, meta, saldo);
  }
}
