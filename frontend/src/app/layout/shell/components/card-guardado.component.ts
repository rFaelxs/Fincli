import { Component, inject } from '@angular/core';
import { GuardadoService } from '../data/guardado.service';

@Component({
  selector: 'app-card-guardado',
  template: `<div class="resumo">
    <span class="rot">GUARDADO</span>
    <span class="val" [class.esqueleto]="estado().carregando">{{ estado().valor }}</span>
    <span class="leg">{{ estado().legenda }}</span>
  </div>`,
  host: { style: 'display: contents' },
})
export class CardGuardadoComponent {
  protected readonly estado = inject(GuardadoService).estado;
}
