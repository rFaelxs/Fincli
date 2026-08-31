package com.rfaelxs.web.api;

import com.rfaelxs.web.api.dto.TransacaoRequest;
import com.rfaelxs.web.api.dto.TransacaoResponse;
import com.rfaelxs.web.domain.Usuario;
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

/** CRUD de transações do usuário da sessão. */
@RestController
@RequestMapping("/api/transacoes")
public class TransacaoController {

  private final TransacaoService transacaoService;

  public TransacaoController(TransacaoService transacaoService) {
    this.transacaoService = transacaoService;
  }

  /** @return transações do usuário, da mais recente para a mais antiga */
  @GetMapping
  public List<TransacaoResponse> listar(@UsuarioAtual Usuario usuario) {
    return transacaoService.listar(usuario).stream().map(TransacaoResponse::de).toList();
  }

  /** @return 201 com a transação criada; 400 se o valor for negativo */
  @PostMapping
  public ResponseEntity<TransacaoResponse> criar(
      @UsuarioAtual Usuario usuario, @Valid @RequestBody TransacaoRequest req) {
    TransacaoResponse criada =
        TransacaoResponse.de(
            transacaoService.adicionar(
                usuario,
                req.valor(),
                req.categoria(),
                req.descricao(),
                req.data(),
                req.tipo(),
                req.essencial()));
    return ResponseEntity.status(HttpStatus.CREATED).body(criada);
  }

  /** @return 200 com a transação atualizada; 404 se não existir ou não for do usuário */
  @PutMapping("/{id}")
  public TransacaoResponse editar(
      @UsuarioAtual Usuario usuario,
      @PathVariable UUID id,
      @Valid @RequestBody TransacaoRequest req) {
    return TransacaoResponse.de(
        transacaoService.editar(
            usuario,
            id,
            req.valor(),
            req.categoria(),
            req.descricao(),
            req.data(),
            req.tipo(),
            req.essencial()));
  }

  /** @return 204; 404 se não existir ou não for do usuário */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> remover(@UsuarioAtual Usuario usuario, @PathVariable UUID id) {
    transacaoService.remover(usuario, id);
    return ResponseEntity.noContent().build();
  }
}
