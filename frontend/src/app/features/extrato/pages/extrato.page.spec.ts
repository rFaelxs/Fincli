import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ToastService } from '../../../shared/ui/toast.component';
import { ExtratoPage } from './extrato.page';

const esperar = () => new Promise((resolve) => setTimeout(resolve, 0));

describe('ExtratoPage', () => {
  let fixture: ComponentFixture<ExtratoPage>;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ExtratoPage],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ExtratoPage);
  });

  afterEach(() => http.verify());

  function cy(nome: string): HTMLElement[] {
    return Array.from((fixture.nativeElement as HTMLElement).querySelectorAll(`[data-cy="${nome}"]`));
  }

  async function responder(corpo: object, status = 200): Promise<void> {
    http.expectOne('/api/extrato').flush(corpo, { status, statusText: String(status) });
    await esperar();
    fixture.detectChanges();
  }

  // Item 2
  it('mostra o título e o subtítulo da spec', async () => {
    await responder([]);

    const tela = fixture.nativeElement as HTMLElement;
    expect(tela.querySelector('h1')?.textContent).toBe('Extrato');
    expect(tela.querySelector('.sub')?.textContent)
      .toBe('transações e movimentações de reserva · cronológico');
  });

  // CA-EXT-02, 03 e item 5
  it('pinta os dias e os itens numa lista plana', async () => {
    await responder([
      { origem: 'TRANSACAO', data: '2026-09-16', valor: 450, tipo: 'ENTRADA',
        categoria: 'Trabalho', descricao: 'Freela' },
      { origem: 'TRANSACAO', data: '2026-09-16', valor: 120, tipo: 'SAIDA',
        categoria: 'Alimentação', descricao: 'Mercado' },
    ]);

    expect(cy('extrato-dia').map((e) => e.textContent)).toEqual(['16 de set. de 2026']);
    expect(cy('extrato-item')).toHaveLength(2);
    expect(cy('extrato-titulo')[0].textContent).toBe('Freela');
    expect(cy('extrato-subtitulo')[0].textContent).toBe('Entrada · Trabalho');
    expect(cy('extrato-valor')[0].textContent).toBe('+R$ 450,00');
    // O símbolo é decorativo para leitor de tela.
    expect(cy('extrato-simbolo')[0].getAttribute('aria-hidden')).toBe('true');
    // Os itens são filhos diretos do .feed: o CSS depende disso.
    expect(cy('extrato-item')[0].parentElement?.classList.contains('feed')).toBe(true);
  });

  // CA-EXT-08
  it('sem nenhum item mostra "Nada por aqui ainda."', async () => {
    await responder([]);

    expect(cy('extrato-vazio')[0].textContent).toBe('Nada por aqui ainda.');
  });

  // CA-EXT-09
  it('erro vai para o toast e a lista fica vazia', async () => {
    const toast = vi.spyOn(TestBed.inject(ToastService), 'mostrar');

    await responder({ detail: 'Não foi possível concluir a operação. Tente novamente.' }, 500);

    expect(toast).toHaveBeenCalledWith('Não foi possível concluir a operação. Tente novamente.');
    expect(cy('extrato-item')).toHaveLength(0);
    expect(cy('extrato-vazio')).toHaveLength(0);
  });
});
