/**
 * Apresentação da tela Relatório: segmentos da barra de fluxo, colunas do histórico e as três
 * perguntas. Tudo sai de números que a API já calculou; aqui só se decide proporção e texto.
 */
import { Relatorio, SobraMes } from '../../core/api/modelos';
import { mesAno, moeda, moedaAbs, moedaCurta } from '../../shared/format/formatos';

/* ── Barra de fluxo ── */

export interface Segmento {
  chave: 'essenciais' | 'superfluas' | 'reservas' | 'sobra';
  rotulo: string;
  valor: number;
  /** Vazio quando o segmento é estreito demais para o nome caber. */
  texto: string;
  title: string;
}

/** Abaixo de 12% da largura, o rótulo sairia cortado ("supér"); o nome fica no title. */
const CABE_ROTULO = 12;

export function segmentosDoFluxo(d: Relatorio): Segmento[] {
  const partes = [
    { chave: 'essenciais' as const, rotulo: 'essenciais', valor: Number(d.essenciais) },
    { chave: 'superfluas' as const, rotulo: 'supérfluas', valor: Number(d.superfluas) },
    { chave: 'reservas' as const, rotulo: 'reservas', valor: Math.max(0, Number(d.aportes)) },
    { chave: 'sobra' as const, rotulo: 'sobra', valor: Math.max(0, Number(d.sobra)) },
  ].filter((p) => p.valor > 0);

  const total = partes.reduce((s, p) => s + p.valor, 0);
  return partes.map((p) => ({
    ...p,
    texto: (p.valor / total) * 100 >= CABE_ROTULO ? p.rotulo : '',
    title: `${p.rotulo} ${moeda(p.valor)}`,
  }));
}

/** A barra é decorativa para quem lê a tela; o texto alternativo repete os números. */
export function descricaoDoFluxo(segmentos: Segmento[]): string {
  if (!segmentos.length) return 'Nada lançado neste mês.';
  return segmentos.map((s) => `${s.rotulo} ${moeda(s.valor)}`).join('. ') + '.';
}

export interface TotalDoMes {
  rotulo: string;
  valor: string;
  classe: 'entrada' | 'saida' | 'aporte' | '';
}

export function totaisDoMes(d: Relatorio): TotalDoMes[] {
  return [
    { rotulo: 'ENTROU', valor: '+' + moedaAbs(d.entradas), classe: 'entrada' },
    { rotulo: 'ESSENCIAIS', valor: '−' + moedaAbs(d.essenciais), classe: 'saida' },
    { rotulo: 'SUPÉRFLUAS', valor: '−' + moedaAbs(d.superfluas), classe: 'saida' },
    {
      rotulo: 'GUARDEI',
      valor: (Number(d.aportes) < 0 ? '−' : '') + moedaAbs(d.aportes),
      classe: 'aporte',
    },
    { rotulo: 'SOBRA', valor: (Number(d.sobra) < 0 ? '−' : '') + moedaAbs(d.sobra), classe: '' },
  ];
}

/* ── Histórico de sobra ── */

export interface Coluna {
  mes: string;
  rotulo: string;
  altura: number;
  atual: boolean;
  negativa: boolean;
}

/**
 * A altura é proporcional ao maior valor absoluto: um mês no vermelho também precisa aparecer,
 * e comparar só os positivos esconderia justamente o mês que interessa.
 */
export function colunasDoHistorico(historico: SobraMes[], mesAtual: string): Coluna[] {
  const maior = Math.max(...historico.map((h) => Math.abs(Number(h.sobra))), 1);
  return historico.map((h) => {
    const v = Number(h.sobra);
    return {
      mes: h.mes,
      rotulo: (v < 0 ? '−' : '') + moedaCurta(v),
      altura: Math.max(2, Math.round((Math.abs(v) / maior) * 100)),
      atual: h.mes === mesAtual,
      negativa: v < 0,
    };
  });
}

/* ── Perguntas ── */

export interface LinhaBarra {
  rotulo: string;
  valor: string;
  /** Percentual já limitado a 0–100. */
  largura: number;
}

export interface Pergunta {
  chave: 'q1' | 'q2' | 'q3';
  titulo: string;
  destaque: string;
  texto: string;
  linhas: LinhaBarra[];
}

const limitar = (v: number) => Math.max(0, Math.min(100, v));

export function perguntasDoMes(d: Relatorio): Pergunta[] {
  const saidas = Number(d.saidas);
  const superfluas = Number(d.superfluas);
  const maiorCategoria = d.topCategorias.length ? Number(d.topCategorias[0].total) : 1;

  const comMeta = d.reservas.filter((r) => Number(r.meta) > 0);
  const noRitmo = comMeta.filter((r) => r.noRitmo).length;

  const p = d.projecao;
  const base = Math.max(Number(d.entradas), 1);
  const sobraPrevista = Number(p.sobraPrevista);

  return [
    {
      chave: 'q1',
      titulo: 'O que puxou meu mês pra baixo?',
      destaque: moeda(superfluas) + ' em supérfluas',
      texto:
        saidas > 0
          ? `É ${Math.round((superfluas / saidas) * 100)}% de tudo que saiu no mês.` +
            ' As categorias abaixo são as que mais pesaram.'
          : 'Nada saiu neste mês, então não há o que explicar.',
      linhas: d.topCategorias.map((c) => ({
        rotulo: c.nome,
        valor: moeda(c.total),
        largura: limitar((Number(c.total) / maiorCategoria) * 100),
      })),
    },
    {
      chave: 'q2',
      titulo: 'Estou no ritmo das minhas metas?',
      destaque: comMeta.length ? `${noRitmo} de ${comMeta.length} no ritmo` : 'nenhuma meta ainda',
      texto: comMeta.length
        ? 'A previsão usa o aporte médio dos últimos 3 meses. Reserva parada não recebe' +
          ' previsão — sem aporte não há como estimar quando ela fecha.'
        : 'Defina uma meta em Reservas para acompanhar o ritmo por aqui.',
      linhas: d.reservas.map((r) => ({
        rotulo: r.nome,
        valor: r.previsao
          ? 'meta em ' + mesAno(r.previsao)
          : Number(r.meta) > 0
            ? 'sem ritmo'
            : 'sem meta',
        largura: limitar(Math.min(100, Number(r.progresso))),
      })),
    },
    {
      chave: 'q3',
      titulo: 'Como fecho o mês se seguir assim?',
      destaque:
        (sobraPrevista < 0 ? '−' : '+') +
        moedaAbs(sobraPrevista) +
        (sobraPrevista < 0 ? ' no vermelho' : ' livres'),
      texto:
        'Projeção com o que já saiu e as contas que ainda vencem ' + `(${moeda(p.aindaVence)}).`,
      linhas: [
        {
          rotulo: 'Já saiu',
          valor: moeda(p.jaSaiu),
          largura: limitar((Number(p.jaSaiu) / base) * 100),
        },
        {
          rotulo: 'Ainda vence',
          valor: moeda(p.aindaVence),
          largura: limitar((Number(p.aindaVence) / base) * 100),
        },
        {
          rotulo: 'Sobra prevista',
          valor: moeda(p.sobraPrevista),
          largura: limitar((Math.abs(sobraPrevista) / base) * 100),
        },
      ],
    },
  ];
}
