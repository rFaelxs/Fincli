package com.rfaelxs.web.service.comando;

import com.rfaelxs.web.service.Dinheiro;
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
 * Texto livre da barra de comando numa intenção estruturada (spec 04, itens 1 a 14).
 *
 * <p>Gramática sem ordem fixa: {@code mercado 120 ontem essencial} e
 * {@code ontem 120 mercado essencial} dão o mesmo resultado. O primeiro número é o valor; o resto
 * vira descrição depois de retiradas as palavras de controle.
 *
 * <p>Esta classe é a fonte de verdade da gramática. O front repete as mesmas tabelas só para o
 * preview; quem decide é o servidor, e o preview nunca é aceito como entrada.
 *
 * <p>As comparações são feitas sobre o texto sem acento e em minúsculas, então cada palavra-chave
 * aparece uma vez só: {@code almoco} pega "almoço". Toda palavra-chave casa como palavra inteira,
 * delimitada por algo que não seja letra: {@code gas} não pega "gasolina", mas {@code mercado}
 * ainda pega "mercado120".
 */
public final class ComandoParser {

  /** O que o usuário quis lançar. */
  public enum Tipo {
    ENTRADA,
    SAIDA,
    APORTE,
    PREVISTA
  }

  /**
   * Categoria deduzida por palavra-chave.
   *
   * @param essencialPorPadrao se um gasto dessa categoria já nasce essencial
   */
  private record Categoria(String nome, Pattern chaves, boolean essencialPorPadrao) {
  }

  private static final List<Categoria> CATEGORIAS = List.of(
      categoria("Alimentação", true,
          "mercado|supermercado|feira|padaria|restaurante|ifood|lanche|almoco|jantar"),
      categoria("Moradia", true, "aluguel|condominio|luz|energia|agua|internet|fibra|gas"),
      categoria("Saúde", true, "farmacia|academia|medico|dentista|remedio"),
      categoria("Transporte", true, "uber|gasolina|onibus|metro|combustivel"),
      categoria("Lazer", false, "show|cinema|streaming|netflix|spotify|bar|viagem|jogo"),
      categoria("Trabalho", false, "salario|freela|freelance|pix|bonus|venda"));

  static final String CATEGORIA_ENTRADA = "Trabalho";
  static final String CATEGORIA_SAIDA = "Outros";
  static final String CATEGORIA_APORTE = "Aporte";
  static final String CATEGORIA_PREVISTA = "Conta";

  static final String DESCRICAO_APORTE = "Aporte";
  static final String DESCRICAO_PREVISTA = "Conta";
  static final String DESCRICAO_PADRAO = "Lançamento";

  private static final Pattern RX_PREVISTA = Pattern.compile("^(prever|conta)(?![a-z])");
  private static final Pattern RX_APORTE = Pattern.compile("^(guardar|poupar|alocar)(?![a-z])");
  private static final Pattern RX_ENTRADA =
      Pattern.compile("^(entrada(?![a-z])|recebi(?![a-z])|\\+)");
  private static final Pattern RX_RENDA = palavras("salario|freela|freelance|pix|bonus");
  private static final Pattern RX_UMA_VEZ =
      Pattern.compile("\\buma vez\\b|\\bso (esse|este) mes\\b");
  private static final Pattern RX_ANTEONTEM = palavras("anteontem");
  private static final Pattern RX_ONTEM = palavras("ontem");
  private static final Pattern RX_ESSENCIAL = palavras("essencial");
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

  /** Artigos descartados quando sobram no começo da descrição: "conta de luz" vira "Luz". */
  private static final Set<String> LIGACAO = Set.of("de", "do", "da", "no", "na", "em");

  private ComandoParser() {
  }

  /**
   * Interpreta o texto digitado.
   *
   * @param texto o que o usuário escreveu
   * @param hoje  referência para {@code hoje}, {@code ontem} e {@code anteontem}
   * @return o comando entendido, ou vazio quando não há número positivo: em dúvida, não se lança
   *     nada
   */
  public static Optional<Comando> parse(String texto, LocalDate hoje) {
    if (texto == null || texto.isBlank()) {
      return Optional.empty();
    }

    List<String> tokens = List.of(texto.strip().split("\\s+"));
    String normalizado = normalizar(texto.strip());

    Tipo tipo = tipoDe(normalizado);
    Analise analise = analisar(tokens, tipo);
    if (analise.valor() == null) {
      return Optional.empty();
    }

    LocalDate data = hoje;
    if (RX_ANTEONTEM.matcher(normalizado).find()) {
      data = hoje.minusDays(2);
    } else if (RX_ONTEM.matcher(normalizado).find()) {
      data = hoje.minusDays(1);
    }

    String descricao = descricaoDe(analise.palavras(), tipo);
    String categoria = categoriaDe(normalizado, tipo);

    if (tipo == Tipo.PREVISTA) {
      // Sem dia explícito, a conta vence hoje: é o palpite que menos infla o "dá pra gastar",
      // porque a conta de hoje ainda conta como a vencer.
      int dia = analise.dia() != null ? analise.dia() : hoje.getDayOfMonth();
      boolean recorrente = !RX_UMA_VEZ.matcher(normalizado).find();
      return Optional.of(new Comando(
          tipo, analise.valor(), descricao, categoria, data, false, dia, recorrente));
    }
    return Optional.of(new Comando(tipo, analise.valor(), descricao, categoria, data,
        essencialDe(normalizado, tipo), null, false));
  }

  /** Texto sem acento e em minúsculas, a forma em que toda comparação é feita. */
  static String normalizar(String texto) {
    return RX_MARCAS.matcher(Normalizer.normalize(texto, Normalizer.Form.NFD)).replaceAll("")
        .toLowerCase(Locale.ROOT);
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
   * <p>O dia é lido antes do valor: em {@code prever energia 210 dia 22} o 22 é o vencimento, e
   * tratá-lo como número solto faria o valor virar 22.
   */
  private static Analise analisar(List<String> tokens, Tipo tipo) {
    BigDecimal valor = null;
    Integer dia = null;
    boolean esperandoDia = false;
    List<String> palavras = new ArrayList<>();

    for (String token : tokens) {
      String n = normalizar(token);

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
   * Converte o número escrito.
   *
   * <p>Com vírgula, o ponto é separador de milhar: {@code 1.234,56} vale 1234,56. Sem vírgula,
   * um único ponto seguido de uma ou duas casas é decimal ({@code 12.50}); qualquer outro ponto é
   * milhar ({@code 1.234} vale 1234).
   *
   * @return o valor em duas casas, ou {@code null} se não for um número positivo
   */
  static BigDecimal paraDecimal(String bruto) {
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
    while (!restantes.isEmpty() && LIGACAO.contains(normalizar(restantes.get(0)))) {
      restantes.remove(0);
    }

    String descricao = String.join(" ", restantes).strip();
    if (descricao.isEmpty()) {
      return switch (tipo) {
        case APORTE -> DESCRICAO_APORTE;
        case PREVISTA -> DESCRICAO_PREVISTA;
        default -> DESCRICAO_PADRAO;
      };
    }
    return descricao.substring(0, 1).toUpperCase(Locale.ROOT) + descricao.substring(1);
  }

  private static String categoriaDe(String normalizado, Tipo tipo) {
    return switch (tipo) {
      case APORTE -> CATEGORIA_APORTE;
      case PREVISTA -> CATEGORIA_PREVISTA;
      case ENTRADA -> categoriaPorPalavra(normalizado).map(Categoria::nome)
          .orElse(CATEGORIA_ENTRADA);
      case SAIDA -> categoriaPorPalavra(normalizado).map(Categoria::nome)
          .orElse(CATEGORIA_SAIDA);
    };
  }

  /** A palavra {@code essencial} força a marcação; sem ela, vale o padrão da categoria. */
  private static boolean essencialDe(String normalizado, Tipo tipo) {
    if (tipo != Tipo.SAIDA) {
      return false;
    }
    return RX_ESSENCIAL.matcher(normalizado).find()
        || categoriaPorPalavra(normalizado).map(Categoria::essencialPorPadrao).orElse(false);
  }

  private static Optional<Categoria> categoriaPorPalavra(String normalizado) {
    return CATEGORIAS.stream().filter(c -> c.chaves().matcher(normalizado).find()).findFirst();
  }

  private static Categoria categoria(String nome, boolean essencial, String chaves) {
    return new Categoria(nome, palavras(chaves), essencial);
  }

  /** Alternativas que só casam como palavra inteira, delimitada por algo que não seja letra. */
  private static Pattern palavras(String alternativas) {
    return Pattern.compile("(?<![a-z])(" + alternativas + ")(?![a-z])");
  }

  private record Analise(BigDecimal valor, Integer dia, List<String> palavras) {
  }
}
