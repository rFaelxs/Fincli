import { Component, OnInit, input, output } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { DadosTransacao, TipoTransacao } from '../../../core/api/modelos';
import { hojeIso } from '../../../shared/format/formatos';
import { ErroFormularioComponent } from '../../../shared/ui/erro-formulario.component';

/**
 * Formulário de transação, usado pela criação e pela edição (spec 06, itens 19 a 27). A validação
 * dos campos é a nativa do navegador; a do servidor chega pelo bloco de erro no topo.
 */
@Component({
  selector: 'app-formulario-transacao',
  imports: [ReactiveFormsModule, RouterLink, ErroFormularioComponent],
  template: `<form class="card form-card" ngNativeValidate [formGroup]="form" (ngSubmit)="submeter()">
    <app-erro-formulario [mensagem]="erro()" />
    <div class="field">
      <label for="descricao">Descrição</label>
      <input id="descricao" data-cy="transacao-descricao" formControlName="descricao"
        maxlength="255" placeholder="Ex.: Mercado do mês">
    </div>
    <div class="field">
      <label for="categoria">Categoria</label>
      <input id="categoria" data-cy="transacao-categoria" formControlName="categoria"
        maxlength="80" required placeholder="Ex.: Alimentação">
    </div>
    <div class="field">
      <label for="valor">Valor (R$)</label>
      <input id="valor" data-cy="transacao-valor" type="number" formControlName="valor"
        step="0.01" min="0" inputmode="decimal" required>
    </div>
    <div class="field">
      <label for="data">Data</label>
      <input id="data" data-cy="transacao-data" type="date" formControlName="data" required>
    </div>
    <div class="field">
      <label for="tipo">Tipo</label>
      <select id="tipo" data-cy="transacao-tipo" formControlName="tipo">
        <option value="SAIDA">Saída</option>
        <option value="ENTRADA">Entrada</option>
      </select>
    </div>
    <label class="check"><input type="checkbox" data-cy="transacao-essencial"
      formControlName="essencial"> Despesa essencial</label>
    <div class="form-acts">
      <a class="btn ghost" routerLink="/transacoes" data-cy="transacao-cancelar">Cancelar</a>
      <button class="btn" type="submit" data-cy="transacao-salvar">{{ rotuloEnvio() }}</button>
    </div>
  </form>`,
})
export class FormularioTransacaoComponent implements OnInit {
  /** Valores da edição; nulo na criação. */
  readonly inicial = input<DadosTransacao | null>(null);
  readonly rotuloEnvio = input.required<string>();
  readonly erro = input('');
  readonly enviar = output<DadosTransacao>();

  // A criação nasce com saída, desmarcada e com a data local de hoje (itens 21 a 23).
  protected readonly form = new FormGroup({
    descricao: new FormControl('', { nonNullable: true }),
    categoria: new FormControl('', { nonNullable: true }),
    valor: new FormControl<number | null>(null),
    data: new FormControl(hojeIso(), { nonNullable: true }),
    tipo: new FormControl<TipoTransacao>('SAIDA', { nonNullable: true }),
    essencial: new FormControl(false, { nonNullable: true }),
  });

  ngOnInit(): void {
    const inicial = this.inicial();
    if (inicial) {
      this.form.setValue({ ...inicial, descricao: inicial.descricao ?? '' });
    }
  }

  /** Descrição em branco vai nula (item 24). */
  protected submeter(): void {
    const v = this.form.getRawValue();
    this.enviar.emit({
      ...v,
      descricao: v.descricao.trim() === '' ? null : v.descricao,
      valor: Number(v.valor),
    });
  }
}
