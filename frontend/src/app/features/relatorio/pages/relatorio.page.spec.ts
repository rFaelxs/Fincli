import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Relatorio } from '../../../core/api/modelos';
import { hojeIso } from '../../../shared/format/formatos';
import { ToastService } from '../../../shared/ui/toast.component';
import { PerguntasAbertas } from '../data/relatorio.service';
import { RelatorioPage } from './relatorio.page';

const esperar = () => new Promise((resolve) => setTimeout(resolve, 0));
const MES_ATUAL = hojeIso().slice(0, 7);

function relatorio(mes: string, sobra = 500): Relatorio {
  return {
    mes, entradas: 5000, saidas: 2000, essenciais: 1200, superfluas: 800, aportes: 300, sobra,
    historico: [{ mes, sobra }], topCategorias: [], reservas: [],
    projecao: { jaSaiu: 2000, aindaVence: 0, sobraPrevista: sobra },
  };
}

describe('RelatorioPage', () => {
  let fixture: ComponentFixture<RelatorioPage>;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [RelatorioPage],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    // O estado dos acordeões é da aplicação; cada teste começa do padrão.
    TestBed.inject(PerguntasAbertas).abertas.set([true, false, false]);
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(RelatorioPage);
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  function cy(nome: string): HTMLElement[] {
    return Array.from((fixture.nativeElement as HTMLElement).querySelectorAll(`[data-cy="${nome}"]`));
  }

  async function responder(mes: string, corpo: object = relatorio(mes), status = 200) {
    http.expectOne((r) => r.url === '/api/relatorio' && r.params.get('mes') === mes)
      .flush(corpo, { status, statusText: String(status) });
    await esperar();
    fixture.detectChanges();
  }

  // CA-REL-01 e CA-REL-02
  it('abre no mês corrente, com 12 opções por extenso', async () => {
    await responder(MES_ATUAL);

    const opcoes = Array.from((cy('relatorio-mes')[0] as HTMLSelectElement).options);
    expect(opcoes).toHaveLength(12);
    expect(opcoes[0].value).toBe(MES_ATUAL);
    expect(opcoes[0].textContent).toMatch(/^[a-zç]+ de \d{4}$/);
    expect(cy('relatorio-mes')[0].getAttribute('aria-label')).toBe('Mês do relatório');
    expect(cy('relatorio-eyebrow')[0].textContent).toMatch(/^RELATÓRIO · [A-ZÇ]+ DE \d{4}$/);
  });

  // CA-REL-03
  it('trocar o mês refaz a chamada e repinta', async () => {
    await responder(MES_ATUAL);
    const select = cy('relatorio-mes')[0] as HTMLSelectElement;
    const outro = select.options[1].value;

    select.value = outro;
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    await responder(outro, relatorio(outro, -150));

    expect(cy('relatorio-total')[4].textContent).toBe('−R$ 150,00');
  });

  // CA-REL-14 e item 17
  it('só a primeira pergunta nasce aberta', async () => {
    await responder(MES_ATUAL);

    const botoes = cy('relatorio-pergunta-botao');
    expect(botoes.map((b) => b.getAttribute('aria-expanded'))).toEqual(['true', 'false', 'false']);
    expect(botoes[0].getAttribute('aria-controls')).toBe('resposta-0');
    expect(botoes[0].textContent).toContain('▴');
    expect(botoes[1].textContent).toContain('▾');
  });

  // CA-REL-15 e item 18
  it('abrir uma pergunta mantém o foco no botão dela', async () => {
    await responder(MES_ATUAL);
    const botao = cy('relatorio-pergunta-botao')[1];

    botao.focus();
    botao.click();
    fixture.detectChanges();

    expect(botao.getAttribute('aria-expanded')).toBe('true');
    expect(document.activeElement).toBe(botao);
  });

  // CA-REL-07
  it('mês sem nada mostra o bloco único', async () => {
    await responder(MES_ATUAL, { ...relatorio(MES_ATUAL, 0), essenciais: 0, superfluas: 0, aportes: 0 });

    expect(cy('relatorio-fluxo-vazio')[0].textContent).toBe('nada lançado neste mês');
    expect(cy('relatorio-fluxo')[0].getAttribute('aria-label')).toBe('nada lançado neste mês');
  });

  // CA-REL-28
  it('erro vai para o toast e não limpa a tela', async () => {
    await responder(MES_ATUAL);
    const toast = vi.spyOn(TestBed.inject(ToastService), 'mostrar');
    const select = cy('relatorio-mes')[0] as HTMLSelectElement;
    const outro = select.options[1].value;

    select.value = outro;
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    await responder(outro, { detail: 'Erro interno.' }, 500);

    expect(toast).toHaveBeenCalledWith('Erro interno.');
    expect(cy('relatorio-total')[0].textContent).toBe('+R$ 5.000,00');
  });
});
