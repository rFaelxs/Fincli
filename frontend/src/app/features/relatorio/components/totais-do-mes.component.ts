import { Component, input } from '@angular/core';
import { TotalDoMes } from '../relatorio';

@Component({
  selector: 'app-totais-do-mes',
  template: `
    <div class="fluxo-totais">
      @for (t of totais(); track t.rotulo) {
        <div>
          <span class="rot">{{ t.rotulo }}</span
          ><span class="val" [class]="t.classe">{{ t.valor }}</span>
        </div>
      }
    </div>
  `,
  styles: `
    .fluxo-totais {
      display: flex;
      justify-content: space-between;
      flex-wrap: wrap;
      gap: var(--fin-s-3);
    }
    .fluxo-totais > div {
      display: flex;
      flex-direction: column;
      gap: 3px;
    }
    .fluxo-totais .rot {
      font-family: var(--fin-font-mono);
      font-size: 12px;
      color: var(--fin-label);
    }
    .fluxo-totais .val {
      font-family: var(--fin-font-mono);
      font-weight: 700;
      font-size: 18px;
      font-variant-numeric: tabular-nums;
      white-space: nowrap;
    }
    .fluxo-totais .entrada {
      color: var(--fin-entrada);
    }
    .fluxo-totais .saida {
      color: var(--fin-saida);
    }
    .fluxo-totais .aporte {
      color: var(--fin-aporte);
    }
  `,
})
export class TotaisDoMesComponent {
  readonly totais = input.required<TotalDoMes[]>();
}
