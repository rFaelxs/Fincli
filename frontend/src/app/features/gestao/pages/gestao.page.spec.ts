import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ComandoResponse, Hoje } from '../../../core/api/modelos';
import { ToastService } from '../../../shared/ui/toast.component';
import { GestaoPage } from './gestao.page';

const R = 'R$ ';

function hoje(parcial: Partial<Hoje> = {}): Hoje {
  return {
    hoje: '2026-09-18',
    mes: '2026-09',
    fimDoMes: '2026-09-30',
    diasRestantes: 12,
    livre: 1014,
    porDia: 84.5,
    entradas: 5000,
    saidas: 2500,
    essenciais: 1700,
    superfluas: 800,
    aportes: 300,
    sobra: 2200,
    previstasTotal: 210,
    ultimosLancamentos: [],
    previstas: [],
    ...parcial,
  };
}

/** Deixa as promessas andarem e renderiza: o agendador zoneless pode ficar para depois. */
async function tick() {
  await new Promise((r) => setTimeout(r, 0));
  fixtureAtual?.detectChanges();
}
let fixtureAtual: ComponentFixture<GestaoPage> | undefined;

describe('tela Gestão', () => {
  let fixture: ComponentFixture<GestaoPage>;
  let http: HttpTestingController;
  let el: HTMLElement;

  const texto = (seletor: string) => el.querySelector(seletor)?.textContent?.trim() ?? '';

  /** Responde o card GUARDADO, que recarrega junto com a tela. */
  function responderGuardado() {
    http.match('/api/reservas').forEach((r) => r.flush([]));
  }

  async function abrir(dados: Hoje) {
    fixture = fixtureAtual = TestBed.createComponent(GestaoPage);
    el = fixture.nativeElement;
    fixture.autoDetectChanges();
    await tick();
    http.expectOne('/api/hoje').flush(dados);
    await tick();
    responderGuardado();
    await tick();
  }

  async function digitar(valor: string) {
    const campo = el.querySelector<HTMLInputElement>('#comando')!;
    campo.value = valor;
    campo.dispatchEvent(new Event('input'));
    await tick();
  }

  async function enter() {
    el.querySelector('#comando')!.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter' }));
    await tick();
  }

  async function recarregar(dados: Hoje) {
    http.expectOne('/api/hoje').flush(dados);
    await tick();
    responderGuardado();
    await tick();
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GestaoPage],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  describe('o número grande', () => {
    it('CA-GESTAO-01 e 02: quanto dá para gastar até o fim do mês', async () => {
      await abrir(hoje());
      expect(texto('.eyebrow')).toBe('DÁ PRA GASTAR · ATÉ 30 DE SETEMBRO');
      expect(texto('.heroi-valor')).toBe(R + '1.014,00');
      expect(texto('.pilula')).toBe(R + '84,50 por dia · 12 dias');
    });

    it('CA-GESTAO-03: um dia no singular', async () => {
      await abrir(hoje({ diasRestantes: 1 }));
      expect(texto('.pilula').endsWith('1 dia')).toBe(true);
    });

    it('CA-GESTAO-04: no vermelho, sem sinal no número', async () => {
      await abrir(hoje({ livre: -300 }));
      expect(texto('.eyebrow').startsWith('NO VERMELHO ·')).toBe(true);
      expect(texto('.heroi-valor')).toBe(R + '300,00');
      expect(texto('.pilula')).toBe(R + '300,00 no vermelho');
      expect(el.querySelector('.heroi-valor')!.classList).toContain('negativo');
    });

    it('CA-GESTAO-05: mês em branco destaca a barra de comando', async () => {
      await abrir(hoje({ entradas: 0, saidas: 0, aportes: 0, livre: 0 }));
      expect(texto('.eyebrow')).toBe('SEM LANÇAMENTOS AINDA');
      expect(texto('.pilula')).toBe('setembro está em branco');
      expect(el.querySelector('.comando-secao')!.classList).toContain('destaque');
    });
  });

  describe('o mês em três números', () => {
    it('CA-GESTAO-06 e 07: o sinal vem junto do valor', async () => {
      await abrir(hoje({ aportes: -200 }));
      const valores = [...el.querySelectorAll('.kpi-valor')].map((e) => e.textContent?.trim());
      expect(valores).toEqual(['+' + R + '5.000,00', '−' + R + '2.500,00', '−' + R + '200,00']);
      const barras = [...el.querySelectorAll<HTMLElement>('.meter > i')].map((i) => i.style.width);
      expect(barras.slice(0, 2)).toEqual(['100%', '50%']);
    });
  });

  describe('últimos lançamentos', () => {
    it('CA-GESTAO-09 e 10: saída com sinal; aporte com selo e sem sinal', async () => {
      await abrir(
        hoje({
          ultimosLancamentos: [
            {
              id: 'a',
              data: '2026-09-16',
              descricao: 'Mercado',
              categoria: 'Alimentação',
              essencial: true,
              tipo: 'SAIDA',
              valor: 120,
            },
            {
              id: null,
              data: '2026-09-15',
              descricao: 'Viagem',
              categoria: 'Reserva',
              essencial: false,
              tipo: 'APORTE',
              valor: 300,
            },
          ],
        }),
      );
      const [saida, aporte] = [...el.querySelectorAll('.linha')];
      expect(saida.querySelector('.dia')!.textContent).toBe('16/09');
      expect(saida.querySelector('.chip')!.textContent).toBe('essencial');
      expect(saida.querySelector('.valor')!.textContent).toBe('saída de −' + R + '120,00');
      expect(aporte.querySelector('.chip')!.textContent).toBe('guardado');
      expect(aporte.querySelector('.valor')!.textContent).toBe('guardado ' + R + '300,00');
    });

    it('CA-GESTAO-11: mês sem lançamentos', async () => {
      await abrir(hoje());
      expect(texto('.vazio-lista')).toBe('Nenhum lançamento neste mês ainda.');
    });
  });

  describe('barra de comando', () => {
    it('CA-CMD-19 e 21: o preview substitui as sugestões', async () => {
      await abrir(hoje());
      expect(texto('.sugestoes')).toContain('tente:');

      await digitar('uber 32 hoje');
      const preview = texto('#preview');
      expect(preview).toContain('vai lançar');
      expect(preview).toContain('saída ' + R + '32,00');
      expect(preview).toContain('transporte');
      expect(preview).toContain('essencial');
      expect(el.querySelector('.sugestoes')).toBeNull();
      expect(el.querySelector('#preview')!.getAttribute('aria-live')).toBe('polite');
    });

    it('CA-CMD-24: Esc limpa o campo e o preview', async () => {
      await abrir(hoje());
      await digitar('mercado 120');
      el.querySelector('#comando')!.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
      await tick();
      expect(el.querySelector<HTMLInputElement>('#comando')!.value).toBe('');
      expect(texto('#preview')).toBe('');
    });

    it('texto sem número não é enviado', async () => {
      await abrir(hoje());
      await digitar('mercado sem numero');
      await enter();
      http.expectNone('/api/comando');
    });

    it('CA-CMD-25 e 27: lança, mostra a confirmação e desfaz a saída', async () => {
      await abrir(hoje());
      await digitar('mercado 120');
      await enter();

      const req = http.expectOne('/api/comando');
      expect(req.request.body).toEqual({ texto: 'mercado 120' });
      req.flush({
        tipo: 'SAIDA',
        id: 't1',
        reservaId: null,
        valor: 120,
        descricao: 'Mercado',
        categoria: 'Alimentação',
        mensagem: 'Lançado R$ 120,00 · Mercado',
      } satisfies ComandoResponse);
      await tick();
      await recarregar(hoje());

      expect(texto('.faixa .texto')).toBe('Lançado R$ 120,00 · Mercado');
      expect(texto('.faixa .acao')).toBe('desfazer');
      expect(el.querySelector<HTMLInputElement>('#comando')!.value).toBe('');

      el.querySelector<HTMLButtonElement>('.faixa .acao')!.click();
      await tick();
      const desfazer = http.expectOne('/api/transacoes/t1');
      expect(desfazer.request.method).toBe('DELETE');
      desfazer.flush(null);
      await tick();
      await recarregar(hoje());
      expect(el.querySelector('.faixa')).toBeNull();
    });

    it('CA-CMD-28: desfazer um aporte é um saque de mesmo valor, com aviso', async () => {
      await abrir(hoje());
      const toast = vi.spyOn(TestBed.inject(ToastService), 'mostrar');
      await digitar('guardar 300');
      await enter();
      http.expectOne('/api/comando').flush({
        tipo: 'APORTE',
        id: null,
        reservaId: 'r1',
        valor: 300,
        descricao: 'Reserva de Emergência',
        categoria: 'Aporte',
        mensagem: 'Guardado R$ 300,00 · Reserva de Emergência',
      } satisfies ComandoResponse);
      await tick();
      await recarregar(hoje());

      el.querySelector<HTMLButtonElement>('.faixa .acao')!.click();
      await tick();
      const saque = http.expectOne('/api/reservas/r1/sacar');
      expect(saque.request.body).toEqual({ valor: 300 });
      saque.flush(null);
      await tick();
      await recarregar(hoje());

      expect(toast).toHaveBeenCalledWith('Aporte desfeito — o saque ficou registrado no extrato.');
    });

    it('CA-CMD-31: erro vai para a faixa e esconde as sugestões', async () => {
      await abrir(hoje());
      await digitar('guardar 300 viagem');
      await enter();
      http
        .expectOne('/api/comando')
        .flush(
          { detail: 'Não achei a reserva "Viagem". Crie a reserva antes de guardar nela.' },
          { status: 422, statusText: '' },
        );
      await tick();

      expect(texto('.faixa.erro .texto')).toBe(
        'Não achei a reserva "Viagem". Crie a reserva antes de guardar nela.',
      );
      expect(texto('.faixa .acao')).toBe('tentar de novo');
      expect(el.querySelector('.sugestoes')).toBeNull();
    });
  });

  describe('contas que ainda vencem', () => {
    const energia = {
      id: 'c1',
      nome: 'Energia',
      valor: 210,
      diaVencimento: 22,
      vencimento: '2026-09-22',
      recorrente: true,
    };

    it('CA-GESTAO-13: nome, valor e vencimento', async () => {
      await abrir(hoje({ previstas: [energia] }));
      expect(texto('.prevista .nome')).toBe('Energia');
      expect(texto('.prevista .valor')).toBe(R + '210,00');
      expect(texto('.prevista .vence')).toBe('vence 22/09');
      expect(el.querySelector('.tirar')!.getAttribute('aria-label')).toBe('Remover Energia');
    });

    it('CA-GESTAO-15 e 16: remover e desfazer recria a conta recorrente', async () => {
      await abrir(hoje({ previstas: [energia] }));
      el.querySelector<HTMLButtonElement>('.tirar')!.click();
      await tick();
      http.expectOne('/api/previstas/c1').flush(null);
      await tick();
      await recarregar(hoje());
      expect(texto('.faixa .texto')).toBe('Removido Energia');

      el.querySelector<HTMLButtonElement>('.faixa .acao')!.click();
      await tick();
      const recriar = http.expectOne('/api/previstas');
      expect(recriar.request.body).toEqual({
        nome: 'Energia',
        valor: 210,
        diaVencimento: 22,
        mesReferencia: null,
      });
      recriar.flush(energia);
      await tick();
      await recarregar(hoje({ previstas: [energia] }));
    });

    it('CA-GESTAO-14: sem contas, orienta a usar a barra', async () => {
      await abrir(hoje());
      expect(texto('.previstas .vazio')).toContain('prever energia 210 dia 22');
    });
  });

  it('CA-GESTAO-18 e 19: falha na carga oferece tentar de novo', async () => {
    fixture = fixtureAtual = TestBed.createComponent(GestaoPage);
    el = fixture.nativeElement;
    fixture.autoDetectChanges();
    await tick();
    http.expectOne('/api/hoje').flush(null, { status: 500, statusText: '' });
    await tick();
    expect(texto('.faixa .acao')).toBe('tentar de novo');

    el.querySelector<HTMLButtonElement>('.faixa .acao')!.click();
    await tick();
    await recarregar(hoje());
    expect(el.querySelector('.faixa')).toBeNull();
    expect(texto('.heroi-valor')).toBe(R + '1.014,00');
  });
});
