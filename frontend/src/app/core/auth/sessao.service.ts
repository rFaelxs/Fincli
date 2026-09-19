import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { Usuario } from '../api/modelos';
import { silencioso } from '../http/silencioso';

/**
 * Quem está logado.
 *
 * A sessão vive num cookie `HttpOnly` que o JavaScript não lê (ADR 0006): o front só sabe dela
 * perguntando ao servidor. O usuário fica guardado aqui entre uma navegação e outra.
 */
@Injectable({ providedIn: 'root' })
export class SessaoService {
  private readonly http = inject(HttpClient);
  private readonly atual = signal<Usuario | null>(null);

  readonly usuario = this.atual.asReadonly();

  /**
   * Bootstrap: emite o cookie `XSRF-TOKEN` e pergunta, em silêncio, quem está logado.
   *
   * @returns o usuário da sessão, ou `null` sem sessão
   */
  async iniciar(): Promise<Usuario | null> {
    try {
      await firstValueFrom(this.http.get('/api/csrf'));
      const usuario = await firstValueFrom(
        this.http.get<Usuario>('/api/me', { context: silencioso() }),
      );
      this.atual.set(usuario);
      return usuario;
    } catch {
      this.atual.set(null);
      return null;
    }
  }

  /** @returns o usuário já conhecido, sem ir ao servidor, ou o resultado de {@link iniciar} */
  async garantir(): Promise<Usuario | null> {
    return this.atual() ?? this.iniciar();
  }

  /** Guarda o usuário que acabou de entrar. */
  entrar(usuario: Usuario): void {
    this.atual.set(usuario);
  }

  /** Encerra a sessão no servidor; um erro aqui não impede de sair. */
  async sair(): Promise<void> {
    try {
      await firstValueFrom(this.http.post('/api/logout', null));
    } catch {
      // sessão já encerrada do lado do servidor
    }
    this.atual.set(null);
  }

  /** Esquece o usuário localmente, quando o servidor já disse que a sessão acabou. */
  esquecer(): void {
    this.atual.set(null);
  }
}
