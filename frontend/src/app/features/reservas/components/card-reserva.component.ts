import { Component, computed, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Reserva } from '../../../core/api/modelos';
import { moeda, pct } from '../../../shared/format/formatos';

/** Nome, progresso, medidor e ações de uma reserva (spec 07, itens 3 a 6). */
@Component({
  selector: 'app-card-reserva',
  imports: [RouterLink],
  template: `<article class="card" data-cy="reserva-card">
    <div class="res-head">
      <span class="res-nome" data-cy="reserva-nome">{{ reserva().nome }}</span>
      @if (reserva().emergencia) {
        <span class="chip" data-cy="reserva-protegida">protegida</span>
      } @else {
        <button class="btn danger" type="button" (click)="excluir.emit(reserva())"
          [attr.aria-label]="'Excluir ' + reserva().nome" data-cy="reserva-excluir">Excluir</button>
      }
    </div>
    <div class="meter-row">
      <span class="meter-pct" data-cy="reserva-progresso">{{ pct(reserva().progresso) }}</span>
    </div>
    <div class="meter" role="progressbar" aria-valuemin="0" aria-valuemax="100"
      [attr.aria-valuenow]="medidor()" [attr.aria-label]="'Progresso de ' + reserva().nome">
      <i [style.width.%]="medidor()"></i>
    </div>
    <div class="meter-legend">
      <span data-cy="reserva-saldo-meta"><b>{{ moeda(reserva().saldoAtual) }}</b> / {{
        moeda(reserva().metaValor) }}</span>
    </div>
    <div class="res-acts">
      <a class="btn ghost" [routerLink]="['/reservas', reserva().id, 'alocar']"
        data-cy="reserva-alocar">Alocar</a>
      <a class="btn ghost" [routerLink]="['/reservas', reserva().id, 'sacar']"
        data-cy="reserva-sacar">Sacar</a>
      <a class="btn ghost" [routerLink]="['/reservas', reserva().id, 'meta']"
        data-cy="reserva-meta">Meta</a>
    </div>
  </article>`,
})
export class CardReservaComponent {
  readonly reserva = input.required<Reserva>();
  readonly excluir = output<Reserva>();

  /** O medidor para em 100%, mesmo quando o progresso passa disso (item 4). */
  protected readonly medidor = computed(() =>
    Math.min(Math.max(this.reserva().progresso, 0), 100),
  );
  protected readonly moeda = moeda;
  protected readonly pct = pct;
}
