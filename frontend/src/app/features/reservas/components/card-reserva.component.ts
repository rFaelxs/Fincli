import { Component, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Reserva } from '../../../core/api/modelos';
import { MoedaPipe, PctPipe } from '../../../shared/format/pipes';
import { MedidorComponent } from '../../../shared/ui/medidor.component';

@Component({
  selector: 'app-card-reserva',
  imports: [RouterLink, MoedaPipe, PctPipe, MedidorComponent],
  template: `
    <div class="card">
      <div class="res-head">
        <span class="res-nome">{{ reserva().nome }}</span>
        @if (reserva().emergencia) {
          <span class="chip neutral">protegida</span>
        } @else {
          <button class="btn danger" title="Excluir" type="button" (click)="excluir.emit()">
            ×
          </button>
        }
      </div>
      <div class="meter-row" style="margin-top: 0.5rem">
        <span class="meter-pct num">{{ reserva().progresso | pct }}</span>
        <span class="sub num"
          >{{ reserva().saldoAtual | moeda }} / {{ reserva().metaValor | moeda }}</span
        >
      </div>
      <app-medidor [percentual]="reserva().progresso" />
      <div class="res-acts">
        <a class="btn" [routerLink]="['/reservas', reserva().id, 'alocar']">Alocar</a>
        <a class="btn ghost" [routerLink]="['/reservas', reserva().id, 'sacar']">Sacar</a>
        <a class="btn ghost" [routerLink]="['/reservas', reserva().id, 'meta']">Meta</a>
      </div>
    </div>
  `,
  // O .card é o item do grid: é ele que estica para a altura da linha.
  host: { style: 'display: contents' },
})
export class CardReservaComponent {
  readonly reserva = input.required<Reserva>();
  readonly excluir = output<void>();
}
