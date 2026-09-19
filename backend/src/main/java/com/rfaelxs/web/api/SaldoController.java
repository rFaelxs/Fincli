package com.rfaelxs.web.api;

import com.rfaelxs.web.api.dto.SaldoResponse;
import com.rfaelxs.web.domain.Usuario;
import com.rfaelxs.web.service.TransacaoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Saldo disponível do usuário da sessão.
 *
 * <p>A tela de alocar só precisa deste número, que antes vinha junto de todo o dashboard.
 */
@RestController
@RequestMapping("/api/saldo")
public class SaldoController {

  private final TransacaoService transacaoService;

  public SaldoController(TransacaoService transacaoService) {
    this.transacaoService = transacaoService;
  }

  /** @return o teto de uma alocação: entradas − saídas − total já alocado */
  @GetMapping
  public SaldoResponse saldo(@UsuarioAtual Usuario usuario) {
    return new SaldoResponse(transacaoService.calcularSaldoDisponivel(usuario));
  }
}
