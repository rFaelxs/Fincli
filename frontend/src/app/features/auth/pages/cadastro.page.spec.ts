import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { SILENCIOSO } from '../../../core/http/silencioso';
import { CadastroPage } from './cadastro.page';

const esperar = () => new Promise((resolve) => setTimeout(resolve, 0));

describe('CadastroPage', () => {
  let fixture: ComponentFixture<CadastroPage>;
  let http: HttpTestingController;
  let navegar: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [CadastroPage],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    navegar = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    fixture = TestBed.createComponent(CadastroPage);
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  function tela(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function preencher(valores: Record<string, string>): void {
    for (const [id, valor] of Object.entries(valores)) {
      const campo = tela().querySelector<HTMLInputElement>(`#${id}`)!;
      campo.value = valor;
      campo.dispatchEvent(new Event('input'));
    }
  }

  function enviar(): void {
    tela().querySelector('form')!.dispatchEvent(new Event('submit'));
  }

  // Itens 9 a 11
  it('mostra os textos e os limites da spec', () => {
    expect(tela().querySelector('h1')?.textContent).toBe('Criar conta');
    expect(tela().textContent).toContain('A Reserva de Emergência já vem criada.');
    expect(tela().querySelector('label[for=senha]')?.textContent)
      .toBe('Senha (mínimo 8 caracteres)');
    expect(tela().querySelector('#senha')?.getAttribute('minlength')).toBe('8');
    expect(tela().querySelector('#senha')?.getAttribute('maxlength')).toBe('100');
    expect(tela().querySelector('#nome')?.getAttribute('maxlength')).toBe('120');
    expect(tela().querySelector('button[type=submit]')?.textContent).toBe('Cadastrar e entrar');
    expect(tela().textContent)
      .toContain('Atenção: ainda não existe recuperação de senha — guarde-a com cuidado.');
    expect(tela().querySelector('.auth-alt a')?.getAttribute('href')).toBe('/login');
  });

  // CA-AUTH-10
  it('cadastra, entra com as mesmas credenciais e vai para a Gestão', async () => {
    preencher({ nome: 'Rafael', cpf: '529.982.247-25', senha: 'segredo123' });

    enviar();
    const cadastro = http.expectOne('/api/cadastro');
    expect(cadastro.request.body).toEqual(
      { nome: 'Rafael', cpf: '529.982.247-25', senha: 'segredo123' });
    expect(cadastro.request.context.get(SILENCIOSO)).toBe(true);
    http.expectNone('/api/login');
    cadastro.flush({ id: 'u1', nome: 'Rafael' }, { status: 201, statusText: 'Created' });
    await esperar();

    const login = http.expectOne('/api/login');
    expect(login.request.body).toEqual({ cpf: '529.982.247-25', senha: 'segredo123' });
    login.flush({ id: 'u1', nome: 'Rafael' });
    await esperar();

    expect(navegar).toHaveBeenCalledWith('/gestao', { replaceUrl: true });
  });

  // CA-AUTH-07
  it('CPF repetido mostra a mensagem do servidor e não tenta entrar', async () => {
    preencher({ nome: 'Rafael', cpf: '529.982.247-25', senha: 'segredo123' });

    enviar();
    http.expectOne('/api/cadastro').flush({ detail: 'CPF já cadastrado.' },
      { status: 409, statusText: 'Conflict' });
    await esperar();
    fixture.detectChanges();

    expect(tela().querySelector('.form-erro')?.textContent).toBe('CPF já cadastrado.');
    http.expectNone('/api/login');
    expect(navegar).not.toHaveBeenCalled();
  });
});
