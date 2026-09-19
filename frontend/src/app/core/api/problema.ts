import { HttpErrorResponse } from '@angular/common/http';

/**
 * Mensagem de erro pronta para a tela.
 *
 * Lê o `application/problem+json` do backend: `detail`, mais as mensagens de `campos` quando a
 * validação é campo a campo. Sem corpo legível, "Erro " seguido do status.
 */
export function lerErro(erro: unknown): string {
  if (erro instanceof HttpErrorResponse) {
    let mensagem = 'Erro ' + erro.status;
    const corpo: unknown = erro.error;
    if (corpo && typeof corpo === 'object') {
      const { detail, campos } = corpo as { detail?: unknown; campos?: unknown };
      if (typeof detail === 'string' && detail) {
        mensagem = detail;
      }
      if (campos && typeof campos === 'object') {
        mensagem += ' ' + Object.values(campos).join(' ');
      }
    }
    return mensagem;
  }
  if (erro instanceof Error) {
    return erro.message;
  }
  return String(erro);
}
