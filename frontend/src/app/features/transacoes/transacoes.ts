import { TipoTransacao, Transacao } from '../../core/api/modelos';
import { mesPorExtenso, valor } from '../../shared/format/formatos';

/** O que a lista mostra: um mês, um tipo e o texto da busca (spec 06, itens 2 a 4). */
export interface Filtros {
  /** `yyyy-MM`. */
  mes: string;
  tipo: TipoTransacao | 'TODOS';
  busca: string;
}

export interface OpcaoMes {
  valor: string;
  rotulo: string;
}

/** Os `n` últimos meses, do corrente para trás, por extenso. Não há opção "todos". */
export function ultimosMeses(n: number, agora: Date = new Date()): OpcaoMes[] {
  return Array.from({ length: n }, (_, i) => {
    const mes = new Date(agora.getFullYear(), agora.getMonth() - i, 1);
    const valorMes = `${mes.getFullYear()}-${String(mes.getMonth() + 1).padStart(2, '0')}`;
    return { valor: valorMes, rotulo: mesPorExtenso(mes) };
  });
}

/**
 * Aplica os três filtros. O mês compara o começo do texto da data; a busca casa com a descrição
 * ou a categoria, ignorando maiúsculas.
 */
export function filtrar(transacoes: Transacao[], filtros: Filtros): Transacao[] {
  const busca = filtros.busca.trim().toLocaleLowerCase('pt-BR');
  return transacoes.filter(
    (t) =>
      t.data.startsWith(filtros.mes) &&
      (filtros.tipo === 'TODOS' || t.tipo === filtros.tipo) &&
      (busca === '' ||
        (t.descricao ?? '').toLocaleLowerCase('pt-BR').includes(busca) ||
        t.categoria.toLocaleLowerCase('pt-BR').includes(busca)),
  );
}

/**
 * Entradas menos saídas do que está filtrado, ou nulo quando não há nada. É a única conta de
 * dinheiro que o front faz (spec 06, item 12); a soma é em centavos para não acumular erro de
 * ponto flutuante.
 */
export function totalDoPeriodo(transacoes: Transacao[]): number | null {
  if (transacoes.length === 0) {
    return null;
  }
  const centavos = transacoes.reduce((soma, t) => {
    const c = Math.round(t.valor * 100);
    return t.tipo === 'ENTRADA' ? soma + c : soma - c;
  }, 0);
  return centavos / 100;
}

/** "+2.500,00", "−300,00" ou o traço de lista vazia (itens 11 a 13). */
export function totalFormatado(total: number | null): string {
  if (total === null) {
    return '—';
  }
  return `${total < 0 ? '−' : '+'}${valor(Math.abs(total))}`;
}

/** O valor de uma linha: `+` em entrada, `−` em saída, sem "R$" (item 11). */
export function valorDaLinha(t: Transacao): string {
  return `${t.tipo === 'ENTRADA' ? '+' : '−'}${valor(t.valor)}`;
}

/** "1 lançamento" ou "N lançamentos" (item 6). */
export function contador(n: number): string {
  return n === 1 ? '1 lançamento' : `${n} lançamentos`;
}
