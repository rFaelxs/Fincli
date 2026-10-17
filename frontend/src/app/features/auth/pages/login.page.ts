import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { mensagemDeErro } from '../../../core/api/problema';
import { SessaoService } from '../../../core/auth/sessao.service';
import { ErroFormularioComponent } from '../../../shared/ui/erro-formulario.component';
import { AuthService } from '../data/auth.service';

/** Mesma frase para CPF inexistente e senha errada (spec 02, item 7). */
export const CREDENCIAIS_INVALIDAS = 'CPF ou senha inválidos.';

/**
 * Tela de login. A validação dos campos é a nativa do navegador: `ngNativeValidate` impede o
 * Angular de desligá-la.
 */
@Component({
  selector: 'app-login-page',
  imports: [ReactiveFormsModule, RouterLink, ErroFormularioComponent],
  styleUrl: '../auth.css',
  template: `<div class="auth-plane">
    <main class="auth fin-card">
      <div class="marca"><img src="assets/img/fincli-logo.png" alt="FinCLI" width="150" height="57"></div>
      <div class="auth-title">
        <h1 class="t">Acesse sua conta</h1>
        <p class="sub">Receitas, despesas e reservas em um só lugar.</p>
      </div>
      <app-erro-formulario [mensagem]="erro()" />
      <form class="form-card" ngNativeValidate [formGroup]="form" (ngSubmit)="entrar()">
        <div class="field">
          <label for="cpf">CPF</label>
          <input id="cpf" formControlName="cpf" inputmode="numeric" autocomplete="username"
            placeholder="000.000.000-00" required>
        </div>
        <div class="field">
          <label for="senha">Senha</label>
          <input id="senha" type="password" formControlName="senha" autocomplete="current-password"
            placeholder="••••••••" required>
        </div>
        <button class="btn" type="submit">Entrar</button>
      </form>
      <p class="auth-alt">Não tem conta? <a routerLink="/cadastro">Cadastre-se</a></p>
    </main>
  </div>`,
})
export class LoginPage {
  private readonly auth = inject(AuthService);
  private readonly sessao = inject(SessaoService);
  private readonly router = inject(Router);

  protected readonly form = new FormGroup({
    cpf: new FormControl('', { nonNullable: true }),
    senha: new FormControl('', { nonNullable: true }),
  });
  protected readonly erro = signal('');

  protected async entrar(): Promise<void> {
    this.erro.set('');
    const { cpf, senha } = this.form.getRawValue();
    try {
      this.sessao.entrar(await this.auth.login(cpf, senha));
      await this.router.navigateByUrl('/gestao', { replaceUrl: true });
    } catch (e) {
      const recusado = e instanceof HttpErrorResponse && e.status === 401;
      this.erro.set(recusado ? CREDENCIAIS_INVALIDAS : mensagemDeErro(e));
    }
  }
}
