import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { Reserva } from '../../../core/api/modelos';
import { pct } from '../../../shared/format/formatos';

/** O que o card "GUARDADO" mostra. */
export type EstadoGuardado =
  | { tipo: 'carregando' }
  | { tipo: 'erro' }
  | { tipo: 'pronto'; total: number; legenda: string };

/**
 * Soma o guardado e monta a legenda (spec 01, itens 6 a 8). A soma é em centavos, para que
 * 0,10 + 0,20 não vire 0,30000000000000004.
 */
export function resumirGuardado(reservas: Reserva[]): { total: number; legenda: string } {
  const centavos = reservas.reduce((soma, r) => soma + Math.round(r.saldoAtual * 100), 0);
  const total = centavos / 100;
  if (reservas.length === 0) {
    return { total, legenda: 'nenhuma reserva ainda' };
  }

  let legenda = reservas.length === 1 ? 'em 1 reserva' : `em ${reservas.length} reservas`;
  const emergencia = reservas.find((r) => r.emergencia);
  if (emergencia && emergencia.metaValor > 0) {
    legenda += ` · emergência ${pct(emergencia.progresso)}`;
  }
  return { total, legenda };
}

/**
 * Carrega o card "GUARDADO". Falha em silêncio: o card mostra o erro, mas o resto da tela segue
 * funcionando (item 10).
 */
@Injectable({ providedIn: 'root' })
export class GuardadoService {
  private readonly http = inject(HttpClient);
  private readonly atual = signal<EstadoGuardado>({ tipo: 'carregando' });

  readonly estado = this.atual.asReadonly();

  async carregar(): Promise<void> {
    try {
      const reservas = await firstValueFrom(this.http.get<Reserva[]>('/api/reservas'));
      this.atual.set({ tipo: 'pronto', ...resumirGuardado(reservas) });
    } catch {
      this.atual.set({ tipo: 'erro' });
    }
  }
}
