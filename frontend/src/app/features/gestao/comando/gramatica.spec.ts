import { Comando, interpretar } from './gramatica';

/**
 * Os mesmos casos de ComandoParserTest.java. Se um lado mudar e o outro não, o preview passa a
 * prometer o que o servidor não grava — é para isso que as duas suítes se espelham.
 */
const HOJE = new Date(2026, 8, 16);

function parse(texto: string): Comando {
  const c = interpretar(texto, HOJE);
  expect(c, `não entendeu: ${texto}`).not.toBeNull();
  return c!;
}

describe('gramática da barra de comando', () => {
  describe('a tabela do README', () => {
    it('mercado 120 ontem essencial', () => {
      const c = parse('mercado 120 ontem essencial');
      expect(c.tipo).toBe('SAIDA');
      expect(c.valor).toBe(120);
      expect(c.descricao).toBe('Mercado');
      expect(c.categoria).toBe('Alimentação');
      expect(c.data).toBe('2026-09-15');
      expect(c.essencial).toBe(true);
    });

    it('guardar 300 viagem', () => {
      const c = parse('guardar 300 viagem');
      expect(c.tipo).toBe('APORTE');
      expect(c.valor).toBe(300);
      expect(c.descricao).toBe('Viagem');
      expect(c.categoria).toBe('Aporte');
      expect(c.essencial).toBe(false);
    });

    it('freela 450', () => {
      const c = parse('freela 450');
      expect(c.tipo).toBe('ENTRADA');
      expect(c.valor).toBe(450);
      expect(c.descricao).toBe('Freela');
      expect(c.categoria).toBe('Trabalho');
      expect(c.data).toBe('2026-09-16');
      expect(c.essencial).toBe(false);
    });

    it('uber 32 hoje', () => {
      const c = parse('uber 32 hoje');
      expect(c.tipo).toBe('SAIDA');
      expect(c.valor).toBe(32);
      expect(c.descricao).toBe('Uber');
      expect(c.categoria).toBe('Transporte');
      expect(c.data).toBe('2026-09-16');
      expect(c.essencial).toBe(true);
    });

    it('show 120', () => {
      const c = parse('show 120');
      expect(c.tipo).toBe('SAIDA');
      expect(c.categoria).toBe('Lazer');
      expect(c.essencial).toBe(false);
    });
  });

  describe('valor', () => {
    it('aceita separador de milhar', () => expect(parse('aluguel 1.234,56').valor).toBe(1234.56));
    it('aceita ponto como decimal', () => expect(parse('padaria 12.50').valor).toBe(12.5));
    it('ponto sem casa decimal é milhar', () => expect(parse('aluguel 1.234').valor).toBe(1234));
    it('usa o primeiro número do texto', () =>
      expect(parse('mercado 120 dividido em 3').valor).toBe(120));

    it('número colado na palavra ainda é lido', () => {
      const c = parse('mercado120');
      expect(c.valor).toBe(120);
      expect(c.descricao).toBe('Mercado');
    });

    it.each(['mercado', 'guardar', '', '   ', 'cafe da manha', '0', '0,00'])(
      'sem número positivo não há comando: "%s"',
      (texto) => expect(interpretar(texto, HOJE)).toBeNull(),
    );

    it('texto nulo não quebra', () => expect(interpretar(null, HOJE)).toBeNull());
  });

  describe('ordem, acento e datas', () => {
    it('a ordem dos termos não importa', () => {
      const direto = parse('mercado 120 ontem essencial');
      const invertido = parse('ontem essencial 120 mercado');
      expect(invertido.tipo).toBe(direto.tipo);
      expect(invertido.valor).toBe(direto.valor);
      expect(invertido.categoria).toBe(direto.categoria);
      expect(invertido.data).toBe(direto.data);
      expect(invertido.essencial).toBe(direto.essencial);
    });

    it('palavra-chave com acento cai na mesma categoria', () => {
      expect(parse('farmácia 172,42').categoria).toBe('Saúde');
      expect(parse('almoço 38').categoria).toBe('Alimentação');
      expect(parse('água 90').categoria).toBe('Moradia');
    });

    it('anteontem volta dois dias', () =>
      expect(parse('uber 32 anteontem').data).toBe('2026-09-14'));

    it('a descrição preserva acento e maiúscula', () =>
      expect(parse('restaurante japonês 268,40').descricao).toBe('Restaurante japonês'));
  });

  describe('tipo', () => {
    it('prefixo de entrada vence a palavra de categoria', () =>
      expect(parse('recebi 200 do mercado').tipo).toBe('ENTRADA'));
    it('mais no começo é entrada', () =>
      expect(parse('+450 venda do notebook').tipo).toBe('ENTRADA'));
    it('palavra de renda no meio também é entrada', () =>
      expect(parse('pix da mãe 200').tipo).toBe('ENTRADA'));
    it('entrada nunca vem marcada como essencial', () =>
      expect(parse('salário 6800 essencial').essencial).toBe(false));
    it('aporte sem descrição recebe rótulo padrão', () =>
      expect(parse('guardar 300').descricao).toBe('Aporte'));
    it('essencial força a marcação em categoria supérflua por padrão', () =>
      expect(parse('show 120 essencial').essencial).toBe(true));
  });

  describe('contas previstas', () => {
    it('prever cria conta recorrente com dia', () => {
      const c = parse('prever energia 210 dia 22');
      expect(c.tipo).toBe('PREVISTA');
      expect(c.valor).toBe(210);
      expect(c.descricao).toBe('Energia');
      expect(c.diaVencimento).toBe(22);
      expect(c.recorrente).toBe(true);
    });

    it('o dia não é confundido com o valor', () => {
      const c = parse('prever cartão 980 dia 20');
      expect(c.valor).toBe(980);
      expect(c.diaVencimento).toBe(20);
    });

    it('dia antes do valor também funciona', () => {
      const c = parse('prever dia 5 aluguel 1150');
      expect(c.valor).toBe(1150);
      expect(c.diaVencimento).toBe(5);
      expect(c.descricao).toBe('Aluguel');
    });

    it('uma vez torna a conta avulsa', () =>
      expect(parse('prever ipva 840 dia 15 uma vez').recorrente).toBe(false));

    it('conta de luz não vira "De luz"', () => {
      const c = parse('conta de luz 210 dia 10');
      expect(c.tipo).toBe('PREVISTA');
      expect(c.descricao).toBe('Luz');
    });

    it('sem dia a conta vence hoje', () =>
      expect(parse('prever faxina 103,10').diaVencimento).toBe(16));
  });
});
