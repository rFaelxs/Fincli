/**
 * Formatação de dinheiro, percentual e datas.
 *
 * Só formata: nenhum valor é recalculado aqui. Datas da API chegam em ISO puro
 * (`yyyy-MM-dd`) e são lidas como texto — `new Date('2026-09-16')` é interpretada como UTC e vira
 * 15/09 em fuso negativo.
 */

const BRL = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
const INTEIRO = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 0 });

const MESES = [
  'janeiro',
  'fevereiro',
  'março',
  'abril',
  'maio',
  'junho',
  'julho',
  'agosto',
  'setembro',
  'outubro',
  'novembro',
  'dezembro',
];
const MESES_CURTOS = [
  'jan',
  'fev',
  'mar',
  'abr',
  'mai',
  'jun',
  'jul',
  'ago',
  'set',
  'out',
  'nov',
  'dez',
];

/* ── Dinheiro ── */

/** 1500 → "R$ 1.500,00"; negativo leva o sinal do próprio Intl. */
export function moeda(valor: number): string {
  return BRL.format(valor);
}

/** Valor sem sinal: quem mostra o `+`/`−` é a tela, junto com o rótulo textual. */
export function moedaAbs(valor: number): string {
  return BRL.format(Math.abs(Number(valor)));
}

/** Forma compacta para rótulo de barra: 3210 → "3,2k"; abaixo de mil, o inteiro. */
export function moedaCurta(valor: number): string {
  const n = Math.abs(Number(valor));
  if (n >= 1000) {
    return (n / 1000).toFixed(1).replace('.', ',') + 'k';
  }
  return INTEIRO.format(n);
}

/** Número com duas casas, sem símbolo de moeda: 120 → "120,00". */
export function decimal(valor: number): string {
  return Number(valor).toLocaleString('pt-BR', { minimumFractionDigits: 2 });
}

/** 12.5 → "12,5%" */
export function pct(valor: number): string {
  return Number(valor).toFixed(1).replace('.', ',') + '%';
}

/* ── Datas ── */

function mesDe(iso: string): number {
  return Number(iso.slice(5, 7)) - 1;
}

/** '2026-09-16' → '16/09' */
export function diaMes(iso: string): string {
  return iso.slice(8, 10) + '/' + iso.slice(5, 7);
}

/** '2026-09-16' → '16 de setembro' */
export function diaPorExtenso(iso: string): string {
  return Number(iso.slice(8, 10)) + ' de ' + MESES[mesDe(iso)];
}

/** '2026-09-16' → '16 de set. de 2026' */
export function dataCurta(iso: string): string {
  return iso.slice(8, 10) + ' de ' + MESES_CURTOS[mesDe(iso)] + '. de ' + iso.slice(0, 4);
}

/** '2026-09' → 'setembro de 2026' */
export function mesPorExtenso(iso: string): string {
  return MESES[mesDe(iso)] + ' de ' + iso.slice(0, 4);
}

/** '2026-09' → 'setembro' — sem o ano, para uso no meio de uma frase */
export function soMes(iso: string): string {
  return MESES[mesDe(iso)];
}

/** '2026-09' → 'set' */
export function mesCurto(iso: string): string {
  return MESES_CURTOS[mesDe(iso)];
}

/** '2026-09' → 'set/26' — usado onde o ano importa (previsão de meta) */
export function mesAno(iso: string): string {
  return MESES_CURTOS[mesDe(iso)] + '/' + iso.slice(2, 4);
}

/** Data local em ISO — `toISOString` converteria para UTC e poderia voltar um dia. */
export function isoLocal(data: Date): string {
  return (
    data.getFullYear() +
    '-' +
    String(data.getMonth() + 1).padStart(2, '0') +
    '-' +
    String(data.getDate()).padStart(2, '0')
  );
}

export interface OpcaoMes {
  valor: string;
  rotulo: string;
}

/** Os `n` últimos meses, do corrente para trás, para os seletores de mês. */
export function ultimosMeses(n: number, agora: Date = new Date()): OpcaoMes[] {
  const lista: OpcaoMes[] = [];
  for (let i = 0; i < n; i++) {
    const d = new Date(agora.getFullYear(), agora.getMonth() - i, 1);
    const valor = d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0');
    lista.push({ valor, rotulo: mesPorExtenso(valor) });
  }
  return lista;
}
