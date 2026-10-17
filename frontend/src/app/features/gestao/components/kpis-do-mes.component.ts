import { Component, computed, inject } from '@angular/core';
import { GestaoStore } from '../data/gestao.store';
import { kpis } from '../gestao';

/** ENTROU, SAIU e GUARDEI, com os três medidores (spec 03, itens 10 e 11). */
@Component({
  selector: 'app-kpis-do-mes',
  styles: `
    .kpis { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: var(--fin-s-3); }
    .kpi { display: flex; flex-direction: column; gap: var(--fin-s-2); }
    .kpi .valor { font-family: var(--fin-font-mono); font-weight: 700; font-size: 20px;
      font-variant-numeric: tabular-nums; white-space: nowrap; }
  `,
  template: `<section class="kpis" aria-label="Números do mês">
    @for (k of lista(); track k.rotulo) {
      <div class="card kpi" data-cy="gestao-kpi">
        <span class="label" data-cy="gestao-kpi-rotulo">{{ k.rotulo }}</span>
        <span class="valor" [class.esqueleto]="carregando()" data-cy="gestao-kpi-valor">{{ k.valor }}</span>
        <div class="meter" role="progressbar" aria-valuemin="0" aria-valuemax="100"
          [attr.aria-valuenow]="k.medidor" [attr.aria-label]="k.rotulo">
          <i [style.width.%]="k.medidor" data-cy="gestao-kpi-medidor"></i>
        </div>
      </div>
    }
  </section>`,
})
export class KpisDoMesComponent {
  private readonly store = inject(GestaoStore);

  protected readonly carregando = computed(() => this.store.dados() === null);
  /** Enquanto carrega, os três mostram "R$ 0,00" com esqueleto (item 2). */
  protected readonly lista = computed(() => {
    const dados = this.store.dados();
    return dados
      ? kpis(dados)
      : ['ENTROU', 'SAIU', 'GUARDEI'].map((rotulo) => ({ rotulo, valor: 'R$ 0,00', medidor: 0 }));
  });
}
