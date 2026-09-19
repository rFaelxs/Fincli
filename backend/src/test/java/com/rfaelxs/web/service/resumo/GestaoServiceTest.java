package com.rfaelxs.web.service.resumo;

import static com.rfaelxs.web.service.resumo.DadosDeResumo.SETEMBRO;
import static com.rfaelxs.web.service.resumo.DadosDeResumo.alocacao;
import static com.rfaelxs.web.service.resumo.DadosDeResumo.entrada;
import static com.rfaelxs.web.service.resumo.DadosDeResumo.saida;
import static com.rfaelxs.web.service.resumo.DadosDeResumo.saque;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.rfaelxs.web.domain.ContaPrevista;
import com.rfaelxs.web.domain.Usuario;
import com.rfaelxs.web.repository.MovimentacaoReservaRepository;
import com.rfaelxs.web.repository.TotalMensal;
import com.rfaelxs.web.repository.TotalMovimentacaoMensal;
import com.rfaelxs.web.repository.TransacaoRepository;
import com.rfaelxs.web.service.ContaPrevistaService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Fórmulas da tela Gestão (RN-01 a RN-08).
 *
 * <p>A calculadora de totais é real, sobre os repositórios mockados: o que se verifica é a
 * conta, não a chamada.
 */
@ExtendWith(MockitoExtension.class)
class GestaoServiceTest {

  @Mock private TransacaoRepository transacaoRepository;
  @Mock private MovimentacaoReservaRepository movimentacaoRepository;
  @Mock private ContaPrevistaService contaPrevistaService;

  private GestaoService service;
  private Usuario usuario;

  @BeforeEach
  void setUp() {
    service = new GestaoService(
        new CalculadoraDeTotais(transacaoRepository, movimentacaoRepository),
        transacaoRepository, movimentacaoRepository, contaPrevistaService,
        DadosDeResumo.relogio());
    usuario = new Usuario("Teste", "11144477735", "hash");

    lenient().when(transacaoRepository
        .findByUsuarioIdAndDataTransacaoBetweenOrderByDataTransacaoDescIdDesc(any(), any(), any()))
        .thenReturn(List.of());
    lenient().when(movimentacaoRepository
        .findByUsuarioIdAndDataMovimentoBetweenOrderByDataMovimentoDescIdDesc(any(), any(), any()))
        .thenReturn(List.of());
  }

  @Test
  void livreDescontaSaidasAportesEContasAVencer() {
    transacoes(
        entrada(SETEMBRO, "7480.00"),
        saida(SETEMBRO, "2000.00", true),
        saida(SETEMBRO, "813.40", false));
    movimentacoes(alocacao(SETEMBRO, "1100.00"));
    previstas(conta("Cartão", "980.00", 20), conta("Energia", "210.00", 22));

    Gestao gestao = service.montar(usuario);

    // sobra = 7480 − 2813,40 − 1100 = 3566,60 ; livre = 3566,60 − 1190 = 2376,60
    assertEquals(new BigDecimal("3566.60"), gestao.totais().sobra());
    assertEquals(new BigDecimal("1190.00"), gestao.previstasTotal());
    assertEquals(new BigDecimal("2376.60"), gestao.livre());
  }

  @Test
  void superfluasSaoSaidasMenosEssenciais() {
    transacoes(
        entrada(SETEMBRO, "7480.00"),
        saida(SETEMBRO, "2000.00", true),
        saida(SETEMBRO, "813.40", false));
    movimentacoes();
    previstas();

    Totais totais = service.montar(usuario).totais();

    assertEquals(new BigDecimal("2813.40"), totais.saidas());
    assertEquals(new BigDecimal("2000.00"), totais.essenciais());
    assertEquals(new BigDecimal("813.40"), totais.superfluas());
  }

  @Test
  void aporteDoMesEhAlocacaoMenosSaque() {
    transacoes(entrada(SETEMBRO, "1000.00"));
    movimentacoes(alocacao(SETEMBRO, "800.00"), saque(SETEMBRO, "300.00"));
    previstas();

    Totais totais = service.montar(usuario).totais();

    assertEquals(new BigDecimal("500.00"), totais.aportes());
    assertEquals(new BigDecimal("500.00"), totais.sobra());
  }

  @Test
  void porDiaDivideOLivrePelosDiasQueFaltam() {
    transacoes(entrada(SETEMBRO, "1400.00"));
    movimentacoes();
    previstas();

    Gestao gestao = service.montar(usuario);

    // 30 − 16 = 14 dias; 1400 / 14 = 100,00
    assertEquals(14, gestao.diasRestantes());
    assertEquals(new BigDecimal("100.00"), gestao.porDia());
  }

  @Test
  void livrePodeSerNegativo() {
    transacoes(entrada(SETEMBRO, "1000.00"), saida(SETEMBRO, "1500.00", true));
    movimentacoes();
    previstas(conta("Cartão", "200.00", 20));

    assertEquals(new BigDecimal("-700.00"), service.montar(usuario).livre());
  }

  @Test
  void mesSemMovimentoDaZeroEmVezDeQuebrar() {
    transacoes();
    movimentacoes();
    previstas();

    Gestao gestao = service.montar(usuario);

    assertEquals(new BigDecimal("0.00"), gestao.livre());
    assertEquals(new BigDecimal("0.00"), gestao.totais().entradas());
    assertTrue(gestao.ultimosLancamentos().isEmpty());
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

  private ContaPrevista conta(String nome, String valor, int dia) {
    return DadosDeResumo.conta(usuario, nome, valor, dia);
  }
}
