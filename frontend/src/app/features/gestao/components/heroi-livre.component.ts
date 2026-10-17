import { Component, computed, inject } from '@angular/core';
import { GestaoStore } from '../data/gestao.store';
import { heroi } from '../gestao';

/** Rótulo, número grande, pílula e texto de apoio (spec 03, itens 2 a 9). */
@Component({
  selector: 'app-heroi-livre',
  styleUrl: './heroi-livre.component.css',
  template: `<section class="heroi" aria-label="Quanto dá para gastar">
    @if (h(); as h) {
      <p class="eyebrow" data-cy="gestao-rotulo">{{ h.rotulo }}</p>
      <div class="heroi-linha">
        <span class="heroi-valor" [class.negativo]="h.negativo" data-cy="gestao-livre">{{ h.valor }}</span>
        <span class="pilula" [class.negativo]="h.negativo" data-cy="gestao-pilula">{{ h.pilula }}</span>
      </div>
      <p class="heroi-texto" data-cy="gestao-texto">{{ h.texto }}</p>
    } @else {
      <div class="heroi-linha">
        <span class="heroi-valor esqueleto" data-cy="gestao-livre">R$ 0,00</span>
      </div>
    }
  </section>`,
})
export class HeroiLivreComponent {
  private readonly store = inject(GestaoStore);

  protected readonly h = computed(() => {
    const dados = this.store.dados();
    return dados ? heroi(dados) : null;
  });
}
