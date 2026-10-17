import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { SessaoService } from '../../../core/auth/sessao.service';
import { SILENCIOSO } from '../../../core/http/silencioso';
import { LoginPage } from './login.page';

const esperar = () => new Promise((resolve) => setTimeout(resolve, 0));

describe('LoginPage', () => {
  let fixture: ComponentFixture<LoginPage>;
  let http: HttpTestingController;
  let navegar: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [LoginPage],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    navegar = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    fixture = TestBed.createComponent(LoginPage);
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  function tela(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function preencher(cpf: string, senha: string): void {
    for (const [id, valor] of [['cpf', cpf], ['senha', senha]]) {
      const campo = tela().querySelector<HTMLInputElement>(`#${id}`)!;
      campo.value = valor;
      campo.dispatchEvent(new Event('input'));
    }
  }

  function enviar(): void {
    tela().querySelector('form')!.dispatchEvent(new Event('submit'));
  }

  function erro(): string {
    fixture.detectChanges();
    return tela().querySelector('.form-erro')?.textContent ?? '';
  }

  it('mostra os textos da spec', () => {
    expect(tela().querySelector('h1')?.textContent).toBe('Acesse sua conta');
    expect(tela().textContent).toContain('Receitas, despesas e reservas em um só lugar.');
    expect(tela().querySelector('#cpf')?.getAttribute('inputmode')).toBe('numeric');
    expect(tela().querySelector('#cpf')?.getAttribute('placeholder')).toBe('000.000.000-00');
    expect(tela().querySelector('button[type=submit]')?.textContent).toBe('Entrar');
    expect(tela().querySelector('.auth-alt a')?.getAttribute('href')).toBe('/cadastro');
    // A validação é a do navegador: o Angular não pode pôr `novalidate` no formulário.
    expect(tela().querySelector('form')?.hasAttribute('novalidate')).toBe(false);
  });

  // CA-AUTH-03
  it('envia o CPF com máscara, em silêncio, e vai para a Gestão', async () => {
    preencher('529.982.247-25', 'segredo123');

    enviar();
    const login = http.expectOne('/api/login');
    expect(login.request.body).toEqual({ cpf: '529.982.247-25', senha: 'segredo123' });
    expect(login.request.context.get(SILENCIOSO)).toBe(true);
    login.flush({ id: 'u1', nome: 'Rafael' });
    await esperar();

    expect(TestBed.inject(SessaoService).usuario()?.nome).toBe('Rafael');
    expect(navegar).toHaveBeenCalledWith('/gestao', { replaceUrl: true });
  });

  // CA-AUTH-04 e CA-AUTH-05
  it('credencial recusada mostra a mensagem única', async () => {
    preencher('123.456.789-09', 'errada123');

    enviar();
    http.expectOne('/api/login').flush({ detail: 'CPF ou senha inválidos.' },
      { status: 401, statusText: 'Unauthorized' });
    await esperar();

    expect(erro()).toBe('CPF ou senha inválidos.');
    expect(navegar).not.toHaveBeenCalled();
  });

  // Item 8
  it('outro erro mostra o detail do servidor', async () => {
    preencher('', '');

    enviar();
    http.expectOne('/api/login').flush(
      { detail: 'Verifique os campos enviados.', campos: { cpf: 'Informe seu CPF.' } },
      { status: 400, statusText: 'Bad Request' });
    await esperar();

    expect(erro()).toBe('Verifique os campos enviados. Informe seu CPF.');
  });

  // CA-AUTH-13
  it('o erro some antes do novo envio', async () => {
    preencher('123.456.789-09', 'errada123');
    enviar();
    http.expectOne('/api/login').flush({}, { status: 401, statusText: 'Unauthorized' });
    await esperar();
    expect(erro()).not.toBe('');

    enviar();

    expect(erro()).toBe('');
    http.expectOne('/api/login').flush({ id: 'u1', nome: 'Rafael' });
    await esperar();
  });
});
