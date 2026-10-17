package com.rfaelxs.web.service;

import com.rfaelxs.web.domain.ContaPrevista;
import com.rfaelxs.web.domain.Reserva;
import com.rfaelxs.web.domain.TipoTransacao;
import com.rfaelxs.web.domain.Transacao;
import com.rfaelxs.web.domain.Usuario;
import com.rfaelxs.web.service.ComandoParser.Comando;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Executa o que a barra de comando pediu.
 *
 * <p>O cliente faz o mesmo parse para mostrar o preview instantâneo, mas o texto cru é
 * reinterpretado aqui: aceitar tipo, valor ou categoria vindos do navegador deixaria o usuário
 * escolher o que gravar sem passar pelas regras.
 */
@Service
public class ComandoService {

  private static final Pattern RX_MARCAS = Pattern.compile("\\p{M}+");

  /** Quanto do texto do usuário volta numa mensagem de erro. */
  private static final int LIMITE_ECO = 40;

  private final TransacaoService transacaoService;
  private final ReservaService reservaService;
  private final ContaPrevistaService contaPrevistaService;
  private final Clock relogio;

  public ComandoService(
      TransacaoService transacaoService,
      ReservaService reservaService,
      ContaPrevistaService contaPrevistaService,
      Clock relogio) {
    this.transacaoService = transacaoService;
    this.reservaService = reservaService;
    this.contaPrevistaService = contaPrevistaService;
    this.relogio = relogio;
  }

  /**
   * Interpreta e grava o comando.
   *
   * @param usuario dono do lançamento
   * @param texto   o que o usuário digitou
   * @return o que foi criado, com o necessário para desfazer
   * @throws ComandoNaoEntendidoException se o texto não virar um lançamento
   */
  @Transactional
  public Resultado executar(Usuario usuario, String texto) {
    Comando comando =
        ComandoParser.parse(texto, LocalDate.now(relogio))
            .orElseThrow(() -> new ComandoNaoEntendidoException(
                "Não entendi \"%s\" — tente algo como `mercado 120 ontem`."
                    .formatted(resumir(texto))));

    return switch (comando.tipo()) {
      case ENTRADA -> transacao(usuario, comando, TipoTransacao.ENTRADA);
      case SAIDA -> transacao(usuario, comando, TipoTransacao.SAIDA);
      case APORTE -> aporte(usuario, comando);
      case PREVISTA -> prevista(usuario, comando);
    };
  }

  private Resultado transacao(Usuario usuario, Comando c, TipoTransacao tipo) {
    Transacao t =
        transacaoService.adicionar(
            usuario, c.valor(), c.categoria(), c.descricao(), c.data(), tipo, c.essencial());
    return new Resultado(
        c.tipo(), t.getPublicId(), null, c.valor(), c.descricao(), c.categoria());
  }

  /**
   * Aporte na reserva que o texto indica.
   *
   * <p>Sem nome de reserva o dinheiro vai para a Reserva de Emergência — é a única que sempre
   * existe. Nome que casa com mais de uma reserva não é resolvido no chute: quem decide é o
   * usuário.
   */
  private Resultado aporte(Usuario usuario, Comando c) {
    Reserva reserva = escolherReserva(usuario, c.descricao());
    reservaService.alocar(usuario, reserva.getPublicId(), c.valor());
    return new Resultado(
        c.tipo(), null, reserva.getPublicId(), c.valor(), reserva.getNome(), "Aporte");
  }

  private Reserva escolherReserva(Usuario usuario, String descricao) {
    List<Reserva> reservas = reservaService.listar(usuario);
    if (reservas.isEmpty()) {
      throw new ComandoNaoEntendidoException(
          "Você ainda não tem reservas para guardar dinheiro.");
    }
    if ("Aporte".equals(descricao)) {
      return reservas.stream().filter(Reserva::isEmergencia).findFirst().orElse(reservas.get(0));
    }

    String alvo = chave(descricao);
    List<Reserva> candidatas =
        reservas.stream().filter(r -> chave(r.getNome()).contains(alvo)).toList();

    if (candidatas.isEmpty()) {
      throw new ComandoNaoEntendidoException(
          "Não achei a reserva \"%s\". Crie a reserva antes de guardar nela."
              .formatted(resumir(descricao)));
    }
    if (candidatas.size() > 1) {
      String nomes = candidatas.stream().map(Reserva::getNome).collect(Collectors.joining(", "));
      throw new ComandoNaoEntendidoException(
          "\"%s\" casa com mais de uma reserva: %s. Escreva o nome inteiro."
              .formatted(resumir(descricao), nomes));
    }
    return candidatas.get(0);
  }

  private Resultado prevista(Usuario usuario, Comando c) {
    YearMonth referencia = c.recorrente() ? null : YearMonth.from(c.data());
    ContaPrevista conta =
        contaPrevistaService.criar(
            usuario, c.descricao(), c.valor(), c.diaVencimento(), referencia);
    return new Resultado(c.tipo(), conta.getPublicId(), null, c.valor(), c.descricao(), "Conta");
  }

  /** @return o texto reduzido a letras e dígitos minúsculos sem acento, para comparar nomes */
  private static String chave(String texto) {
    String semAcento =
        RX_MARCAS.matcher(Normalizer.normalize(texto, Normalizer.Form.NFD)).replaceAll("");
    return semAcento.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
  }

  /** Corta o texto que volta na mensagem de erro — o campo aceita qualquer coisa. */
  private static String resumir(String texto) {
    String limpo = texto == null ? "" : texto.strip().replaceAll("\\s+", " ");
    return limpo.length() > LIMITE_ECO ? limpo.substring(0, LIMITE_ECO) + "…" : limpo;
  }

  /**
   * O que o comando criou.
   *
   * @param id        id do recurso criado; nulo em aporte, que grava uma movimentação sem id
   *                  público
   * @param reservaId preenchido só em aporte — é por ele que o "desfazer" saca o valor de volta
   */
  public record Resultado(
      ComandoParser.Tipo tipo,
      UUID id,
      UUID reservaId,
      BigDecimal valor,
      String descricao,
      String categoria) {
  }
}
