import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Reserva } from '../../../core/api/modelos';
import { mensagemDeErro } from '../../../core/api/problema';
import { GuardadoService, resumirGuardado } from '../../../layout/shell/data/guardado.service';
import { moeda } from '../../../shared/format/formatos';
import { ToastService } from '../../../shared/ui/toast.component';
import { CardReservaComponent } from '../components/card-reserva.component';
import { ReservasService } from '../data/reservas.service';

/** `/reservas`: cards, total guardado e exclusão (spec 07, itens 1 a 11). */
@Component({
  selector: 'app-lista-reservas',
  imports: [RouterLink, CardReservaComponent],
  template: `<div class="pagina">
    <header class="head">
      <div>
        <h1>Reservas</h1>
        <p class="sub" data-cy="reservas-total">{{ moeda(total()) }} guardados no total</p>
      </div>
      <a class="btn" routerLink="/reservas/nova" data-cy="reservas-nova">+ Nova reserva</a>
    </header>
    <section class="grid-res" aria-label="Reservas">
      @for (r of reservas(); track r.id) {
        <app-card-reserva [reserva]="r" (excluir)="excluir($event)" />
      }
    </section>
  </div>`,
})
export class ListaReservasPage {
  private readonly servico = inject(ReservasService);
  private readonly toast = inject(ToastService);
  private readonly guardado = inject(GuardadoService);

  protected readonly reservas = signal<Reserva[]>([]);
  /** Mesma soma em centavos do card "GUARDADO" do shell. */
  protected readonly total = computed(() => resumirGuardado(this.reservas()).total);
  protected readonly moeda = moeda;

  constructor() {
    void this.carregar();
  }

  /** Só chama o servidor depois da confirmação (itens 8 e 9). */
  protected async excluir(r: Reserva): Promise<void> {
    if (!window.confirm('Excluir esta reserva?')) {
      return;
    }
    try {
      await this.servico.excluir(r.id);
      this.toast.mostrar('Reserva excluída.');
      // Excluir não navega, e o card "GUARDADO" do shell só recarrega em navegação.
      void this.guardado.carregar();
      await this.carregar();
    } catch (e) {
      this.toast.mostrar(mensagemDeErro(e));
    }
  }

  private async carregar(): Promise<void> {
    try {
      this.reservas.set(await this.servico.listar());
    } catch (e) {
      this.toast.mostrar(mensagemDeErro(e));
    }
  }
}
