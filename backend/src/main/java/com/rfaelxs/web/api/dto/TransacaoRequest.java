package com.rfaelxs.web.api.dto;

import com.rfaelxs.web.domain.TipoTransacao;
import com.rfaelxs.web.service.DadosTransacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Criação ou edição de uma transação.
 *
 * <p>A mensagem de valor negativo é a mesma do serviço, para que a frase da spec chegue à tela
 * por qualquer um dos dois caminhos.
 */
public record TransacaoRequest(
    @NotNull(message = "Informe o valor.")
    @PositiveOrZero(message = "O valor da transação não pode ser negativo.")
    BigDecimal valor,

    @NotBlank(message = "Informe a categoria.")
    @Size(max = 80, message = "A categoria deve ter no máximo 80 caracteres.")
    String categoria,

    @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres.")
    String descricao,

    @NotNull(message = "Informe a data.")
    LocalDate data,

    @NotNull(message = "Informe o tipo (ENTRADA ou SAIDA).")
    TipoTransacao tipo,

    boolean essencial) {

  public DadosTransacao dados() {
    return new DadosTransacao(valor, categoria, descricao, data, tipo, essencial);
  }
}
