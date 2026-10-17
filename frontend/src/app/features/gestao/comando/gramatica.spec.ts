import { Comando, interpretar } from './gramatica';

/**
 * Os mesmos casos do `ComandoParserTest.java`. Se um muda, o outro muda junto: é o que mantém o
 * preview fiel ao que o servidor grava.
 */
const HOJE = new Date(2026, 8, 16);

function parse(texto: string): Comando {
  const c = interpretar(texto, HOJE);
  if (!c) {
    throw new Error(`não entendeu: ${texto}`);
  }
  return c;
}

describe('gramatica', () => {
  // CA-CMD-01
  it('mercado 120 ontem', () => {
    expect(parse('mercado 120 ontem')).toMatchObject({
      tipo: 'SAIDA', valor: 120, categoria: 'Alimentação', essencial: true,
      data: '2026-09-15', descricao: 'Mercado',
    });
  });

  // CA-CMD-02 e item 5
  it('a ordem dos termos não importa', () => {
    expect(parse('ontem 120 mercado')).toEqual(parse('mercado 120 ontem'));
    expect(parse('ontem essencial 120 mercado')).toEqual(parse('mercado 120 ontem essencial'));
  });

  // CA-CMD-03 e CA-CMD-04
  it('lazer não é essencial a menos que o texto diga', () => {
    expect(parse('cinema 60')).toMatchObject({ categoria: 'Lazer', essencial: false });
    expect(parse('cinema 60 essencial').essencial).toBe(true);
  });

  // CA-CMD-05
  it('freela 450 é entrada de trabalho', () => {
    expect(parse('freela 450')).toMatchObject({
      tipo: 'ENTRADA', valor: 450, categoria: 'Trabalho', descricao: 'Freela',
    });
  });

  // CA-CMD-06 a CA-CMD-08 e item 3
  it.each([
    ['1.234,56 aluguel', 1234.56],
    ['almoço 12.50', 12.5],
    ['feira 1.234', 1234],
    ['mercado 120 dividido em 3', 120],
    ['padaria 12,5', 12.5],
  ])('lê o valor de "%s"', (texto, esperado) => {
    expect(parse(texto).valor).toBe(esperado);
  });

  it('número colado na palavra ainda é lido', () => {
    expect(parse('mercado120')).toMatchObject({
      valor: 120, descricao: 'Mercado', categoria: 'Alimentação',
    });
  });

  // CA-CMD-09 e item 4
  it.each(['mercado sem numero', 'guardar', '', '   ', 'cafe da manha', '0', '0,00'])(
    'sem número positivo não há comando: "%s"',
    (texto) => {
      expect(interpretar(texto, HOJE)).toBeNull();
    },
  );

  it('palavra-chave com acento cai na mesma categoria', () => {
    expect(parse('farmácia 172,42').categoria).toBe('Saúde');
    expect(parse('almoço 38').categoria).toBe('Alimentação');
    expect(parse('água 90').categoria).toBe('Moradia');
  });

  it('palavra-chave não casa dentro de outra palavra', () => {
    expect(parse('gasolina 200').categoria).toBe('Transporte');
    expect(parse('gas 90').categoria).toBe('Moradia');
    expect(parse('lanche barato 15').categoria).toBe('Alimentação');
    expect(parse('presente barato 30').categoria).toBe('Outros');
  });

  // Item 6
  it('datas relativas', () => {
    expect(parse('uber 32 hoje').data).toBe('2026-09-16');
    expect(parse('uber 32').data).toBe('2026-09-16');
    expect(parse('uber 32 anteontem').data).toBe('2026-09-14');
  });

  it('a descrição preserva acento e maiúscula', () => {
    expect(parse('restaurante japonês 268,40').descricao).toBe('Restaurante japonês');
  });

  // Itens 1 e 2
  it('tipos de entrada', () => {
    expect(parse('recebi 200 do mercado').tipo).toBe('ENTRADA');
    expect(parse('+450 venda do notebook').tipo).toBe('ENTRADA');
    expect(parse('pix da mãe 200').tipo).toBe('ENTRADA');
    expect(parse('freelance 500').tipo).toBe('ENTRADA');
    expect(parse('pixel 300').tipo).toBe('SAIDA');
  });

  it('entrada nunca vem marcada como essencial', () => {
    expect(parse('salário 6800 essencial').essencial).toBe(false);
  });

  // Item 10
  it('descrição vazia recebe rótulo padrão', () => {
    expect(parse('guardar 300').descricao).toBe('Aporte');
    expect(parse('prever 300').descricao).toBe('Conta');
    expect(parse('300').descricao).toBe('Lançamento');
  });

  it('aporte guarda o nome da reserva na descrição', () => {
    expect(parse('guardar 300 viagem')).toMatchObject({
      tipo: 'APORTE', descricao: 'Viagem', categoria: 'Aporte',
    });
  });

  // CA-CMD-11
  it('prever cria conta recorrente com dia', () => {
    expect(parse('prever energia 210 dia 22')).toMatchObject({
      tipo: 'PREVISTA', valor: 210, descricao: 'Energia', diaVencimento: 22, recorrente: true,
    });
  });

  it('o dia não é confundido com o valor', () => {
    expect(parse('prever cartão 980 dia 20').valor).toBe(980);
    expect(parse('prever dia 5 aluguel 1150')).toMatchObject({ valor: 1150, descricao: 'Aluguel' });
  });

  // CA-CMD-12 e item 12
  it.each(['prever ipva 840 dia 15 uma vez', 'prever ipva 840 só este mês',
    'prever ipva 840 so esse mes'])('"%s" é avulsa', (texto) => {
    expect(parse(texto).recorrente).toBe(false);
  });

  it('conta de luz perde o artigo', () => {
    expect(parse('conta de luz 210 dia 10').descricao).toBe('Luz');
  });

  // CA-CMD-13 e item 11
  it('sem dia a conta vence hoje', () => {
    expect(parse('prever faxina 103,10').diaVencimento).toBe(16);
  });
});
