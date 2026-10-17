import { Component, input } from '@angular/core';

/**
 * Lugar provisório das telas que ainda não foram implementadas. Cada uma sai quando a spec dela
 * for construída.
 */
@Component({
  selector: 'app-em-construcao',
  template: `<div class="pagina">
    <header class="head">
      <h1>{{ titulo() }}</h1>
    </header>
    <p class="vazio">Esta tela ainda está em construção.</p>
  </div>`,
})
export class EmConstrucaoPage {
  /** Vem do `data` da rota. */
  readonly titulo = input.required<string>();
}
