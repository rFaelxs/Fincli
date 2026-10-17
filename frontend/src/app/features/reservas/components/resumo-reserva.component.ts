import { Component, input } from '@angular/core';
import { moeda } from '../../../shared/format/formatos';

/**
 * "Reserva:", "Guardado hoje:" e, só na tela de alocar, "Saldo disponível:" (spec 07, itens 18,
 * 24 e 25). `disponivel` indefinido esconde a linha; nulo mostra que a leitura falhou.
 */
@Component({
  selector: 'app-resumo-reserva',
  template: `<div class="note-open">
    <p>Reserva: <b data-cy="reserva-resumo-nome">{{ nome() }}</b></p>
    <p>Guardado hoje: <b data-cy="reserva-resumo-guardado">{{ moeda(guardado()) }}</b></p>
    @if (disponivel() !== undefined) {
      <p>Saldo disponível: <b data-cy="reserva-resumo-disponivel">{{
        disponivel() === null ? '—' : moeda(disponivel()!) }}</b></p>
    }
  </div>`,
})
export class ResumoReservaComponent {
  readonly nome = input.required<string>();
  readonly guardado = input.required<number>();
  readonly disponivel = input<number | null | undefined>(undefined);

  protected readonly moeda = moeda;
}
