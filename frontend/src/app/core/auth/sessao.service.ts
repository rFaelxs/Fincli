import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { Usuario } from '../api/modelos';
import { silencioso } from '../http/silencioso';

/** Quem está logado. A sessão em si vive num cookie HttpOnly que o front não lê (ADR 0006). */
@Injectable({ providedIn: 'root' })
export class SessaoService {
  private readonly http = inject(HttpClient);
  private readonly atual = signal<Usuario | null>(null);

  /** O usuário da sessão; nulo sem sessão. */
  readonly usuario = this.atual.asReadonly();

  /**
   * Pede o token CSRF e depois pergunta quem está logado, as duas em silêncio (spec 01, item 1).
   * O CSRF vem antes porque toda mutação seguinte precisa do cookie.
   *
   * @returns o usuário, ou nulo quando não há sessão
   */
  async iniciar(): Promise<Usuario | null> {
    const opcoes = { context: silencioso() };
    try {
      await firstValueFrom(this.http.get<void>('/api/csrf', opcoes));
      const usuario = await firstValueFrom(this.http.get<Usuario>('/api/me', opcoes));
      this.atual.set(usuario);
      return usuario;
    } catch {
      this.atual.set(null);
      return null;
    }
  }

  /** Grava o usuário devolvido pelo login. */
  entrar(usuario: Usuario): void {
    this.atual.set(usuario);
  }

  /** Esquece o usuário sem falar com o servidor, como quando a sessão expira. */
  esquecer(): void {
    this.atual.set(null);
  }

  /** Encerra a sessão. Um erro do servidor é ignorado: quem pediu para sair sai (item 12). */
  async sair(): Promise<void> {
    try {
      await firstValueFrom(this.http.post<void>('/api/logout', null, { context: silencioso() }));
    } catch {
      // A navegação para o login acontece do mesmo jeito.
    } finally {
      this.atual.set(null);
    }
  }
}
