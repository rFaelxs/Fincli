import { ItemExtrato } from '../../core/api/modelos';
import { LinhaExtrato, diaPorExtenso, linhasDoExtrato } from './extrato';

function item(extra: Partial<ItemExtrato>): ItemExtrato {
  return {
    origem: 'TRANSACAO', data: '2026-09-16', valor: 120, tipo: 'SAIDA',
    categoria: 'Alimentação', descricao: 'Mercado', ...extra,
  };
}

/** Só os itens, sem os cabeçalhos de dia. */
function itens(linhas: LinhaExtrato[]) {
  return linhas.filter((l) => l.tipo === 'item');
}

describe('extrato', () => {
  it('data por extenso sem passar pelo fuso UTC', () => {
    expect(diaPorExtenso('2026-09-16')).toBe('16 de set. de 2026');
    expect(diaPorExtenso('2026-01-01')).toBe('1 de jan. de 2026');
  });

  // CA-EXT-01: a ordem é a recebida
  it('mantém a ordem do servidor', () => {
    const linhas = linhasDoExtrato([
      item({ origem: 'RESERVA', tipo: 'ALOCACAO', data: '2026-09-18', descricao: 'Viagem' }),
      item({ data: '2026-09-16' }),
    ]);

    expect(itens(linhas).map((l) => l.titulo)).toEqual(['Viagem', 'Mercado']);
  });

  // CA-EXT-02 e item 4
  it('agrupa por dia, com um cabeçalho a cada mudança de data', () => {
    const linhas = linhasDoExtrato([
      item({ data: '2026-09-16' }),
      item({ data: '2026-09-16', descricao: 'Padaria' }),
      item({ data: '2026-09-15', descricao: 'Uber' }),
    ]);

    expect(linhas.map((l) => (l.tipo === 'dia' ? `# ${l.rotulo}` : l.titulo))).toEqual([
      '# 16 de set. de 2026', 'Mercado', 'Padaria', '# 15 de set. de 2026', 'Uber',
    ]);
  });

  // CA-EXT-03
  it('entrada: seta para cima, rótulo e valor com +', () => {
    const [l] = itens(linhasDoExtrato([
      item({ tipo: 'ENTRADA', valor: 450, descricao: 'Freela', categoria: 'Trabalho' }),
    ]));

    expect(l).toMatchObject({ simbolo: '↑', titulo: 'Freela', subtitulo: 'Entrada · Trabalho',
      valor: '+R$ 450,00', selo: 'in' });
  });

  // CA-EXT-04
  it('saída: seta para baixo e valor com −', () => {
    const [l] = itens(linhasDoExtrato([item({})]));

    expect(l).toMatchObject({ simbolo: '↓', valor: '−R$ 120,00', selo: 'out' });
  });

  // CA-EXT-05 e CA-EXT-06
  it('movimentação de reserva vai sem sinal', () => {
    const linhas = itens(linhasDoExtrato([
      item({ origem: 'RESERVA', tipo: 'ALOCACAO', valor: 300, descricao: 'Viagem', categoria: 'Reserva' }),
      item({ origem: 'RESERVA', tipo: 'SAQUE', valor: 300, descricao: 'Viagem', categoria: 'Reserva' }),
    ]));

    expect(linhas[0]).toMatchObject({ simbolo: '◎', titulo: 'Viagem',
      subtitulo: 'Alocação · Reserva', valor: 'R$ 300,00', selo: 'mov' });
    expect(linhas[1]).toMatchObject({ subtitulo: 'Saque · Reserva', valor: 'R$ 300,00' });
  });

  // CA-EXT-07
  it('sem descrição, a categoria vira o título', () => {
    const [l] = itens(linhasDoExtrato([item({ descricao: null })]));

    expect(l).toMatchObject({ titulo: 'Alimentação' });
  });
});
