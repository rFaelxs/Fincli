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

export type TipoLancamento = 'ENTRADA' | 'SAIDA' | 'APORTE' | 'SAQUE';

/** Uma linha de "últimos lançamentos" da Gestão. `id` é nulo em movimentação de reserva. */
export interface Lancamento {
  id: string | null;
  data: string;
  descricao: string;
  categoria: string;
  essencial: boolean;
  tipo: TipoLancamento;
  valor: number;
}

/** Conta prevista dentro do mês, com o vencimento já ajustado ao tamanho do mês. */
export interface ContaPrevista {
  id: string;
  nome: string;
  valor: number;
  diaVencimento: number;
  vencimento: string;
  recorrente: boolean;
}

/** `GET /api/hoje`: os números da Gestão, todos calculados no servidor (spec 03). */
export interface Hoje {
  hoje: string;
  /** `yyyy-MM`. */
  mes: string;
  fimDoMes: string;
  diasRestantes: number;
  livre: number;
  porDia: number;
  entradas: number;
  saidas: number;
  essenciais: number;
  superfluas: number;
  aportes: number;
  sobra: number;
  previstasTotal: number;
  ultimosLancamentos: Lancamento[];
  previstas: ContaPrevista[];
}

export type TipoComando = 'ENTRADA' | 'SAIDA' | 'APORTE' | 'PREVISTA';

/** `POST /api/comando`: o que foi criado e como desfazer (spec 04). */
export interface ResultadoComando {
  tipo: TipoComando;
  /** Transação ou conta criada; nulo em aporte. */
  id: string | null;
  /** Só em aporte: o desfazer saca o mesmo valor dessa reserva. */
  reservaId: string | null;
  valor: number;
  descricao: string;
  categoria: string;
  mensagem: string;
}

/** `GET /api/relatorio`: os números do mês escolhido, todos calculados no servidor (spec 05). */
export interface Relatorio {
  /** `yyyy-MM`. */
  mes: string;
  entradas: number;
  saidas: number;
  essenciais: number;
  superfluas: number;
  aportes: number;
  sobra: number;
  /** Seis meses, do mais antigo ao mais recente, com os vazios zerados. */
  historico: { mes: string; sobra: number }[];
  /** Até quatro categorias de saída, da maior para a menor. */
  topCategorias: { nome: string; total: number }[];
  reservas: {
    id: string;
    nome: string;
    saldo: number;
    meta: number;
    progresso: number;
    aporteMedio: number;
    /** `yyyy-MM`; nulo sem meta ou sem aporte. */
    previsao: string | null;
    noRitmo: boolean;
  }[];
  projecao: { jaSaiu: number; aindaVence: number; sobraPrevista: number };
}

/** Corpo de erro `application/problem+json`. */
export interface Problema {
  status?: number;
  title?: string;
  detail?: string;
  /** Mensagem por campo, na validação. */
  campos?: Record<string, string>;
}
