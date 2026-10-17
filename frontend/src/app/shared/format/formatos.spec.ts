import { iniciais, mesPorExtenso, moeda, pct } from './formatos';

describe('formatos', () => {
  it('moeda em pt-BR com espaço comum depois do símbolo', () => {
    expect(moeda(1500)).toBe('R$ 1.500,00');
    expect(moeda(0)).toBe('R$ 0,00');
  });

  it('percentual com uma casa', () => {
    expect(pct(12.5)).toBe('12,5%');
    expect(pct(0)).toBe('0,0%');
  });

  // CA-SHELL-09
  it('iniciais do primeiro e do último nome', () => {
    expect(iniciais('Rafael Siqueira')).toBe('RS');
    expect(iniciais('rafael de souza siqueira')).toBe('RS');
    expect(iniciais('Rafael')).toBe('R');
    expect(iniciais('  ')).toBe('');
  });

  it('mês por extenso', () => {
    expect(mesPorExtenso(new Date(2026, 8, 16))).toBe('setembro de 2026');
  });
});
