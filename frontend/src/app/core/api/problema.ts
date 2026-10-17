import { HttpErrorResponse } from '@angular/common/http';
import { Problema } from './modelos';

/**
 * A frase que a tela mostra para um erro (spec 01, item 14): o `detail` do problem+json e, havendo
 * `campos`, as mensagens de campo em seguida. Sem corpo legível, "Erro " e o status.
 */
export function mensagemDeErro(erro: unknown): string {
  if (erro instanceof HttpErrorResponse) {
    const corpo = erro.error as Problema | null;
    if (corpo && typeof corpo === 'object' && typeof corpo.detail === 'string') {
      const campos = corpo.campos ? Object.values(corpo.campos) : [];
      return [corpo.detail, ...campos].join(' ');
    }
    return `Erro ${erro.status}`;
  }
  if (erro instanceof Error) {
    return erro.message;
  }
  return 'Erro inesperado.';
}
