package com.rfaelxs.web.api;

import com.rfaelxs.web.api.dto.ExtratoItemResponse;
import com.rfaelxs.web.service.ExtratoService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Extrato do usuário da sessão (spec 08). */
@RestController
@RequestMapping("/api/extrato")
public class ExtratoController {

  private final ExtratoService extratoService;

  public ExtratoController(ExtratoService extratoService) {
    this.extratoService = extratoService;
  }

  /** Histórico inteiro, da data mais recente para a mais antiga. */
  @GetMapping
  public List<ExtratoItemResponse> listar(@UsuarioAtual Long usuarioId) {
    return extratoService.listar(usuarioId).stream().map(ExtratoItemResponse::de).toList();
  }
}
