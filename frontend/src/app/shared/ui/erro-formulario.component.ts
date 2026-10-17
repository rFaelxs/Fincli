import { Component, input } from '@angular/core';

/** Bloco fixo de erro acima de um formulário. Some quando a mensagem está vazia. */
@Component({
  selector: 'app-erro-formulario',
  template: `<p class="form-erro" [class.on]="!!mensagem()" role="alert">{{ mensagem() }}</p>`,
  host: { style: 'display: contents' },
})
export class ErroFormularioComponent {
  readonly mensagem = input('');
}
