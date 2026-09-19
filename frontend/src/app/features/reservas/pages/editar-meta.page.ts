import { Component, OnInit, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Reserva } from '../../../core/api/modelos';
import { lerErro } from '../../../core/api/problema';
import { ErroFormularioComponent } from '../../../shared/ui/erro-formulario.component';
import { ResumoReservaComponent } from '../components/resumo-reserva.component';
import { ReservasService } from '../data/reservas.service';

@Component({
  selector: 'app-editar-meta-page',
  imports: [ReactiveFormsModule, RouterLink, ErroFormularioComponent, ResumoReservaComponent],
  host: { class: 'pagina' },
  template: `
    <div class="head">
      <div>
        <h1>Editar meta</h1>
        <span class="sub">{{ subtitulo() }}</span>
      </div>
    </div>

    @if (reserva(); as r) {
      <form class="card form-card" [formGroup]="form" (ngSubmit)="salvar()" ngNativeValidate>
        <app-erro-formulario [mensagem]="erro()" />
        <app-resumo-reserva [nome]="r.nome" [saldo]="r.saldoAtual" />
        <div class="field">
          <label for="meta">Nova meta (R$)</label>
          <input
            id="meta"
            class="num"
            type="number"
            step="0.01"
            min="0"
            required
            formControlName="meta"
          />
        </div>
        <div class="form-acts">
          <a class="btn ghost" routerLink="/reservas">Cancelar</a>
          <button class="btn" type="submit">Salvar meta</button>
        </div>
      </form>
    }
  `,
})
export class EditarMetaPage implements OnInit {
  private readonly servico = inject(ReservasService);
  private readonly router = inject(Router);

  /** Da rota `/reservas/:id/meta`. */
  readonly id = input.required<string>();

  protected readonly reserva = signal<Reserva | null>(null);
  protected readonly subtitulo = signal('Carregando…');
  protected readonly erro = signal('');
  protected readonly form = inject(FormBuilder).group({ meta: [null as number | null] });

  async ngOnInit(): Promise<void> {
    try {
      const r = await this.servico.buscar(this.id());
      this.form.setValue({ meta: r.metaValor });
      this.reserva.set(r);
      this.subtitulo.set('Defina o novo valor-alvo.');
    } catch (e) {
      this.subtitulo.set(lerErro(e));
    }
  }

  protected async salvar(): Promise<void> {
    this.erro.set('');
    try {
      await this.servico.atualizarMeta(this.id(), Number(this.form.getRawValue().meta));
      await this.router.navigateByUrl('/reservas', { replaceUrl: true });
    } catch (e) {
      this.erro.set(lerErro(e));
    }
  }
}
