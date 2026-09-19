import {
  dataCurta,
  decimal,
  diaMes,
  diaPorExtenso,
  isoLocal,
  mesAno,
  mesCurto,
  mesPorExtenso,
  moeda,
  moedaAbs,
  moedaCurta,
  pct,
  soMes,
  ultimosMeses,
} from './formatos';

/** O Intl pt-BR separa o símbolo do número com espaço não separável. */
const R = 'R$ ';

describe('formatos', () => {
  describe('dinheiro', () => {
    it('moeda formata em BRL', () => expect(moeda(1500)).toBe(R + '1.500,00'));
    it('moeda mantém o sinal do valor', () => expect(moeda(-150)).toBe('-' + R + '150,00'));
    it('moedaAbs tira o sinal: quem mostra o sinal é a tela', () =>
      expect(moedaAbs(-300)).toBe(R + '300,00'));
    it('moedaCurta abrevia a partir de mil', () => {
      expect(moedaCurta(3210)).toBe('3,2k');
      expect(moedaCurta(-3210)).toBe('3,2k');
      expect(moedaCurta(950)).toBe('950');
      expect(moedaCurta(0)).toBe('0');
    });
    it('decimal tem duas casas e nenhum símbolo', () => {
      expect(decimal(120)).toBe('120,00');
      expect(decimal(2500)).toBe('2.500,00');
    });
    it('pct usa vírgula e uma casa', () => {
      expect(pct(12.5)).toBe('12,5%');
      expect(pct(120)).toBe('120,0%');
    });
  });

  describe('datas lidas como texto', () => {
    it('diaMes', () => expect(diaMes('2026-09-16')).toBe('16/09'));
    it('diaPorExtenso', () => expect(diaPorExtenso('2026-09-30')).toBe('30 de setembro'));
    it('mesPorExtenso', () => expect(mesPorExtenso('2026-09')).toBe('setembro de 2026'));
    it('soMes', () => expect(soMes('2026-03')).toBe('março'));
    it('mesCurto', () => expect(mesCurto('2026-04')).toBe('abr'));
    it('mesAno', () => expect(mesAno('2027-09')).toBe('set/27'));

    it('o primeiro dia do mês não volta para o mês anterior', () => {
      // new Date('2026-09-01') seria 31/08 em fuso negativo.
      expect(diaMes('2026-09-01')).toBe('01/09');
      expect(diaPorExtenso('2026-09-01')).toBe('1 de setembro');
    });

    it('dataCurta bate com o Intl pt-BR em todos os meses', () => {
      for (let m = 1; m <= 12; m++) {
        const iso = `2026-${String(m).padStart(2, '0')}-16`;
        const intl = new Date(2026, m - 1, 16).toLocaleDateString('pt-BR', {
          day: '2-digit',
          month: 'short',
          year: 'numeric',
        });
        expect(dataCurta(iso)).toBe(intl);
      }
    });

    it('isoLocal usa a data local, não a UTC', () => {
      expect(isoLocal(new Date(2026, 8, 18, 23, 30))).toBe('2026-09-18');
    });

    it('ultimosMeses conta do corrente para trás, atravessando o ano', () => {
      const meses = ultimosMeses(3, new Date(2026, 0, 20));
      expect(meses.map((m) => m.valor)).toEqual(['2026-01', '2025-12', '2025-11']);
      expect(meses[0].rotulo).toBe('janeiro de 2026');
    });
  });
});
