import { Component, input } from '@angular/core';
import { MoedaPipe } from '../../../shared/format/pipes';

/** Quadro no topo dos formulários de meta e de movimentação. */
@Component({
  selector: 'app-resumo-reserva',
  imports: [MoedaPipe],
  template: `
    <div class="resumo-mov">
      <span
        >Reserva: <b>{{ nome() }}</b></span
      >
      <span
        >Guardado hoje: <b class="num">{{ saldo() | moeda }}</b></span
      >
      @if (disponivel(); as valor) {
        <span
          >Saldo disponível: <b class="num">{{ valor.saldo | moeda }}</b></span
        >
      }
    </div>
  `,
  styles: `
    .resumo-mov {
      display: flex;
      flex-direction: column;
      gap: var(--fin-s-1);
      padding: var(--fin-s-3) var(--fin-s-4);
      border: 1px solid var(--fin-border);
      border-radius: var(--fin-r-lg);
      background: var(--fin-bg);
      font-size: 14px;
      color: var(--fin-text);
    }
    .resumo-mov b {
      color: var(--fin-ink);
      font-weight: 600;
    }
  `,
  host: { style: 'display: contents' },
})
export class ResumoReservaComponent {
  readonly nome = input.required<string>();
  readonly saldo = input.required<number>();
  /** Só na alocação, onde é o teto do valor. Embrulhado para que 0 não esconda a linha. */
  readonly disponivel = input<{ saldo: number } | null>(null);
}
