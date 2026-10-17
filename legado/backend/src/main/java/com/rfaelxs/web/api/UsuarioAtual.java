package com.rfaelxs.web.api;

import com.rfaelxs.web.domain.Usuario;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

/**
 * Atalho para injetar o {@link Usuario} da sessão em métodos de controller.
 *
 * <p>Nenhum endpoint aceita o id do dono vindo do cliente: ele vem sempre da sessão. É o que
 * impede um usuário de alcançar o recurso de outro (escopo-web.md §4).
 */
@AuthenticationPrincipal(expression = "usuario")
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Target(java.lang.annotation.ElementType.PARAMETER)
public @interface UsuarioAtual {
}
