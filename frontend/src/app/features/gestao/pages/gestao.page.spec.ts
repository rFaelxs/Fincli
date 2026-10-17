import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Hoje, Lancamento } from '../../../core/api/modelos';
import { ToastService } from '../../../shared/ui/toast.component';
import { APORTE_DESFEITO } from '../data/gestao.store';
import { GestaoPage } from './gestao.page';

const esperar = () => new Promise((resolve) => setTimeout(resolve, 0));

const ENERGIA = { id: 'c1', nome: 'Energia', valor: 210, diaVencimento: 22,
  vencimento: '2026-09-22', recorrente: true };

function hoje(extra: Partial<Hoje> = {}): Hoje {
  return {
    hoje: '2026-09-18', mes: '2026-09', fimDoMes: '2026-09-30', diasRestantes: 12,
    livre: 1014, porDia: 84.5, entradas: 5000, saidas: 2500, essenciais: 1000,
    superfluas: 1500, aportes: 300, sobra: 2200, previstasTotal: 210,
    ultimosLancamentos: [], previstas: [ENERGIA], ...extra,
  };
}

describe('GestaoPage', () => {
  let fixture: ComponentFixture<GestaoPage>;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GestaoPage],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(GestaoPage);
    fixture.detectChanges();
  });

  afterEach(() => {
    // O card "GUARDADO" do shell é avisado depois de cada mudança; aqui ele não importa.
    http.match('/api/reservas').forEach((r) => r.flush([]));
    http.verify();
  });

  function cy(nome: string): HTMLElement[] {
    return Array.from((fixture.nativeElement as HTMLElement).querySelectorAll(`[data-cy="${nome}"]`));
  }

  function texto(nome: string, i = 0): string {
    return cy(nome)[i]?.textContent?.trim() ?? '';
  }

  async function carregar(dados: Hoje = hoje()): Promise<void> {
    http.expectOne('/api/hoje').flush(dados);
    await esperar();
    fixture.detectChanges();
  }

  function digitar(valor: string): HTMLInputElement {
    const campo = cy('comando-campo')[0] as HTMLInputElement;
    campo.value = valor;
    campo.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    return campo;
  }

  function teclar(campo: HTMLElement, key: string): void {
    campo.dispatchEvent(new KeyboardEvent('keydown', { key }));
    fixture.detectChanges();
  }

  // Item 2: enquanto carrega, o número e os KPIs ficam em esqueleto
  it('mostra esqueleto até a resposta chegar', async () => {
    expect(cy('gestao-livre')[0].classList.contains('esqueleto')).toBe(true);
    expect(cy('gestao-kpi-valor').map((e) => e.textContent)).toEqual(['R$ 0,00', 'R$ 0,00', 'R$ 0,00']);

    await carregar();

    expect(texto('gestao-livre')).toBe('R$ 1.014,00');
    expect(texto('gestao-rotulo')).toBe('DÁ PRA GASTAR · ATÉ 30 DE SETEMBRO');
  });

  // CA-GESTAO-11
  it('sem lançamentos a lista avisa', async () => {
    await carregar();

    expect(texto('gestao-lancamentos-vazio')).toBe('Nenhum lançamento neste mês ainda.');
    expect(cy('gestao-ver-mes')[0].getAttribute('href')).toBe('/relatorio');
  });

  // CA-GESTAO-12: o servidor já manda no máximo 6
  it('pinta as linhas recebidas', async () => {
    const lista: Lancamento[] = Array.from({ length: 6 }, (_, i) => ({
      id: `l${i}`, data: '2026-09-16', descricao: `Gasto ${i}`, categoria: 'Outros',
      essencial: false, tipo: 'SAIDA', valor: 10,
    }));
    await carregar(hoje({ ultimosLancamentos: lista }));

    expect(cy('gestao-lancamento')).toHaveLength(6);
  });

  // CA-GESTAO-13 e item 17
  it('contas a vencer com título do mês e rótulo de remover', async () => {
    await carregar();

    expect(texto('gestao-contas-titulo')).toBe('Ainda vence em setembro');
    expect(texto('gestao-conta-nome')).toBe('Energia');
    expect(texto('gestao-conta-valor')).toBe('R$ 210,00');
    expect(texto('gestao-conta-vencimento')).toBe('vence 22/09');
    expect(cy('gestao-conta-remover')[0].getAttribute('aria-label')).toBe('Remover Energia');
  });

  // CA-GESTAO-14
  it('sem contas orienta a usar a barra', async () => {
    await carregar(hoje({ previstas: [] }));

    expect(texto('gestao-contas-vazio')).toContain('prever energia 210 dia 22');
  });

  // CA-GESTAO-15 e CA-GESTAO-16
  it('remover conta recorrente e desfazer a recria sem mês', async () => {
    await carregar();

    cy('gestao-conta-remover')[0].click();
    http.expectOne('/api/previstas/c1').flush(null, { status: 204, statusText: 'No Content' });
    await esperar();
    http.expectOne('/api/hoje').flush(hoje({ previstas: [] }));
    await esperar();
    fixture.detectChanges();
    expect(texto('faixa-texto')).toBe('Removido Energia');
    expect(texto('faixa-acao')).toBe('desfazer');

    cy('faixa-acao')[0].click();
    const recria = http.expectOne('/api/previstas');
    expect(recria.request.body).toEqual(
      { nome: 'Energia', valor: 210, diaVencimento: 22, mesReferencia: null });
    recria.flush({}, { status: 201, statusText: 'Created' });
    await esperar();
    http.expectOne('/api/hoje').flush(hoje());
    await esperar();
  });

  // CA-GESTAO-17
  it('desfazer uma conta avulsa repete o mês da tela', async () => {
    await carregar(hoje({ previstas: [{ ...ENERGIA, recorrente: false }] }));

    cy('gestao-conta-remover')[0].click();
    http.expectOne('/api/previstas/c1').flush(null, { status: 204, statusText: 'No Content' });
    await esperar();
    http.expectOne('/api/hoje').flush(hoje({ previstas: [] }));
    await esperar();
    fixture.detectChanges();
    cy('faixa-acao')[0].click();

    expect(http.expectOne('/api/previstas').request.body.mesReferencia).toBe('2026-09');
  });

  // CA-GESTAO-18 e CA-GESTAO-19
  it('erro na carga mostra a faixa e "tentar de novo" recarrega', async () => {
    http.expectOne('/api/hoje').flush({ detail: 'Erro interno.' }, { status: 500, statusText: 'Erro' });
    await esperar();
    fixture.detectChanges();
    expect(texto('faixa-acao')).toBe('tentar de novo');
    expect(cy('faixa')[0].classList.contains('erro')).toBe(true);

    cy('faixa-acao')[0].click();
    fixture.detectChanges();
    expect(cy('faixa')).toHaveLength(0);
    http.expectOne('/api/hoje').flush(hoje());
    await esperar();
  });

  // CA-CMD-09, CA-CMD-21 e item 23
  it('sem preview mostra as sugestões e não envia', async () => {
    await carregar();

    expect(cy('comando-sugestao').map((e) => e.textContent))
      .toEqual(['guardar 300 viagem', 'uber 32 hoje', 'freela 450']);
    const campo = digitar('mercado sem numero');
    expect(cy('comando-preview')[0].textContent?.trim()).toBe('');
    teclar(campo, 'Enter');
    http.expectNone('/api/comando');
  });

  // CA-CMD-22
  it('clicar numa sugestão preenche, foca e mostra o preview', async () => {
    await carregar();

    cy('comando-sugestao')[1].click();
    fixture.detectChanges();

    const campo = cy('comando-campo')[0] as HTMLInputElement;
    expect(campo.value).toBe('uber 32 hoje');
    expect(document.activeElement).toBe(campo);
    expect(texto('comando-preview')).toContain('vai lançar');
  });

  // CA-CMD-24
  it('Esc limpa o campo e o preview', async () => {
    await carregar();
    const campo = digitar('mercado 120');

    teclar(campo, 'Escape');

    expect(campo.value).toBe('');
    expect(cy('comando-preview')[0].textContent?.trim()).toBe('');
  });

  // CA-CMD-25, CA-CMD-27 e CA-CMD-30
  it('Enter envia com o campo desabilitado e o desfazer apaga a transação', async () => {
    await carregar();
    const campo = digitar('mercado 120');

    teclar(campo, 'Enter');
    expect(campo.disabled).toBe(true);
    const envio = http.expectOne('/api/comando');
    expect(envio.request.body).toEqual({ texto: 'mercado 120' });
    envio.flush({ tipo: 'SAIDA', id: 't1', reservaId: null, valor: 120, descricao: 'Mercado',
      categoria: 'Alimentação', mensagem: 'Lançado R$ 120,00 · Mercado' });
    await esperar();
    http.expectOne('/api/hoje').flush(hoje());
    await esperar();
    fixture.detectChanges();

    expect(campo.disabled).toBe(false);
    expect(campo.value).toBe('');
    expect(texto('faixa-texto')).toBe('Lançado R$ 120,00 · Mercado');

    cy('faixa-acao')[0].click();
    expect(http.expectOne('/api/transacoes/t1').request.method).toBe('DELETE');
  });

  // CA-CMD-28
  it('desfazer um aporte saca o mesmo valor e avisa', async () => {
    await carregar();
    const toast = vi.spyOn(TestBed.inject(ToastService), 'mostrar');
    teclar(digitar('guardar 300'), 'Enter');
    http.expectOne('/api/comando').flush({ tipo: 'APORTE', id: null, reservaId: 'r1', valor: 300,
      descricao: 'Reserva de Emergência', categoria: 'Aporte',
      mensagem: 'Guardado R$ 300,00 · Reserva de Emergência' });
    await esperar();
    http.expectOne('/api/hoje').flush(hoje());
    await esperar();
    fixture.detectChanges();

    cy('faixa-acao')[0].click();
    const saque = http.expectOne('/api/reservas/r1/sacar');
    expect(saque.request.body).toEqual({ valor: 300 });
    saque.flush({});
    await esperar();

    expect(toast).toHaveBeenCalledWith(APORTE_DESFEITO);
    http.expectOne('/api/hoje').flush(hoje());
    await esperar();
  });

  // CA-CMD-31
  it('erro no envio mostra a faixa de erro e esconde as sugestões', async () => {
    await carregar();
    teclar(digitar('guardar 300 viagem'), 'Enter');
    http.expectOne('/api/comando').flush(
      { detail: 'Não achei a reserva "viagem". Crie a reserva antes de guardar nela.' },
      { status: 422, statusText: 'Unprocessable' });
    await esperar();
    fixture.detectChanges();

    expect(texto('faixa-texto')).toContain('Não achei a reserva');
    expect(cy('faixa')[0].classList.contains('erro')).toBe(true);
    expect(cy('comando-sugestoes')).toHaveLength(0);
    // O texto fica no campo para o usuário corrigir.
    expect((cy('comando-campo')[0] as HTMLInputElement).value).toBe('guardar 300 viagem');
  });
});
