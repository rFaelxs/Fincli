import { Relatorio, RitmoReserva } from '../../core/api/modelos';
import {
  colunasDoHistorico,
  descricaoDoFluxo,
  perguntasDoMes,
  segmentosDoFluxo,
  totaisDoMes,
} from './relatorio';

const R = 'R$ ';

function relatorio(parcial: Partial<Relatorio> = {}): Relatorio {
  return {
    mes: '2026-09',
    entradas: 5000,
    saidas: 2500,
    essenciais: 1700,
    superfluas: 800,
    aportes: 300,
    sobra: 500,
    historico: [],
    topCategorias: [],
    reservas: [],
    projecao: { jaSaiu: 2500, aindaVence: 210, sobraPrevista: 500 },
    ...parcial,
  };
}

function ritmo(parcial: Partial<RitmoReserva>): RitmoReserva {
  return {
    id: crypto.randomUUID(),
    nome: 'Viagem',
    saldo: 0,
    meta: 0,
    progresso: 0,
    aporteMedio: 0,
    previsao: null,
    noRitmo: false,
    ...parcial,
  };
}

describe('Relatório', () => {
  describe('barra de fluxo', () => {
    it('CA-REL-04: um segmento por parte positiva, proporcional ao valor', () => {
      const s = segmentosDoFluxo(
        relatorio({ essenciais: 1200, superfluas: 800, aportes: 300, sobra: 500 }),
      );
      expect(s.map((x) => [x.chave, x.valor])).toEqual([
        ['essenciais', 1200],
        ['superfluas', 800],
        ['reservas', 300],
        ['sobra', 500],
      ]);
    });

    it('CA-REL-05: aporte negativo não vira segmento', () => {
      const s = segmentosDoFluxo(relatorio({ aportes: -200 }));
      expect(s.map((x) => x.chave)).not.toContain('reservas');
    });

    it('CA-REL-06: segmento estreito fica sem rótulo, mas guarda o title', () => {
      const s = segmentosDoFluxo(
        relatorio({ essenciais: 1900, superfluas: 100, aportes: 0, sobra: 0 }),
      );
      const superfluas = s.find((x) => x.chave === 'superfluas')!;
      expect(superfluas.texto).toBe('');
      expect(superfluas.title).toBe('supérfluas ' + R + '100,00');
    });

    it('CA-REL-07: mês vazio', () => {
      const s = segmentosDoFluxo(relatorio({ essenciais: 0, superfluas: 0, aportes: 0, sobra: 0 }));
      expect(s).toEqual([]);
      expect(descricaoDoFluxo(s)).toBe('Nada lançado neste mês.');
    });

    it('CA-REL-08 e 09: totais com o sinal na frente', () => {
      const t = totaisDoMes(relatorio({ entradas: 5000, sobra: -150 }));
      expect(t[0]).toEqual({ rotulo: 'ENTROU', valor: '+' + R + '5.000,00', classe: 'entrada' });
      expect(t[4]).toEqual({ rotulo: 'SOBRA', valor: '−' + R + '150,00', classe: '' });
    });
  });

  describe('histórico de sobra', () => {
    const historico = [400, -200, 0, 900, 100, 300].map((sobra, i) => ({
      mes: `2026-0${i + 4}`,
      sobra,
    }));

    it('CA-REL-10: a maior sobra é a coluna cheia; a negativa muda de cor', () => {
      const c = colunasDoHistorico(historico, '2026-09');
      expect(c[3].altura).toBe(100);
      expect(c[1]).toMatchObject({ negativa: true, rotulo: '−200', altura: 22 });
      expect(c[5].atual).toBe(true);
    });

    it('CA-REL-11: sobra zero ainda aparece no eixo', () => {
      expect(colunasDoHistorico(historico, '2026-09')[2].altura).toBe(2);
    });

    it('CA-REL-12: rótulo compacto', () => {
      expect(colunasDoHistorico([{ mes: '2026-09', sobra: 3210 }], '2026-09')[0].rotulo).toBe(
        '3,2k',
      );
    });
  });

  describe('perguntas', () => {
    it('CA-REL-16: supérfluas sobre o que saiu', () => {
      const [q1] = perguntasDoMes(relatorio({ superfluas: 800, saidas: 2500 }));
      expect(q1.destaque).toBe(R + '800,00 em supérfluas');
      expect(q1.texto.startsWith('É 32%')).toBe(true);
    });

    it('CA-REL-17: nada saiu', () => {
      const [q1] = perguntasDoMes(relatorio({ saidas: 0, superfluas: 0 }));
      expect(q1.texto).toBe('Nada saiu neste mês, então não há o que explicar.');
    });

    it('CA-REL-19: conta só as reservas com meta', () => {
      const [, q2] = perguntasDoMes(
        relatorio({
          reservas: [
            ritmo({ meta: 1000, noRitmo: true }),
            ritmo({ meta: 2000, noRitmo: true }),
            ritmo({ meta: 3000 }),
            ritmo({ meta: 0 }),
          ],
        }),
      );
      expect(q2.destaque).toBe('2 de 3 no ritmo');
    });

    it('CA-REL-20: nenhuma meta', () => {
      const [, q2] = perguntasDoMes(relatorio({ reservas: [ritmo({ meta: 0 })] }));
      expect(q2.destaque).toBe('nenhuma meta ainda');
      expect(q2.texto).toBe('Defina uma meta em Reservas para acompanhar o ritmo por aqui.');
    });

    it('CA-REL-21 a 23: previsão, sem ritmo e sem meta', () => {
      const [, q2] = perguntasDoMes(
        relatorio({
          reservas: [
            ritmo({ meta: 10000, previsao: '2027-09', progresso: 40 }),
            ritmo({ meta: 5000 }),
            ritmo({ meta: 0 }),
            ritmo({ meta: 100, progresso: 120 }),
          ],
        }),
      );
      expect(q2.linhas.map((l) => l.valor)).toEqual([
        'meta em set/27',
        'sem ritmo',
        'sem meta',
        'sem ritmo',
      ]);
      expect(q2.linhas[3].largura).toBe(100);
    });

    it('CA-REL-25: fechamento positivo', () => {
      const [, , q3] = perguntasDoMes(
        relatorio({ projecao: { jaSaiu: 2500, aindaVence: 210, sobraPrevista: 500 } }),
      );
      expect(q3.destaque).toBe('+' + R + '500,00 livres');
      expect(q3.texto.endsWith('(' + R + '210,00).')).toBe(true);
    });

    it('CA-REL-26: fechamento no vermelho', () => {
      const [, , q3] = perguntasDoMes(
        relatorio({ projecao: { jaSaiu: 2500, aindaVence: 0, sobraPrevista: -200 } }),
      );
      expect(q3.destaque).toBe('−' + R + '200,00 no vermelho');
    });
  });
});
