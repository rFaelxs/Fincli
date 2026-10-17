import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { mensagemDeErro } from '../../../core/api/problema';
import { ErroFormularioComponent } from '../../../shared/ui/erro-formulario.component';
import { ReservasService } from '../data/reservas.service';

/** `/reservas/nova` (spec 07, itens 12 a 16). */
@Component({
  selector: 'app-nova-reserva',
  imports: [ReactiveFormsModule, RouterLink, ErroFormularioComponent],
  template: `<div class="pagina">
    <header class="head">
      <div>
        <h1>Nova reserva</h1>
        <p class="sub">Uma meta para guardar dinheiro com propósito.</p>
      </div>
    </header>
    <form class="card form-card" ngNativeValidate [formGroup]="form" (ngSubmit)="criar()">
      <app-erro-formulario [mensagem]="erro()" />
      <div class="field">
        <label for="nome">Nome</label>
        <input id="nome" data-cy="reserva-form-nome" formControlName="nome" maxlength="120"
          required placeholder="Ex.: Viagem">
      </div>
      <div class="field">
        <label for="meta">Meta (R$)</label>
        <input id="meta" data-cy="reserva-form-meta" type="number" formControlName="metaValor"
          step="0.01" min="0" inputmode="decimal" required>
      </div>
      <div class="form-acts">
        <a class="btn ghost" routerLink="/reservas" data-cy="reserva-form-cancelar">Cancelar</a>
        <button class="btn" type="submit" data-cy="reserva-form-criar">Criar</button>
      </div>
    </form>
  </div>`,
})
export class NovaReservaPage {
  private readonly servico = inject(ReservasService);
  private readonly router = inject(Router);

  protected readonly form = new FormGroup({
    nome: new FormControl('', { nonNullable: true }),
    metaValor: new FormControl<number | null>(null),
  });
  protected readonly erro = signal('');

  /** Sucesso troca a rota; erro fica no topo com o formulário preenchido (itens 15 e 16). */
  protected async criar(): Promise<void> {
    this.erro.set('');
    const { nome, metaValor } = this.form.getRawValue();
    try {
      await this.servico.criar(nome, Number(metaValor));
      await this.router.navigateByUrl('/reservas', { replaceUrl: true });
    } catch (e) {
      this.erro.set(mensagemDeErro(e));
    }
  }
}
