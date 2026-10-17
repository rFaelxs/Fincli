import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Reserva } from '../../../core/api/modelos';
import { ToastService } from '../../../shared/ui/toast.component';
import { ListaReservasPage } from './lista-reservas.page';

const esperar = () => new Promise((resolve) => setTimeout(resolve, 0));

function reserva(extra: Partial<Reserva>): Reserva {
  return {
    id: 'r1', nome: 'Viagem', metaValor: 0, saldoAtual: 0, progresso: 0, emergencia: false,
    ...extra,
  };
}

const EMERGENCIA = reserva({ id: 'e', nome: 'Reserva de Emergência', emergencia: true });

describe('ListaReservasPage', () => {
  let fixture: ComponentFixture<ListaReservasPage>;
  let http: HttpTestingController;

  async function abrir(lista: Reserva[]): Promise<void> {
    TestBed.configureTestingModule({
      imports: [ListaReservasPage],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ListaReservasPage);
    http.expectOne('/api/reservas').flush(lista);
    await esperar();
    fixture.detectChanges();
  }

  function cy(nome: string): HTMLElement[] {
    return Array.from((fixture.nativeElement as HTMLElement).querySelectorAll(`[data-cy="${nome}"]`));
  }

  function texto(nome: string, i = 0): string {
    return cy(nome)[i]?.textContent?.trim() ?? '';
  }

  afterEach(() => http.verify());

  // CA-RES-01: a ordem é a do servidor, emergência primeiro e as demais por nome
  it('mostra os cards na ordem recebida', async () => {
    await abrir([EMERGENCIA, reserva({ id: 'c', nome: 'Carro' }), reserva({ id: 'v', nome: 'Viagem' })]);

    expect(cy('reserva-nome').map((e) => e.textContent)).toEqual(
      ['Reserva de Emergência', 'Carro', 'Viagem']);
  });

  // CA-RES-02
  it('o subtítulo soma o guardado', async () => {
    await abrir([reserva({ id: 'a', saldoAtual: 1200 }), reserva({ id: 'b', saldoAtual: 300 })]);

    expect(texto('reservas-total')).toBe('R$ 1.500,00 guardados no total');
  });

  // CA-RES-03
  it('o card mostra o progresso e saldo / meta', async () => {
    await abrir([reserva({ saldoAtual: 1250, metaValor: 10000, progresso: 12.5 })]);

    expect(texto('reserva-progresso')).toBe('12,5%');
    expect(texto('reserva-saldo-meta')).toBe('R$ 1.250,00 / R$ 10.000,00');
  });

  // CA-RES-04
  it('o medidor para em 100% mesmo com o progresso maior', async () => {
    await abrir([reserva({ saldoAtual: 12000, metaValor: 10000, progresso: 120 })]);

    expect(texto('reserva-progresso')).toBe('120,0%');
    const medidor = (fixture.nativeElement as HTMLElement).querySelector('.meter')!;
    expect(medidor.getAttribute('aria-valuenow')).toBe('100');
    expect((medidor.querySelector('i') as HTMLElement).style.width).toBe('100%');
  });

  // CA-RES-06
  it('a emergência mostra "protegida" e nenhum botão de excluir', async () => {
    await abrir([EMERGENCIA]);

    expect(texto('reserva-protegida')).toBe('protegida');
    expect(cy('reserva-excluir')).toHaveLength(0);
  });

  // Item 6
  it('as ações levam às rotas da reserva', async () => {
    await abrir([reserva({ id: 'r9' })]);

    expect(cy('reserva-alocar')[0].getAttribute('href')).toBe('/reservas/r9/alocar');
    expect(cy('reserva-sacar')[0].getAttribute('href')).toBe('/reservas/r9/sacar');
    expect(cy('reserva-meta')[0].getAttribute('href')).toBe('/reservas/r9/meta');
    expect(cy('reservas-nova')[0].getAttribute('href')).toBe('/reservas/nova');
  });

  // CA-RES-07
  it('cancelar a confirmação não chama o servidor', async () => {
    await abrir([reserva({})]);
    const confirmar = vi.spyOn(window, 'confirm').mockReturnValue(false);

    cy('reserva-excluir')[0].click();

    expect(confirmar).toHaveBeenCalledWith('Excluir esta reserva?');
    http.expectNone('/api/reservas/r1');
  });

  // CA-RES-08
  it('exclusão confirmada mostra o toast e recarrega', async () => {
    await abrir([reserva({})]);
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    const toast = vi.spyOn(TestBed.inject(ToastService), 'mostrar');

    cy('reserva-excluir')[0].click();
    const req = http.expectOne('/api/reservas/r1');
    expect(req.request.method).toBe('DELETE');
    req.flush(null, { status: 204, statusText: 'No Content' });
    await esperar();

    expect(toast).toHaveBeenCalledWith('Reserva excluída.');
    // Dois GETs: o da lista e o do card "GUARDADO" do shell, que não recarregaria sozinho.
    const recargas = http.match('/api/reservas');
    expect(recargas).toHaveLength(2);
    recargas.forEach((req) => req.flush([EMERGENCIA]));
  });

  // CA-RES-09
  it('reserva com saldo: o 409 vai para o toast', async () => {
    await abrir([reserva({ saldoAtual: 300 })]);
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    const toast = vi.spyOn(TestBed.inject(ToastService), 'mostrar');

    cy('reserva-excluir')[0].click();
    http.expectOne('/api/reservas/r1').flush(
      { detail: 'A reserva ainda tem saldo. Saque o valor antes de excluí-la.' },
      { status: 409, statusText: 'Conflict' });
    await esperar();

    expect(toast).toHaveBeenCalledWith('A reserva ainda tem saldo. Saque o valor antes de excluí-la.');
  });
});
