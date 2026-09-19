package com.rfaelxs.web.api;

import com.rfaelxs.web.api.dto.HojeResponse;
import com.rfaelxs.web.api.dto.RelatorioResponse;
import com.rfaelxs.web.domain.Usuario;
import com.rfaelxs.web.service.resumo.GestaoService;
import com.rfaelxs.web.service.resumo.RelatorioService;
import java.time.YearMonth;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * As duas telas do redesign.
 *
 * <p>Nenhuma das rotas aceita id de usuário: o dono vem da sessão, como todo o resto da API
 * (ADR 0006).
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

  /**
   * Números da tela Gestão, sempre do mês corrente — a pergunta é sobre agora.
   *
   * <p>A rota guarda o nome antigo da tela, Hoje, para não mudar o contrato.
   */
  @GetMapping("/hoje")
  public HojeResponse hoje(@UsuarioAtual Usuario usuario) {
    return HojeResponse.de(gestaoService.montar(usuario));
  }

  /**
   * Números da tela Relatório.
   *
   * @param mes mês de referência em {@code yyyy-MM}; ausente significa o mês corrente
   */
  @GetMapping("/relatorio")
  public RelatorioResponse relatorio(
      @UsuarioAtual Usuario usuario,
      @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes) {
    return RelatorioResponse.de(
        relatorioService.montar(usuario, mes != null ? mes : YearMonth.now()));
  }
}
