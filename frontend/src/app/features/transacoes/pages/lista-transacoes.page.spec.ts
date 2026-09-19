import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Transacao } from '../../../core/api/modelos';
import { ultimosMeses } from '../../../shared/format/formatos';
import { ListaTransacoesPage } from './lista-transacoes.page';

describe('lista de transações', () => {
  let fixture: ComponentFixture<ListaTransacoesPage>;
  let http: HttpTestingController;
  let el: HTMLElement;
  const [atual, anterior] = ultimosMeses(2).map((m) => m.valor);

  const tx = (parcial: Partial<Transacao>): Transacao => ({
    id: crypto.randomUUID(),
    valor: 100,
    categoria: 'Alimentação',
    descricao: 'Mercado',
    data: `${atual}-10`,
    tipo: 'SAIDA',
    essencial: false,
    ...parcial,
  });

  async function tick() {
    await new Promise((r) => setTimeout(r, 0));
    fixture.detectChanges();
  }

  async function abrir(lista: Transacao[]) {
    fixture = TestBed.createComponent(ListaTransacoesPage);
    el = fixture.nativeElement;
    fixture.autoDetectChanges();
    await tick();
    http.expectOne('/api/transacoes').flush(lista);
    await tick();
  }

  const linhas = () => [...el.querySelectorAll('tbody tr')];

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ListaTransacoesPage],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });

  it('CA-TRX-01: começa no mês corrente', async () => {
    await abrir([tx({}), tx({ data: `${anterior}-05` })]);
    expect(linhas().length).toBe(1);
    expect(el.querySelector('.asof')!.textContent).toBe('1 lançamento(s)');
  });

  it('CA-TRX-04 e 05: saída essencial com os dois chips, separados', async () => {
    await abrir([tx({ valor: 120, essencial: true, data: `${atual}-16` })]);
    const [linha] = linhas();
    expect(linha.querySelector('.date')!.textContent).toBe(`16/${atual.slice(5)}`);
    expect(linha.querySelectorAll('.chip').length).toBe(2);
    expect(linha.children[3].textContent!.replace(/\s+/g, ' ').trim()).toBe('↓ saída essencial');
    expect(linha.querySelector('td.num')!.textContent!.trim()).toBe('−120,00');
  });

  it('CA-TRX-06: entrada essencial não mostra o chip', async () => {
    await abrir([tx({ tipo: 'ENTRADA', essencial: true })]);
    expect(linhas()[0].querySelectorAll('.chip').length).toBe(1);
  });

  it('CA-TRX-08 e 09: total do período com sinal', async () => {
    await abrir([tx({ tipo: 'ENTRADA', valor: 5000 }), tx({ valor: 2500 })]);
    expect(el.querySelector('tfoot .num')!.textContent).toBe('+2.500,00');
  });

  it('CA-TRX-02 e 03: filtros por tipo e por texto', async () => {
    await abrir([
      tx({ tipo: 'ENTRADA', categoria: 'Trabalho', descricao: 'Freela' }),
      tx({ descricao: 'Mercado' }),
      tx({ descricao: null, categoria: 'MERCADO livre' }),
    ]);

    const busca = el.querySelector<HTMLInputElement>('#f-busca')!;
    busca.value = 'merc';
    busca.dispatchEvent(new Event('input'));
    await tick();
    expect(linhas().length).toBe(2);

    const tipo = el.querySelectorAll('select')[1];
    tipo.value = 'ENTRADA';
    tipo.dispatchEvent(new Event('change'));
    await tick();
    expect(el.querySelector('tbody .vazio')!.textContent).toBe('Nenhuma transação encontrada.');
    expect(el.querySelector('tfoot .num')!.textContent).toBe('—');
  });

  it('CA-TRX-11: cancelar a confirmação não chama a API', async () => {
    await abrir([tx({})]);
    vi.spyOn(window, 'confirm').mockReturnValue(false);
    el.querySelector<HTMLButtonElement>('button[title="Excluir"]')!.click();
    await tick();
    http.expectNone((r) => r.method === 'DELETE');
  });
});
