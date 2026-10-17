package com.rfaelxs.web.api;

import com.rfaelxs.web.api.dto.HojeResponse;
import com.rfaelxs.web.api.dto.RelatorioResponse;
import com.rfaelxs.web.service.resumo.GestaoService;
import com.rfaelxs.web.service.resumo.RelatorioService;
import java.time.YearMonth;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * As telas de resumo, cada uma com o seu serviço (ADR 0005). Nenhuma rota aceita id de usuário:
 * o dono vem da sessão.
 */
@RestController
@RequestMapping("/api")
public class ResumoController {

  private final GestaoService gestaoService;
  private final RelatorioService relatorioService;

  public ResumoController(GestaoService gestaoService, RelatorioService relatorioService) {
    this.gestaoService = gestaoService;
    this.relatorioService = relatorioService;
  }

  /** Números da Gestão, sempre do mês corrente: a pergunta é sobre agora. */
  @GetMapping("/hoje")
  public HojeResponse hoje(@UsuarioAtual Long usuarioId) {
    return HojeResponse.de(gestaoService.hoje(usuarioId));
  }

  /**
   * Números do Relatório.
   *
   * @param mes mês em {@code yyyy-MM}; ausente significa o mês corrente
   */
  @GetMapping("/relatorio")
  public RelatorioResponse relatorio(@UsuarioAtual Long usuarioId,
      @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes) {
    return RelatorioResponse.de(mes == null
        ? relatorioService.doMesCorrente(usuarioId)
        : relatorioService.doMes(usuarioId, mes));
  }
}
