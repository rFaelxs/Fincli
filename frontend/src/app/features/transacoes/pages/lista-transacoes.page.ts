import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Transacao } from '../../../core/api/modelos';
import { mensagemDeErro } from '../../../core/api/problema';
import { ToastService } from '../../../shared/ui/toast.component';
import { FiltrosTransacoesComponent } from '../components/filtros-transacoes.component';
import { TabelaTransacoesComponent } from '../components/tabela-transacoes.component';
import { TransacoesService } from '../data/transacoes.service';
import { Filtros, contador, filtrar, totalDoPeriodo, ultimosMeses } from '../transacoes';

/** `/transacoes`: lista filtrada, total do período e exclusão (spec 06, itens 1 a 18). */
@Component({
  selector: 'app-lista-transacoes',
  imports: [RouterLink, FiltrosTransacoesComponent, TabelaTransacoesComponent],
  // O contador fica ao lado do título, na mesma linha (spec 06, item 6).
  styles: `.titulo-com-contador { display: flex; align-items: baseline; gap: var(--fin-s-3); }`,
  template: `<div class="pagina">
    <header class="head">
      <div class="titulo-com-contador">
        <h1>Transações</h1>
        <span class="asof" data-cy="transacoes-contador">{{ contador(visiveis().length) }}</span>
      </div>
      <a class="btn" routerLink="/transacoes/nova" data-cy="transacoes-nova">+ Nova transação</a>
    </header>
    <app-filtros-transacoes [filtros]="filtros()" [meses]="meses" (mudou)="filtrarPor($event)" />
    <app-tabela-transacoes [transacoes]="visiveis()" [total]="total()" (excluir)="excluir($event)" />
  </div>`,
})
export class ListaTransacoesPage {
  private readonly transacoes = inject(TransacoesService);
  private readonly toast = inject(ToastService);
  private readonly todas = signal<Transacao[]>([]);

  protected readonly meses = ultimosMeses(6);
  protected readonly filtros = signal<Filtros>({
    mes: this.meses[0].valor,
    tipo: 'TODOS',
    busca: '',
  });
  protected readonly visiveis = computed(() => filtrar(this.todas(), this.filtros()));
  protected readonly total = computed(() => totalDoPeriodo(this.visiveis()));
  protected readonly contador = contador;

  constructor() {
    void this.carregar();
  }

  protected filtrarPor(mudanca: Partial<Filtros>): void {
    this.filtros.update((atual) => ({ ...atual, ...mudanca }));
  }

  /** Só chama o servidor depois da confirmação (itens 16 e 17). */
  protected async excluir(t: Transacao): Promise<void> {
    if (!window.confirm('Excluir esta transação?')) {
      return;
    }
    try {
      await this.transacoes.excluir(t.id);
      this.toast.mostrar('Transação excluída.');
      await this.carregar();
    } catch (e) {
      this.toast.mostrar(mensagemDeErro(e));
    }
  }

  private async carregar(): Promise<void> {
    try {
      this.todas.set(await this.transacoes.listar());
    } catch (e) {
      this.toast.mostrar(mensagemDeErro(e));
    }
  }
}
