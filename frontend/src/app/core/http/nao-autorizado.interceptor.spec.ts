import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { SessaoService } from '../auth/sessao.service';
import { SessaoExpirada, naoAutorizadoInterceptor } from './nao-autorizado.interceptor';
import { silencioso } from './silencioso';

describe('interceptor de 401', () => {
  let http: HttpClient;
  let backend: HttpTestingController;
  let navegar: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([naoAutorizadoInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
    navegar = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    TestBed.inject(SessaoService).entrar({ id: '1', nome: 'Rafael', cpf: '***.***.**01' });
  });

  it('CA-SHELL-01: sessão caída leva ao login, sem deixar a tela no histórico', async () => {
    const chamada = firstValueFrom(http.get('/api/reservas'));
    backend.expectOne('/api/reservas').flush(null, { status: 401, statusText: '' });

    await expect(chamada).rejects.toBeInstanceOf(SessaoExpirada);
    await expect(chamada).rejects.toThrow('Sessão expirada.');
    expect(navegar).toHaveBeenCalledWith('/login', { replaceUrl: true });
    expect(TestBed.inject(SessaoService).usuario()).toBeNull();
  });

  it('chamada silenciosa recebe o 401 sem redirecionar', async () => {
    const chamada = firstValueFrom(http.get('/api/me', { context: silencioso() }));
    backend.expectOne('/api/me').flush(null, { status: 401, statusText: '' });

    await expect(chamada).rejects.toMatchObject({ status: 401 });
    expect(navegar).not.toHaveBeenCalled();
  });

  it('outros erros passam direto', async () => {
    const chamada = firstValueFrom(http.get('/api/hoje'));
    backend.expectOne('/api/hoje').flush(null, { status: 500, statusText: '' });

    await expect(chamada).rejects.toMatchObject({ status: 500 });
    expect(navegar).not.toHaveBeenCalled();
  });
});
