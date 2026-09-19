import { Component, OnInit, inject, viewChild } from '@angular/core';
import { lerErro } from '../../../core/api/problema';
import { ToastService } from '../../../shared/ui/toast.component';
import { BarraComandoComponent } from '../components/barra-comando.component';
import { ContasPrevistasComponent } from '../components/contas-previstas.component';
import { HeroiLivreComponent } from '../components/heroi-livre.component';
import { KpisDoMesComponent } from '../components/kpis-do-mes.component';
import { ListaLancamentosComponent } from '../components/lista-lancamentos.component';
import { GestaoStore } from '../data/gestao.store';

/** Tela Gestão — antes chamada Hoje. Os números vêm de `GET /api/hoje`. */
@Component({
  selector: 'app-gestao-page',
  imports: [
    HeroiLivreComponent,
    BarraComandoComponent,
    KpisDoMesComponent,
    ListaLancamentosComponent,
    ContasPrevistasComponent,
  ],
  providers: [GestaoStore],
  host: { class: 'pagina' },
  template: `
    <app-heroi-livre />
    <app-barra-comando />
    <app-kpis-do-mes />
    <app-lista-lancamentos />
    <app-contas-previstas />
  `,
})
export class GestaoPage implements OnInit {
  private readonly store = inject(GestaoStore);
  private readonly toast = inject(ToastService);
  private readonly barra = viewChild.required(BarraComandoComponent);

  async ngOnInit(): Promise<void> {
    try {
      await this.store.carregar();
      this.barra().focar();
    } catch (e) {
      const mensagem = lerErro(e);
      this.store.mostrarErro(mensagem);
      this.toast.mostrar(mensagem);
    }
  }
}
