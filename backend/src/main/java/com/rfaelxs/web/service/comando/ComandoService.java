package com.rfaelxs.web.service.comando;

import com.rfaelxs.web.domain.ContaPrevista;
import com.rfaelxs.web.domain.Reserva;
import com.rfaelxs.web.domain.TipoTransacao;
import com.rfaelxs.web.domain.Transacao;
import com.rfaelxs.web.service.ComandoNaoEntendidoException;
import com.rfaelxs.web.service.ContaPrevistaService;
import com.rfaelxs.web.service.DadosTransacao;
import com.rfaelxs.web.service.ReservaService;
import com.rfaelxs.web.service.TransacaoService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Executa o que a barra de comando pediu.
 *
 * <p>O front faz o mesmo parse para o preview, mas o texto cru é reinterpretado aqui: aceitar
 * tipo, valor ou categoria vindos do navegador deixaria o usuário escolher o que gravar sem passar
 * pelas regras. Cada tipo é gravado pelo serviço do seu módulo, com as mesmas validações da tela
 * correspondente.
 */
@Service
public class ComandoService {

  /** Quanto do texto do usuário volta numa mensagem de erro (spec 04, item 31). */
  static final int LIMITE_ECO = 40;

  private final TransacaoService transacaoService;
  private final ReservaService reservaService;
  private final ContaPrevistaService contaPrevistaService;
  private final Clock relogio;

  /** Cria o serviço com os serviços que gravam cada tipo de lançamento. */
  public ComandoService(TransacaoService transacaoService, ReservaService reservaService,
      ContaPrevistaService contaPrevistaService, Clock relogio) {
    this.transacaoService = transacaoService;
    this.reservaService = reservaService;
    this.contaPrevistaService = contaPrevistaService;
    this.relogio = relogio;
  }

  /**
   * Interpreta e grava o comando.
   *
   * @return o que foi criado, com o necessário para desfazer
   * @throws ComandoNaoEntendidoException se o texto não virar lançamento, ou o aporte não achar
   *     a reserva
   */
  @Transactional
  public Resultado executar(Long usuarioId, String texto) {
    Comando comando = ComandoParser.parse(texto, LocalDate.now(relogio))
        .orElseThrow(() -> new ComandoNaoEntendidoException(
            "Não entendi \"%s\" — tente algo como `mercado 120 ontem`.".formatted(eco(texto))));

    return switch (comando.tipo()) {
      case ENTRADA -> transacao(usuarioId, comando, TipoTransacao.ENTRADA);
      case SAIDA -> transacao(usuarioId, comando, TipoTransacao.SAIDA);
      case APORTE -> aporte(usuarioId, comando);
      case PREVISTA -> prevista(usuarioId, comando);
    };
  }

  private Resultado transacao(Long usuarioId, Comando c, TipoTransacao tipo) {
    Transacao t = transacaoService.criar(usuarioId, new DadosTransacao(
        c.valor(), c.categoria(), c.descricao(), c.data(), tipo, c.essencial()));
    return new Resultado(c.tipo(), t.getPublicId(), null, c.valor(), c.descricao(),
        c.categoria());
  }

  private Resultado aporte(Long usuarioId, Comando c) {
    Reserva reserva = escolherReserva(usuarioId, c.descricao());
    reservaService.alocar(usuarioId, reserva.getPublicId(), c.valor());
    return new Resultado(c.tipo(), null, reserva.getPublicId(), c.valor(), reserva.getNome(),
        ComandoParser.CATEGORIA_APORTE);
  }

  /**
   * A reserva que o texto indica. Sem nome, a Reserva de Emergência, que todo usuário tem. Um nome
   * que casa com mais de uma reserva não é resolvido no chute: quem decide é o usuário.
   */
  private Reserva escolherReserva(Long usuarioId, String descricao) {
    List<Reserva> reservas = reservaService.listar(usuarioId);
    if (reservas.isEmpty()) {
      throw new ComandoNaoEntendidoException(
          "Você ainda não tem reservas para guardar dinheiro.");
    }

    String alvo = chave(descricao);
    // Sem nome, ou com um "nome" só de pontuação, o aporte vai para a emergência.
    if (ComandoParser.DESCRICAO_APORTE.equals(descricao) || alvo.isEmpty()) {
      return reservas.stream().filter(Reserva::isEmergencia).findFirst()
          .orElse(reservas.get(0));
    }

    List<Reserva> candidatas =
        reservas.stream().filter(r -> chave(r.getNome()).contains(alvo)).toList();
    if (candidatas.isEmpty()) {
      throw new ComandoNaoEntendidoException(
          "Não achei a reserva \"%s\". Crie a reserva antes de guardar nela."
              .formatted(eco(descricao)));
    }
    if (candidatas.size() > 1) {
      String nomes = candidatas.stream().map(Reserva::getNome).collect(Collectors.joining(", "));
      throw new ComandoNaoEntendidoException(
          "\"%s\" casa com mais de uma reserva: %s. Escreva o nome inteiro."
              .formatted(eco(descricao), nomes));
    }
    return candidatas.get(0);
  }

  private Resultado prevista(Long usuarioId, Comando c) {
    YearMonth referencia = c.recorrente() ? null : YearMonth.from(c.data());
    ContaPrevista conta = contaPrevistaService.criar(
        usuarioId, c.descricao(), c.valor(), c.diaVencimento(), referencia);
    return new Resultado(c.tipo(), conta.getPublicId(), null, c.valor(), c.descricao(),
        ComandoParser.CATEGORIA_PREVISTA);
  }

  /** O texto reduzido a letras e dígitos minúsculos sem acento, para comparar nomes. */
  static String chave(String texto) {
    return ComandoParser.normalizar(texto).replaceAll("[^a-z0-9]", "");
  }

  /** Corta o texto que volta na mensagem de erro: o campo aceita qualquer coisa. */
  static String eco(String texto) {
    String limpo = texto == null ? "" : texto.strip().replaceAll("\\s+", " ");
    return limpo.length() > LIMITE_ECO ? limpo.substring(0, LIMITE_ECO) + "…" : limpo;
  }

  /**
   * O que o comando criou.
   *
   * @param id        transação em entrada e saída, conta em conta prevista; nulo em aporte
   * @param reservaId só em aporte: é por ele que o "desfazer" saca o valor de volta
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
