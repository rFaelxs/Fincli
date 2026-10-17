package com.rfaelxs.web.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.rfaelxs.web.service.ComandoParser.Comando;
import com.rfaelxs.web.service.ComandoParser.Tipo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Gramática da barra de comando.
 *
 * <p>A primeira bateria é a tabela do handoff de design, linha por linha. O resto cobre o que a
 * tabela não diz: separador de milhar, ordem trocada, acento, entrada sem número e as contas
 * previstas, que são gramática nova deste projeto.
 */
class ComandoParserTest {

  private static final LocalDate HOJE = LocalDate.of(2026, 9, 16);

  // ── a tabela do README ──

  @Test
  void mercado120OntemEssencial() {
    Comando c = parse("mercado 120 ontem essencial");

    assertEquals(Tipo.SAIDA, c.tipo());
    assertEquals(new BigDecimal("120.00"), c.valor());
    assertEquals("Mercado", c.descricao());
    assertEquals("Alimentação", c.categoria());
    assertEquals(LocalDate.of(2026, 9, 15), c.data());
    assertTrue(c.essencial());
  }

  @Test
  void guardar300Viagem() {
    Comando c = parse("guardar 300 viagem");

    assertEquals(Tipo.APORTE, c.tipo());
    assertEquals(new BigDecimal("300.00"), c.valor());
    assertEquals("Viagem", c.descricao());
    assertEquals("Aporte", c.categoria());
    assertFalse(c.essencial());
  }

  @Test
  void freela450() {
    Comando c = parse("freela 450");

    assertEquals(Tipo.ENTRADA, c.tipo());
    assertEquals(new BigDecimal("450.00"), c.valor());
    assertEquals("Freela", c.descricao());
    assertEquals("Trabalho", c.categoria());
    assertEquals(HOJE, c.data());
    assertFalse(c.essencial());
  }

  @Test
  void uber32Hoje() {
    Comando c = parse("uber 32 hoje");

    assertEquals(Tipo.SAIDA, c.tipo());
    assertEquals(new BigDecimal("32.00"), c.valor());
    assertEquals("Uber", c.descricao());
    assertEquals("Transporte", c.categoria());
    assertEquals(HOJE, c.data());
    assertTrue(c.essencial());
  }

  @Test
  void show120() {
    Comando c = parse("show 120");

    assertEquals(Tipo.SAIDA, c.tipo());
    assertEquals(new BigDecimal("120.00"), c.valor());
    assertEquals("Show", c.descricao());
    assertEquals("Lazer", c.categoria());
    assertFalse(c.essencial());
  }

  // ── valor ──

  @Test
  void aceitaSeparadorDeMilhar() {
    assertEquals(new BigDecimal("1234.56"), parse("aluguel 1.234,56").valor());
  }

  @Test
  void aceitaPontoComoDecimal() {
    assertEquals(new BigDecimal("12.50"), parse("padaria 12.50").valor());
  }

  @Test
  void pontoSemCasaDecimalEhMilhar() {
    assertEquals(new BigDecimal("1234.00"), parse("aluguel 1.234").valor());
  }

  @Test
  void usaOPrimeiroNumeroDoTexto() {
    assertEquals(new BigDecimal("120.00"), parse("mercado 120 dividido em 3").valor());
  }

  @Test
  void numeroColadoNaPalavraAindaEhLido() {
    Comando c = parse("mercado120");

    assertEquals(new BigDecimal("120.00"), c.valor());
    assertEquals("Mercado", c.descricao());
  }

  @ParameterizedTest
  @ValueSource(strings = {"mercado", "guardar", "", "   ", "cafe da manha", "0", "0,00"})
  void semNumeroPositivoNaoHaComando(String texto) {
    assertTrue(ComandoParser.parse(texto, HOJE).isEmpty());
  }

  @Test
  void textoNuloNaoQuebra() {
    assertTrue(ComandoParser.parse(null, HOJE).isEmpty());
  }

  // ── ordem, acento e datas ──

  @Test
  void aOrdemDosTermosNaoImporta() {
    Comando direto = parse("mercado 120 ontem essencial");
    Comando invertido = parse("ontem essencial 120 mercado");

    assertEquals(direto.tipo(), invertido.tipo());
    assertEquals(direto.valor(), invertido.valor());
    assertEquals(direto.categoria(), invertido.categoria());
    assertEquals(direto.data(), invertido.data());
    assertEquals(direto.essencial(), invertido.essencial());
  }

  @Test
  void palavraChaveComAcentoCaiNaMesmaCategoria() {
    assertEquals("Saúde", parse("farmácia 172,42").categoria());
    assertEquals("Alimentação", parse("almoço 38").categoria());
    assertEquals("Moradia", parse("água 90").categoria());
  }

  @Test
  void anteontemVoltaDoisDias() {
    assertEquals(LocalDate.of(2026, 9, 14), parse("uber 32 anteontem").data());
  }

  @Test
  void aDescricaoPreservaAcentoEMaiuscula() {
    assertEquals("Restaurante japonês", parse("restaurante japonês 268,40").descricao());
  }

  // ── tipo ──

  @Test
  void prefixoDeEntradaVenceAPalavraDeCategoria() {
    assertEquals(Tipo.ENTRADA, parse("recebi 200 do mercado").tipo());
  }

  @Test
  void maisNoComecoEhEntrada() {
    assertEquals(Tipo.ENTRADA, parse("+450 venda do notebook").tipo());
  }

  @Test
  void palavraDeRendaNoMeioTambemEhEntrada() {
    assertEquals(Tipo.ENTRADA, parse("pix da mãe 200").tipo());
  }

  @Test
  void entradaNuncaVemMarcadaComoEssencial() {
    assertFalse(parse("salário 6800 essencial").essencial());
  }

  @Test
  void aporteSemDescricaoRecebeRotuloPadrao() {
    assertEquals("Aporte", parse("guardar 300").descricao());
  }

  @Test
  void essencialForcaAMarcacaoEmCategoriaSupfluaPorPadrao() {
    assertTrue(parse("show 120 essencial").essencial());
  }

  // ── contas previstas ──

  @Test
  void preverCriaContaRecorrenteComDia() {
    Comando c = parse("prever energia 210 dia 22");

    assertEquals(Tipo.PREVISTA, c.tipo());
    assertEquals(new BigDecimal("210.00"), c.valor());
    assertEquals("Energia", c.descricao());
    assertEquals(22, c.diaVencimento());
    assertTrue(c.recorrente());
  }

  @Test
  void oDiaNaoEhConfundidoComOValor() {
    assertEquals(new BigDecimal("980.00"), parse("prever cartão 980 dia 20").valor());
    assertEquals(20, parse("prever cartão 980 dia 20").diaVencimento());
  }

  @Test
  void diaAntesDoValorTambemFunciona() {
    Comando c = parse("prever dia 5 aluguel 1150");

    assertEquals(new BigDecimal("1150.00"), c.valor());
    assertEquals(5, c.diaVencimento());
    assertEquals("Aluguel", c.descricao());
  }

  @Test
  void umaVezTornaAContaAvulsa() {
    assertFalse(parse("prever ipva 840 dia 15 uma vez").recorrente());
  }

  @Test
  void contaDeLuzNaoVira() {
    Comando c = parse("conta de luz 210 dia 10");

    assertEquals(Tipo.PREVISTA, c.tipo());
    assertEquals("Luz", c.descricao());
  }

  @Test
  void semDiaAContaVenceHoje() {
    assertEquals(16, parse("prever faxina 103,10").diaVencimento());
  }

  // ── apoio ──

  private static Comando parse(String texto) {
    Optional<Comando> c = ComandoParser.parse(texto, HOJE);
    assertTrue(c.isPresent(), () -> "não entendeu: " + texto);
    return c.get();
  }

}
