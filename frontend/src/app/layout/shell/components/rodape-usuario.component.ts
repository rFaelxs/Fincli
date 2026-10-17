import { Component, computed, input, output } from '@angular/core';
import { iniciais, mesPorExtenso } from '../../../shared/format/formatos';

/** Avatar, nome, mês corrente e o botão "Sair" (spec 01, item 11). */
@Component({
  selector: 'app-rodape-usuario',
  template: `<div class="side-foot">
    <div class="user">
      <div class="avatar" aria-hidden="true" data-cy="shell-avatar">{{ avatar() }}</div>
      <div>
        <div class="who" data-cy="shell-usuario-nome">{{ nome() }}</div>
        <div class="quando">{{ mes }}</div>
      </div>
    </div>
    <button class="btn ghost" type="button" (click)="sair.emit()" data-cy="shell-sair">Sair</button>
  </div>`,
  host: { style: 'display: contents' },
})
export class RodapeUsuarioComponent {
  readonly nome = input.required<string>();
  readonly sair = output<void>();

  protected readonly avatar = computed(() => iniciais(this.nome()));
  protected readonly mes = mesPorExtenso(new Date());
}
