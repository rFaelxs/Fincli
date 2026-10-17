import { Component, computed, inject } from '@angular/core';
import { moeda } from '../../../shared/format/formatos';
import { GestaoStore } from '../data/gestao.store';
import { nomeDoMes, vencimento } from '../gestao';

/** "Ainda vence em setembro": as contas a vencer e o remover (spec 03, itens 17 a 21). */
@Component({
  selector: 'app-contas-previstas',
  styleUrl: './lista-lancamentos.component.css',
  template: `<section class="secao" aria-labelledby="titulo-contas">
    <h2 class="titulo-secao" id="titulo-contas" data-cy="gestao-contas-titulo">{{ titulo() }}</h2>
    <div class="lista">
      @for (c of contas(); track c.id) {
        <div class="linha" data-cy="gestao-conta">
          <div>
            <div class="desc" data-cy="gestao-conta-nome">{{ c.nome }}</div>
            <div class="cat" data-cy="gestao-conta-vencimento">{{ vencimento(c) }}</div>
          </div>
          <span class="valor" data-cy="gestao-conta-valor">{{ moeda(c.valor) }}</span>
          <button class="btn danger" type="button" (click)="store.removerConta(c)"
            [attr.aria-label]="'Remover ' + c.nome" data-cy="gestao-conta-remover">Remover</button>
        </div>
      } @empty {
        <div class="linha"><p class="vazio-lista" data-cy="gestao-contas-vazio">Nenhuma conta prevista
          para o resto do mês. Escreva <code>prever energia 210 dia 22</code> na barra para
          adicionar uma.</p></div>
      }
    </div>
  </section>`,
})
export class ContasPrevistasComponent {
  protected readonly store = inject(GestaoStore);

  protected readonly contas = computed(() => this.store.dados()?.previstas ?? []);
  protected readonly titulo = computed(() => {
    const mes = this.store.dados()?.mes;
    return mes ? `Ainda vence em ${nomeDoMes(mes)}` : 'Ainda vence no mês';
  });
  protected readonly moeda = moeda;
  protected readonly vencimento = vencimento;
}
