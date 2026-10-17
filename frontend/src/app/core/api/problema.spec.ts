import { HttpErrorResponse } from '@angular/common/http';
import { mensagemDeErro } from './problema';

describe('mensagemDeErro', () => {
  it('junta o detail e as mensagens de campo (CA-SHELL-15)', () => {
    const erro = new HttpErrorResponse({
      status: 400,
      error: { detail: 'Verifique os campos enviados.', campos: { valor: 'Informe o valor.' } },
    });

    const mensagem = mensagemDeErro(erro);

    expect(mensagem).toContain('Verifique os campos enviados.');
    expect(mensagem).toContain('Informe o valor.');
  });

  it('usa só o detail quando não há campos', () => {
    const erro = new HttpErrorResponse({ status: 409, error: { detail: 'CPF já cadastrado.' } });

    expect(mensagemDeErro(erro)).toBe('CPF já cadastrado.');
  });

  it('sem corpo legível mostra "Erro" e o status', () => {
    expect(mensagemDeErro(new HttpErrorResponse({ status: 502, error: 'Bad Gateway' })))
      .toBe('Erro 502');
  });

  it('erro comum mostra a própria mensagem', () => {
    expect(mensagemDeErro(new Error('Sessão expirada.'))).toBe('Sessão expirada.');
  });
});
