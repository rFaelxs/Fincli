import { Component, computed, input, output } from '@angular/core';

@Component({
  selector: 'app-rodape-usuario',
  template: `<div class="side-foot">
    <div class="user">
      <div class="avatar" aria-hidden="true">{{ iniciais() }}</div>
      <div style="min-width: 0">
        <div class="who">{{ nome() }}</div>
        <div class="quando">{{ mesAtual }}</div>
      </div>
    </div>
    <button class="btn ghost" type="button" (click)="sair.emit()">Sair</button>
  </div>`,
  host: { style: 'display: contents' },
})
export class RodapeUsuarioComponent {
  readonly nome = input.required<string>();
  readonly sair = output<void>();

  protected readonly iniciais = computed(() =>
    this.nome()
      .trim()
      .split(/\s+/)
      .slice(0, 2)
      .map((parte) => parte[0])
      .join('')
      .toUpperCase(),
  );

  protected readonly mesAtual = new Date().toLocaleDateString('pt-BR', {
    month: 'long',
    year: 'numeric',
  });
}
