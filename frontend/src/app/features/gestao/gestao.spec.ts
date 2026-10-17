import { ContaPrevista, Hoje, Lancamento } from '../../core/api/modelos';
import { interpretar } from './comando/gramatica';
import { heroi, kpis, linhaLancamento, partesDoPreview, vencimento } from './gestao';

function hoje(extra: Partial<Hoje> = {}): Hoje {
  return {
    hoje: '2026-09-18', mes: '2026-09', fimDoMes: '2026-09-30', diasRestantes: 12,
    livre: 1014, porDia: 84.5, entradas: 5000, saidas: 2500, essenciais: 1000,
    superfluas: 1500, aportes: 300, sobra: 2200, previstasTotal: 210,
    ultimosLancamentos: [], previstas: [], ...extra,
  };
}

function lancamento(extra: Partial<Lancamento>): Lancamento {
  return {
    id: 'l1', data: '2026-09-16', descricao: 'Mercado', categoria: 'Alimentação',
    essencial: false, tipo: 'SAIDA', valor: 120, ...extra,
  };
}

describe('gestao', () => {
  // CA-GESTAO-01 e itens 3 e 6
  it('livre positivo', () => {
    const h = heroi(hoje());

    expect(h.rotulo).toBe('DÁ PRA GASTAR · ATÉ 30 DE SETEMBRO');
    expect(h.valor).toBe('R$ 1.014,00');
    expect(h.texto).toBe('Já descontei as contas que ainda vencem no mês e o aporte das reservas. '
      + 'É dinheiro que você pode gastar sem estragar setembro.');
    expect(h.negativo).toBe(false);
  });

  // CA-GESTAO-02 e CA-GESTAO-03
  it('pílula com o valor por dia e os dias restantes, no singular quando é um', () => {
    expect(heroi(hoje()).pilula).toBe('R$ 84,50 por dia · 12 dias');
    expect(heroi(hoje({ diasRestantes: 1 })).pilula.endsWith('1 dia')).toBe(true);
  });

  // CA-GESTAO-04 e itens 7 e 8
  it('no vermelho', () => {
    const h = heroi(hoje({ livre: -300 }));

    expect(h.rotulo.startsWith('NO VERMELHO ·')).toBe(true);
    expect(h.pilula).toBe('R$ 300,00 no vermelho');
    expect(h.valor).toBe('R$ 300,00');
    expect(h.negativo).toBe(true);
    expect(h.texto).toBe('As contas que ainda vencem e o que já saiu passam do que entrou em '
      + 'setembro. Cortar aqui é escolher o que não vai acontecer.');
  });

  // CA-GESTAO-05 e item 9
  it('mês em branco', () => {
    const h = heroi(hoje({ entradas: 0, saidas: 0, aportes: 0, livre: 0 }));

    expect(h.rotulo).toBe('SEM LANÇAMENTOS AINDA');
    expect(h.valor).toBe('R$ 0,00');
    expect(h.pilula).toBe('setembro está em branco');
    expect(h.texto).toBe('Lance o primeiro gasto na barra abaixo — é só escrever o que aconteceu.');
    expect(h.vazio).toBe(true);
  });

  // CA-GESTAO-06
  it('KPIs com sinais e o medidor de SAIU sobre as entradas', () => {
    const [entrou, saiu] = kpis(hoje());

    expect(entrou).toEqual({ rotulo: 'ENTROU', valor: '+R$ 5.000,00', medidor: 100 });
    expect(saiu).toEqual({ rotulo: 'SAIU', valor: '−R$ 2.500,00', medidor: 50 });
  });

  // CA-GESTAO-07
  it('GUARDEI negativo leva o sinal', () => {
    expect(kpis(hoje({ aportes: -200 }))[2]).toMatchObject({ valor: '−R$ 200,00', medidor: 0 });
    expect(kpis(hoje())[2].valor).toBe('R$ 300,00');
  });

  // CA-GESTAO-08
  it('sem entradas os medidores ficam vazios', () => {
    const [entrou, saiu] = kpis(hoje({ entradas: 0, saidas: 800 }));

    expect(entrou.medidor).toBe(0);
    expect(saiu.medidor).toBe(0);
  });

  it('o medidor para em 100%', () => {
    expect(kpis(hoje({ entradas: 100, saidas: 800 }))[1].medidor).toBe(100);
  });

  // CA-GESTAO-09
  it('saída essencial', () => {
    expect(linhaLancamento(lancamento({ essencial: true }))).toMatchObject({
      dia: '16/09', descricao: 'Mercado', chip: 'essencial', valor: '−R$ 120,00',
      rotuloOculto: 'saída de',
    });
  });

  // CA-GESTAO-10 e item 14
  it('aporte sem sinal, com o chip e o rótulo oculto "guardado"', () => {
    expect(linhaLancamento(lancamento({ tipo: 'APORTE', valor: 300, id: null }))).toMatchObject({
      valor: 'R$ 300,00', chip: 'guardado', rotuloOculto: 'guardado', classe: 'aporte',
    });
    expect(linhaLancamento(lancamento({ tipo: 'SAQUE' }))).toMatchObject({
      chip: 'sacado', rotuloOculto: 'sacado',
    });
    expect(linhaLancamento(lancamento({ tipo: 'ENTRADA', valor: 450 }))).toMatchObject({
      valor: '+R$ 450,00', rotuloOculto: 'entrada de', chip: null,
    });
  });

  // CA-GESTAO-13
  it('vencimento da conta', () => {
    const conta: ContaPrevista = {
      id: 'c1', nome: 'Energia', valor: 210, diaVencimento: 22, vencimento: '2026-09-22',
      recorrente: true,
    };

    expect(vencimento(conta)).toBe('vence 22/09');
  });

  // CA-CMD-19
  it('preview de uma saída', () => {
    const agora = new Date(2026, 8, 16);

    expect(partesDoPreview(interpretar('uber 32 hoje', agora)!)).toEqual({
      verbo: 'vai lançar', valor: 'saída R$ 32,00', extras: ['transporte', '16/09', 'essencial'],
    });
  });

  // CA-CMD-20
  it('preview de uma conta prevista', () => {
    expect(partesDoPreview(interpretar('prever energia 210 dia 22')!)).toEqual({
      verbo: 'vai prever', valor: 'conta R$ 210,00', extras: ['vence dia 22', 'todo mês'],
    });
    expect(partesDoPreview(interpretar('freela 450', new Date(2026, 8, 16))!)).toEqual({
      verbo: 'vai receber', valor: 'entrada R$ 450,00', extras: ['trabalho', '16/09'],
    });
  });
});
