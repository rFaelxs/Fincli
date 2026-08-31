package com.rfaelxs.web.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Dados de cadastro.
 *
 * @param nome  nome de exibição
 * @param cpf   CPF com ou sem máscara; validado pelos dígitos verificadores
 * @param senha senha em texto puro, gravada como hash BCrypt
 * @param email opcional, reservado para futura redefinição de senha (escopo-web.md §1.1)
 */
public record CadastroRequest(
    @NotBlank(message = "Informe seu nome.")
    @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres.")
    String nome,

    @NotBlank(message = "Informe seu CPF.")
    String cpf,

    @NotBlank(message = "Informe uma senha.")
    @Size(min = 8, max = 100, message = "A senha deve ter entre 8 e 100 caracteres.")
    String senha,

    @Email(message = "E-mail inválido.")
    @Size(max = 254, message = "O e-mail deve ter no máximo 254 caracteres.")
    String email) {
}
