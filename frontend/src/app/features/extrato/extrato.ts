import { ItemExtrato } from '../../core/api/modelos';
import { moeda } from '../../shared/format/formatos';

/** O que a lista plana do extrato pinta: um cabeçalho de dia ou um item. */
export type LinhaExtrato =
  | { tipo: 'dia'; rotulo: string }
  | {
      tipo: 'item';
      simbolo: string;
      /** Classe do selo: entrada, saída ou movimentação. */
      selo: 'in' | 'out' | 'mov';
      titulo: string;
      subtitulo: string;
      valor: string;
    };

const SIMBOLO = { ENTRADA: '↑', SAIDA: '↓', ALOCACAO: '◎', SAQUE: '◎' } as const;
const SELO = { ENTRADA: 'in', SAIDA: 'out', ALOCACAO: 'mov', SAQUE: 'mov' } as const;
const ROTULO = { ENTRADA: 'Entrada', SAIDA: 'Saída', ALOCACAO: 'Alocação', SAQUE: 'Saque' } as const;

const DIA_POR_EXTENSO = new Intl.DateTimeFormat('pt-BR', {
  day: 'numeric',
  month: 'short',
  year: 'numeric',
});

/**
 * "2026-09-16" → "16 de set. de 2026". A data é montada a partir das partes do texto, no relógio
 * local; `new Date(iso)` leria a meia-noite UTC e, no Brasil, cairia no dia anterior.
 */
export function diaPorExtenso(iso: string): string {
  const [ano, mes, dia] = iso.split('-').map(Number);
  return DIA_POR_EXTENSO.format(new Date(ano, mes - 1, dia));
}

/**
 * Transação leva `+` ou `−`; movimentação de reserva vai sem sinal, porque o dinheiro mudou de
 * lugar e não entrou nem saiu (spec 08, itens 9 e 10).
 */
function valorDoItem(item: ItemExtrato): string {
  if (item.origem === 'RESERVA') {
    return moeda(item.valor);
  }
  return `${item.tipo === 'ENTRADA' ? '+' : '−'}${moeda(item.valor)}`;
}

/**
 * A lista plana do extrato, na ordem recebida, com um cabeçalho sempre que a data muda em relação
 * ao item anterior (spec 08, itens 1 a 10).
 */
export function linhasDoExtrato(itens: ItemExtrato[]): LinhaExtrato[] {
  const linhas: LinhaExtrato[] = [];
  let diaAnterior: string | null = null;
  for (const item of itens) {
    if (item.data !== diaAnterior) {
      linhas.push({ tipo: 'dia', rotulo: diaPorExtenso(item.data) });
      diaAnterior = item.data;
    }
    linhas.push({
      tipo: 'item',
      simbolo: SIMBOLO[item.tipo],
      selo: SELO[item.tipo],
      // Sem descrição, a categoria vira o título (item 8).
      titulo: item.descricao || item.categoria,
      subtitulo: `${ROTULO[item.tipo]} · ${item.categoria}`,
      valor: valorDoItem(item),
    });
  }
  return linhas;
}
