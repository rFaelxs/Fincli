import { Relatorio } from '../../core/api/modelos';
import { moeda } from '../../shared/format/formatos';

/**
 * Textos e proporções do Relatório (spec 05). Os números vêm prontos do servidor; a única conta
 * da tela é o percentual de supérfluas sobre as saídas (item 20).
 */

const MESES = ['janeiro', 'fevereiro', 'março', 'abril', 'maio', 'junho', 'julho', 'agosto',
  'setembro', 'outubro', 'novembro', 'dezembro'];

function partes(mes: string): { ano: number; indice: number } {
  const [ano, m] = mes.split('-').map(Number);
  return { ano, indice: m - 1 };
}

/** "2026-09" → "RELATÓRIO · SETEMBRO DE 2026" (item 4). */
export function eyebrow(mes: string): string {
  const { ano, indice } = partes(mes);
  return `RELATÓRIO · ${MESES[indice].toUpperCase()} DE ${ano}`;
}

/** "2026-04" → "abr" (item 15). */
export function mesCurto(mes: string): string {
  return MESES[partes(mes).indice].slice(0, 3);
}

/** "2027-09" → "set/27" (item 26). */
export function mesAnoCurto(mes: string): string {
  const { ano, indice } = partes(mes);
  return `${MESES[indice].slice(0, 3)}/${String(ano).slice(2)}`;
}

/** 3210 → "3,2k"; abaixo de mil, o número inteiro; negativo leva "−" (itens 12 e 14). */
export function moedaCurta(valor: number): string {
  const abs = Math.abs(valor);
  const texto = abs >= 1000
    ? `${(abs / 1000).toFixed(1).replace('.', ',')}k`
    : String(Math.round(abs));
  return valor < 0 ? `−${texto}` : texto;
}

function comSinal(valor: number): string {
  return valor < 0 ? `−${moeda(Math.abs(valor))}` : moeda(valor);
}

function limitar(pct: number): number {
  return Math.min(Math.max(pct, 0), 100);
}

export interface Segmento {
  nome: string;
  classe: 'essenciais' | 'superfluas' | 'reservas' | 'sobra';
  largura: number;
  /** Segmento estreito demais fica sem rótulo visível (item 7). */
  rotuloVisivel: boolean;
  titulo: string;
}

/**
 * Até quatro segmentos, proporcionais ao valor. Reservas e sobra são limitadas a zero por baixo, e
 * segmento sem valor não aparece (itens 5 e 6). Lista vazia quando nada foi lançado (item 9).
 */
export function segmentos(r: Relatorio): Segmento[] {
  const candidatos = [
    { nome: 'essenciais', classe: 'essenciais' as const, valor: r.essenciais },
    { nome: 'supérfluas', classe: 'superfluas' as const, valor: r.superfluas },
    { nome: 'reservas', classe: 'reservas' as const, valor: Math.max(r.aportes, 0) },
    { nome: 'sobra', classe: 'sobra' as const, valor: Math.max(r.sobra, 0) },
  ].filter((s) => s.valor > 0);
  const total = candidatos.reduce((soma, s) => soma + s.valor, 0);
  return candidatos.map((s) => {
    const largura = (s.valor / total) * 100;
    return { nome: s.nome, classe: s.classe, largura, rotuloVisivel: largura >= 12,
      titulo: `${s.nome} ${moeda(s.valor)}` };
  });
}

/** O texto alternativo da barra repete os pares nome e valor (item 8). */
export function descricaoDaBarra(lista: Segmento[]): string {
  return lista.length === 0
    ? 'nada lançado neste mês'
    : `Composição do mês: ${lista.map((s) => s.titulo).join(', ')}`;
}

/** Os cinco totais abaixo da barra (item 10). */
export function totais(r: Relatorio): { rotulo: string; valor: string; classe: string }[] {
  return [
    { rotulo: 'ENTROU', valor: `+${moeda(r.entradas)}`, classe: 'entrada' },
    { rotulo: 'ESSENCIAIS', valor: `−${moeda(r.essenciais)}`, classe: 'saida' },
    { rotulo: 'SUPÉRFLUAS', valor: `−${moeda(r.superfluas)}`, classe: 'saida' },
    { rotulo: 'GUARDEI', valor: comSinal(r.aportes), classe: 'aporte' },
    { rotulo: 'SOBRA', valor: comSinal(r.sobra), classe: r.sobra < 0 ? 'saida' : '' },
  ];
}

export interface Coluna {
  mes: string;
  rotulo: string;
  curto: string;
  altura: number;
  negativa: boolean;
  atual: boolean;
}

/**
 * Uma coluna por mês, proporcional ao maior valor absoluto, com 2% de mínimo para o mês continuar
 * visível (itens 11 a 15).
 */
export function colunas(r: Relatorio): Coluna[] {
  const maior = Math.max(...r.historico.map((h) => Math.abs(h.sobra)), 0);
  return r.historico.map((h) => ({
    mes: h.mes,
    rotulo: moedaCurta(h.sobra),
    curto: mesCurto(h.mes),
    altura: maior > 0 ? Math.max((Math.abs(h.sobra) / maior) * 100, 2) : 2,
    negativa: h.sobra < 0,
    atual: h.mes === r.mes,
  }));
}

export interface LinhaBarra {
  rotulo: string;
  largura: number;
  valor: string;
}

export interface Pergunta {
  titulo: string;
  destaque: string;
  texto: string;
  linhas: LinhaBarra[];
}

/** As três "Perguntas do mês" (itens 19 a 29). */
export function perguntas(r: Relatorio): Pergunta[] {
  return [puxouParaBaixo(r), ritmoDasMetas(r), comoFecho(r)];
}

function puxouParaBaixo(r: Relatorio): Pergunta {
  const maior = Math.max(...r.topCategorias.map((c) => c.total), 0);
  return {
    titulo: 'O que puxou meu mês pra baixo?',
    destaque: `${moeda(r.superfluas)} em supérfluas`,
    texto: r.saidas > 0
      ? `É ${Math.round((r.superfluas / r.saidas) * 100)}% de tudo que saiu no mês. `
        + 'As categorias abaixo são as que mais pesaram.'
      : 'Nada saiu neste mês, então não há o que explicar.',
    linhas: r.topCategorias.map((c) => ({
      rotulo: c.nome,
      largura: maior > 0 ? limitar((c.total / maior) * 100) : 0,
      valor: moeda(c.total),
    })),
  };
}

function ritmoDasMetas(r: Relatorio): Pergunta {
  const comMeta = r.reservas.filter((x) => x.meta > 0);
  const noRitmo = comMeta.filter((x) => x.noRitmo).length;
  return {
    titulo: 'Estou no ritmo das minhas metas?',
    destaque: comMeta.length > 0 ? `${noRitmo} de ${comMeta.length} no ritmo` : 'nenhuma meta ainda',
    texto: comMeta.length > 0
      ? 'A previsão usa o aporte médio dos últimos 3 meses. Reserva parada não recebe previsão — '
        + 'sem aporte não há como estimar quando ela fecha.'
      : 'Defina uma meta em Reservas para acompanhar o ritmo por aqui.',
    linhas: r.reservas.map((x) => ({
      rotulo: x.nome,
      largura: limitar(x.progresso),
      valor: x.meta <= 0 ? 'sem meta' : x.previsao ? `meta em ${mesAnoCurto(x.previsao)}` : 'sem ritmo',
    })),
  };
}

function comoFecho(r: Relatorio): Pergunta {
  const p = r.projecao;
  const proporcao = (v: number) => (r.entradas > 0 ? limitar((v / r.entradas) * 100) : 0);
  return {
    titulo: 'Como fecho o mês se seguir assim?',
    destaque: p.sobraPrevista >= 0
      ? `+${moeda(p.sobraPrevista)} livres`
      : `−${moeda(Math.abs(p.sobraPrevista))} no vermelho`,
    texto: `Projeção com o que já saiu e as contas que ainda vencem (${moeda(p.aindaVence)}).`,
    linhas: [
      { rotulo: 'Já saiu', largura: proporcao(p.jaSaiu), valor: moeda(p.jaSaiu) },
      { rotulo: 'Ainda vence', largura: proporcao(p.aindaVence), valor: moeda(p.aindaVence) },
      { rotulo: 'Sobra prevista', largura: proporcao(p.sobraPrevista),
        valor: comSinal(p.sobraPrevista) },
    ],
  };
}
