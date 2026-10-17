import { Component, input, output } from '@angular/core';
import { Filtros, OpcaoMes } from '../transacoes';

/** Mês, tipo e busca (spec 06, itens 2 a 5). Cada mudança repinta a tabela na hora. */
@Component({
  selector: 'app-filtros-transacoes',
  template: `<div class="filters" role="search">
    <label class="visualmente-oculto" for="filtro-mes">Mês</label>
    <select id="filtro-mes" (change)="mudou.emit({ mes: valorDe($event) })">
      @for (m of meses(); track m.valor) {
        <option [value]="m.valor" [selected]="m.valor === filtros().mes">{{ m.rotulo }}</option>
      }
    </select>
    <label class="visualmente-oculto" for="filtro-tipo">Tipo</label>
    <select id="filtro-tipo" (change)="mudouTipo($event)">
      @for (t of tipos; track t.valor) {
        <option [value]="t.valor" [selected]="t.valor === filtros().tipo">{{ t.rotulo }}</option>
      }
    </select>
    <label class="visualmente-oculto" for="filtro-busca">Buscar por descrição ou categoria</label>
    <input id="filtro-busca" type="search" placeholder="Buscar" [value]="filtros().busca"
      (input)="mudou.emit({ busca: valorDe($event) })">
  </div>`,
})
export class FiltrosTransacoesComponent {
  readonly filtros = input.required<Filtros>();
  readonly meses = input.required<OpcaoMes[]>();
  readonly mudou = output<Partial<Filtros>>();

  protected readonly tipos = [
    { valor: 'TODOS', rotulo: 'Todos os tipos' },
    { valor: 'ENTRADA', rotulo: 'Entradas' },
    { valor: 'SAIDA', rotulo: 'Saídas' },
  ] as const;

  protected valorDe(evento: Event): string {
    return (evento.target as HTMLInputElement | HTMLSelectElement).value;
  }

  protected mudouTipo(evento: Event): void {
    this.mudou.emit({ tipo: this.valorDe(evento) as Filtros['tipo'] });
  }
}
