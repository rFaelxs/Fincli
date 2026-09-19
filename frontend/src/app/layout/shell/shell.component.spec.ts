import { Component } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, RouterOutlet, provideRouter } from '@angular/router';
import { SessaoService } from '../../core/auth/sessao.service';
import { ShellComponent } from './shell.component';

@Component({ template: '<input id="comando" />' })
class TelaFalsa {}

describe('shell', () => {
  let router: Router;
  let el: HTMLElement;

  async function tick() {
    await new Promise((r) => setTimeout(r, 0));
  }

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([
          {
            path: '',
            component: ShellComponent,
            children: [
              { path: 'gestao', component: TelaFalsa },
              { path: 'relatorio', component: TelaFalsa },
              { path: 'transacoes', component: TelaFalsa },
            ],
          },
        ]),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    TestBed.inject(SessaoService).entrar({ id: '1', nome: 'Rafael Siqueira', cpf: '***' });
    router = TestBed.inject(Router);
    const fixture = TestBed.createComponent(ShellHost);
    el = fixture.nativeElement;
    fixture.autoDetectChanges();
    await router.navigateByUrl('/transacoes');
    await tick();
    fixture.detectChanges();
    TestBed.inject(HttpTestingController)
      .match('/api/reservas')
      .forEach((r) => r.flush([]));
  });

  const tecla = (key: string, alvo: EventTarget = document.body) =>
    alvo.dispatchEvent(new KeyboardEvent('keydown', { key, bubbles: true }));

  it('CA-SHELL-04: só o item da rota atual tem aria-current', () => {
    const ativos = el.querySelectorAll('nav a[aria-current="page"]');
    expect(ativos.length).toBe(1);
    expect(ativos[0].textContent!.trim()).toBe('Transações');
  });

  it('CA-SHELL-09: iniciais do nome no avatar', () => {
    expect(el.querySelector('.avatar')!.textContent).toBe('RS');
  });

  it('CA-SHELL-10: G leva à Gestão', async () => {
    tecla('g');
    await tick();
    expect(router.url).toBe('/gestao');
  });

  it('CA-SHELL-11: digitando num campo, G é só uma letra', async () => {
    await router.navigateByUrl('/gestao');
    await tick();
    const campo = document.querySelector('#comando')!;
    const navegar = vi.spyOn(router, 'navigateByUrl');
    tecla('r', campo);
    await tick();
    expect(navegar).not.toHaveBeenCalled();
  });

  it('CA-SHELL-12: G na própria Gestão não navega', async () => {
    await router.navigateByUrl('/gestao');
    await tick();
    const navegar = vi.spyOn(router, 'navigateByUrl');
    tecla('g');
    await tick();
    expect(navegar).not.toHaveBeenCalled();
  });
});

@Component({
  imports: [RouterOutlet],
  template: '<router-outlet />',
})
class ShellHost {}
