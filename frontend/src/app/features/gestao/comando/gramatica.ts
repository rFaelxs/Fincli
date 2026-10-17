import { TipoComando } from '../../../core/api/modelos';
import { hojeIso } from '../../../shared/format/formatos';

/**
 * Espelho do `ComandoParser.java`, só para o preview (spec 04, itens 1 a 14). Quem decide é o
 * servidor, que reinterpreta o texto cru: o preview nunca é aceito como entrada. As tabelas e os
 * casos de teste (`gramatica.spec.ts` x `ComandoParserTest.java`) têm de andar juntos.
 */
export interface Comando {
  tipo: TipoComando;
  valor: number;
  descricao: string;
  categoria: string;
  /** `yyyy-MM-dd`. */
  data: string;
  essencial: boolean;
  /** Só em conta prevista. */
  diaVencimento: number | null;
  recorrente: boolean;
}

/** Alternativas que só casam como palavra inteira, delimitada por algo que não seja letra. */
function palavras(alternativas: string): RegExp {
  return new RegExp(`(?<![a-z])(${alternativas})(?![a-z])`);
}

const CATEGORIAS: { nome: string; chaves: RegExp; essencial: boolean }[] = [
  { nome: 'Alimentação', essencial: true,
    chaves: palavras('mercado|supermercado|feira|padaria|restaurante|ifood|lanche|almoco|jantar') },
  { nome: 'Moradia', essencial: true,
    chaves: palavras('aluguel|condominio|luz|energia|agua|internet|fibra|gas') },
  { nome: 'Saúde', essencial: true, chaves: palavras('farmacia|academia|medico|dentista|remedio') },
  { nome: 'Transporte', essencial: true, chaves: palavras('uber|gasolina|onibus|metro|combustivel') },
  { nome: 'Lazer', essencial: false,
    chaves: palavras('show|cinema|streaming|netflix|spotify|bar|viagem|jogo') },
  { nome: 'Trabalho', essencial: false,
    chaves: palavras('salario|freela|freelance|pix|bonus|venda') },
];

const RX_PREVISTA = /^(prever|conta)(?![a-z])/;
const RX_APORTE = /^(guardar|poupar|alocar)(?![a-z])/;
const RX_ENTRADA = /^(entrada(?![a-z])|recebi(?![a-z])|\+)/;
const RX_RENDA = palavras('salario|freela|freelance|pix|bonus');
const RX_UMA_VEZ = /\buma vez\b|\bso (esse|este) mes\b/;
const RX_ANTEONTEM = palavras('anteontem');
const RX_ONTEM = palavras('ontem');
const RX_ESSENCIAL = palavras('essencial');
const RX_DIA = /^(\d{1,2})$/;
const RX_VALOR = /\d[\d.]*(?:,\d+)?/;

const CONTROLE = new Set(['ontem', 'hoje', 'anteontem', 'essencial', 'guardar', 'poupar',
  'alocar', 'entrada', 'recebi', 'prever']);
const CONTROLE_PREVISTA = new Set(['conta', 'dia', 'todo', 'todos', 'mes', 'meses', 'mensal',
  'recorrente', 'uma', 'vez', 'so', 'esse', 'este']);
const LIGACAO = new Set(['de', 'do', 'da', 'no', 'na', 'em']);

/** Sem acento e em minúsculas, a forma em que toda comparação é feita. */
export function normalizar(texto: string): string {
  return texto.normalize('NFD').replace(/\p{M}+/gu, '').toLowerCase();
}

/**
 * O número escrito: com vírgula, o ponto é milhar (`1.234,56`); sem vírgula, um ponto com uma ou
 * duas casas é decimal (`12.50`) e qualquer outro é milhar (`1.234`). Nulo se não for positivo.
 */
function paraDecimal(bruto: string): number | null {
  let limpo: string;
  if (bruto.includes(',')) {
    limpo = bruto.replaceAll('.', '').replace(',', '.');
  } else if (/^\d+\.\d{1,2}$/.test(bruto)) {
    limpo = bruto;
  } else {
    limpo = bruto.replaceAll('.', '');
  }
  const valor = Number(limpo);
  return Number.isFinite(valor) && valor > 0 ? Math.round(valor * 100) / 100 : null;
}

function tipoDe(normalizado: string): TipoComando {
  if (RX_PREVISTA.test(normalizado)) {
    return 'PREVISTA';
  }
  if (RX_APORTE.test(normalizado)) {
    return 'APORTE';
  }
  if (RX_ENTRADA.test(normalizado) || RX_RENDA.test(normalizado)) {
    return 'ENTRADA';
  }
  return 'SAIDA';
}

function categoriaPorPalavra(normalizado: string) {
  return CATEGORIAS.find((c) => c.chaves.test(normalizado));
}

function menosDias(hoje: Date, dias: number): Date {
  return new Date(hoje.getFullYear(), hoje.getMonth(), hoje.getDate() - dias);
}

/**
 * Interpreta o texto. Nulo quando não há número positivo: em dúvida, não se lança nada.
 *
 * @param hoje referência de "hoje", "ontem" e "anteontem", no relógio local
 */
export function interpretar(texto: string, hoje: Date = new Date()): Comando | null {
  if (!texto || texto.trim() === '') {
    return null;
  }
  const tokens = texto.trim().split(/\s+/);
  const normalizado = normalizar(texto.trim());
  const tipo = tipoDe(normalizado);

  let valor: number | null = null;
  let dia: number | null = null;
  let esperandoDia = false;
  const sobra: string[] = [];

  for (const token of tokens) {
    const n = normalizar(token);
    if (esperandoDia) {
      esperandoDia = false;
      const m = RX_DIA.exec(n);
      if (m) {
        dia = Number(m[1]);
        continue;
      }
    }
    // O dia vem antes do valor: em "prever energia 210 dia 22", o 22 é o vencimento.
    if (tipo === 'PREVISTA' && n === 'dia') {
      esperandoDia = true;
      continue;
    }
    const numero = valor === null ? RX_VALOR.exec(n) : null;
    if (numero) {
      valor = paraDecimal(numero[0]);
      // "mercado120" deixa "mercado" para a descrição.
      const resto = n.slice(0, numero.index) + n.slice(numero.index + numero[0].length);
      if (/\p{L}/u.test(resto)) {
        sobra.push(token.slice(0, numero.index) + token.slice(numero.index + numero[0].length));
      }
      continue;
    }
    if (CONTROLE.has(n) || (tipo === 'PREVISTA' && CONTROLE_PREVISTA.has(n))) {
      continue;
    }
    sobra.push(token);
  }

  if (valor === null) {
    return null;
  }

  let data = hoje;
  if (RX_ANTEONTEM.test(normalizado)) {
    data = menosDias(hoje, 2);
  } else if (RX_ONTEM.test(normalizado)) {
    data = menosDias(hoje, 1);
  }

  const categoria = categoriaPorPalavra(normalizado);
  return {
    tipo,
    valor,
    descricao: descricaoDe(sobra, tipo),
    categoria: {
      APORTE: 'Aporte',
      PREVISTA: 'Conta',
      ENTRADA: categoria?.nome ?? 'Trabalho',
      SAIDA: categoria?.nome ?? 'Outros',
    }[tipo],
    data: hojeIso(data),
    essencial: tipo === 'SAIDA' && (RX_ESSENCIAL.test(normalizado) || !!categoria?.essencial),
    // Sem dia explícito, a conta vence hoje.
    diaVencimento: tipo === 'PREVISTA' ? (dia ?? hoje.getDate()) : null,
    recorrente: tipo === 'PREVISTA' && !RX_UMA_VEZ.test(normalizado),
  };
}

function descricaoDe(sobra: string[], tipo: TipoComando): string {
  const restantes = [...sobra];
  // "conta de luz" perde o "conta" e ficaria "De luz".
  while (restantes.length > 0 && LIGACAO.has(normalizar(restantes[0]))) {
    restantes.shift();
  }
  const descricao = restantes.join(' ').trim();
  if (descricao === '') {
    return tipo === 'APORTE' ? 'Aporte' : tipo === 'PREVISTA' ? 'Conta' : 'Lançamento';
  }
  return descricao[0].toUpperCase() + descricao.slice(1);
}
