import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { SessaoFalsa } from '../../../testing/sessao-falsa';
import { routes } from '../../app.routes';
import { Reserva } from '../../core/api/modelos';
import { SessaoService } from '../../core/auth/sessao.service';

const esperar = () => new Promise((resolve) => setTimeout(resolve, 0));

describe('ShellComponent', () => {
  let harness: RouterTestingHarness;
  let router: Router;
  let http: HttpTestingController;
  let sessao: SessaoFalsa;

  beforeEach(async () => {
    sessao = new SessaoFalsa({ id: 'u1', nome: 'Rafael Siqueira' });
    TestBed.configureTestingModule({
      providers: [
        provideRouter(routes, withComponentInputBinding()),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: SessaoService, useValue: sessao },
      ],
    });
    harness = await RouterTestingHarness.create();
    router = TestBed.inject(Router);
    http = TestBed.inject(HttpTestingController);
  });

  /** Navega e responde o GET do card "GUARDADO" que toda navegação dispara. */
  async function abrir(url: string, reservas: Reserva[] = []): Promise<void> {
    await harness.navigateByUrl(url);
    http.expectOne('/api/reservas').flush(reservas);
    await esperar();
    harness.fixture.detectChanges();
  }

  function tela(): HTMLElement {
    return harness.fixture.nativeElement as HTMLElement;
  }

  function teclar(key: string, opcoes: KeyboardEventInit = {}, alvo: EventTarget = document) {
    alvo.dispatchEvent(new KeyboardEvent('keydown', { key, bubbles: true, ...opcoes }));
  }

  // Item 4 e CA-SHELL-04
  it('lista a navegação na ordem e marca só a rota atual', async () => {
    await abrir('/transacoes');

    const links = Array.from(tela().querySelectorAll<HTMLAnchorElement>('nav a'));
    expect(links.map((a) => a.textContent?.trim()))
      .toEqual(['Gestão', 'Relatório', 'Transações', 'Reservas', 'Extrato']);
    const atuais = links.filter((a) => a.getAttribute('aria-current') === 'page');
    expect(atuais.map((a) => a.textContent?.trim())).toEqual(['Transações']);
    expect(links.map((a) => a.getAttribute('data-cy'))).toEqual([
      'shell-nav-gestao', 'shell-nav-relatorio', 'shell-nav-transacoes', 'shell-nav-reservas',
      'shell-nav-extrato',
    ]);
  });

  // Item 5
  it('a marca leva à Gestão com rótulo acessível', async () => {
    await abrir('/extrato');

    const marca = tela().querySelector('a.marca');
    expect(marca?.getAttribute('aria-label')).toBe('FinCLI, ir para Gestão');
    expect(marca?.getAttribute('href')).toBe('/gestao');
  });

  // CA-SHELL-05
  it('o card soma o guardado', async () => {
    await abrir('/gestao', [
      { id: 'a', nome: 'Emergência', metaValor: 0, saldoAtual: 1200, progresso: 0, emergencia: true },
      { id: 'b', nome: 'Viagem', metaValor: 0, saldoAtual: 300, progresso: 0, emergencia: false },
    ]);

    expect(tela().querySelector('.resumo .val')?.textContent).toBe('R$ 1.500,00');
    expect(tela().querySelector('.resumo .leg')?.textContent).toBe('em 2 reservas');
  });

  // CA-SHELL-08
  it('falha no card não derruba a tela', async () => {
    await harness.navigateByUrl('/gestao');
    http.expectOne('/api/reservas').flush({}, { status: 500, statusText: 'Erro' });
    await esperar();
    harness.fixture.detectChanges();

    expect(tela().querySelector('.resumo .val')?.textContent).toBe('—');
    expect(tela().querySelector('.resumo .leg')?.textContent).toBe('não consegui carregar');
    expect(tela().querySelector('main h1')?.textContent).toBe('Gestão');
  });

  // CA-SHELL-09 e item 11
  it('o rodapé mostra as iniciais e o nome', async () => {
    await abrir('/gestao');

    expect(tela().querySelector('.avatar')?.textContent?.trim()).toBe('RS');
    expect(tela().querySelector('.who')?.textContent).toBe('Rafael Siqueira');
  });

  // CA-SHELL-10
  it('G leva à Gestão', async () => {
    await abrir('/relatorio');

    teclar('g');
    await harness.fixture.whenStable();

    expect(router.url).toBe('/gestao');
  });

  it('R leva ao Relatório', async () => {
    await abrir('/gestao');

    teclar('R');
    await harness.fixture.whenStable();

    expect(router.url).toBe('/relatorio');
  });

  // CA-SHELL-11
  it('letra digitada num campo de texto não navega', async () => {
    await abrir('/relatorio');
    const campo = document.createElement('input');
    document.body.appendChild(campo);

    teclar('g', {}, campo);
    await harness.fixture.whenStable();

    expect(router.url).toBe('/relatorio');
    campo.remove();
  });

  it('atalho com Ctrl, Meta ou Alt não navega', async () => {
    await abrir('/relatorio');

    teclar('g', { ctrlKey: true });
    teclar('g', { metaKey: true });
    teclar('g', { altKey: true });
    await harness.fixture.whenStable();

    expect(router.url).toBe('/relatorio');
  });

  // CA-SHELL-12
  it('G na própria Gestão não dispara navegação', async () => {
    await abrir('/gestao');
    const navegar = vi.spyOn(router, 'navigateByUrl');

    teclar('g');

    expect(navegar).not.toHaveBeenCalled();
  });

  // CA-SHELL-13
  it('Sair vai para o login', async () => {
    await abrir('/gestao');

    tela().querySelector<HTMLButtonElement>('.side-foot button')?.click();
    await harness.fixture.whenStable();

    expect(sessao.sair).toHaveBeenCalled();
    expect(router.url).toBe('/login');
  });

  it('o link de pulo leva o foco ao conteúdo sem sair da rota', async () => {
    await abrir('/extrato');
    const pular = tela().querySelector<HTMLAnchorElement>('a.pular');

    expect(pular?.textContent).toBe('Pular para o conteúdo');
    pular?.click();

    expect(document.activeElement?.id).toBe('conteudo');
    expect(router.url).toBe('/extrato');
  });
});
