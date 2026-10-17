/**
 * Formatação pt-BR. O front não calcula regra financeira: só apresenta o que a API devolve.
 */

const BRL = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
const UMA_CASA = new Intl.NumberFormat('pt-BR', {
  minimumFractionDigits: 1,
  maximumFractionDigits: 1,
});

/**
 * 1500 → "R$ 1.500,00". O Intl põe um espaço inseparável depois do "R$"; aqui ele vira espaço
 * comum, para o texto bater com o que as specs e o servidor escrevem. A classe `.num` do CSS
 * já impede a quebra de linha.
 */
export function moeda(valor: number): string {
  return BRL.format(valor).replace(/\s/g, ' ');
}

/** 12.5 → "12,5%". */
export function pct(valor: number): string {
  return `${UMA_CASA.format(valor)}%`;
}

/** "Rafael Siqueira" → "RS": a inicial do primeiro e a do último nome. */
export function iniciais(nome: string): string {
  const partes = nome.trim().split(/\s+/).filter(Boolean);
  if (partes.length === 0) {
    return '';
  }
  const escolhidas = partes.length === 1 ? [partes[0]] : [partes[0], partes[partes.length - 1]];
  return escolhidas.map((p) => p[0]).join('').toUpperCase();
}

/** O mês da data, por extenso: "outubro de 2026". */
export function mesPorExtenso(data: Date): string {
  return data.toLocaleDateString('pt-BR', { month: 'long', year: 'numeric' });
}
