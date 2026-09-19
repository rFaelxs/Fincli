import { Component, model } from '@angular/core';
import { ultimosMeses } from '../../../shared/format/formatos';

@Component({
  selector: 'app-filtros-transacoes',
  template: `
    <div class="filters">
      <select aria-label="Mês" (change)="mes.set(valorDe($event))">
        @for (m of meses; track m.valor) {
          <option [value]="m.valor" [selected]="m.valor === mes()">{{ m.rotulo }}</option>
        }
      </select>
      <select aria-label="Tipo" (change)="tipo.set(valorDe($event))">
        <option value="" [selected]="tipo() === ''">Todos os tipos</option>
        <option value="ENTRADA" [selected]="tipo() === 'ENTRADA'">Entradas</option>
        <option value="SAIDA" [selected]="tipo() === 'SAIDA'">Saídas</option>
      </select>
      <input
        id="f-busca"
        type="search"
        placeholder="Buscar descrição ou categoria…"
        aria-label="Buscar"
        [value]="busca()"
        (input)="busca.set(valorDe($event))"
      />
    </div>
  `,
  styles: `
    #f-busca {
      min-width: 15rem;
      flex: 1 1 auto;
      max-width: 24rem;
    }
  `,
})
export class FiltrosTransacoesComponent {
  /** Os seis últimos meses; o primeiro, o corrente, é o filtro inicial. Não há "todos". */
  protected readonly meses = ultimosMeses(6);

  readonly mes = model(this.meses[0].valor);
  readonly tipo = model('');
  readonly busca = model('');

  protected valorDe(e: Event): string {
    return (e.target as HTMLInputElement | HTMLSelectElement).value;
  }
}
