package com.rfaelxs.web.api;

import com.rfaelxs.web.api.dto.SaldoResponse;
import com.rfaelxs.web.service.SaldoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Saldo disponível, para a tela de alocar orientar o usuário antes do envio.
 *
 * <p>É uma leitura informativa: quem decide a alocação é a releitura feita dentro da transação,
 * com o usuário travado.
 */
@RestController
@RequestMapping("/api/saldo")
public class SaldoController {

  private final SaldoService saldoService;

  public SaldoController(SaldoService saldoService) {
    this.saldoService = saldoService;
  }

  @GetMapping
  public SaldoResponse saldo(@UsuarioAtual Long usuarioId) {
    return new SaldoResponse(saldoService.disponivel(usuarioId));
  }
}
