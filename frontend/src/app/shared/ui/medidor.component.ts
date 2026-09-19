import { Component, computed, input } from '@angular/core';

/**
 * Barra de progresso.
 *
 * A largura chega pronta, em percentual, e só é limitada a 0–100: um progresso de 120% enche a
 * barra sem vazar do trilho.
 */
@Component({
  selector: 'app-medidor',
  template: `<div class="meter"><i [class]="tom()" [style.width.%]="largura()"></i></div>`,
  styles: `
    :host {
      display: block;
    }
    .meter > i.entrada {
      background: var(--fin-entrada);
    }
    .meter > i.saida {
      background: var(--fin-saida);
    }
  `,
})
export class MedidorComponent {
  readonly percentual = input.required<number>();
  readonly tom = input<'entrada' | 'saida' | ''>('');

  protected readonly largura = computed(() => Math.max(0, Math.min(100, this.percentual())));
}
