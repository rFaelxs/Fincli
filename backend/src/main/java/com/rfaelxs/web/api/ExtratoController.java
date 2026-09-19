package com.rfaelxs.web.api;

import com.rfaelxs.web.api.dto.ExtratoItemResponse;
import com.rfaelxs.web.domain.Usuario;
import com.rfaelxs.web.service.ExtratoService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Extrato consolidado do usuário da sessão. */
@RestController
@RequestMapping("/api/extrato")
public class ExtratoController {

  private final ExtratoService extratoService;

  public ExtratoController(ExtratoService extratoService) {
    this.extratoService = extratoService;
  }

  /** @return transações e movimentações de reserva, do mais recente para o mais antigo */
  @GetMapping
  public List<ExtratoItemResponse> listar(@UsuarioAtual Usuario usuario) {
    return extratoService.listar(usuario).stream().map(ExtratoItemResponse::de).toList();
  }
}
