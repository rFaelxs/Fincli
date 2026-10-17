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

/** Corpo de erro `application/problem+json`. */
export interface Problema {
  status?: number;
  title?: string;
  detail?: string;
  /** Mensagem por campo, na validação. */
  campos?: Record<string, string>;
}
