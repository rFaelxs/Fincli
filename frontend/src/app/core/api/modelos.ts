/**
 * Corpos da API, como o backend os devolve.
 *
 * Datas chegam em ISO (`yyyy-MM-dd`) e meses em `yyyy-MM`, e são tratadas como texto. Valores
 * chegam como número com duas casas; o front só formata, nunca recalcula.
 */

export interface Usuario {
  id: string;
  nome: string;
  /** Mascarado: `***.***.**NN`. */
  cpf: string;
}

export interface CadastroRequest {
  nome: string;
  cpf: string;
  senha: string;
}

export interface LoginRequest {
  cpf: string;
  senha: string;
}

export type TipoTransacao = 'ENTRADA' | 'SAIDA';

export interface Transacao {
  id: string;
  valor: number;
  categoria: string;
  descricao: string | null;
  data: string;
  tipo: TipoTransacao;
  essencial: boolean;
}

export interface TransacaoRequest {
  valor: number;
  categoria: string;
  descricao: string | null;
  data: string;
  tipo: TipoTransacao;
  essencial: boolean;
}

export interface Reserva {
  id: string;
  nome: string;
  metaValor: number;
  saldoAtual: number;
  /** Percentual da meta, com uma casa; 0 quando não há meta. */
  progresso: number;
  emergencia: boolean;
}

export interface ReservaRequest {
  nome: string;
  metaValor: number;
}

export interface ValorRequest {
  valor: number;
}

export interface Saldo {
  saldoDisponivel: number;
}

export interface ItemExtrato {
  origem: 'TRANSACAO' | 'RESERVA';
  data: string;
  valor: number;
  tipo: 'ENTRADA' | 'SAIDA' | 'ALOCACAO' | 'SAQUE';
  categoria: string;
  descricao: string | null;
}

export interface ContaPrevista {
  id: string;
  nome: string;
  valor: number;
  diaVencimento: number;
  /** Já ajustado ao tamanho do mês: dia 31 em fevereiro vira o último dia. */
  vencimento: string;
  recorrente: boolean;
}

export interface ContaPrevistaRequest {
  nome: string;
  valor: number;
  diaVencimento: number;
  /** `yyyy-MM` para conta avulsa; nulo torna a conta recorrente. */
  mesReferencia: string | null;
}

export type TipoLancamento = 'ENTRADA' | 'SAIDA' | 'APORTE' | 'SAQUE';

export interface Lancamento {
  /** Nulo em movimentação de reserva. */
  id: string | null;
  data: string;
  descricao: string;
  categoria: string;
  essencial: boolean;
  tipo: TipoLancamento;
  valor: number;
}

/** Tela Gestão, servida por `GET /api/hoje`. */
export interface Hoje {
  hoje: string;
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

export interface SobraMes {
  mes: string;
  sobra: number;
}

export interface CategoriaTotal {
  nome: string;
  total: number;
}

export interface RitmoReserva {
  id: string;
  nome: string;
  saldo: number;
  meta: number;
  progresso: number;
  aporteMedio: number;
  /** `yyyy-MM`; nulo quando a reserva está parada ou não tem meta. */
  previsao: string | null;
  noRitmo: boolean;
}

export interface Projecao {
  jaSaiu: number;
  aindaVence: number;
  sobraPrevista: number;
}

export interface Relatorio {
  mes: string;
  entradas: number;
  saidas: number;
  essenciais: number;
  superfluas: number;
  aportes: number;
  sobra: number;
  historico: SobraMes[];
  topCategorias: CategoriaTotal[];
  reservas: RitmoReserva[];
  projecao: Projecao;
}

export type TipoComando = 'ENTRADA' | 'SAIDA' | 'APORTE' | 'PREVISTA';

export interface ComandoResponse {
  tipo: TipoComando;
  /** Transação em ENTRADA/SAIDA, conta em PREVISTA; nulo em APORTE. */
  id: string | null;
  /** Só em APORTE: desfazer é um saque de mesmo valor nessa reserva. */
  reservaId: string | null;
  valor: number;
  descricao: string;
  categoria: string;
  mensagem: string;
}
