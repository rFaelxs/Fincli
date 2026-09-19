import { Component, Injectable, inject, signal } from '@angular/core';

/** Aviso curto no rodapé da tela, que some sozinho. */
@Injectable({ providedIn: 'root' })
export class ToastService {
  private static readonly DURACAO_MS = 3200;

  readonly mensagem = signal('');
  readonly visivel = signal(false);
  private timer?: ReturnType<typeof setTimeout>;

  mostrar(mensagem: string): void {
    this.mensagem.set(mensagem);
    this.visivel.set(true);
    clearTimeout(this.timer);
    this.timer = setTimeout(() => this.visivel.set(false), ToastService.DURACAO_MS);
  }
}

@Component({
  selector: 'app-toast',
  template: `<div class="toast" [class.on]="toast.visivel()" role="status">
    {{ toast.mensagem() }}
  </div>`,
})
export class ToastComponent {
  protected readonly toast = inject(ToastService);
}
