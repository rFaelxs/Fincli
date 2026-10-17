import { Transacao } from '../../core/api/modelos';
import { contador, filtrar, totalDoPeriodo, totalFormatado, ultimosMeses, valorDaLinha } from './transacoes';

let seq = 0;
function t(data: string, valor: number, extra: Partial<Transacao> = {}): Transacao {
  return {
    id: `t${++seq}`,
    valor,
    categoria: 'Outros',
    descricao: null,
    data,
    tipo: 'SAIDA',
    essencial: false,
    ...extra,
  };
}

describe('transacoes', () => {
  it('oferece os 6 últimos meses por extenso, a partir do corrente', () => {
    const meses = ultimosMeses(6, new Date(2026, 1, 10));

    expect(meses).toHaveLength(6);
    expect(meses[0]).toEqual({ valor: '2026-02', rotulo: 'fevereiro de 2026' });
    expect(meses[5]).toEqual({ valor: '2025-09', rotulo: 'setembro de 2025' });
  });

  // CA-TRX-01
  it('só mostra o mês escolhido', () => {
    const lista = [
      ...Array.from({ length: 5 }, () => t('2026-09-10', 10)),
      ...Array.from({ length: 4 }, () => t('2026-08-10', 10)),
      ...Array.from({ length: 3 }, () => t('2026-07-10', 10)),
    ];

    const visiveis = filtrar(lista, { mes: '2026-09', tipo: 'TODOS', busca: '' });

    expect(visiveis).toHaveLength(5);
    expect(contador(visiveis.length)).toBe('5 lançamentos');
    expect(contador(1)).toBe('1 lançamento');
  });

  // CA-TRX-02
  it('o filtro de tipo esconde o outro tipo', () => {
    const lista = [t('2026-09-01', 10, { tipo: 'ENTRADA' }), t('2026-09-02', 10)];

    const entradas = filtrar(lista, { mes: '2026-09', tipo: 'ENTRADA', busca: '' });

    expect(entradas.every((x) => x.tipo === 'ENTRADA')).toBe(true);
    expect(entradas).toHaveLength(1);
  });

  // CA-TRX-03
  it('a busca casa com descrição ou categoria, em qualquer caixa', () => {
    const lista = [
      t('2026-09-01', 10, { descricao: 'Mercado do mês' }),
      t('2026-09-02', 10, { categoria: 'SUPERMERCADO' }),
      t('2026-09-03', 10, { descricao: 'Uber' }),
    ];

    const achadas = filtrar(lista, { mes: '2026-09', tipo: 'TODOS', busca: 'merc' });

    expect(achadas.map((x) => x.id)).toEqual([lista[0].id, lista[1].id]);
  });

  // CA-TRX-04
  it('o valor da linha leva o sinal do tipo e não leva "R$"', () => {
    expect(valorDaLinha(t('2026-09-16', 120))).toBe('−120,00');
    expect(valorDaLinha(t('2026-09-16', 1234.5, { tipo: 'ENTRADA' }))).toBe('+1.234,50');
  });

  // CA-TRX-08
  it('total do período é entradas menos saídas', () => {
    const lista = [t('2026-09-01', 5000, { tipo: 'ENTRADA' }), t('2026-09-02', 2500)];

    expect(totalFormatado(totalDoPeriodo(lista))).toBe('+2.500,00');
  });

  // CA-TRX-09
  it('total negativo começa com "−"', () => {
    const lista = [t('2026-09-01', 100, { tipo: 'ENTRADA' }), t('2026-09-02', 300)];

    expect(totalFormatado(totalDoPeriodo(lista))).toBe('−200,00');
  });

  // CA-TRX-10
  it('sem resultado o total vira um traço', () => {
    expect(totalDoPeriodo([])).toBeNull();
    expect(totalFormatado(null)).toBe('—');
  });

  it('soma em centavos, sem erro de ponto flutuante', () => {
    const lista = [t('2026-09-01', 0.1, { tipo: 'ENTRADA' }), t('2026-09-02', 0.2, { tipo: 'ENTRADA' })];

    expect(totalDoPeriodo(lista)).toBe(0.3);
  });
});
