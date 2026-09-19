import { Component, input } from '@angular/core';
import { mesCurto } from '../../../shared/format/formatos';
import { Coluna } from '../relatorio';

@Component({
  selector: 'app-historico-sobra',
  template: `
    <div class="card historico-card">
      <div class="historico">
        @for (c of colunas(); track c.mes) {
          <div class="col">
            <span class="rotulo">{{ c.rotulo }}</span>
            <div
              class="barra"
              [class.atual]="c.atual"
              [class.negativa]="c.negativa"
              [style.height.%]="c.altura"
            ></div>
          </div>
        }
      </div>
      <div class="historico-meses">
        @for (c of colunas(); track c.mes) {
          <span>{{ mesCurto(c.mes) }}</span>
        }
      </div>
    </div>
  `,
  styles: `
    .historico-card {
      padding: 24px;
      display: flex;
      flex-direction: column;
      gap: 10px;
      border-radius: var(--fin-r-2xl);
    }

    .historico {
      display: flex;
      align-items: flex-end;
      gap: var(--fin-s-4);
      height: 150px;
    }
    .historico .col {
      flex: 1;
      height: 100%;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: flex-end;
      gap: var(--fin-s-2);
    }
    .historico .rotulo {
      font-family: var(--fin-font-mono);
      font-weight: 500;
      font-size: 12px;
      color: var(--fin-text-3);
      white-space: nowrap;
    }
    .historico .barra {
      width: 100%;
      border-radius: 7px 7px 0 0;
      background: var(--fin-track);
    }
    .historico .barra.atual {
      background: var(--fin-brand);
    }
    /* Mês negativo: a barra cresce para baixo não teria como, então muda de cor e o rótulo
       carrega o sinal. */
    .historico .barra.negativa {
      background: var(--fin-saida);
    }

    .historico-meses {
      display: flex;
      gap: var(--fin-s-4);
      border-top: 1px solid var(--fin-track);
      padding-top: var(--fin-s-2);
    }
    .historico-meses span {
      flex: 1;
      text-align: center;
      font-family: var(--fin-font-mono);
      font-size: 12px;
      color: var(--fin-faint);
    }
  `,
})
export class HistoricoSobraComponent {
  readonly colunas = input.required<Coluna[]>();
  protected readonly mesCurto = mesCurto;
}
