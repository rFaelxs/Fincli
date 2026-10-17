import { Reserva } from '../../../core/api/modelos';
import { resumirGuardado } from './guardado.service';

function reserva(saldoAtual: number, extra: Partial<Reserva> = {}): Reserva {
  return {
    id: crypto.randomUUID(),
    nome: 'Viagem',
    metaValor: 0,
    saldoAtual,
    progresso: 0,
    emergencia: false,
    ...extra,
  };
}

describe('resumirGuardado', () => {
  // CA-SHELL-05
  it('soma o saldo das reservas e conta quantas são', () => {
    const r = resumirGuardado([reserva(1200), reserva(300)]);

    expect(r.total).toBe(1500);
    expect(r.legenda.startsWith('em 2 reservas')).toBe(true);
  });

  it('soma em centavos, sem erro de ponto flutuante', () => {
    expect(resumirGuardado([reserva(0.1), reserva(0.2)]).total).toBe(0.3);
  });

  // CA-SHELL-06
  it('emergência sem meta não mostra percentual', () => {
    const r = resumirGuardado([reserva(500, { emergencia: true, metaValor: 0 })]);

    expect(r.legenda).toBe('em 1 reserva');
  });

  // CA-SHELL-07
  it('emergência com meta mostra o progresso', () => {
    const r = resumirGuardado([
      reserva(1250, { emergencia: true, metaValor: 10000, progresso: 12.5 }),
      reserva(0),
    ]);

    expect(r.legenda).toBe('em 2 reservas · emergência 12,5%');
  });

  // Item 8
  it('sem reservas', () => {
    expect(resumirGuardado([])).toEqual({ total: 0, legenda: 'nenhuma reserva ainda' });
  });
});
