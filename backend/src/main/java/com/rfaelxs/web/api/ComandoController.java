package com.rfaelxs.web.api;

import com.rfaelxs.web.api.dto.ComandoRequest;
import com.rfaelxs.web.api.dto.ComandoResponse;
import com.rfaelxs.web.service.comando.ComandoService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Barra de comando: texto livre vira lançamento (spec 04). */
@RestController
@RequestMapping("/api/comando")
public class ComandoController {

  private final ComandoService comandoService;

  public ComandoController(ComandoService comandoService) {
    this.comandoService = comandoService;
  }

  /**
   * Interpreta o texto e grava o lançamento.
   *
   * @return 200 com o que foi criado; 422 quando o texto não vira lançamento; 400 quando o valor
   *     fere uma regra, como saldo insuficiente
   */
  @PostMapping
  public ComandoResponse executar(
      @UsuarioAtual Long usuarioId, @Valid @RequestBody ComandoRequest req) {
    return ComandoResponse.de(comandoService.executar(usuarioId, req.texto()));
  }
}
