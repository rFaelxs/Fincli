package com.rfaelxs.web.api;

import com.rfaelxs.web.api.dto.ComandoRequest;
import com.rfaelxs.web.api.dto.ComandoResponse;
import com.rfaelxs.web.domain.Usuario;
import com.rfaelxs.web.service.ComandoService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Barra de comando: texto livre vira lançamento. */
@RestController
@RequestMapping("/api/comando")
public class ComandoController {

  private static final Locale BR = Locale.of("pt", "BR");

  private final ComandoService comandoService;

  public ComandoController(ComandoService comandoService) {
    this.comandoService = comandoService;
  }

  /**
   * Interpreta o texto e grava o lançamento.
   *
   * @return 200 com o que foi criado e o caminho para desfazer; 422 quando o texto não vira
   *     lançamento; 400 quando o valor fere uma regra (saldo insuficiente, por exemplo)
   */
  @PostMapping
  public ComandoResponse executar(
      @UsuarioAtual Usuario usuario, @Valid @RequestBody ComandoRequest req) {
    ComandoService.Resultado r = comandoService.executar(usuario, req.texto());
    return ComandoResponse.de(r, mensagem(r));
  }

  /** @return a frase da faixa de confirmação, no tempo verbal de cada tipo */
  private static String mensagem(ComandoService.Resultado r) {
    String verbo = switch (r.tipo()) {
      case ENTRADA -> "Recebido";
      case SAIDA -> "Lançado";
      case APORTE -> "Guardado";
      case PREVISTA -> "Previsto";
    };
    return "%s %s · %s".formatted(verbo, moeda(r.valor()), r.descricao());
  }

  private static String moeda(BigDecimal valor) {
    return NumberFormat.getCurrencyInstance(BR).format(valor);
  }
}
