import { Relatorio } from '../../core/api/modelos';
import {
  colunas, descricaoDaBarra, eyebrow, mesAnoCurto, mesCurto, moedaCurta, perguntas, segmentos,
  totais,
} from './relatorio';

function relatorio(extra: Partial<Relatorio> = {}): Relatorio {
  return {
    mes: '2026-09', entradas: 5000, saidas: 2000, essenciais: 1200, superfluas: 800,
    aportes: 300, sobra: 500, historico: [], topCategorias: [], reservas: [],
    projecao: { jaSaiu: 2000, aindaVence: 210, sobraPrevista: 290 }, ...extra,
  };
}

type Reserva = Relatorio['reservas'][number];
function reserva(extra: Partial<Reserva>): Reserva {
  return { id: 'r', nome: 'Viagem', saldo: 0, meta: 0, progresso: 0, aporteMedio: 0,
    previsao: null, noRitmo: false, ...extra };
}

describe('relatorio', () => {
  // CA-REL-01
  it('rótulo do mês', () => {
    expect(eyebrow('2026-09')).toBe('RELATÓRIO · SETEMBRO DE 2026');
  });

  // CA-REL-04
  it('quatro segmentos proporcionais', () => {
    const s = segmentos(relatorio());

    expect(s.map((x) => x.nome)).toEqual(['essenciais', 'supérfluas', 'reservas', 'sobra']);
    expect(s.map((x) => Math.round(x.largura * 10) / 10)).toEqual([42.9, 28.6, 10.7, 17.9]);
  });

  // CA-REL-05 e item 6
  it('aporte negativo esconde o segmento de reservas', () => {
    expect(segmentos(relatorio({ aportes: -200 })).map((x) => x.nome)).not.toContain('reservas');
    expect(segmentos(relatorio({ sobra: -100 })).map((x) => x.nome)).not.toContain('sobra');
  });

  // CA-REL-06
  it('segmento estreito fica sem rótulo e mantém o título', () => {
    const s = segmentos(relatorio({ essenciais: 950, superfluas: 50, aportes: 0, sobra: 0 }));

    expect(s[1]).toMatchObject({ largura: 5, rotuloVisivel: false, titulo: 'supérfluas R$ 50,00' });
    expect(s[0].rotuloVisivel).toBe(true);
  });

  // CA-REL-07 e item 8
  it('mês sem nada vira um bloco único', () => {
    const vazio = segmentos(relatorio({ essenciais: 0, superfluas: 0, aportes: 0, sobra: 0 }));

    expect(vazio).toEqual([]);
    expect(descricaoDaBarra(vazio)).toBe('nada lançado neste mês');
    expect(descricaoDaBarra(segmentos(relatorio()))).toContain('essenciais R$ 1.200,00');
  });

  // CA-REL-08 e CA-REL-09
  it('totais com os sinais da spec', () => {
    const t = totais(relatorio({ sobra: -150 }));

    expect(t.map((x) => `${x.rotulo} ${x.valor}`)).toEqual([
      'ENTROU +R$ 5.000,00', 'ESSENCIAIS −R$ 1.200,00', 'SUPÉRFLUAS −R$ 800,00',
      'GUARDEI R$ 300,00', 'SOBRA −R$ 150,00',
    ]);
  });

  // CA-REL-10 a CA-REL-13
  it('colunas proporcionais ao maior valor absoluto, com mínimo de 2%', () => {
    const meses = ['2026-04', '2026-05', '2026-06', '2026-07', '2026-08', '2026-09'];
    const sobras = [400, -200, 0, 900, 100, 3210];
    const c = colunas(relatorio({
      historico: meses.map((mes, i) => ({ mes, sobra: sobras[i] })),
    }));

    expect(c[5].altura).toBe(100);
    expect(c[1]).toMatchObject({ negativa: true, rotulo: '−200' });
    expect(c[2]).toMatchObject({ altura: 2, rotulo: '0' });
    expect(c[5]).toMatchObject({ rotulo: '3,2k', atual: true });
    expect(c.map((x) => x.curto)).toEqual(['abr', 'mai', 'jun', 'jul', 'ago', 'set']);
  });

  it('seis meses sem movimento mostram "0"', () => {
    const c = colunas(relatorio({
      historico: Array.from({ length: 6 }, (_, i) => ({ mes: `2026-0${i + 4}`, sobra: 0 })),
    }));

    expect(c.map((x) => x.rotulo)).toEqual(['0', '0', '0', '0', '0', '0']);
    expect(c.every((x) => x.altura === 2)).toBe(true);
  });

  it('formatos curtos', () => {
    expect(moedaCurta(3210)).toBe('3,2k');
    expect(moedaCurta(900)).toBe('900');
    expect(mesCurto('2026-04')).toBe('abr');
    expect(mesAnoCurto('2027-09')).toBe('set/27');
  });

  // CA-REL-16
  it('primeira pergunta com o percentual de supérfluas', () => {
    const p = perguntas(relatorio({ superfluas: 800, saidas: 2500 }))[0];

    expect(p.titulo).toBe('O que puxou meu mês pra baixo?');
    expect(p.destaque).toBe('R$ 800,00 em supérfluas');
    expect(p.texto.startsWith('É 32%')).toBe(true);
  });

  // CA-REL-17
  it('sem saídas não há o que explicar', () => {
    expect(perguntas(relatorio({ saidas: 0 }))[0].texto)
      .toBe('Nada saiu neste mês, então não há o que explicar.');
  });

  it('categorias com barra proporcional à maior', () => {
    const p = perguntas(relatorio({
      topCategorias: [{ nome: 'Moradia', total: 1500 }, { nome: 'Lazer', total: 300 }],
    }))[0];

    expect(p.linhas).toEqual([
      { rotulo: 'Moradia', largura: 100, valor: 'R$ 1.500,00' },
      { rotulo: 'Lazer', largura: 20, valor: 'R$ 300,00' },
    ]);
  });

  // CA-REL-19, 21, 22 e 23
  it('ritmo das metas conta só as reservas com meta', () => {
    const p = perguntas(relatorio({
      reservas: [
        reserva({ nome: 'Viagem', meta: 10000, progresso: 40, previsao: '2027-09', noRitmo: true }),
        reserva({ nome: 'Carro', meta: 5000, previsao: '2027-01', noRitmo: true }),
        reserva({ nome: 'PC', meta: 5000 }),
        reserva({ nome: 'Emergência', meta: 0 }),
      ],
    }))[1];

    expect(p.destaque).toBe('2 de 3 no ritmo');
    expect(p.linhas.map((l) => l.valor)).toEqual(
      ['meta em set/27', 'meta em jan/27', 'sem ritmo', 'sem meta']);
  });

  // CA-REL-20
  it('sem nenhuma meta', () => {
    const p = perguntas(relatorio({ reservas: [reserva({ meta: 0 })] }))[1];

    expect(p.destaque).toBe('nenhuma meta ainda');
    expect(p.texto).toBe('Defina uma meta em Reservas para acompanhar o ritmo por aqui.');
  });

  // CA-REL-25
  it('projeção positiva', () => {
    const p = perguntas(relatorio({
      projecao: { jaSaiu: 2500, aindaVence: 210, sobraPrevista: 500 },
    }))[2];

    expect(p.destaque).toBe('+R$ 500,00 livres');
    expect(p.texto.endsWith('(R$ 210,00).')).toBe(true);
    expect(p.linhas.map((l) => l.rotulo)).toEqual(['Já saiu', 'Ainda vence', 'Sobra prevista']);
    expect(p.linhas[0].largura).toBe(50);
  });

  // CA-REL-26
  it('projeção no vermelho', () => {
    const p = perguntas(relatorio({
      projecao: { jaSaiu: 2500, aindaVence: 210, sobraPrevista: -200 },
    }))[2];

    expect(p.destaque).toBe('−R$ 200,00 no vermelho');
    expect(p.linhas[2]).toMatchObject({ largura: 0, valor: '−R$ 200,00' });
  });
});
