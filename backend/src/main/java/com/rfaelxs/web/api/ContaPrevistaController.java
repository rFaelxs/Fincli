package com.rfaelxs.web.api;

import com.rfaelxs.web.api.dto.ContaPrevistaRequest;
import com.rfaelxs.web.api.dto.ContaPrevistaResponse;
import com.rfaelxs.web.service.ContaPrevistaService;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.YearMonth;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contas previstas do usuário da sessão.
 *
 * <p>No uso normal a conta nasce pela barra de comando. O POST daqui serve ao "desfazer" de uma
 * remoção: recriar a conta com os campos exatos é mais confiável do que remontar o texto do
 * comando e torcer para o parser devolver os mesmos valores.
 */
@RestController
@RequestMapping("/api/previstas")
public class ContaPrevistaController {

  private final ContaPrevistaService contaPrevistaService;
  private final Clock relogio;

  public ContaPrevistaController(ContaPrevistaService contaPrevistaService, Clock relogio) {
    this.contaPrevistaService = contaPrevistaService;
    this.relogio = relogio;
  }

  /** 201 com a conta, o vencimento calculado no mês dela ou no corrente; 400 se inválida. */
  @PostMapping
  public ResponseEntity<ContaPrevistaResponse> criar(
      @UsuarioAtual Long usuarioId, @Valid @RequestBody ContaPrevistaRequest req) {
    var criada = contaPrevistaService.criar(
        usuarioId, req.nome(), req.valor(), req.diaVencimento(), req.mesReferencia());
    YearMonth mes = req.mesReferencia() != null ? req.mesReferencia() : YearMonth.now(relogio);
    return ResponseEntity.status(HttpStatus.CREATED).body(ContaPrevistaResponse.de(criada, mes));
  }

  /** 204; 404 se não existir ou for de outro usuário. */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> remover(@UsuarioAtual Long usuarioId, @PathVariable UUID id) {
    contaPrevistaService.remover(usuarioId, id);
    return ResponseEntity.noContent().build();
  }
}
