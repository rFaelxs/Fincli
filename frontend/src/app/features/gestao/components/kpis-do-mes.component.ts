import { Component, computed, inject } from '@angular/core';
import { moedaAbs } from '../../../shared/format/formatos';
import { MedidorComponent } from '../../../shared/ui/medidor.component';
import { GestaoStore } from '../data/gestao.store';

/**
 * O mês em três números.
 *
 * O sinal vem sempre junto do valor: entrada e saída não podem se distinguir só pela cor. Aporte
 * não tem sinal — o dinheiro não sumiu, mudou de lugar, e quem diz isso é o rótulo.
 */
@Component({
  selector: 'app-kpis-do-mes',
  imports: [MedidorComponent],
  template: `
    <section class="kpis">
      <div class="card kpi">
        <span class="label">ENTROU</span>
        <span class="kpi-valor num entradas" [class.esqueleto]="!dados()">{{ entrou() }}</span>
        <app-medidor [percentual]="barraEntradas()" tom="entrada" />
      </div>
      <div class="card kpi">
        <span class="label">SAIU</span>
        <span class="kpi-valor num saidas" [class.esqueleto]="!dados()">{{ saiu() }}</span>
        <app-medidor [percentual]="barraSaidas()" tom="saida" />
      </div>
      <div class="card kpi">
        <span class="label">GUARDEI</span>
        <span class="kpi-valor num aportes" [class.esqueleto]="!dados()">{{ guardei() }}</span>
        <app-medidor [percentual]="barraAportes()" />
      </div>
    </section>
  `,
  styles: `
    .kpis {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: var(--fin-s-4);
    }
    .kpi {
      display: flex;
      flex-direction: column;
      gap: 10px;
    }
    .kpi-valor {
      font-family: var(--fin-font-mono);
      font-weight: 700;
      font-size: 22px;
      line-height: 1;
      letter-spacing: -0.03em;
    }
    .entradas {
      color: var(--fin-entrada);
    }
    .saidas {
      color: var(--fin-saida);
    }
    .aportes {
      color: var(--fin-ink);
    }
  `,
})
export class KpisDoMesComponent {
  protected readonly dados = inject(GestaoStore).dados;

  protected readonly entrou = computed(() => '+' + moedaAbs(this.dados()?.entradas ?? 0));
  protected readonly saiu = computed(() => '−' + moedaAbs(this.dados()?.saidas ?? 0));
  protected readonly guardei = computed(() => {
    const aportes = Number(this.dados()?.aportes ?? 0);
    return (aportes < 0 ? '−' : '') + moedaAbs(aportes);
  });

  /** Antes da primeira resposta a barra de entradas nasce cheia, como na página antiga. */
  protected readonly barraEntradas = computed(() => {
    const d = this.dados();
    return !d || Number(d.entradas) > 0 ? 100 : 0;
  });
  protected readonly barraSaidas = computed(() => this.proporcao(this.dados()?.saidas ?? 0));
  protected readonly barraAportes = computed(() =>
    this.proporcao(Math.abs(Number(this.dados()?.aportes ?? 0))),
  );

  /** Largura da barra sobre as entradas do mês; sem entradas, vazia. */
  private proporcao(valor: number): number {
    const entradas = Number(this.dados()?.entradas ?? 0);
    return entradas > 0 ? Math.min(100, (Number(valor) / entradas) * 100) : 0;
  }
}
