import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { SessaoService } from '../auth/sessao.service';
import { SILENCIOSO } from './silencioso';

/** O erro que chega a quem chamou quando a sessão caiu no meio do uso. */
export class SessaoExpirada extends Error {
  constructor() {
    super('Sessão expirada.');
  }
}

/**
 * Sessão expirada no meio do uso: volta ao login sem deixar a tela no histórico.
 *
 * Chamadas silenciosas passam direto — nelas o 401 é uma resposta, não um problema.
 */
export const naoAutorizadoInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const sessao = inject(SessaoService);

  return next(req).pipe(
    catchError((erro: unknown) => {
      if (
        erro instanceof HttpErrorResponse &&
        erro.status === 401 &&
        !req.context.get(SILENCIOSO)
      ) {
        sessao.esquecer();
        void router.navigateByUrl('/login', { replaceUrl: true });
        return throwError(() => new SessaoExpirada());
      }
      return throwError(() => erro);
    }),
  );
};
