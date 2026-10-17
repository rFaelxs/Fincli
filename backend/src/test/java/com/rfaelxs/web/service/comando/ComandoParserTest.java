package com.rfaelxs.web.service.comando;

import static org.assertj.core.api.Assertions.assertThat;

import com.rfaelxs.web.service.comando.ComandoParser.Tipo;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Casos da gramática. O {@code gramatica.spec.ts} do front deve repetir estes mesmos casos: é o
 * que mantém o preview fiel ao que o servidor grava.
 */
class ComandoParserTest {

  private static final LocalDate HOJE = LocalDate.of(2026, 9, 16);

  // CA-CMD-01
  @Test
  void mercado120Ontem() {
    Comando c = parse("mercado 120 ontem");

    assertThat(c.tipo()).isEqualTo(Tipo.SAIDA);
    assertThat(c.valor()).isEqualByComparingTo("120.00");
    assertThat(c.categoria()).isEqualTo("Alimentação");
    assertThat(c.essencial()).isTrue();
    assertThat(c.data()).isEqualTo(LocalDate.of(2026, 9, 15));
    assertThat(c.descricao()).isEqualTo("Mercado");
  }

  // CA-CMD-02 e item 5
  @Test
  void aOrdemDosTermosNaoImporta() {
    assertThat(parse("ontem 120 mercado")).isEqualTo(parse("mercado 120 ontem"));
    assertThat(parse("ontem essencial 120 mercado"))
        .isEqualTo(parse("mercado 120 ontem essencial"));
  }

  // CA-CMD-03 e CA-CMD-04
  @Test
  void lazerNaoEhEssencialAMenosQueOTextoDiga() {
    assertThat(parse("cinema 60").categoria()).isEqualTo("Lazer");
    assertThat(parse("cinema 60").essencial()).isFalse();
    assertThat(parse("cinema 60 essencial").essencial()).isTrue();
  }

  // CA-CMD-05
  @Test
  void freela450EhEntradaDeTrabalho() {
    Comando c = parse("freela 450");

    assertThat(c.tipo()).isEqualTo(Tipo.ENTRADA);
    assertThat(c.valor()).isEqualByComparingTo("450.00");
    assertThat(c.categoria()).isEqualTo("Trabalho");
    assertThat(c.descricao()).isEqualTo("Freela");
  }

  // CA-CMD-06 a CA-CMD-08 e item 3
  @ParameterizedTest
  @CsvSource(delimiter = ';', value = {
      "1.234,56 aluguel; 1234.56",
      "almoço 12.50; 12.50",
      "feira 1.234; 1234.00",
      "mercado 120 dividido em 3; 120.00",
      "padaria 12,5; 12.50"})
  void leOValor(String texto, String esperado) {
    assertThat(parse(texto).valor()).isEqualByComparingTo(esperado);
  }

  @Test
  void numeroColadoNaPalavraAindaEhLido() {
    Comando c = parse("mercado120");

    assertThat(c.valor()).isEqualByComparingTo("120.00");
    assertThat(c.descricao()).isEqualTo("Mercado");
    assertThat(c.categoria()).isEqualTo("Alimentação");
  }

  // CA-CMD-09 e item 4
  @ParameterizedTest
  @ValueSource(strings = {"mercado sem numero", "guardar", "", "   ", "cafe da manha", "0",
      "0,00"})
  void semNumeroPositivoNaoHaComando(String texto) {
    assertThat(ComandoParser.parse(texto, HOJE)).isEmpty();
  }

  @Test
  void textoNuloNaoQuebra() {
    assertThat(ComandoParser.parse(null, HOJE)).isEmpty();
  }

  @Test
  void palavraChaveComAcentoCaiNaMesmaCategoria() {
    assertThat(parse("farmácia 172,42").categoria()).isEqualTo("Saúde");
    assertThat(parse("almoço 38").categoria()).isEqualTo("Alimentação");
    assertThat(parse("água 90").categoria()).isEqualTo("Moradia");
  }

  // Palavra-chave casa como palavra inteira: "gas" não pega "gasolina", "bar" não pega "barato".
  @Test
  void palavraChaveNaoCasaDentroDeOutraPalavra() {
    assertThat(parse("gasolina 200").categoria()).isEqualTo("Transporte");
    assertThat(parse("gas 90").categoria()).isEqualTo("Moradia");
    assertThat(parse("lanche barato 15").categoria()).isEqualTo("Alimentação");
    assertThat(parse("presente barato 30").categoria()).isEqualTo("Outros");
  }

  // Item 6
  @Test
  void datasRelativas() {
    assertThat(parse("uber 32 hoje").data()).isEqualTo(HOJE);
    assertThat(parse("uber 32").data()).isEqualTo(HOJE);
    assertThat(parse("uber 32 anteontem").data()).isEqualTo(LocalDate.of(2026, 9, 14));
  }

  @Test
  void aDescricaoPreservaAcentoEMaiuscula() {
    assertThat(parse("restaurante japonês 268,40").descricao()).isEqualTo("Restaurante japonês");
  }

  // Itens 1 e 2
  @Test
  void tiposDeEntrada() {
    assertThat(parse("recebi 200 do mercado").tipo()).isEqualTo(Tipo.ENTRADA);
    assertThat(parse("+450 venda do notebook").tipo()).isEqualTo(Tipo.ENTRADA);
    assertThat(parse("pix da mãe 200").tipo()).isEqualTo(Tipo.ENTRADA);
    assertThat(parse("freelance 500").tipo()).isEqualTo(Tipo.ENTRADA);
    assertThat(parse("pixel 300").tipo()).isEqualTo(Tipo.SAIDA);
  }

  @Test
  void entradaNuncaVemMarcadaComoEssencial() {
    assertThat(parse("salário 6800 essencial").essencial()).isFalse();
  }

  // Item 10
  @Test
  void descricaoVaziaRecebeRotuloPadrao() {
    assertThat(parse("guardar 300").descricao()).isEqualTo("Aporte");
    assertThat(parse("prever 300").descricao()).isEqualTo("Conta");
    assertThat(parse("300").descricao()).isEqualTo("Lançamento");
  }

  @Test
  void aporteGuardaONomeDaReservaNaDescricao() {
    Comando c = parse("guardar 300 viagem");

    assertThat(c.tipo()).isEqualTo(Tipo.APORTE);
    assertThat(c.descricao()).isEqualTo("Viagem");
    assertThat(c.categoria()).isEqualTo("Aporte");
  }

  // CA-CMD-11
  @Test
  void preverCriaContaRecorrenteComDia() {
    Comando c = parse("prever energia 210 dia 22");

    assertThat(c.tipo()).isEqualTo(Tipo.PREVISTA);
    assertThat(c.valor()).isEqualByComparingTo("210.00");
    assertThat(c.descricao()).isEqualTo("Energia");
    assertThat(c.diaVencimento()).isEqualTo(22);
    assertThat(c.recorrente()).isTrue();
  }

  @Test
  void oDiaNaoEhConfundidoComOValor() {
    assertThat(parse("prever cartão 980 dia 20").valor()).isEqualByComparingTo("980.00");
    assertThat(parse("prever dia 5 aluguel 1150").valor()).isEqualByComparingTo("1150.00");
    assertThat(parse("prever dia 5 aluguel 1150").descricao()).isEqualTo("Aluguel");
  }

  // CA-CMD-12 e item 12
  @ParameterizedTest
  @ValueSource(strings = {"prever ipva 840 dia 15 uma vez", "prever ipva 840 só este mês",
      "prever ipva 840 so esse mes"})
  void umaVezTornaAContaAvulsa(String texto) {
    assertThat(parse(texto).recorrente()).isFalse();
  }

  @Test
  void contaDeLuzPerdeOArtigo() {
    assertThat(parse("conta de luz 210 dia 10").descricao()).isEqualTo("Luz");
  }

  // CA-CMD-13 e item 11
  @Test
  void semDiaAContaVenceHoje() {
    assertThat(parse("prever faxina 103,10").diaVencimento()).isEqualTo(16);
  }

  private static Comando parse(String texto) {
    Optional<Comando> c = ComandoParser.parse(texto, HOJE);
    assertThat(c).as("não entendeu: %s", texto).isPresent();
    return c.get();
  }
}
