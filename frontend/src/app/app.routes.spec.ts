import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { SessaoFalsa } from '../testing/sessao-falsa';
import { routes } from './app.routes';
import { SessaoService } from './core/auth/sessao.service';

/** Monta as rotas reais com uma sessão decidida pelo teste. */
async function montar(logado: boolean) {
  const sessao = new SessaoFalsa(logado ? { id: 'u1', nome: 'Rafael Siqueira' } : null);
  TestBed.configureTestingModule({
    providers: [
      provideRouter(routes, withComponentInputBinding()),
      provideHttpClient(),
      provideHttpClientTesting(),
      { provide: SessaoService, useValue: sessao },
    ],
  });
  const harness = await RouterTestingHarness.create();
  return { harness, router: TestBed.inject(Router), sessao };
}

describe('rotas', () => {
  // O card "GUARDADO" do shell faz o GET dele; aqui só importa para onde a rota foi.
  afterEach(() => TestBed.inject(HttpTestingController).match('/api/reservas'));

  // CA-SHELL-01
  it('sem sessão, uma rota autenticada vai para o login', async () => {
    const { harness, router } = await montar(false);

    await harness.navigateByUrl('/gestao');

    expect(router.url).toBe('/login');
  });

  // CA-AUTH-01
  it('com sessão, o login vai para a Gestão sem mostrar o formulário', async () => {
    const { harness, router } = await montar(true);

    await harness.navigateByUrl('/login');

    expect(router.url).toBe('/gestao');
    expect(harness.routeNativeElement?.querySelector('form')).toBeNull();
  });

  it('com sessão, o cadastro também vai para a Gestão', async () => {
    const { harness, router } = await montar(true);

    await harness.navigateByUrl('/cadastro');

    expect(router.url).toBe('/gestao');
  });

  // CA-AUTH-02
  it('sem sessão, o login aparece', async () => {
    const { harness, router } = await montar(false);

    await harness.navigateByUrl('/login');

    expect(router.url).toBe('/login');
    expect(harness.routeNativeElement?.textContent).toContain('Acesse sua conta');
  });

  it('a raiz decide pela sessão', async () => {
    const comSessao = await montar(true);
    await comSessao.harness.navigateByUrl('/');
    expect(comSessao.router.url).toBe('/gestao');

    TestBed.resetTestingModule();
    const semSessao = await montar(false);
    await semSessao.harness.navigateByUrl('/');
    expect(semSessao.router.url).toBe('/login');
  });

  // CA-SHELL-16
  it('/dashboard redireciona para a Gestão', async () => {
    const { harness, router } = await montar(true);

    await harness.navigateByUrl('/dashboard');

    expect(router.url).toBe('/gestao');
  });

  it('a sessão já conhecida não é perguntada de novo a cada tela', async () => {
    const { harness, sessao } = await montar(true);

    await harness.navigateByUrl('/gestao');
    await harness.navigateByUrl('/extrato');

    expect(sessao.iniciar).not.toHaveBeenCalled();
  });
});
