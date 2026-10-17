package com.rfaelxs.web.api;

import com.rfaelxs.web.api.dto.HojeResponse;
import com.rfaelxs.web.service.resumo.GestaoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * As telas de resumo: Gestão e, depois, Relatório (ADR 0005). Nenhuma rota aceita id de usuário:
 * o dono vem da sessão.
 */
@RestController
@RequestMapping("/api")
public class ResumoController {

  private final GestaoService gestaoService;

  public ResumoController(GestaoService gestaoService) {
    this.gestaoService = gestaoService;
  }

  /** Números da Gestão, sempre do mês corrente: a pergunta é sobre agora. */
  @GetMapping("/hoje")
  public HojeResponse hoje(@UsuarioAtual Long usuarioId) {
    return HojeResponse.de(gestaoService.hoje(usuarioId));
  }
}
