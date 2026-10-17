package com.rfaelxs.web.api;

import com.rfaelxs.web.api.dto.TransacaoRequest;
import com.rfaelxs.web.api.dto.TransacaoResponse;
import com.rfaelxs.web.service.TransacaoService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Transações do usuário da sessão (spec 06). */
@RestController
@RequestMapping("/api/transacoes")
public class TransacaoController {

  private final TransacaoService transacaoService;

  public TransacaoController(TransacaoService transacaoService) {
    this.transacaoService = transacaoService;
  }

  /** Lista inteira, sem filtro: mês, tipo e busca são aplicados no front. */
  @GetMapping
  public List<TransacaoResponse> listar(@UsuarioAtual Long usuarioId) {
    return transacaoService.listar(usuarioId).stream().map(TransacaoResponse::de).toList();
  }

  /** 200 com a transação; 404 se não existir ou for de outro usuário. */
  @GetMapping("/{id}")
  public TransacaoResponse buscar(@UsuarioAtual Long usuarioId, @PathVariable UUID id) {
    return TransacaoResponse.de(transacaoService.buscar(usuarioId, id));
  }

  /** 201 com a transação criada; 400 se algum campo for inválido. */
  @PostMapping
  public ResponseEntity<TransacaoResponse> criar(
      @UsuarioAtual Long usuarioId, @Valid @RequestBody TransacaoRequest req) {
    var criada = transacaoService.criar(usuarioId, req.dados());
    return ResponseEntity.status(HttpStatus.CREATED).body(TransacaoResponse.de(criada));
  }

  /** 200 com a transação alterada; 400 se algum campo for inválido; 404 se não for do usuário. */
  @PutMapping("/{id}")
  public TransacaoResponse editar(@UsuarioAtual Long usuarioId, @PathVariable UUID id,
      @Valid @RequestBody TransacaoRequest req) {
    return TransacaoResponse.de(transacaoService.editar(usuarioId, id, req.dados()));
  }

  /** 204; 404 se não existir ou for de outro usuário. */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> excluir(@UsuarioAtual Long usuarioId, @PathVariable UUID id) {
    transacaoService.excluir(usuarioId, id);
    return ResponseEntity.noContent().build();
  }
}
