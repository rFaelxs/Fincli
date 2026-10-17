/**
 * Corpos da API, como o backend os devolve. Datas e meses chegam como texto (`yyyy-MM-dd`,
 * `yyyy-MM`) e continuam texto: nunca passam por `new Date(iso)`.
 */

/** Usuário da sessão. O CPF nunca volta nas respostas (ADR 0009). */
export interface Usuario {
  id: string;
  nome: string;
}

export interface Reserva {
  id: string;
  nome: string;
  metaValor: number;
  saldoAtual: number;
  /** Percentual da meta, com uma casa, calculado no servidor; 0 sem meta. */
  progresso: number;
  emergencia: boolean;
}

export type TipoTransacao = 'ENTRADA' | 'SAIDA';

/** Transação como a API devolve. `descricao` pode vir nula. */
export interface Transacao {
  id: string;
  valor: number;
  categoria: string;
  descricao: string | null;
  /** `yyyy-MM-dd`, tratado como texto. */
  data: string;
  tipo: TipoTransacao;
  essencial: boolean;
}

/** Corpo do POST e do PUT de transação. */
export type DadosTransacao = Omit<Transacao, 'id'>;

/** Uma linha do extrato: transação ou movimentação de reserva. */
export interface ItemExtrato {
  origem: 'TRANSACAO' | 'RESERVA';
  /** `yyyy-MM-dd`, tratado como texto. */
  data: string;
  valor: number;
  tipo: 'ENTRADA' | 'SAIDA' | 'ALOCACAO' | 'SAQUE';
  /** "Reserva" nas movimentações. */
  categoria: string;
  /** Texto livre da transação, ou o nome da reserva no momento do movimento. */
  descricao: string | null;
}

/** Corpo de erro `application/problem+json`. */
export interface Problema {
  status?: number;
  title?: string;
  detail?: string;
  /** Mensagem por campo, na validação. */
  campos?: Record<string, string>;
}
