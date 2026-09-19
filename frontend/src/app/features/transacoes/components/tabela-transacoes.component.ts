import { Component, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Transacao } from '../../../core/api/modelos';
import { decimal } from '../../../shared/format/formatos';
import { DiaMesPipe } from '../../../shared/format/pipes';

@Component({
  selector: 'app-tabela-transacoes',
  imports: [RouterLink, DiaMesPipe],
  template: `
    <div class="card">
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Data</th>
              <th>Descrição</th>
              <th>Categoria</th>
              <th>Tipo</th>
              <th style="text-align: right">Valor (R$)</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            @for (t of transacoes(); track t.id) {
              <tr>
                <td class="date">{{ t.data | diaMes }}</td>
                <td class="desc">
                  @if (t.descricao) {
                    {{ t.descricao }}
                  } @else {
                    <span class="muted">—</span>
                  }
                </td>
                <td class="muted">{{ t.categoria }}</td>
                <td>
                  @if (t.tipo === 'ENTRADA') {
                    <span class="chip in">↑ entrada</span>
                  } @else {
                    <span class="chip out">↓ saída</span>
                    @if (t.essencial) {
                      &ngsp;<span class="chip neutral">essencial</span>
                    }
                  }
                </td>
                <td
                  class="num"
                  [class.money-in]="t.tipo === 'ENTRADA'"
                  [class.money-out]="t.tipo === 'SAIDA'"
                >
                  {{ t.tipo === 'ENTRADA' ? '+' : '−' }}{{ decimal(t.valor) }}
                </td>
                <td class="acts">
                  <a
                    class="btn danger"
                    title="Editar"
                    [routerLink]="['/transacoes', t.id, 'editar']"
                    >✎</a
                  >
                  <button
                    class="btn danger"
                    title="Excluir"
                    type="button"
                    (click)="excluir.emit(t.id)"
                  >
                    ×
                  </button>
                </td>
              </tr>
            } @empty {
              @if (carregado()) {
                <tr>
                  <td colspan="6" class="vazio">Nenhuma transação encontrada.</td>
                </tr>
              }
            }
          </tbody>
          <tfoot>
            <tr>
              <td colspan="4">Total do período</td>
              <td class="num">{{ total() }}</td>
              <td></td>
            </tr>
          </tfoot>
        </table>
      </div>
    </div>
  `,
  styles: `
    .acts {
      display: inline-flex;
      gap: var(--fin-s-1);
    }
  `,
})
export class TabelaTransacoesComponent {
  readonly transacoes = input.required<Transacao[]>();
  readonly carregado = input.required<boolean>();
  /** Já formatado, com sinal; um traço sem resultado. */
  readonly total = input.required<string>();
  readonly excluir = output<string>();

  protected readonly decimal = decimal;
}
