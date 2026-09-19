import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { lerErro } from '../../../core/api/problema';
import { SessaoService } from '../../../core/auth/sessao.service';
import { ErroFormularioComponent } from '../../../shared/ui/erro-formulario.component';
import { AuthService } from '../data/auth.service';

@Component({
  selector: 'app-login-page',
  imports: [ReactiveFormsModule, RouterLink, ErroFormularioComponent],
  styleUrl: '../auth.css',
  template: `
    <div class="auth-plane">
      <form class="auth card" [formGroup]="form" (ngSubmit)="entrar()" ngNativeValidate>
        <div class="marca"><img src="/assets/img/fincli-logo.png" alt="FinCLI" /></div>
        <div class="auth-title">
          <div class="t">Acesse sua conta</div>
          <div class="sub">Receitas, despesas e reservas em um só lugar.</div>
        </div>
        <app-erro-formulario [mensagem]="erro()" />
        <div class="field">
          <label for="cpf">CPF</label>
          <input
            id="cpf"
            class="mono"
            inputmode="numeric"
            placeholder="000.000.000-00"
            autocomplete="username"
            required
            formControlName="cpf"
          />
        </div>
        <div class="field">
          <label for="senha">Senha</label>
          <input
            id="senha"
            type="password"
            placeholder="••••••••"
            autocomplete="current-password"
            required
            formControlName="senha"
          />
        </div>
        <button class="btn" type="submit">Entrar</button>
        <div class="auth-alt">Não tem conta? <a routerLink="/cadastro">Cadastre-se</a></div>
      </form>
    </div>
  `,
})
export class LoginPage {
  private readonly auth = inject(AuthService);
  private readonly sessao = inject(SessaoService);
  private readonly router = inject(Router);

  protected readonly form = inject(FormBuilder).nonNullable.group({ cpf: '', senha: '' });
  protected readonly erro = signal('');

  protected async entrar(): Promise<void> {
    this.erro.set('');
    try {
      const usuario = await this.auth.entrar(this.form.getRawValue());
      this.sessao.entrar(usuario);
      await this.router.navigateByUrl('/gestao', { replaceUrl: true });
    } catch (e) {
      // A mesma frase para CPF inexistente e senha errada: distinguir os dois revelaria quem
      // tem conta (ADR 0007).
      this.erro.set(
        e instanceof HttpErrorResponse && e.status === 401 ? 'CPF ou senha inválidos.' : lerErro(e),
      );
    }
  }
}
