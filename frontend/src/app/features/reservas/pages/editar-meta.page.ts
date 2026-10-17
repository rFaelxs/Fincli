import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Reserva } from '../../../core/api/modelos';
import { mensagemDeErro } from '../../../core/api/problema';
import { ErroFormularioComponent } from '../../../shared/ui/erro-formulario.component';
import { ResumoReservaComponent } from '../components/resumo-reserva.component';
import { ReservasService } from '../data/reservas.service';

type Carga =
  | { tipo: 'carregando' }
  | { tipo: 'falhou'; mensagem: string }
  | { tipo: 'pronta'; reserva: Reserva };

/** `/reservas/:id/meta` (spec 07, itens 17 a 21). */
@Component({
  selector: 'app-editar-meta',
  imports: [ReactiveFormsModule, ErroFormularioComponent, ResumoReservaComponent],
  template: `<div class="pagina">
    <header class="head">
      <div>
        <h1>Editar meta</h1>
        <p class="sub" data-cy="reserva-subtitulo">{{ subtitulo() }}</p>
      </div>
    </header>
    @if (carga(); as c) {
      @if (c.tipo === 'pronta') {
        <form class="card form-card" ngNativeValidate [formGroup]="form" (ngSubmit)="salvar()">
          <app-erro-formulario [mensagem]="erro()" />
          <app-resumo-reserva [nome]="c.reserva.nome" [guardado]="c.reserva.saldoAtual" />
          <div class="field">
            <label for="meta">Nova meta (R$)</label>
            <input id="meta" data-cy="reserva-meta-valor" type="number" formControlName="valor"
              step="0.01" min="0" inputmode="decimal" required>
          </div>
          <div class="form-acts">
            <button class="btn" type="submit" data-cy="reserva-meta-salvar">Salvar meta</button>
          </div>
        </form>
      }
    }
  </div>`,
})
export class EditarMetaPage implements OnInit {
  private readonly servico = inject(ReservasService);
  private readonly router = inject(Router);

  /** Vem do parâmetro da rota. */
  readonly id = input<string>();

  protected readonly carga = signal<Carga>({ tipo: 'carregando' });
  protected readonly erro = signal('');
  protected readonly form = new FormGroup({ valor: new FormControl<number | null>(null) });

  protected readonly subtitulo = computed(() => {
    const c = this.carga();
    if (c.tipo === 'carregando') {
      return 'Carregando…';
    }
    return c.tipo === 'falhou' ? c.mensagem : 'Defina o novo valor-alvo.';
  });

  async ngOnInit(): Promise<void> {
    const id = this.id();
    if (!id) {
      await this.router.navigateByUrl('/reservas', { replaceUrl: true });
      return;
    }
    try {
      const reserva = await this.servico.buscar(id);
      // O campo nasce com a meta vigente (item 19).
      this.form.setValue({ valor: reserva.metaValor });
      this.carga.set({ tipo: 'pronta', reserva });
    } catch (e) {
      this.carga.set({ tipo: 'falhou', mensagem: mensagemDeErro(e) });
    }
  }

  protected async salvar(): Promise<void> {
    this.erro.set('');
    try {
      await this.servico.alterarMeta(this.id()!, Number(this.form.getRawValue().valor));
      await this.router.navigateByUrl('/reservas', { replaceUrl: true });
    } catch (e) {
      this.erro.set(mensagemDeErro(e));
    }
  }
}
