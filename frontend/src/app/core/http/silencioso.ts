import { HttpContext, HttpContextToken } from '@angular/common/http';

/**
 * Chamada em que um 401 só quer dizer "sem sessão" — o bootstrap e as telas públicas
 * perguntam quem está logado sem mandar ninguém para o login.
 */
export const SILENCIOSO = new HttpContextToken<boolean>(() => false);

export function silencioso(): HttpContext {
  return new HttpContext().set(SILENCIOSO, true);
}
