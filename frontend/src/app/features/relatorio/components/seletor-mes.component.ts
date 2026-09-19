import { Component, model } from '@angular/core';
import { ultimosMeses } from '../../../shared/format/formatos';

/** Os doze últimos meses, do corrente para trás. */
@Component({
  selector: 'app-seletor-mes',
  template: `
    <select aria-label="Mês do relatório" (change)="mes.set($any($event.target).value)">
      @for (m of meses; track m.valor) {
        <option [value]="m.valor" [selected]="m.valor === mes()">{{ m.rotulo }}</option>
      }
    </select>
  `,
})
export class SeletorMesComponent {
  protected readonly meses = ultimosMeses(12);
  readonly mes = model(this.meses[0].valor);
}
