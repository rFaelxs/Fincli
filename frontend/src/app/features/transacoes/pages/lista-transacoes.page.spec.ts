import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Transacao } from '../../../core/api/modelos';
import { hojeIso } from '../../../shared/format/formatos';
import { ToastService } from '../../../shared/ui/toast.component';
import { ListaTransacoesPage } from './lista-transacoes.page';

const esperar = () => new Promise((resolve) => setTimeout(resolve, 0));
const MES = hojeIso().slice(0, 7);

function transacao(extra: Partial<Transacao>): Transacao {
  return {
    id: 't1',
    valor: 120,
    categoria: 'Alimentação',
    descricao: 'Mercado',
    data: `${MES}-16`,
    tipo: 'SAIDA',
    essencial: false,
    ...extra,
  };
}

describe('ListaTransacoesPage', () => {
  let fixture: ComponentFixture<ListaTransacoesPage>;
  let http: HttpTestingController;

  async function abrir(lista: Transacao[]): Promise<void> {
    TestBed.configureTestingModule({
      imports: [ListaTransacoesPage],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ListaTransacoesPage);
    http.expectOne('/api/transacoes').flush(lista);
    await esperar();
    fixture.detectChanges();
  }

  function tela(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function celulas(linha = 0): string[] {
    const tr = tela().querySelectorAll('tbody tr')[linha];
    return Array.from(tr.querySelectorAll('td')).map((td) => td.textContent?.trim() ?? '');
  }

  afterEach(() => http.verify());

  // CA-TRX-04
  it('pinta a linha com data, descrição, categoria, chip e valor', async () => {
    await abrir([transacao({})]);

    const [data, descricao, categoria, tipo, valor] = celulas();
    expect(data).toBe('16/' + MES.slice(5));
    expect(descricao).toBe('Mercado');
    expect(categoria).toBe('Alimentação');
    expect(tipo).toBe('↓ saída');
    expect(valor).toBe('−120,00');
    expect(tela().querySelector('th.num')?.textContent).toBe('Valor (R$)');
  });

  // CA-TRX-05 e CA-TRX-06
  it('o chip "essencial" só aparece em saída essencial', async () => {
    await abrir([
      transacao({ id: 'a', essencial: true }),
      transacao({ id: 'b', tipo: 'ENTRADA', essencial: true }),
    ]);

    expect(celulas(0)[3]).toContain('essencial');
    expect(celulas(1)[3]).toBe('↑ entrada');
  });

  // CA-TRX-07
  it('descrição nula vira um traço apagado', async () => {
    await abrir([transacao({ descricao: null })]);

    const traco = tela().querySelector('tbody td.desc .muted');
    expect(traco?.textContent).toBe('—');
  });

  // CA-TRX-10 e item 6
  it('sem resultado mostra a mensagem, o total em traço e o contador zerado', async () => {
    await abrir([]);

    expect(tela().querySelector('tbody')?.textContent?.trim()).toBe('Nenhuma transação encontrada.');
    expect(tela().querySelector('tfoot td.num')?.textContent).toBe('—');
    expect(tela().querySelector('.asof')?.textContent).toBe('0 lançamentos');
  });

  it('a busca repinta a cada tecla', async () => {
    await abrir([transacao({ id: 'a', descricao: 'Mercado' }), transacao({ id: 'b', descricao: 'Uber' })]);
    const busca = tela().querySelector<HTMLInputElement>('#filtro-busca')!;

    busca.value = 'ub';
    busca.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    expect(tela().querySelectorAll('tbody tr')).toHaveLength(1);
    expect(celulas()[1]).toBe('Uber');
  });

  // CA-TRX-11
  it('cancelar a confirmação não chama o servidor', async () => {
    await abrir([transacao({})]);
    const confirmar = vi.spyOn(window, 'confirm').mockReturnValue(false);

    tela().querySelector<HTMLButtonElement>('tbody button')!.click();

    expect(confirmar).toHaveBeenCalledWith('Excluir esta transação?');
    http.expectNone('/api/transacoes/t1');
  });

  // CA-TRX-12
  it('exclusão confirmada mostra o toast e recarrega a lista', async () => {
    await abrir([transacao({})]);
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    const toast = vi.spyOn(TestBed.inject(ToastService), 'mostrar');

    tela().querySelector<HTMLButtonElement>('tbody button')!.click();
    const exclusao = http.expectOne('/api/transacoes/t1');
    expect(exclusao.request.method).toBe('DELETE');
    exclusao.flush(null, { status: 204, statusText: 'No Content' });
    await esperar();

    expect(toast).toHaveBeenCalledWith('Transação excluída.');
    http.expectOne('/api/transacoes').flush([]);
    await esperar();
    fixture.detectChanges();
    expect(tela().querySelector('tbody')?.textContent?.trim()).toBe('Nenhuma transação encontrada.');
  });

  // Item 18
  it('erro na exclusão vai para o toast com o detail', async () => {
    await abrir([transacao({})]);
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    const toast = vi.spyOn(TestBed.inject(ToastService), 'mostrar');

    tela().querySelector<HTMLButtonElement>('tbody button')!.click();
    http.expectOne('/api/transacoes/t1').flush({ detail: 'Transação não encontrada.' },
      { status: 404, statusText: 'Not Found' });
    await esperar();

    expect(toast).toHaveBeenCalledWith('Transação não encontrada.');
  });
});
