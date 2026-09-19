import { Component, input, output } from '@angular/core';

/** Faixa de confirmação ou de erro, com uma ação opcional ("desfazer", "tentar de novo"). */
@Component({
  selector: 'app-faixa-aviso',
  template: `<div class="faixa" [class.erro]="erro()">
    <span class="texto">{{ texto() }}</span>
    @if (acao(); as rotulo) {
      <button class="acao" type="button" (click)="acionar.emit()">{{ rotulo }}</button>
    }
  </div>`,
})
export class FaixaAvisoComponent {
  readonly texto = input.required<string>();
  readonly acao = input<string | null>(null);
  readonly erro = input(false);
  readonly acionar = output<void>();
}
