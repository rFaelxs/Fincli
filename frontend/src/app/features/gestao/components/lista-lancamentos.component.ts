import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { GestaoStore } from '../data/gestao.store';
import { linhaLancamento } from '../gestao';

/** "Últimos lançamentos": até 6 itens do mês, já cortados pelo servidor (spec 03, itens 12 a 16). */
@Component({
  selector: 'app-lista-lancamentos',
  imports: [RouterLink],
  styleUrl: './lista-lancamentos.component.css',
  template: `<section class="secao" aria-labelledby="titulo-lancamentos">
    <div class="secao-head">
      <h2 class="titulo-secao" id="titulo-lancamentos">Últimos lançamentos</h2>
      <a class="link-secao" routerLink="/relatorio" data-cy="gestao-ver-mes">ver o mês inteiro →</a>
    </div>
    <div class="lista">
      @for (l of linhas(); track $index) {
        <div class="linha" data-cy="gestao-lancamento">
          <span class="dia">{{ l.dia }}</span>
          <div>
            <div class="desc" data-cy="gestao-lancamento-descricao">{{ l.descricao }}</div>
            <div class="cat">{{ l.categoria }}</div>
          </div>
          @if (l.chip) {
            <span class="chip" data-cy="gestao-lancamento-chip">{{ l.chip }}</span>
          }
          <span class="valor" [class]="'valor ' + l.classe" data-cy="gestao-lancamento-valor"><span
            class="visualmente-oculto">{{ l.rotuloOculto }} </span>{{ l.valor }}</span>
        </div>
      } @empty {
        <div class="linha"><span class="vazio-lista" data-cy="gestao-lancamentos-vazio">Nenhum lançamento neste mês ainda.</span></div>
      }
    </div>
  </section>`,
})
export class ListaLancamentosComponent {
  private readonly store = inject(GestaoStore);

  protected readonly linhas = computed(() =>
    (this.store.dados()?.ultimosLancamentos ?? []).map(linhaLancamento),
  );
}
