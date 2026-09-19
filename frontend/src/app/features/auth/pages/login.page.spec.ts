import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { SessaoService } from '../../../core/auth/sessao.service';
import { LoginPage } from './login.page';

describe('tela de login', () => {
  let fixture: ComponentFixture<LoginPage>;
  let http: HttpTestingController;
  let el: HTMLElement;

  async function tick() {
    await new Promise((r) => setTimeout(r, 0));
    fixture.detectChanges();
  }

  async function entrar(cpf: string, senha: string) {
    for (const [id, valor] of [
      ['cpf', cpf],
      ['senha', senha],
    ]) {
      const campo = el.querySelector<HTMLInputElement>('#' + id)!;
      campo.value = valor;
      campo.dispatchEvent(new Event('input'));
    }
    el.querySelector('form')!.dispatchEvent(new Event('submit'));
    await tick();
  }

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [LoginPage],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(LoginPage);
    el = fixture.nativeElement;
    fixture.autoDetectChanges();
    await tick();
  });

  it('CA-AUTH-03: envia o CPF como digitado e entra na Gestão', async () => {
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    await entrar('529.982.247-25', 'senhaforte1');

    const req = http.expectOne('/api/login');
    expect(req.request.body).toEqual({ cpf: '529.982.247-25', senha: 'senhaforte1' });
    req.flush({ id: '1', nome: 'Rafael', cpf: '***.***.**25' });
    await tick();

    expect(TestBed.inject(SessaoService).usuario()?.nome).toBe('Rafael');
    expect(navegar).toHaveBeenCalledWith('/gestao', { replaceUrl: true });
  });

  it('CA-AUTH-04 e 05: a mesma frase para CPF ou senha errados', async () => {
    await entrar('529.982.247-25', 'errada123');
    http
      .expectOne('/api/login')
      .flush({ detail: 'CPF ou senha inválidos.' }, { status: 401, statusText: '' });
    await tick();

    const erro = el.querySelector('.form-erro')!;
    expect(erro.classList).toContain('on');
    expect(erro.textContent).toBe('CPF ou senha inválidos.');
  });

  it('CA-AUTH-13: um novo envio apaga o erro anterior', async () => {
    await entrar('1', '1');
    http.expectOne('/api/login').flush(null, { status: 401, statusText: '' });
    await tick();

    await entrar('1', '1');
    expect(el.querySelector('.form-erro')!.classList).not.toContain('on');
    http.expectOne('/api/login');
  });
});
