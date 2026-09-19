import { Component, effect, inject, input, output } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { TipoTransacao, Transacao, TransacaoRequest } from '../../../core/api/modelos';
import { isoLocal } from '../../../shared/format/formatos';
import { ErroFormularioComponent } from '../../../shared/ui/erro-formulario.component';

/** Formulário de criação e edição; a validação de campo é a nativa do navegador. */
@Component({
  selector: 'app-formulario-transacao',
  imports: [ReactiveFormsModule, RouterLink, ErroFormularioComponent],
  template: `
    <form class="card form-card" [formGroup]="form" (ngSubmit)="submeter()" ngNativeValidate>
      <app-erro-formulario [mensagem]="erro()" />
      <div class="field">
        <label for="descricao">Descrição</label>
        <input
          id="descricao"
          maxlength="255"
          [attr.placeholder]="comExemplos() ? 'Ex.: Mercado do mês' : null"
          formControlName="descricao"
        />
      </div>
      <div class="field">
        <label for="categoria">Categoria</label>
        <input
          id="categoria"
          maxlength="80"
          [attr.placeholder]="comExemplos() ? 'Ex.: Alimentação' : null"
          required
          formControlName="categoria"
        />
      </div>
      <div class="field">
        <label for="valor">Valor (R$)</label>
        <input
          id="valor"
          class="num"
          type="number"
          step="0.01"
          min="0"
          [attr.placeholder]="comExemplos() ? '0,00' : null"
          required
          formControlName="valor"
        />
      </div>
      <div class="field">
        <label for="data">Data</label>
        <input id="data" type="date" required formControlName="data" />
      </div>
      <div class="field">
        <label for="tipo">Tipo</label>
        <select id="tipo" formControlName="tipo">
          <option value="SAIDA">Saída</option>
          <option value="ENTRADA">Entrada</option>
        </select>
      </div>
      <label class="check">
        <input type="checkbox" id="essencial" formControlName="essencial" /> Despesa essencial
      </label>
      <div class="form-acts">
        <a class="btn ghost" routerLink="/transacoes">Cancelar</a>
        <button class="btn" type="submit">{{ rotuloEnviar() }}</button>
      </div>
    </form>
  `,
})
export class FormularioTransacaoComponent {
  /** Transação a editar; ausente na criação. */
  readonly inicial = input<Transacao | null>(null);
  /** Os exemplos nos campos só aparecem na criação. */
  readonly comExemplos = input(false);
  readonly rotuloEnviar = input.required<string>();
  readonly erro = input('');
  readonly enviar = output<TransacaoRequest>();

  protected readonly form = inject(FormBuilder).group({
    descricao: [''],
    categoria: [''],
    valor: [null as number | null],
    data: [isoLocal(new Date())],
    tipo: ['SAIDA' as TipoTransacao],
    essencial: [false],
  });

  constructor() {
    effect(() => {
      const t = this.inicial();
      if (t) {
        this.form.setValue({
          descricao: t.descricao ?? '',
          categoria: t.categoria,
          valor: t.valor,
          data: t.data,
          tipo: t.tipo,
          essencial: t.essencial,
        });
      }
    });
  }

  protected submeter(): void {
    const v = this.form.getRawValue();
    this.enviar.emit({
      valor: Number(v.valor),
      categoria: v.categoria ?? '',
      // Descrição em branco vai nula, como o backend espera de um campo opcional.
      descricao: v.descricao || null,
      data: v.data ?? '',
      tipo: v.tipo ?? 'SAIDA',
      essencial: !!v.essencial,
    });
  }
}
