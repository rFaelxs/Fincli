import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Relatorio } from '../../../core/api/modelos';
import { ultimosMeses } from '../../../shared/format/formatos';
import { ToastService } from '../../../shared/ui/toast.component';
import { RelatorioPage } from './relatorio.page';

function relatorio(mes: string, sobra = 500): Relatorio {
  return {
    mes,
    entradas: 5000,
    saidas: 2500,
    essenciais: 1700,
    superfluas: 800,
    aportes: 300,
    sobra,
    historico: [{ mes, sobra }],
    topCategorias: [],
    reservas: [],
    projecao: { jaSaiu: 2500, aindaVence: 0, sobraPrevista: sobra },
  };
}

describe('tela Relatório', () => {
  let fixture: ComponentFixture<RelatorioPage>;
  let http: HttpTestingController;
  let el: HTMLElement;
  const [atual, anterior] = ultimosMeses(2).map((m) => m.valor);

  async function tick() {
    await new Promise((r) => setTimeout(r, 0));
    fixture.detectChanges();
  }

  const botoes = () => [...el.querySelectorAll<HTMLButtonElement>('.pergunta > button')];

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [RelatorioPage],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(RelatorioPage);
    el = fixture.nativeElement;
    fixture.autoDetectChanges();
    await tick();
    http.expectOne(`/api/relatorio?mes=${atual}`).flush(relatorio(atual));
    await tick();
  });

  afterEach(() => http.verify());

  it('CA-REL-02: o seletor oferece 12 meses, o corrente primeiro', () => {
    const opcoes = el.querySelectorAll('select option');
    expect(opcoes.length).toBe(12);
    expect((opcoes[0] as HTMLOptionElement).value).toBe(atual);
  });

  it('CA-REL-14: só a primeira pergunta nasce aberta', () => {
    expect(botoes().map((b) => b.getAttribute('aria-expanded'))).toEqual([
      'true',
      'false',
      'false',
    ]);
  });

  it('CA-REL-03: trocar o mês refaz a chamada; o acordeão continua como estava', async () => {
    botoes()[1].click();
    await tick();

    const seletor = el.querySelector('select')!;
    seletor.value = anterior;
    seletor.dispatchEvent(new Event('change'));
    await tick();
    http.expectOne(`/api/relatorio?mes=${anterior}`).flush(relatorio(anterior, -150));
    await tick();

    expect(el.querySelector('.eyebrow')!.textContent).toContain('RELATÓRIO ·');
    expect(botoes()[1].getAttribute('aria-expanded')).toBe('true');
    expect(el.querySelector('.resposta .destaque')!.textContent).toContain('supérfluas');
  });

  it('CA-REL-28: erro não limpa a tela e vai para o toast', async () => {
    const toast = vi.spyOn(TestBed.inject(ToastService), 'mostrar');
    const seletor = el.querySelector('select')!;
    seletor.value = anterior;
    seletor.dispatchEvent(new Event('change'));
    await tick();
    http.expectOne(`/api/relatorio?mes=${anterior}`).flush(
      { detail: 'Não foi possível concluir a operação. Tente novamente.' },
      {
        status: 500,
        statusText: '',
      },
    );
    await tick();

    expect(toast).toHaveBeenCalledWith('Não foi possível concluir a operação. Tente novamente.');
    expect(el.querySelectorAll('.pergunta').length).toBe(3);
  });
});
