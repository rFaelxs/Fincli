import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { Usuario } from '../../../core/api/modelos';
import { silencioso } from '../../../core/http/silencioso';

/** Dados do cadastro. O CPF vai como foi digitado: o servidor tira a máscara. */
export interface Cadastro {
  nome: string;
  cpf: string;
  senha: string;
}

/**
 * Cadastro e login. As duas chamadas são silenciosas: um 401 aqui é senha errada, não sessão
 * expirada, e não pode mandar o usuário para o login de onde ele já está.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  login(cpf: string, senha: string): Promise<Usuario> {
    return firstValueFrom(
      this.http.post<Usuario>('/api/login', { cpf, senha }, { context: silencioso() }),
    );
  }

  cadastrar(dados: Cadastro): Promise<Usuario> {
    return firstValueFrom(
      this.http.post<Usuario>('/api/cadastro', dados, { context: silencioso() }),
    );
  }
}
