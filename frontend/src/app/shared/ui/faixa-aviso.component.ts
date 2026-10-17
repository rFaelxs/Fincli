import { Component, input, output } from '@angular/core';

/** Faixa de aviso: confirmação ou erro, com uma ação ao lado ("desfazer", "tentar de novo"). */
@Component({
  selector: 'app-faixa-aviso',
  template: `<div class="faixa" [class.erro]="erro()" [attr.role]="erro() ? 'alert' : 'status'"
    data-cy="faixa">
    <span class="texto" data-cy="faixa-texto">{{ texto() }}</span>
    <button class="acao" type="button" (click)="agir.emit()" data-cy="faixa-acao">{{ acao() }}</button>
  </div>`,
})
export class FaixaAvisoComponent {
  readonly texto = input.required<string>();
  readonly acao = input.required<string>();
  readonly erro = input(false);
  readonly agir = output<void>();
}
