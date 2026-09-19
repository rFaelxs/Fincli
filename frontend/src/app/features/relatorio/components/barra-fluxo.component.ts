import { Component, computed, input } from '@angular/core';
import { Relatorio } from '../../../core/api/modelos';
import { descricaoDoFluxo, segmentosDoFluxo, totaisDoMes } from '../relatorio';
import { TotaisDoMesComponent } from './totais-do-mes.component';

/** Composição do mês: essenciais, supérfluas, reservas e sobra numa barra só. */
@Component({
  selector: 'app-barra-fluxo',
  imports: [TotaisDoMesComponent],
  template: `
    <section class="card fluxo-card">
      <div class="fluxo" role="img" [attr.aria-label]="descricao()">
        @if (dados()) {
          @for (s of segmentos(); track s.chave) {
            <div class="faixa-seg" [class]="s.chave" [style.flex]="s.valor" [title]="s.title">
              {{ s.texto }}
            </div>
          } @empty {
            <div class="faixa-seg sobra" style="flex: 1">nada lançado neste mês</div>
          }
        }
      </div>
      <app-totais-do-mes [totais]="totais()" />
    </section>
  `,
  styles: `
    .fluxo-card {
      padding: var(--fin-s-6);
      display: flex;
      flex-direction: column;
      gap: 18px;
      border-radius: var(--fin-r-2xl);
    }

    .fluxo {
      display: flex;
      height: 52px;
      overflow: hidden;
      border-radius: var(--fin-r-lg);
      border: 1px solid var(--fin-border);
    }
    .fluxo .faixa-seg {
      display: flex;
      align-items: center;
      padding-left: var(--fin-s-3);
      font-weight: 500;
      font-size: 13px;
      white-space: nowrap;
      overflow: hidden;
      min-width: 0;
    }
    .fluxo .essenciais {
      background: var(--fin-brand);
      color: var(--fin-brand-ink);
    }
    .fluxo .superfluas {
      background: var(--fin-brand-tint-border);
      color: var(--fin-brand-deep);
    }
    .fluxo .reservas {
      background: var(--fin-reserva-faixa);
      color: var(--fin-text);
    }
    .fluxo .sobra {
      background: var(--fin-bg);
      color: var(--fin-text);
    }
  `,
})
export class BarraFluxoComponent {
  readonly dados = input<Relatorio | null>(null);

  protected readonly segmentos = computed(() => {
    const d = this.dados();
    return d ? segmentosDoFluxo(d) : [];
  });
  protected readonly descricao = computed(() =>
    this.dados() ? descricaoDoFluxo(this.segmentos()) : 'Composição do mês',
  );
  protected readonly totais = computed(() => {
    const d = this.dados();
    return d ? totaisDoMes(d) : [];
  });
}
