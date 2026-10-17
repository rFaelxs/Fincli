import { signal } from '@angular/core';
import { Usuario } from '../app/core/api/modelos';

/**
 * Substituto do SessaoService para os testes de rota e de tela: decide se há sessão sem passar
 * pelo HTTP. O `iniciar` real é coberto à parte, no teste do próprio serviço.
 */
export class SessaoFalsa {
  readonly usuario = signal<Usuario | null>(null);
  /** Como o real: depois de sair, o servidor já não reconhece a sessão. */
  readonly iniciar = vi.fn(async () => this.usuario());
  readonly sair = vi.fn(async () => this.usuario.set(null));

  constructor(logado: Usuario | null) {
    this.usuario.set(logado);
  }

  entrar(usuario: Usuario): void {
    this.usuario.set(usuario);
  }

  esquecer(): void {
    this.usuario.set(null);
  }
}
