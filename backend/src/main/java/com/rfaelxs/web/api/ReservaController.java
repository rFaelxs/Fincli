package com.rfaelxs.web.api;

import com.rfaelxs.web.api.dto.ReservaRequest;
import com.rfaelxs.web.api.dto.ReservaResponse;
import com.rfaelxs.web.api.dto.ValorRequest;
import com.rfaelxs.web.domain.Usuario;
import com.rfaelxs.web.service.ReservaService;
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

/** Reservas financeiras do usuário da sessão. */
@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

  private final ReservaService reservaService;

  public ReservaController(ReservaService reservaService) {
    this.reservaService = reservaService;
  }

  /** @return reservas do usuário, com a de emergência primeiro */
  @GetMapping
  public List<ReservaResponse> listar(@UsuarioAtual Usuario usuario) {
    return reservaService.listar(usuario).stream().map(ReservaResponse::de).toList();
  }

  /** @return 200 com a reserva; 404 se não existir ou não for do usuário */
  @GetMapping("/{id}")
  public ReservaResponse buscar(@UsuarioAtual Usuario usuario, @PathVariable UUID id) {
    return ReservaResponse.de(reservaService.buscarPorId(usuario, id));
  }

  /** @return 201 com a reserva criada; 400 se a meta for negativa */
  @PostMapping
  public ResponseEntity<ReservaResponse> criar(
      @UsuarioAtual Usuario usuario, @Valid @RequestBody ReservaRequest req) {
    ReservaResponse criada =
        ReservaResponse.de(reservaService.criar(usuario, req.nome(), req.metaValor()));
    return ResponseEntity.status(HttpStatus.CREATED).body(criada);
  }

  /** @return 200 com a reserva; 400 se o valor exceder o saldo disponível */
  @PostMapping("/{id}/alocar")
  public ReservaResponse alocar(
      @UsuarioAtual Usuario usuario, @PathVariable UUID id, @Valid @RequestBody ValorRequest req) {
    return ReservaResponse.de(reservaService.alocar(usuario, id, req.valor()));
  }

  /** @return 200 com a reserva; 400 se o valor exceder o saldo da reserva */
  @PostMapping("/{id}/sacar")
  public ReservaResponse sacar(
      @UsuarioAtual Usuario usuario, @PathVariable UUID id, @Valid @RequestBody ValorRequest req) {
    return ReservaResponse.de(reservaService.sacar(usuario, id, req.valor()));
  }

  /** @return 200 com a reserva; 400 se a meta for negativa */
  @PutMapping("/{id}/meta")
  public ReservaResponse atualizarMeta(
      @UsuarioAtual Usuario usuario, @PathVariable UUID id, @Valid @RequestBody ValorRequest req) {
    return ReservaResponse.de(reservaService.atualizarMeta(usuario, id, req.valor()));
  }

  /** @return 204; 409 se for a Reserva de Emergência ou ainda tiver saldo */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> excluir(@UsuarioAtual Usuario usuario, @PathVariable UUID id) {
    reservaService.excluir(usuario, id);
    return ResponseEntity.noContent().build();
  }
}
