import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter, withComponentInputBinding } from '@angular/router';
import { Reserva } from '../../../core/api/modelos';
import { EditarMetaPage } from './editar-meta.page';
import { MovimentarPage, Operacao } from './movimentar.page';
import { NovaReservaPage } from './nova-reserva.page';

const esperar = () => new Promise((resolve) => setTimeout(resolve, 0));

const VIAGEM: Reserva = {
  id: 'r1', nome: 'Viagem', metaValor: 5000, saldoAtual: 100, progresso: 2, emergencia: false,
};

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

function cy<T extends HTMLElement = HTMLElement>(fixture: ComponentFixture<unknown>, nome: string): T {
  return (fixture.nativeElement as HTMLElement).querySelector<T>(`[data-cy="${nome}"]`)!;
}

function digitar(fixture: ComponentFixture<unknown>, nome: string, valor: string): void {
  const campo = cy<HTMLInputElement>(fixture, nome);
  campo.value = valor;
  campo.dispatchEvent(new Event('input'));
}

function enviar(fixture: ComponentFixture<unknown>): void {
  (fixture.nativeElement as HTMLElement).querySelector('form')!.dispatchEvent(new Event('submit'));
}

function erro(fixture: ComponentFixture<unknown>): string {
  fixture.detectChanges();
  return cy(fixture, 'form-erro').textContent?.trim() ?? '';
}

describe('NovaReservaPage', () => {
  let fixture: ComponentFixture<NovaReservaPage>;
  let http: HttpTestingController;
  let navegar: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    ({ http, navegar } = configurar());
    fixture = TestBed.createComponent(NovaReservaPage);
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  // Itens 12 a 14
  it('mostra os textos da spec', () => {
    const tela = fixture.nativeElement as HTMLElement;
    expect(tela.querySelector('h1')?.textContent).toBe('Nova reserva');
    expect(tela.querySelector('.sub')?.textContent).toBe('Uma meta para guardar dinheiro com propósito.');
    expect(cy(fixture, 'reserva-form-nome').getAttribute('placeholder')).toBe('Ex.: Viagem');
    expect(cy(fixture, 'reserva-form-nome').getAttribute('maxlength')).toBe('120');
    expect(cy(fixture, 'reserva-form-criar').textContent).toBe('Criar');
    expect(cy(fixture, 'reserva-form-cancelar').getAttribute('href')).toBe('/reservas');
  });

  // CA-RES-11
  it('cria e volta para a lista', async () => {
    digitar(fixture, 'reserva-form-nome', 'Viagem');
    digitar(fixture, 'reserva-form-meta', '8000');

    enviar(fixture);
    const req = http.expectOne('/api/reservas');
    expect(req.request.body).toEqual({ nome: 'Viagem', metaValor: 8000 });
    req.flush(VIAGEM, { status: 201, statusText: 'Created' });
    await esperar();

    expect(navegar).toHaveBeenCalledWith('/reservas', { replaceUrl: true });
  });

  // CA-RES-12 e CA-RES-13
  it('erro do servidor fica no topo do formulário', async () => {
    digitar(fixture, 'reserva-form-nome', 'Viagem');
    digitar(fixture, 'reserva-form-meta', '-1');

    enviar(fixture);
    http.expectOne('/api/reservas').flush(
      { detail: 'Verifique os campos enviados.',
        campos: { metaValor: 'A meta da reserva não pode ser negativa.' } },
      { status: 400, statusText: 'Bad Request' });
    await esperar();

    expect(erro(fixture)).toContain('A meta da reserva não pode ser negativa.');
    expect(cy<HTMLInputElement>(fixture, 'reserva-form-nome').value).toBe('Viagem');
    expect(navegar).not.toHaveBeenCalled();
  });
});

describe('EditarMetaPage', () => {
  let fixture: ComponentFixture<EditarMetaPage>;
  let http: HttpTestingController;
  let navegar: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    ({ http, navegar } = configurar());
    fixture = TestBed.createComponent(EditarMetaPage);
    fixture.componentRef.setInput('id', 'r1');
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  // CA-RES-14 e item 18
  it('carrega com a meta vigente e o resumo', async () => {
    expect(cy(fixture, 'reserva-subtitulo').textContent).toBe('Carregando…');

    http.expectOne('/api/reservas/r1').flush(VIAGEM);
    await esperar();
    fixture.detectChanges();

    expect(cy(fixture, 'reserva-subtitulo').textContent).toBe('Defina o novo valor-alvo.');
    expect(cy<HTMLInputElement>(fixture, 'reserva-meta-valor').value).toBe('5000');
    expect(cy(fixture, 'reserva-resumo-nome').textContent).toBe('Viagem');
    expect(cy(fixture, 'reserva-resumo-guardado').textContent).toBe('R$ 100,00');
    expect(cy(fixture, 'reserva-resumo-disponivel')).toBeNull();
  });

  // CA-RES-15
  it('salva com PUT e volta para a lista', async () => {
    http.expectOne('/api/reservas/r1').flush(VIAGEM);
    await esperar();
    fixture.detectChanges();

    digitar(fixture, 'reserva-meta-valor', '12000');
    enviar(fixture);
    const req = http.expectOne('/api/reservas/r1/meta');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ valor: 12000 });
    req.flush(VIAGEM);
    await esperar();

    expect(navegar).toHaveBeenCalledWith('/reservas', { replaceUrl: true });
  });

  // Item 21
  it('falha na carga esconde o formulário e mostra a mensagem', async () => {
    http.expectOne('/api/reservas/r1').flush({ detail: 'Reserva não encontrada.' },
      { status: 404, statusText: 'Not Found' });
    await esperar();
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).querySelector('form')).toBeNull();
    expect(cy(fixture, 'reserva-subtitulo').textContent).toBe('Reserva não encontrada.');
  });
});

describe('MovimentarPage', () => {
  let fixture: ComponentFixture<MovimentarPage>;
  let http: HttpTestingController;
  let navegar: ReturnType<typeof vi.spyOn>;

  async function abrir(operacao: Operacao): Promise<void> {
    ({ http, navegar } = configurar());
    fixture = TestBed.createComponent(MovimentarPage);
    fixture.componentRef.setInput('id', 'r1');
    fixture.componentRef.setInput('operacao', operacao);
    fixture.detectChanges();
  }

  afterEach(() => http.verify());

  // Itens 22, 25 e CA-RES-16
  it('alocar lê o saldo disponível e mostra no resumo', async () => {
    await abrir('alocar');
    http.expectOne('/api/saldo').flush({ saldoDisponivel: 500 });
    http.expectOne('/api/reservas/r1').flush(VIAGEM);
    await esperar();
    fixture.detectChanges();

    const tela = fixture.nativeElement as HTMLElement;
    expect(tela.querySelector('h1')?.textContent).toBe('Alocar na reserva');
    expect(cy(fixture, 'reserva-subtitulo').textContent)
      .toBe('Move dinheiro do saldo disponível para a reserva.');
    expect(cy(fixture, 'reserva-resumo-disponivel').textContent).toBe('R$ 500,00');
    expect(cy(fixture, 'reserva-movimento-enviar').textContent?.trim()).toBe('Alocar');
    expect(cy(fixture, 'reserva-movimento-valor').getAttribute('min')).toBe('0.01');
  });

  // Item 23 e CA-RES-17
  it('sacar não lê o saldo e não mostra a linha', async () => {
    await abrir('sacar');
    http.expectNone('/api/saldo');
    http.expectOne('/api/reservas/r1').flush(VIAGEM);
    await esperar();
    fixture.detectChanges();

    const tela = fixture.nativeElement as HTMLElement;
    expect(tela.querySelector('h1')?.textContent).toBe('Sacar da reserva');
    expect(tela.textContent).not.toContain('Saldo disponível:');
    expect(cy(fixture, 'reserva-movimento-enviar').textContent?.trim()).toBe('Sacar');
  });

  // CA-RES-19
  it('alocação aceita volta para a lista', async () => {
    await abrir('alocar');
    http.expectOne('/api/saldo').flush({ saldoDisponivel: 500 });
    http.expectOne('/api/reservas/r1').flush(VIAGEM);
    await esperar();
    fixture.detectChanges();

    digitar(fixture, 'reserva-movimento-valor', '300');
    enviar(fixture);
    const req = http.expectOne('/api/reservas/r1/alocar');
    expect(req.request.body).toEqual({ valor: 300 });
    req.flush(VIAGEM);
    await esperar();

    expect(navegar).toHaveBeenCalledWith('/reservas', { replaceUrl: true });
  });

  // CA-RES-18 e CA-RES-21
  it('alocação recusada mostra a mensagem e não sai da tela', async () => {
    await abrir('alocar');
    http.expectOne('/api/saldo').flush({ saldoDisponivel: 200 });
    http.expectOne('/api/reservas/r1').flush(VIAGEM);
    await esperar();
    fixture.detectChanges();

    digitar(fixture, 'reserva-movimento-valor', '300');
    enviar(fixture);
    http.expectOne('/api/reservas/r1/alocar').flush(
      { detail: 'Saldo insuficiente para esta alocação.' },
      { status: 400, statusText: 'Bad Request' });
    await esperar();

    expect(erro(fixture)).toBe('Saldo insuficiente para esta alocação.');
    expect(navegar).not.toHaveBeenCalled();
  });

  // CA-RES-20
  it('saque acima do saldo da reserva mostra a mensagem', async () => {
    await abrir('sacar');
    http.expectOne('/api/reservas/r1').flush(VIAGEM);
    await esperar();
    fixture.detectChanges();

    digitar(fixture, 'reserva-movimento-valor', '150');
    enviar(fixture);
    http.expectOne('/api/reservas/r1/sacar').flush(
      { detail: 'Saldo da reserva insuficiente para este saque.' },
      { status: 400, statusText: 'Bad Request' });
    await esperar();

    expect(erro(fixture)).toBe('Saldo da reserva insuficiente para este saque.');
  });

  it('se o saldo não carregar, a linha mostra um traço', async () => {
    await abrir('alocar');
    http.expectOne('/api/saldo').flush({}, { status: 500, statusText: 'Erro' });
    http.expectOne('/api/reservas/r1').flush(VIAGEM);
    await esperar();
    fixture.detectChanges();

    expect(cy(fixture, 'reserva-resumo-disponivel').textContent).toBe('—');
  });
});
