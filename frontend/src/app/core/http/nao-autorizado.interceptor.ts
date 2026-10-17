import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { SessaoService } from '../auth/sessao.service';
import { SILENCIOSO } from './silencioso';

/**
 * Um 401 numa chamada comum quer dizer que a sessão expirou: o usuário vai para o login e o fluxo
 * para com "Sessão expirada." (spec 01, item 3). Chamadas silenciosas passam intactas.
 */
export const naoAutorizadoInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const sessao = inject(SessaoService);

  return next(req).pipe(
    catchError((erro: unknown) => {
      if (erro instanceof HttpErrorResponse && erro.status === 401 && !req.context.get(SILENCIOSO)) {
        sessao.esquecer();
        void router.navigateByUrl('/login');
        return throwError(() => new Error('Sessão expirada.'));
      }
      return throwError(() => erro);
    }),
  );
};
