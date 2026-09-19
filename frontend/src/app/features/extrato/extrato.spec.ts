import { ItemExtrato } from '../../core/api/modelos';
import { linhasDoExtrato } from './extrato';

const R = 'R$ ';

const item = (parcial: Partial<ItemExtrato>): ItemExtrato => ({
  origem: 'TRANSACAO',
  data: '2026-09-16',
  valor: 120,
  tipo: 'SAIDA',
  categoria: 'Alimentação',
  descricao: 'Mercado',
  ...parcial,
});

describe('linhas do extrato', () => {
  it('CA-EXT-02: um cabeçalho por dia, só na primeira linha dele', () => {
    const linhas = linhasDoExtrato([
      item({ data: '2026-09-16' }),
      item({ data: '2026-09-16' }),
      item({ data: '2026-09-15' }),
    ]);
    expect(linhas.map((l) => l.dia)).toEqual(['16 de set. de 2026', null, '15 de set. de 2026']);
  });

  it('CA-EXT-03: entrada leva seta para cima e sinal de mais', () => {
    const [l] = linhasDoExtrato([
      item({ tipo: 'ENTRADA', valor: 450, categoria: 'Trabalho', descricao: 'Freela' }),
    ]);
    expect(l).toMatchObject({
      glifo: '↑',
      classe: 'in',
      titulo: 'Freela',
      subtitulo: 'Entrada · Trabalho',
      valor: '+' + R + '450,00',
      tom: 'money-in',
    });
  });

  it('CA-EXT-04: saída leva seta para baixo e sinal de menos', () => {
    const [l] = linhasDoExtrato([item({})]);
    expect(l).toMatchObject({ glifo: '↓', valor: '−' + R + '120,00', tom: 'money-out' });
  });

  it('CA-EXT-05: movimentação de reserva não leva sinal', () => {
    const [l] = linhasDoExtrato([
      item({
        origem: 'RESERVA',
        tipo: 'ALOCACAO',
        valor: 300,
        categoria: 'Reserva',
        descricao: 'Viagem',
      }),
    ]);
    expect(l).toMatchObject({
      glifo: '◎',
      classe: 'mov',
      titulo: 'Viagem',
      subtitulo: 'Alocação · Reserva',
      valor: R + '300,00',
      tom: '',
    });
  });

  it('CA-EXT-07: sem descrição, o título é a categoria', () => {
    const [l] = linhasDoExtrato([item({ descricao: null })]);
    expect(l.titulo).toBe('Alimentação');
  });
});
