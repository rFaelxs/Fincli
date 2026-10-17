import { inject } from '@angular/core';
import { CanActivateFn, RedirectCommand, Router } from '@angular/router';
import { SessaoService } from './sessao.service';

/** Redireciona trocando a entrada do histórico, para o "voltar" não cair na rota recusada. */
function substituirPor(router: Router, url: string): RedirectCommand {
  return new RedirectCommand(router.parseUrl(url), { replaceUrl: true });
}

/** Rotas do shell: sem sessão, vão para o login (spec 01, item 1). */
export const autenticadoGuard: CanActivateFn = async () => {
  const sessao = inject(SessaoService);
  const router = inject(Router);
  if (sessao.usuario()) {
    return true;
  }
  return (await sessao.iniciar()) ? true : substituirPor(router, '/login');
};

/** Login e cadastro: com sessão, vão direto para a Gestão (spec 02, item 1). */
export const publicoGuard: CanActivateFn = async () => {
  const sessao = inject(SessaoService);
  const router = inject(Router);
  return (await sessao.iniciar()) ? substituirPor(router, '/gestao') : true;
};

/** A raiz decide entre a Gestão e o login conforme a sessão. */
export const raizGuard: CanActivateFn = async () => {
  const sessao = inject(SessaoService);
  const router = inject(Router);
  const usuario = sessao.usuario() ?? (await sessao.iniciar());
  return substituirPor(router, usuario ? '/gestao' : '/login');
};
