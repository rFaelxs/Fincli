package com.rfaelxs.web.api;

import com.rfaelxs.web.api.dto.ContaPrevistaResponse;
import com.rfaelxs.web.domain.Usuario;
import com.rfaelxs.web.service.ContaPrevistaService;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contas previstas do usuário da sessão.
 *
 * <p>Não há POST: contas nascem pela barra de comando ({@code prever energia 210 dia 22}),
 * que é a forma de lançamento escolhida para o produto. Aqui ficam a listagem, que a tela
 * Hoje usa, e a exclusão, que é o "desfazer" de uma conta recém-criada.
 */
@RestController
@RequestMapping("/api/previstas")
public class ContaPrevistaController {

  private final ContaPrevistaService contaPrevistaService;

  public ContaPrevistaController(ContaPrevistaService contaPrevistaService) {
    this.contaPrevistaService = contaPrevistaService;
  }

  /**
   * @param mes mês de referência em {@code yyyy-MM}; ausente significa o mês corrente
   * @return contas que valem no mês, da que vence primeiro para a última
   */
  @GetMapping
  public List<ContaPrevistaResponse> listar(
      @UsuarioAtual Usuario usuario,
      @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes) {
    YearMonth referencia = mes != null ? mes : YearMonth.now();
    return contaPrevistaService.doMes(usuario, referencia).stream()
        .map(c -> ContaPrevistaResponse.de(c, referencia))
        .toList();
  }

  /** @return 204; 404 se não existir ou não for do usuário */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> remover(@UsuarioAtual Usuario usuario, @PathVariable UUID id) {
    contaPrevistaService.remover(usuario, id);
    return ResponseEntity.noContent().build();
  }
}
