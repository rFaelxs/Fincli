import { Component, DestroyRef, computed, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs';
import { moeda } from '../../../shared/format/formatos';
import { GuardadoService } from '../data/guardado.service';

/**
 * Card "GUARDADO" da barra lateral. Recarrega a cada navegação concluída, como cada página do
 * front antigo fazia ao abrir.
 */
@Component({
  selector: 'app-card-guardado',
  template: `<div class="resumo">
    <span class="rot">GUARDADO</span>
    <span class="val" [class.esqueleto]="carregando()" data-cy="shell-guardado-valor">{{ valor() }}</span>
    <span class="leg" data-cy="shell-guardado-legenda">{{ legenda() }}</span>
  </div>`,
  host: { style: 'display: contents' },
})
export class CardGuardadoComponent {
  private readonly guardado = inject(GuardadoService);

  protected readonly carregando = computed(() => this.guardado.estado().tipo === 'carregando');

  protected readonly valor = computed(() => {
    const estado = this.guardado.estado();
    if (estado.tipo === 'pronto') {
      return moeda(estado.total);
    }
    return estado.tipo === 'erro' ? '—' : moeda(0);
  });

  protected readonly legenda = computed(() => {
    const estado = this.guardado.estado();
    if (estado.tipo === 'pronto') {
      return estado.legenda;
    }
    return estado.tipo === 'erro' ? 'não consegui carregar' : '';
  });

  constructor() {
    inject(Router)
      .events.pipe(
        filter((e) => e instanceof NavigationEnd),
        takeUntilDestroyed(inject(DestroyRef)),
      )
      .subscribe(() => void this.guardado.carregar());
  }
}
