import { HttpContext, HttpContextToken } from '@angular/common/http';

/**
 * Marca as chamadas em que um 401 é resposta, e não sessão expirada: o `GET /api/me` de quem
 * ainda não entrou, o login com senha errada. O interceptor deixa essas passarem sem redirecionar.
 */
export const SILENCIOSO = new HttpContextToken<boolean>(() => false);

/** Contexto de uma chamada silenciosa. */
export function silencioso(): HttpContext {
  return new HttpContext().set(SILENCIOSO, true);
}
