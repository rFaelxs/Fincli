import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { SILENCIOSO } from '../http/silencioso';
import { SessaoService } from './sessao.service';

/** Deixa as promessas pendentes andarem até a próxima chamada HTTP. */
const esperar = () => new Promise((resolve) => setTimeout(resolve, 0));

describe('SessaoService', () => {
  let http: HttpTestingController;
  let sessao: SessaoService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    sessao = TestBed.inject(SessaoService);
  });

  afterEach(() => http.verify());

  // CA-SHELL-02
  it('pede o CSRF antes de perguntar quem está logado, as duas em silêncio', async () => {
    const pronto = sessao.iniciar();

    const csrf = http.expectOne('/api/csrf');
    expect(csrf.request.context.get(SILENCIOSO)).toBe(true);
    http.expectNone('/api/me');
    csrf.flush(null, { status: 204, statusText: 'No Content' });
    await esperar();

    const me = http.expectOne('/api/me');
    expect(me.request.context.get(SILENCIOSO)).toBe(true);
    me.flush({ id: 'u1', nome: 'Rafael Siqueira' });

    expect(await pronto).toEqual({ id: 'u1', nome: 'Rafael Siqueira' });
    expect(sessao.usuario()?.nome).toBe('Rafael Siqueira');
  });

  it('sem sessão devolve nulo', async () => {
    const pronto = sessao.iniciar();
    http.expectOne('/api/csrf').flush(null);
    await esperar();
    http.expectOne('/api/me').flush({ detail: 'Autenticação necessária.' },
      { status: 401, statusText: 'Unauthorized' });

    expect(await pronto).toBeNull();
    expect(sessao.usuario()).toBeNull();
  });

  // CA-SHELL-13, parte do serviço: erro no logout não impede a saída
  it('sair ignora erro do servidor e esquece o usuário', async () => {
    sessao.entrar({ id: 'u1', nome: 'Rafael' });

    const saiu = sessao.sair();
    http.expectOne('/api/logout').flush({ detail: 'x' }, { status: 500, statusText: 'Erro' });

    await expect(saiu).resolves.toBeUndefined();
    expect(sessao.usuario()).toBeNull();
  });
});
