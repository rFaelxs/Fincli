import { Component, computed, inject } from '@angular/core';
import { soMes } from '../../../shared/format/formatos';
import { DiaMesPipe, MoedaPipe } from '../../../shared/format/pipes';
import { GestaoStore } from '../data/gestao.store';

/** A justificativa do número do topo: as contas que ainda saem neste mês. */
@Component({
  selector: 'app-contas-previstas',
  imports: [DiaMesPipe, MoedaPipe],
  template: `
    <section class="secao">
      <h2 class="titulo-secao">{{ titulo() }}</h2>
      <div class="previstas">
        @if (dados(); as d) {
          @for (p of d.previstas; track p.id) {
            <div class="prevista">
              <button
                class="tirar"
                type="button"
                [attr.aria-label]="'Remover ' + p.nome"
                title="Remover"
                (click)="store.removerPrevista(p)"
              >
                ×
              </button>
              <span class="nome">{{ p.nome }}</span>
              <span class="valor">{{ p.valor | moeda }}</span>
              <span class="vence">vence {{ p.vencimento | diaMes }}</span>
            </div>
          } @empty {
            <p class="vazio">
              Nenhuma conta prevista para o resto do mês. Escreva
              <code>prever energia 210 dia 22</code> na barra para adicionar uma.
            </p>
          }
        }
      </div>
    </section>
  `,
  styles: `
    .secao {
      display: flex;
      flex-direction: column;
      gap: var(--fin-s-3);
    }

    .previstas {
      display: flex;
      gap: var(--fin-s-3);
      flex-wrap: wrap;
    }
    .prevista {
      position: relative;
      display: flex;
      flex-direction: column;
      gap: var(--fin-s-1);
      min-width: 150px;
      padding: var(--fin-s-3) var(--fin-s-4);
      border: 1px dashed var(--fin-dashed);
      border-radius: var(--fin-r-lg);
      background: var(--fin-bg);
    }
    .prevista .nome {
      font-weight: 500;
      font-size: 14px;
      padding-right: 18px;
    }
    .prevista .valor {
      font-family: var(--fin-font-mono);
      font-weight: 700;
      font-size: 15px;
      font-variant-numeric: tabular-nums;
      color: var(--fin-text);
    }
    .prevista .vence {
      font-family: var(--fin-font-mono);
      font-size: 12px;
      color: var(--fin-faint);
    }
    .prevista .tirar {
      position: absolute;
      top: var(--fin-s-2);
      right: var(--fin-s-2);
      width: 20px;
      height: 20px;
      border-radius: var(--fin-r-sm);
      display: grid;
      place-items: center;
      font-size: 14px;
      line-height: 1;
      color: var(--fin-faint);
    }
    .prevista .tirar:hover {
      background: var(--fin-chip-bg);
      color: var(--fin-saida);
    }
  `,
})
export class ContasPrevistasComponent {
  protected readonly store = inject(GestaoStore);
  protected readonly dados = this.store.dados;

  protected readonly titulo = computed(() => {
    const d = this.dados();
    return d ? 'Ainda vence em ' + soMes(d.mes) : 'Ainda vence este mês';
  });
}
