import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter, withComponentInputBinding } from '@angular/router';
import { hojeIso } from '../../../shared/format/formatos';
import { EditarTransacaoPage } from './editar-transacao.page';
import { NovaTransacaoPage } from './nova-transacao.page';

const esperar = () => new Promise((resolve) => setTimeout(resolve, 0));

function configurar() {
  TestBed.configureTestingModule({
    providers: [
      provideRouter([], withComponentInputBinding()),
      provideHttpClient(),
      provideHttpClientTesting(),
    ],
  });
  return {
    http: TestBed.inject(HttpTestingController),
    navegar: vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true),
  };
}

function campo<T extends HTMLElement = HTMLInputElement>(raiz: HTMLElement, id: string): T {
  return raiz.querySelector<T>(`#${id}`)!;
}

function digitar(raiz: HTMLElement, id: string, valor: string): void {
  const el = campo(raiz, id);
  el.value = valor;
  el.dispatchEvent(new Event('input'));
}

function enviar(raiz: HTMLElement): void {
  raiz.querySelector('form')!.dispatchEvent(new Event('submit'));
}

describe('NovaTransacaoPage', () => {
  let fixture: ComponentFixture<NovaTransacaoPage>;
  let http: HttpTestingController;
  let navegar: ReturnType<typeof vi.spyOn>;
  let tela: HTMLElement;

  beforeEach(() => {
    ({ http, navegar } = configurar());
    fixture = TestBed.createComponent(NovaTransacaoPage);
    fixture.detectChanges();
    tela = fixture.nativeElement as HTMLElement;
  });

  afterEach(() => http.verify());

  // Itens 19 a 22 e CA-TRX-13
  it('nasce com os textos da spec, a data de hoje e saída', () => {
    expect(tela.querySelector('h1')?.textContent).toBe('Nova transação');
    expect(tela.querySelector('.sub')?.textContent).toBe('Registre uma entrada ou saída.');
    expect(campo(tela, 'data').value).toBe(hojeIso());
    expect(campo<HTMLSelectElement>(tela, 'tipo').value).toBe('SAIDA');
    expect(tela.querySelector<HTMLInputElement>('input[type=checkbox]')!.checked).toBe(false);
    expect(campo(tela, 'descricao').getAttribute('placeholder')).toBe('Ex.: Mercado do mês');
    expect(campo(tela, 'categoria').getAttribute('maxlength')).toBe('80');
    expect(campo(tela, 'valor').getAttribute('step')).toBe('0.01');
    expect(tela.querySelector('button[type=submit]')?.textContent).toBe('Salvar');
  });

  // CA-TRX-14 e CA-TRX-15
  it('descrição em branco vai nula e o sucesso troca a rota', async () => {
    digitar(tela, 'descricao', '   ');
    digitar(tela, 'categoria', 'Alimentação');
    digitar(tela, 'valor', '120.5');

    enviar(tela);
    const req = http.expectOne('/api/transacoes');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      descricao: null,
      categoria: 'Alimentação',
      valor: 120.5,
      data: hojeIso(),
      tipo: 'SAIDA',
      essencial: false,
    });
    req.flush({}, { status: 201, statusText: 'Created' });
    await esperar();

    expect(navegar).toHaveBeenCalledWith('/transacoes', { replaceUrl: true });
  });

  // CA-TRX-16 e CA-TRX-17
  it('erro aparece no topo e os campos continuam preenchidos', async () => {
    digitar(tela, 'categoria', 'Alimentação');
    digitar(tela, 'valor', '-10');

    enviar(tela);
    http.expectOne('/api/transacoes').flush(
      {
        detail: 'Verifique os campos enviados.',
        campos: { valor: 'O valor da transação não pode ser negativo.' },
      },
      { status: 400, statusText: 'Bad Request' });
    await esperar();
    fixture.detectChanges();

    const erro = tela.querySelector('.form-erro')?.textContent ?? '';
    expect(erro).toContain('O valor da transação não pode ser negativo.');
    // O bloco de erro é o primeiro item do formulário, acima dos campos.
    expect(tela.querySelector('form')!.firstElementChild?.tagName).toBe('APP-ERRO-FORMULARIO');
    expect(campo(tela, 'categoria').value).toBe('Alimentação');
    expect(navegar).not.toHaveBeenCalled();
  });

  // CA-TRX-22
  it('Cancelar volta para a lista sem chamar a API', () => {
    const cancelar = tela.querySelector<HTMLAnchorElement>('.form-acts a')!;

    expect(cancelar.textContent).toBe('Cancelar');
    expect(cancelar.getAttribute('href')).toBe('/transacoes');
  });
});

describe('EditarTransacaoPage', () => {
  let fixture: ComponentFixture<EditarTransacaoPage>;
  let http: HttpTestingController;
  let navegar: ReturnType<typeof vi.spyOn>;
  let tela: HTMLElement;

  beforeEach(() => {
    ({ http, navegar } = configurar());
    fixture = TestBed.createComponent(EditarTransacaoPage);
    fixture.componentRef.setInput('id', 't1');
    fixture.detectChanges();
    tela = fixture.nativeElement as HTMLElement;
  });

  afterEach(() => http.verify());

  // Itens 28 e 29 e CA-TRX-18
  it('carrega, preenche o formulário e troca o subtítulo', async () => {
    expect(tela.querySelector('.sub')?.textContent).toBe('Carregando…');
    expect(tela.querySelector('form')).toBeNull();

    http.expectOne('/api/transacoes/t1').flush({
      id: 't1', valor: 120, categoria: 'Alimentação', descricao: null,
      data: '2026-09-16', tipo: 'SAIDA', essencial: true,
    });
    await esperar();
    fixture.detectChanges();

    expect(tela.querySelector('.sub')?.textContent).toBe('Altere os campos e salve.');
    expect(campo(tela, 'valor').value).toBe('120');
    expect(campo(tela, 'data').value).toBe('2026-09-16');
    expect(campo(tela, 'descricao').value).toBe('');
    expect(tela.querySelector('button[type=submit]')?.textContent).toBe('Salvar alterações');
  });

  // CA-TRX-19
  it('falha na carga deixa o formulário escondido e mostra a mensagem', async () => {
    http.expectOne('/api/transacoes/t1').flush({ detail: 'Transação não encontrada.' },
      { status: 404, statusText: 'Not Found' });
    await esperar();
    fixture.detectChanges();

    expect(tela.querySelector('form')).toBeNull();
    expect(tela.querySelector('.sub')?.textContent).toBe('Transação não encontrada.');
  });

  // CA-TRX-21
  it('salva com PUT e volta para a lista', async () => {
    http.expectOne('/api/transacoes/t1').flush({
      id: 't1', valor: 120, categoria: 'Alimentação', descricao: 'Mercado',
      data: '2026-09-16', tipo: 'SAIDA', essencial: false,
    });
    await esperar();
    fixture.detectChanges();

    digitar(tela, 'valor', '150');
    enviar(tela);
    const req = http.expectOne('/api/transacoes/t1');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body.valor).toBe(150);
    expect(req.request.body.descricao).toBe('Mercado');
    req.flush({});
    await esperar();

    expect(navegar).toHaveBeenCalledWith('/transacoes', { replaceUrl: true });
  });
});
