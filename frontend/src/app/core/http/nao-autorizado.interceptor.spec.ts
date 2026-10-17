import {
  HttpClient,
  HttpErrorResponse,
  provideHttpClient,
  withInterceptors,
  withXsrfConfiguration,
} from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { SessaoService } from '../auth/sessao.service';
import { naoAutorizadoInterceptor } from './nao-autorizado.interceptor';
import { silencioso } from './silencioso';

describe('naoAutorizadoInterceptor', () => {
  let http: HttpTestingController;
  let cliente: HttpClient;
  let navegar: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(
          withXsrfConfiguration({ cookieName: 'XSRF-TOKEN', headerName: 'X-XSRF-TOKEN' }),
          withInterceptors([naoAutorizadoInterceptor]),
        ),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    cliente = TestBed.inject(HttpClient);
    navegar = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
  });

  afterEach(() => http.verify());

  it('401 em chamada comum leva ao login com "Sessão expirada."', async () => {
    TestBed.inject(SessaoService).entrar({ id: 'u1', nome: 'Rafael' });
    const resposta = firstValueFrom(cliente.get('/api/reservas'));

    http.expectOne('/api/reservas').flush({}, { status: 401, statusText: 'Unauthorized' });

    await expect(resposta).rejects.toThrow('Sessão expirada.');
    expect(navegar).toHaveBeenCalledWith('/login');
    expect(TestBed.inject(SessaoService).usuario()).toBeNull();
  });

  it('401 em chamada silenciosa passa intacto', async () => {
    const resposta = firstValueFrom(cliente.get('/api/me', { context: silencioso() }));

    http.expectOne('/api/me').flush({}, { status: 401, statusText: 'Unauthorized' });

    await expect(resposta).rejects.toBeInstanceOf(HttpErrorResponse);
    expect(navegar).not.toHaveBeenCalled();
  });

  it('outros erros passam intactos', async () => {
    const resposta = firstValueFrom(cliente.get('/api/reservas'));

    http.expectOne('/api/reservas').flush({}, { status: 500, statusText: 'Erro' });

    await expect(resposta).rejects.toBeInstanceOf(HttpErrorResponse);
    expect(navegar).not.toHaveBeenCalled();
  });

  // CA-SHELL-03
  it('mutação leva o token do cookie no header X-XSRF-TOKEN', () => {
    document.cookie = 'XSRF-TOKEN=abc123';

    cliente.post('/api/logout', null).subscribe();

    expect(http.expectOne('/api/logout').request.headers.get('X-XSRF-TOKEN')).toBe('abc123');
  });
});
