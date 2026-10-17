import { Component, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Transacao } from '../../../core/api/modelos';
import { diaMes } from '../../../shared/format/formatos';
import { totalFormatado, valorDaLinha } from '../transacoes';

/** Linhas, rodapé com o total e as ações de cada transação (spec 06, itens 7 a 15). */
@Component({
  selector: 'app-tabela-transacoes',
  imports: [RouterLink],
  template: `<div class="card table-wrap">
    <table>
      <thead>
        <tr>
          <th scope="col">Data</th>
          <th scope="col">Descrição</th>
          <th scope="col">Categoria</th>
          <th scope="col">Tipo</th>
          <th scope="col" class="num">Valor (R$)</th>
          <th scope="col"><span class="visualmente-oculto">Ações</span></th>
        </tr>
      </thead>
      <tbody>
        @for (t of transacoes(); track t.id) {
          <tr data-cy="transacao-linha">
            <td class="date">{{ diaMes(t.data) }}</td>
            <td class="desc">
              @if (t.descricao) {
                {{ t.descricao }}
              } @else {
                <span class="muted" aria-label="sem descrição">—</span>
              }
            </td>
            <td>{{ t.categoria }}</td>
            <td>
              @if (t.tipo === 'ENTRADA') {
                <span class="chip in">↑ entrada</span>
              } @else {
                <span class="chip out">↓ saída</span>
                @if (t.essencial) {
                  <span class="chip">essencial</span>
                }
              }
            </td>
            <td class="num" [class.money-in]="t.tipo === 'ENTRADA'"
              [class.money-out]="t.tipo === 'SAIDA'">{{ valorDaLinha(t) }}</td>
            <td>
              <a class="link-secao" [routerLink]="['/transacoes', t.id, 'editar']"
                [attr.aria-label]="'Editar ' + (t.descricao || t.categoria)"
                data-cy="transacao-editar">Editar</a>
              <button class="btn danger" type="button" (click)="excluir.emit(t)"
                [attr.aria-label]="'Excluir ' + (t.descricao || t.categoria)"
                data-cy="transacao-excluir">Excluir</button>
            </td>
          </tr>
        } @empty {
          <tr><td colspan="6" class="vazio" data-cy="transacoes-vazio">Nenhuma transação encontrada.</td></tr>
        }
      </tbody>
      <tfoot>
        <tr>
          <td colspan="4">Total do período</td>
          <td class="num" data-cy="transacoes-total">{{ totalFormatado(total()) }}</td>
          <td></td>
        </tr>
      </tfoot>
    </table>
  </div>`,
})
export class TabelaTransacoesComponent {
  readonly transacoes = input.required<Transacao[]>();
  readonly total = input.required<number | null>();
  readonly excluir = output<Transacao>();

  protected readonly diaMes = diaMes;
  protected readonly valorDaLinha = valorDaLinha;
  protected readonly totalFormatado = totalFormatado;
}
