import { inject } from '@angular/core';
import { CanActivateFn, RedirectCommand, Router } from '@angular/router';
import { SessaoService } from './sessao.service';

/** Rotas autenticadas: sem sessão, vai para o login sem deixar a rota no histórico. */
export const autenticadoGuard: CanActivateFn = async () => {
  const router = inject(Router);
  const usuario = await inject(SessaoService).garantir();
  return usuario ? true : new RedirectCommand(router.parseUrl('/login'), { replaceUrl: true });
};

/**
 * Login e cadastro: quem já tem sessão vai direto para a Gestão.
 *
 * Pergunta sempre ao servidor, em vez de confiar no usuário guardado: é também aqui que o
 * cookie `XSRF-TOKEN` é renovado antes do primeiro POST.
 */
export const publicoGuard: CanActivateFn = async () => {
  const router = inject(Router);
  const usuario = await inject(SessaoService).iniciar();
  return usuario ? new RedirectCommand(router.parseUrl('/gestao'), { replaceUrl: true }) : true;
};
