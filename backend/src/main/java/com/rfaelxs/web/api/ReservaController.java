package com.rfaelxs.web.api;

import com.rfaelxs.web.api.dto.ReservaRequest;
import com.rfaelxs.web.api.dto.ReservaResponse;
import com.rfaelxs.web.api.dto.ValorRequest;
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

/** Reservas do usuário da sessão (spec 07). */
@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

  private final ReservaService reservaService;

  public ReservaController(ReservaService reservaService) {
    this.reservaService = reservaService;
  }

  /** A Reserva de Emergência primeiro; as demais, por nome. */
  @GetMapping
  public List<ReservaResponse> listar(@UsuarioAtual Long usuarioId) {
    return reservaService.listar(usuarioId).stream().map(ReservaResponse::de).toList();
  }

  /** 200 com a reserva; 404 se não existir ou for de outro usuário. */
  @GetMapping("/{id}")
  public ReservaResponse buscar(@UsuarioAtual Long usuarioId, @PathVariable UUID id) {
    return ReservaResponse.de(reservaService.buscar(usuarioId, id));
  }

  /** 201 com a reserva criada; 400 se algum campo for inválido. */
  @PostMapping
  public ResponseEntity<ReservaResponse> criar(
      @UsuarioAtual Long usuarioId, @Valid @RequestBody ReservaRequest req) {
    var criada = reservaService.criar(usuarioId, req.nome(), req.metaValor());
    return ResponseEntity.status(HttpStatus.CREATED).body(ReservaResponse.de(criada));
  }

  /** 200 com a reserva; 400 se o valor não for positivo ou passar do saldo disponível. */
  @PostMapping("/{id}/alocar")
  public ReservaResponse alocar(@UsuarioAtual Long usuarioId, @PathVariable UUID id,
      @Valid @RequestBody ValorRequest req) {
    return ReservaResponse.de(reservaService.alocar(usuarioId, id, req.valor()));
  }

  /** 200 com a reserva; 400 se o valor não for positivo ou passar do saldo da reserva. */
  @PostMapping("/{id}/sacar")
  public ReservaResponse sacar(@UsuarioAtual Long usuarioId, @PathVariable UUID id,
      @Valid @RequestBody ValorRequest req) {
    return ReservaResponse.de(reservaService.sacar(usuarioId, id, req.valor()));
  }

  /** 200 com a reserva; 400 se a meta for negativa. */
  @PutMapping("/{id}/meta")
  public ReservaResponse alterarMeta(@UsuarioAtual Long usuarioId, @PathVariable UUID id,
      @Valid @RequestBody ValorRequest req) {
    return ReservaResponse.de(reservaService.alterarMeta(usuarioId, id, req.valor()));
  }

  /** 204; 409 se for a Reserva de Emergência ou ainda tiver saldo. */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> excluir(@UsuarioAtual Long usuarioId, @PathVariable UUID id) {
    reservaService.excluir(usuarioId, id);
    return ResponseEntity.noContent().build();
  }
}
