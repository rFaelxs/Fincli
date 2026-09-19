import { Component, input } from '@angular/core';

/**
 * Bloco de erro no topo de um formulário.
 *
 * O elemento some da caixa: dentro de um formulário em flex com gap, um erro escondido não pode
 * ocupar espaço.
 */
@Component({
  selector: 'app-erro-formulario',
  template: `<div class="form-erro" [class.on]="!!mensagem()">{{ mensagem() }}</div>`,
  host: { style: 'display: contents' },
})
export class ErroFormularioComponent {
  readonly mensagem = input<string>('');
}
