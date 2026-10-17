import { Component, input, output } from '@angular/core';
import { Pergunta } from '../relatorio';

/**
 * Acordeão de uma pergunta (spec 05, itens 16 a 18). O botão continua no DOM ao abrir e fechar,
 * então o foco fica nele.
 */
@Component({
  selector: 'app-pergunta',
  template: `<div class="pergunta" data-cy="relatorio-pergunta">
    <button type="button" [attr.aria-expanded]="aberta()" [attr.aria-controls]="'resposta-' + indice()"
      (click)="alternar.emit()" data-cy="relatorio-pergunta-botao">
      <span>{{ pergunta().titulo }}</span>
      <span class="seta" aria-hidden="true">{{ aberta() ? '▴' : '▾' }}</span>
    </button>
    @if (aberta()) {
      <div class="resposta" [id]="'resposta-' + indice()">
        <span class="destaque" data-cy="relatorio-pergunta-destaque">{{ pergunta().destaque }}</span>
        <p data-cy="relatorio-pergunta-texto">{{ pergunta().texto }}</p>
        @for (l of pergunta().linhas; track $index) {
          <div class="linha-barra" data-cy="relatorio-pergunta-linha">
            <span class="rot">{{ l.rotulo }}</span>
            <span class="trilho" aria-hidden="true"><i [style.width.%]="l.largura"></i></span>
            <span class="val" data-cy="relatorio-pergunta-valor">{{ l.valor }}</span>
          </div>
        }
      </div>
    }
  </div>`,
})
export class PerguntaComponent {
  readonly pergunta = input.required<Pergunta>();
  readonly indice = input.required<number>();
  readonly aberta = input.required<boolean>();
  readonly alternar = output<void>();
}
