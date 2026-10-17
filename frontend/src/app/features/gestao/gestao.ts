import { ContaPrevista, Hoje, Lancamento } from '../../core/api/modelos';
import { diaMes, moeda } from '../../shared/format/formatos';
import { Comando } from './comando/gramatica';

/**
 * Textos e proporções da Gestão (spec 03). Nenhuma fórmula financeira: os números vêm prontos do
 * `GET /api/hoje`, e aqui só se escolhe a frase e o formato.
 */

const MESES = ['janeiro', 'fevereiro', 'março', 'abril', 'maio', 'junho', 'julho', 'agosto',
  'setembro', 'outubro', 'novembro', 'dezembro'];

/** "2026-09" → "setembro". O mês vem da resposta, não do relógio do navegador. */
export function nomeDoMes(mes: string): string {
  return MESES[Number(mes.split('-')[1]) - 1];
}

/** "2026-09-30" → "30 DE SETEMBRO". */
export function fimPorExtenso(iso: string): string {
  const [, mes, dia] = iso.split('-');
  return `${Number(dia)} DE ${MESES[Number(mes) - 1].toUpperCase()}`;
}

export interface Heroi {
  rotulo: string;
  valor: string;
  pilula: string;
  texto: string;
  negativo: boolean;
  vazio: boolean;
}

/** O número grande e o que vai em volta dele (itens 3 a 9). */
export function heroi(h: Hoje): Heroi {
  const mes = nomeDoMes(h.mes);
  const fim = fimPorExtenso(h.fimDoMes);
  if (h.entradas === 0 && h.saidas === 0 && h.aportes === 0) {
    return {
      rotulo: 'SEM LANÇAMENTOS AINDA',
      valor: moeda(0),
      pilula: `${mes} está em branco`,
      texto: 'Lance o primeiro gasto na barra abaixo — é só escrever o que aconteceu.',
      negativo: false,
      vazio: true,
    };
  }
  if (h.livre < 0) {
    return {
      rotulo: `NO VERMELHO · ATÉ ${fim}`,
      valor: moeda(Math.abs(h.livre)),
      pilula: `${moeda(Math.abs(h.livre))} no vermelho`,
      texto: `As contas que ainda vencem e o que já saiu passam do que entrou em ${mes}. `
        + 'Cortar aqui é escolher o que não vai acontecer.',
      negativo: true,
      vazio: false,
    };
  }
  const dias = h.diasRestantes === 1 ? '1 dia' : `${h.diasRestantes} dias`;
  return {
    rotulo: `DÁ PRA GASTAR · ATÉ ${fim}`,
    valor: moeda(h.livre),
    pilula: `${moeda(h.porDia)} por dia · ${dias}`,
    texto: 'Já descontei as contas que ainda vencem no mês e o aporte das reservas. '
      + `É dinheiro que você pode gastar sem estragar ${mes}.`,
    negativo: false,
    vazio: false,
  };
}

export interface Kpi {
  rotulo: string;
  valor: string;
  /** Percentual do medidor, entre 0 e 100. */
  medidor: number;
}

function proporcao(parte: number, todo: number): number {
  return todo > 0 ? Math.min(Math.max((parte / todo) * 100, 0), 100) : 0;
}

/**
 * ENTROU, SAIU e GUARDEI (itens 10 e 11). "ENTROU" fica cheio quando há entradas; os outros dois
 * mostram a proporção sobre as entradas, limitada a 100%.
 */
export function kpis(h: Hoje): Kpi[] {
  return [
    { rotulo: 'ENTROU', valor: `+${moeda(h.entradas)}`, medidor: h.entradas > 0 ? 100 : 0 },
    { rotulo: 'SAIU', valor: `−${moeda(h.saidas)}`, medidor: proporcao(h.saidas, h.entradas) },
    {
      rotulo: 'GUARDEI',
      valor: h.aportes < 0 ? `−${moeda(Math.abs(h.aportes))}` : moeda(h.aportes),
      medidor: proporcao(h.aportes, h.entradas),
    },
  ];
}

export interface LinhaLancamento {
  dia: string;
  descricao: string;
  categoria: string;
  chip: string | null;
  valor: string;
  /** Lido só pelo leitor de tela, antes do valor (item 15). */
  rotuloOculto: string;
  classe: 'entrada' | 'saida' | 'aporte';
}

const ROTULO_OCULTO = {
  ENTRADA: 'entrada de', SAIDA: 'saída de', APORTE: 'guardado', SAQUE: 'sacado',
} as const;

/** Uma linha de "Últimos lançamentos" (itens 13 a 15). */
export function linhaLancamento(l: Lancamento): LinhaLancamento {
  const reserva = l.tipo === 'APORTE' || l.tipo === 'SAQUE';
  return {
    dia: diaMes(l.data),
    descricao: l.descricao,
    categoria: l.categoria,
    chip: l.tipo === 'APORTE' ? 'guardado' : l.tipo === 'SAQUE' ? 'sacado'
      : l.essencial ? 'essencial' : null,
    valor: reserva ? moeda(l.valor) : `${l.tipo === 'ENTRADA' ? '+' : '−'}${moeda(l.valor)}`,
    rotuloOculto: ROTULO_OCULTO[l.tipo],
    classe: reserva ? 'aporte' : l.tipo === 'ENTRADA' ? 'entrada' : 'saida',
  };
}

/** "vence 22/09" (item 18). */
export function vencimento(c: ContaPrevista): string {
  return `vence ${diaMes(c.vencimento)}`;
}

/** As partes do preview da barra, na ordem em que aparecem (spec 04, itens 19 a 22). */
export function partesDoPreview(c: Comando): { verbo: string; valor: string; extras: string[] } {
  const verbo = { SAIDA: 'vai lançar', ENTRADA: 'vai receber', APORTE: 'vai guardar',
    PREVISTA: 'vai prever' }[c.tipo];
  const rotulo = { SAIDA: 'saída', ENTRADA: 'entrada', APORTE: 'aporte', PREVISTA: 'conta' }[c.tipo];
  const extras = c.tipo === 'PREVISTA'
    ? [`vence dia ${c.diaVencimento}`, c.recorrente ? 'todo mês' : 'só este mês']
    : [
      c.categoria.toLocaleLowerCase('pt-BR'),
      diaMes(c.data),
      ...(c.tipo === 'SAIDA' ? [c.essencial ? 'essencial' : 'supérflua'] : []),
    ];
  return { verbo, valor: `${rotulo} ${moeda(c.valor)}`, extras };
}
