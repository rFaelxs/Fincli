import { Component, computed, inject } from '@angular/core';
import { BarraComandoComponent } from '../components/barra-comando.component';
import { ContasPrevistasComponent } from '../components/contas-previstas.component';
import { HeroiLivreComponent } from '../components/heroi-livre.component';
import { KpisDoMesComponent } from '../components/kpis-do-mes.component';
import { ListaLancamentosComponent } from '../components/lista-lancamentos.component';
import { GestaoStore } from '../data/gestao.store';
import { heroi } from '../gestao';

/** `/gestao`: a tela de abertura (spec 03 e 04). Uma chamada a `GET /api/hoje` monta tudo. */
@Component({
  selector: 'app-gestao',
  imports: [
    HeroiLivreComponent, BarraComandoComponent, KpisDoMesComponent, ListaLancamentosComponent,
    ContasPrevistasComponent,
  ],
  providers: [GestaoStore],
  template: `<div class="pagina">
    <app-heroi-livre />
    <app-barra-comando [destaque]="vazio()" />
    <app-kpis-do-mes />
    <app-lista-lancamentos />
    <app-contas-previstas />
  </div>`,
})
export class GestaoPage {
  private readonly store = inject(GestaoStore);

  protected readonly vazio = computed(() => {
    const dados = this.store.dados();
    return dados ? heroi(dados).vazio : false;
  });

  constructor() {
    void this.store.carregar();
  }
}
