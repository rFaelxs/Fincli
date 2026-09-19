import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { lerErro } from '../../../core/api/problema';
import { ErroFormularioComponent } from '../../../shared/ui/erro-formulario.component';
import { ReservasService } from '../data/reservas.service';

@Component({
  selector: 'app-nova-reserva-page',
  imports: [ReactiveFormsModule, RouterLink, ErroFormularioComponent],
  host: { class: 'pagina' },
  template: `
    <div class="head">
      <div>
        <h1>Nova reserva</h1>
        <span class="sub">Uma meta para guardar dinheiro com propósito.</span>
      </div>
    </div>

    <form class="card form-card" [formGroup]="form" (ngSubmit)="criar()" ngNativeValidate>
      <app-erro-formulario [mensagem]="erro()" />
      <div class="field">
        <label for="nome">Nome</label>
        <input
          id="nome"
          maxlength="120"
          placeholder="Ex.: Viagem"
          required
          formControlName="nome"
        />
      </div>
      <div class="field">
        <label for="meta">Meta (R$)</label>
        <input
          id="meta"
          class="num"
          type="number"
          step="0.01"
          min="0"
          placeholder="0,00"
          required
          formControlName="meta"
        />
      </div>
      <div class="form-acts">
        <a class="btn ghost" routerLink="/reservas">Cancelar</a>
        <button class="btn" type="submit">Criar</button>
      </div>
    </form>
  `,
})
export class NovaReservaPage {
  private readonly servico = inject(ReservasService);
  private readonly router = inject(Router);

  protected readonly form = inject(FormBuilder).group({
    nome: [''],
    meta: [null as number | null],
  });
  protected readonly erro = signal('');

  protected async criar(): Promise<void> {
    this.erro.set('');
    const { nome, meta } = this.form.getRawValue();
    try {
      await this.servico.criar({ nome: nome ?? '', metaValor: Number(meta) });
      await this.router.navigateByUrl('/reservas', { replaceUrl: true });
    } catch (e) {
      this.erro.set(lerErro(e));
    }
  }
}
