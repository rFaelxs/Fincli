import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Reserva } from '../../../core/api/modelos';
import { lerErro } from '../../../core/api/problema';
import { moeda } from '../../../shared/format/formatos';
import { ToastService } from '../../../shared/ui/toast.component';
import { CardReservaComponent } from '../components/card-reserva.component';
import { ReservasService } from '../data/reservas.service';

@Component({
  selector: 'app-lista-reservas-page',
  imports: [RouterLink, CardReservaComponent],
  host: { class: 'pagina' },
  template: `
    <div class="head">
      <div>
        <h1>Reservas</h1>
        <span class="asof num">{{ totalGuardado() }}</span>
      </div>
      <a class="btn" routerLink="/reservas/nova">+ Nova reserva</a>
    </div>

    <div class="grid-res">
      @for (r of reservas(); track r.id) {
        <app-card-reserva [reserva]="r" (excluir)="excluir(r.id)" />
      }
    </div>
  `,
})
export class ListaReservasPage implements OnInit {
  private readonly servico = inject(ReservasService);
  private readonly toast = inject(ToastService);

  protected readonly reservas = signal<Reserva[] | null>(null);

  protected readonly totalGuardado = computed(() => {
    const lista = this.reservas();
    if (!lista) {
      return '';
    }
    return moeda(lista.reduce((s, r) => s + Number(r.saldoAtual), 0)) + ' guardados no total';
  });

  async ngOnInit(): Promise<void> {
    await this.carregar();
  }

  protected async excluir(id: string): Promise<void> {
    if (!confirm('Excluir esta reserva?')) {
      return;
    }
    try {
      await this.servico.excluir(id);
      this.toast.mostrar('Reserva excluída.');
      await this.carregar();
    } catch (e) {
      this.toast.mostrar(lerErro(e));
    }
  }

  private async carregar(): Promise<void> {
    try {
      this.reservas.set(await this.servico.listar());
    } catch (e) {
      this.toast.mostrar(lerErro(e));
    }
  }
}
