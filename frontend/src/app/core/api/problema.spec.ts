import { HttpErrorResponse } from '@angular/common/http';
import { lerErro } from './problema';

const erro = (status: number, error: unknown) => new HttpErrorResponse({ status, error });

describe('lerErro', () => {
  it('usa o detail do problem+json', () => {
    expect(lerErro(erro(409, { detail: 'CPF já cadastrado.' }))).toBe('CPF já cadastrado.');
  });

  it('acrescenta as mensagens de campo', () => {
    const corpo = {
      detail: 'Verifique os campos enviados.',
      campos: { valor: 'Informe o valor.', categoria: 'Informe a categoria.' },
    };
    expect(lerErro(erro(400, corpo))).toBe(
      'Verifique os campos enviados. Informe o valor. Informe a categoria.',
    );
  });

  it('sem corpo legível, diz o status', () => {
    expect(lerErro(erro(502, '<html>Bad Gateway</html>'))).toBe('Erro 502');
  });

  it('erro comum devolve a própria mensagem', () => {
    expect(lerErro(new Error('Sessão expirada.'))).toBe('Sessão expirada.');
  });
});
