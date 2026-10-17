package com.rfaelxs.web.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Credenciais de login.
 *
 * @param cpf   CPF com ou sem máscara
 * @param senha senha em texto puro
 */
public record LoginRequest(
    @NotBlank(message = "Informe seu CPF.") String cpf,
    @NotBlank(message = "Informe sua senha.") String senha) {
}
