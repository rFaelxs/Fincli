import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Transacao } from '../../../core/api/modelos';
import { lerErro } from '../../../core/api/problema';
import { decimal, ultimosMeses } from '../../../shared/format/formatos';
import { ToastService } from '../../../shared/ui/toast.component';
import { FiltrosTransacoesComponent } from '../components/filtros-transacoes.component';
import { TabelaTransacoesComponent } from '../components/tabela-transacoes.component';
import { TransacoesService } from '../data/transacoes.service';

@Component({
  selector: 'app-lista-transacoes-page',
  imports: [RouterLink, FiltrosTransacoesComponent, TabelaTransacoesComponent],
  host: { class: 'pagina' },
  template: `
    <div class="head">
      <div>
        <h1>Transações</h1>
        <span class="asof">{{ contador() }}</span>
      </div>
      <a class="btn" routerLink="/transacoes/nova">+ Nova transação</a>
    </div>

    <app-filtros-transacoes [(mes)]="mes" [(tipo)]="tipo" [(busca)]="busca" />

    <app-tabela-transacoes
      [transacoes]="filtradas()"
      [carregado]="carregado()"
      [total]="total()"
      (excluir)="excluir($event)"
    />
  `,
})
export class ListaTransacoesPage implements OnInit {
  private readonly transacoes = inject(TransacoesService);
  private readonly toast = inject(ToastService);

  private readonly todas = signal<Transacao[]>([]);
  protected readonly carregado = signal(false);

  protected readonly mes = signal(ultimosMeses(1)[0].valor);
  protected readonly tipo = signal('');
  protected readonly busca = signal('');

  protected readonly filtradas = computed(() => {
    const mes = this.mes();
    const tipo = this.tipo();
    const busca = this.busca().trim().toLowerCase();
    return this.todas().filter(
      (t) =>
        (!mes || t.data.startsWith(mes)) &&
        (!tipo || t.tipo === tipo) &&
        (!busca ||
          (t.descricao ?? '').toLowerCase().includes(busca) ||
          t.categoria.toLowerCase().includes(busca)),
    );
  });

  protected readonly contador = computed(() =>
    this.carregado() ? this.filtradas().length + ' lançamento(s)' : '',
  );

  /** Entradas menos saídas do que está filtrado — a única soma de dinheiro feita na tela. */
  protected readonly total = computed(() => {
    const lista = this.filtradas();
    if (!lista.length) {
      return '—';
    }
    const soma = lista.reduce((s, t) => s + (t.tipo === 'ENTRADA' ? 1 : -1) * Number(t.valor), 0);
    return (soma >= 0 ? '+' : '−') + decimal(Math.abs(soma));
  });

  async ngOnInit(): Promise<void> {
    await this.carregar();
  }

  protected async excluir(id: string): Promise<void> {
    if (!confirm('Excluir esta transação?')) {
      return;
    }
    try {
      await this.transacoes.remover(id);
      this.toast.mostrar('Transação excluída.');
      await this.carregar();
    } catch (e) {
      this.toast.mostrar(lerErro(e));
    }
  }

  private async carregar(): Promise<void> {
    try {
      this.todas.set(await this.transacoes.listar());
      this.carregado.set(true);
    } catch (e) {
      this.toast.mostrar(lerErro(e));
    }
  }
}
