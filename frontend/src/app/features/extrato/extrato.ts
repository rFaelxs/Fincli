import { ItemExtrato } from '../../core/api/modelos';
import { dataCurta, moeda } from '../../shared/format/formatos';

const ROTULOS: Record<string, string> = {
  ENTRADA: 'Entrada',
  SAIDA: 'Saída',
  ALOCACAO: 'Alocação',
  SAQUE: 'Saque',
};

/** Uma linha do extrato, pronta para a tela. */
export interface LinhaExtrato {
  /** Preenchido só na primeira linha de cada dia: é o cabeçalho que vem antes dela. */
  dia: string | null;
  classe: 'in' | 'out' | 'mov';
  /** O símbolo distingue entrada de saída sem depender da cor. */
  glifo: string;
  titulo: string;
  subtitulo: string;
  valor: string;
  tom: 'money-in' | 'money-out' | '';
}

/**
 * Monta as linhas na ordem recebida, abrindo um cabeçalho sempre que a data muda.
 *
 * Transação leva sinal; movimentação de reserva não — o dinheiro mudou de lugar, não entrou nem
 * saiu.
 */
export function linhasDoExtrato(itens: ItemExtrato[]): LinhaExtrato[] {
  let diaAnterior = '';
  return itens.map((it) => {
    const dia = it.data !== diaAnterior ? dataCurta(it.data) : null;
    diaAnterior = it.data;

    const transacao = it.origem === 'TRANSACAO';
    const entrada = it.tipo === 'ENTRADA';
    const rotulo = ROTULOS[it.tipo] ?? it.tipo;
    return {
      dia,
      classe: transacao ? (entrada ? 'in' : 'out') : 'mov',
      glifo: transacao ? (entrada ? '↑' : '↓') : '◎',
      titulo: it.descricao || it.categoria,
      subtitulo: `${rotulo} · ${it.categoria}`,
      valor: transacao ? (entrada ? '+' : '−') + moeda(it.valor) : moeda(it.valor),
      tom: transacao ? (entrada ? 'money-in' : 'money-out') : '',
    };
  });
}
