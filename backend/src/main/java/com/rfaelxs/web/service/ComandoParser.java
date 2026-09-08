package com.rfaelxs.web.service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Texto livre da barra de comando em uma intenção estruturada.
 *
 * <p>Gramática sem ordem fixa: {@code mercado 120 ontem essencial} e
 * {@code ontem 120 mercado essencial} dão o mesmo resultado. O primeiro número do texto é o
 * valor; o resto vira descrição depois de retiradas as palavras de controle.
 *
 * <p>Esta classe é a fonte de verdade da gramática. O arquivo
 * {@code static/assets/js/comando.js} repete as mesmas tabelas para dar preview instantâneo no
 * cliente, mas quem decide é o servidor: o preview do navegador nunca é aceito como entrada
 * (README do handoff, seção "Endpoint sugerido").
 *
 * <p>Comparações são feitas sobre o texto sem acento e em minúsculas, então cada palavra-chave
 * aparece uma vez só na tabela — {@code almoco} pega "almoço" e {@code saude} pega "Saúde".
 */
public final class ComandoParser {

  /** O que o usuário quis dizer. */
  public enum Tipo {
    ENTRADA,
    SAIDA,
    APORTE,
    PREVISTA
  }

  /**
   * Categoria deduzida de palavras-chave.
   *
   * @param essencialPorPadrao se um gasto dessa categoria já nasce marcado como essencial
   */
  private record Categoria(String nome, Pattern chaves, boolean essencialPorPadrao) {
  }

  private static final List<Categoria> CATEGORIAS = List.of(
      cat("Alimentação", true,
          "mercado|supermercado|feira|padaria|restaurante|ifood|lanche|almoco|jantar"),
      cat("Moradia", true, "aluguel|condominio|luz|energia|agua|internet|fibra|gas"),
      cat("Saúde", true, "farmacia|academia|medico|dentista|remedio"),
      cat("Transporte", true, "uber|gasolina|onibus|metro|combustivel"),
      cat("Lazer", false, "show|cinema|streaming|netflix|spotify|bar|viagem|jogo"),
      cat("Trabalho", false, "salario|freela|freelance|pix|bonus|venda"));

  private static final String CATEGORIA_ENTRADA = "Trabalho";
  private static final String CATEGORIA_SAIDA = "Outros";
  private static final String CATEGORIA_APORTE = "Aporte";
  private static final String CATEGORIA_PREVISTA = "Conta";

  private static final Pattern RX_PREVISTA = Pattern.compile("^(prever|conta)\\b");
  private static final Pattern RX_APORTE = Pattern.compile("^(guardar|poupar|alocar)\\b");
  private static final Pattern RX_ENTRADA = Pattern.compile("^(entrada|recebi|\\+)");
  private static final Pattern RX_RENDA = Pattern.compile("salario|freela|pix|bonus");
  private static final Pattern RX_UMA_VEZ = Pattern.compile("\\buma vez\\b|\\bso (esse|este) mes\\b");
  private static final Pattern RX_DIA = Pattern.compile("^(\\d{1,2})$");

  /** Número dentro de um token: {@code 1.234,56}, {@code 1234.56} ou {@code 120}. */
  private static final Pattern RX_VALOR = Pattern.compile("\\d[\\d.]*(?:,\\d+)?");

  private static final Pattern RX_MARCAS = Pattern.compile("\\p{M}+");

  /** Palavras que orientam o parser e por isso não entram na descrição. */
  private static final Set<String> CONTROLE = Set.of(
      "ontem", "hoje", "anteontem", "essencial",
      "guardar", "poupar", "alocar", "entrada", "recebi", "prever");

  /** Palavras que só são ruído em conta prevista. */
  private static final Set<String> CONTROLE_PREVISTA = Set.of(
      "conta", "dia", "todo", "todos", "mes", "meses", "mensal", "recorrente", "uma", "vez",
      "so", "esse", "este");

  /** Artigos descartados quando sobram no começo da descrição. */
  private static final Set<String> LIGACAO = Set.of("de", "do", "da", "no", "na", "em");

  private ComandoParser() {
  }

  /**
   * Interpreta o texto digitado.
   *
   * @param texto o que o usuário escreveu
   * @param hoje  data de referência para {@code hoje}, {@code ontem} e {@code anteontem}
   * @return o comando entendido, ou vazio quando não há número — em dúvida não se lança nada
   */
  public static Optional<Comando> parse(String texto, LocalDate hoje) {
    if (texto == null || texto.isBlank()) {
      return Optional.empty();
    }

    List<String> tokens = List.of(texto.strip().split("\\s+"));
    String normalizado = semAcento(texto.strip()).toLowerCase(Locale.ROOT);

    Tipo tipo = tipoDe(normalizado);
    Analise analise = analisar(tokens, tipo);
    if (analise.valor() == null) {
      return Optional.empty();
    }

    LocalDate data = hoje;
    if (normalizado.contains("anteontem")) {
      data = hoje.minusDays(2);
    } else if (normalizado.contains("ontem")) {
      data = hoje.minusDays(1);
    }

    String descricao = descricaoDe(analise.palavras(), tipo);
    String categoria = categoriaDe(normalizado, tipo);
    boolean essencial = essencialDe(normalizado, tipo);

    if (tipo == Tipo.PREVISTA) {
      // Sem dia explícito, a conta vence hoje: é o palpite que menos infla o "dá pra gastar",
      // porque uma conta de hoje ainda conta como a vencer.
      int dia = analise.dia() != null ? analise.dia() : hoje.getDayOfMonth();
      boolean recorrente = !RX_UMA_VEZ.matcher(normalizado).find();
      return Optional.of(new Comando(
          tipo, analise.valor(), descricao, categoria, data, false, dia, recorrente));
    }
    return Optional.of(new Comando(
        tipo, analise.valor(), descricao, categoria, data, essencial, null, false));
  }

  private static Tipo tipoDe(String normalizado) {
    if (RX_PREVISTA.matcher(normalizado).find()) {
      return Tipo.PREVISTA;
    }
    if (RX_APORTE.matcher(normalizado).find()) {
      return Tipo.APORTE;
    }
    if (RX_ENTRADA.matcher(normalizado).find() || RX_RENDA.matcher(normalizado).find()) {
      return Tipo.ENTRADA;
    }
    return Tipo.SAIDA;
  }

  /**
   * Percorre os tokens uma vez separando valor, dia de vencimento e palavras da descrição.
   *
   * <p>O dia é lido antes do valor: em {@code prever energia 210 dia 22} o 22 pertence ao
   * vencimento, e tratá-lo como número solto faria o valor virar 22.
   */
  private static Analise analisar(List<String> tokens, Tipo tipo) {
    BigDecimal valor = null;
    Integer dia = null;
    boolean esperandoDia = false;
    List<String> palavras = new ArrayList<>();

    for (String token : tokens) {
      String n = semAcento(token).toLowerCase(Locale.ROOT);

      if (esperandoDia) {
        esperandoDia = false;
        Matcher numero = RX_DIA.matcher(n);
        if (numero.matches()) {
          dia = Integer.parseInt(numero.group(1));
          continue;
        }
      }
      if (tipo == Tipo.PREVISTA && n.equals("dia")) {
        esperandoDia = true;
        continue;
      }

      Matcher numero = RX_VALOR.matcher(n);
      if (valor == null && numero.find()) {
        valor = paraDecimal(numero.group());
        // "mercado120" deixa "mercado" para a descrição; "120" sozinho não deixa nada.
        String resto = n.substring(0, numero.start()) + n.substring(numero.end());
        if (resto.chars().anyMatch(Character::isLetter)) {
          palavras.add(token.substring(0, numero.start()) + token.substring(numero.end()));
        }
        continue;
      }

      if (CONTROLE.contains(n) || (tipo == Tipo.PREVISTA && CONTROLE_PREVISTA.contains(n))) {
        continue;
      }
      palavras.add(token);
    }

    return new Analise(valor, dia, palavras);
  }

  /**
   * Converte o número escrito para {@code BigDecimal}.
   *
   * <p>Com vírgula, o ponto é separador de milhar: {@code 1.234,56} vale 1234,56. Sem vírgula,
   * um único ponto seguido de uma ou duas casas é decimal ({@code 12.50}); qualquer outro ponto
   * é milhar ({@code 1.234} vale 1234).
   *
   * @return o valor, ou {@code null} se o que sobrou não for um número positivo
   */
  private static BigDecimal paraDecimal(String bruto) {
    String limpo;
    if (bruto.indexOf(',') >= 0) {
      limpo = bruto.replace(".", "").replace(',', '.');
    } else if (bruto.matches("\\d+\\.\\d{1,2}")) {
      limpo = bruto;
    } else {
      limpo = bruto.replace(".", "");
    }
    try {
      BigDecimal valor = new BigDecimal(limpo);
      return valor.signum() > 0 ? Dinheiro.normalizar(valor) : null;
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static String descricaoDe(List<String> palavras, Tipo tipo) {
    List<String> restantes = new ArrayList<>(palavras);
    // "conta de luz" perde o "conta" e ficaria "De luz".
    while (!restantes.isEmpty()
        && LIGACAO.contains(semAcento(restantes.get(0)).toLowerCase(Locale.ROOT))) {
      restantes.remove(0);
    }

    String descricao = String.join(" ", restantes).strip();
    if (descricao.isEmpty()) {
      return switch (tipo) {
        case APORTE -> "Aporte";
        case PREVISTA -> "Conta";
        default -> "Lançamento";
      };
    }
    return descricao.substring(0, 1).toUpperCase(Locale.ROOT) + descricao.substring(1);
  }

  private static String categoriaDe(String normalizado, Tipo tipo) {
    return switch (tipo) {
      case APORTE -> CATEGORIA_APORTE;
      case PREVISTA -> CATEGORIA_PREVISTA;
      case ENTRADA -> categoriaPorPalavra(normalizado)
          .map(Categoria::nome)
          .orElse(CATEGORIA_ENTRADA);
      case SAIDA -> categoriaPorPalavra(normalizado)
          .map(Categoria::nome)
          .orElse(CATEGORIA_SAIDA);
    };
  }

  /** A palavra {@code essencial} força a marcação; sem ela, vale o padrão da categoria. */
  private static boolean essencialDe(String normalizado, Tipo tipo) {
    if (tipo != Tipo.SAIDA) {
      return false;
    }
    return normalizado.contains("essencial")
        || categoriaPorPalavra(normalizado)
            .map(Categoria::essencialPorPadrao)
            .orElse(false);
  }

  private static Optional<Categoria> categoriaPorPalavra(String normalizado) {
    return CATEGORIAS.stream().filter(c -> c.chaves().matcher(normalizado).find()).findFirst();
  }

  private static Categoria cat(String nome, boolean essencial, String chaves) {
    return new Categoria(nome, Pattern.compile(chaves), essencial);
  }

  /** @return o texto sem sinais diacríticos, para que a tabela de palavras não os repita */
  private static String semAcento(String texto) {
    return RX_MARCAS.matcher(Normalizer.normalize(texto, Normalizer.Form.NFD)).replaceAll("");
  }

  private record Analise(BigDecimal valor, Integer dia, List<String> palavras) {
  }

  /**
   * O que o usuário quis lançar.
   *
   * @param diaVencimento dia do mês; preenchido só em {@link Tipo#PREVISTA}
   * @param recorrente    se a conta prevista se repete todo mês; falso nos demais tipos
   */
  public record Comando(
      Tipo tipo,
      BigDecimal valor,
      String descricao,
      String categoria,
      LocalDate data,
      boolean essencial,
      Integer diaVencimento,
      boolean recorrente) {
  }
}
