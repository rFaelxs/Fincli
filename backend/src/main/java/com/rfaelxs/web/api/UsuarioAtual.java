package com.rfaelxs.web.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

/**
 * Injeta o id interno do usuário da sessão num parâmetro {@code Long} de controller.
 *
 * <p>Nenhum endpoint aceita o id do dono vindo do cliente: ele vem sempre da sessão. É o que
 * impede um usuário de alcançar o recurso de outro (ADR 0006).
 */
@AuthenticationPrincipal(expression = "id")
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface UsuarioAtual {
}
