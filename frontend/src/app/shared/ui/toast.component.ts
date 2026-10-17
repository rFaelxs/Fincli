import { Component, Injectable, inject, signal } from '@angular/core';

/** Quanto tempo o aviso fica na tela (spec 01, item 15). */
export const DURACAO_TOAST_MS = 3200;

/** Avisos curtos, como "Transação excluída.". Um por vez: o novo substitui o anterior. */
@Injectable({ providedIn: 'root' })
export class ToastService {
  private readonly atual = signal('');
  private temporizador: ReturnType<typeof setTimeout> | undefined;

  readonly mensagem = this.atual.asReadonly();

  mostrar(mensagem: string): void {
    this.atual.set(mensagem);
    clearTimeout(this.temporizador);
    this.temporizador = setTimeout(() => this.atual.set(''), DURACAO_TOAST_MS);
  }
}

/** O aviso em si, montado uma vez na raiz da aplicação. */
@Component({
  selector: 'app-toast',
  template: `<div class="toast" [class.on]="!!toast.mensagem()" role="status">
    {{ toast.mensagem() }}
  </div>`,
})
export class ToastComponent {
  protected readonly toast = inject(ToastService);
}
