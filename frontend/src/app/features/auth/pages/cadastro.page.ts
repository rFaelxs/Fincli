import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { mensagemDeErro } from '../../../core/api/problema';
import { SessaoService } from '../../../core/auth/sessao.service';
import { ErroFormularioComponent } from '../../../shared/ui/erro-formulario.component';
import { AuthService } from '../data/auth.service';

/**
 * Tela de cadastro. Cadastra e já entra com as mesmas credenciais (spec 02, item 12). O e-mail
 * opcional do contrato fica fora do formulário, como a spec manda.
 */
@Component({
  selector: 'app-cadastro-page',
  imports: [ReactiveFormsModule, RouterLink, ErroFormularioComponent],
  styleUrl: '../auth.css',
  template: `<div class="auth-plane">
    <main class="auth fin-card">
      <div class="marca"><img src="assets/img/fincli-logo.png" alt="FinCLI" width="150" height="57"></div>
      <div class="auth-title">
        <h1 class="t">Criar conta</h1>
        <p class="sub">A Reserva de Emergência já vem criada.</p>
      </div>
      <app-erro-formulario [mensagem]="erro()" />
      <form class="form-card" ngNativeValidate [formGroup]="form" (ngSubmit)="cadastrar()">
        <div class="field">
          <label for="nome">Nome</label>
          <input id="nome" data-cy="cadastro-nome" formControlName="nome" autocomplete="name"
            maxlength="120" required>
        </div>
        <div class="field">
          <label for="cpf">CPF</label>
          <input id="cpf" data-cy="cadastro-cpf" formControlName="cpf" inputmode="numeric"
            autocomplete="username" placeholder="000.000.000-00" required>
        </div>
        <div class="field">
          <label for="senha">Senha (mínimo 8 caracteres)</label>
          <input id="senha" data-cy="cadastro-senha" type="password" formControlName="senha"
            autocomplete="new-password" placeholder="••••••••" minlength="8" maxlength="100" required>
        </div>
        <button class="btn" type="submit" data-cy="cadastro-enviar">Cadastrar e entrar</button>
      </form>
      <p class="auth-alt">
        Atenção: ainda não existe recuperação de senha — guarde-a com cuidado.
      </p>
      <p class="auth-alt">Já tem conta? <a routerLink="/login" data-cy="cadastro-ir-login">Entrar</a></p>
    </main>
  </div>`,
})
export class CadastroPage {
  private readonly auth = inject(AuthService);
  private readonly sessao = inject(SessaoService);
  private readonly router = inject(Router);

  protected readonly form = new FormGroup({
    nome: new FormControl('', { nonNullable: true }),
    cpf: new FormControl('', { nonNullable: true }),
    senha: new FormControl('', { nonNullable: true }),
  });
  protected readonly erro = signal('');

  protected async cadastrar(): Promise<void> {
    this.erro.set('');
    const dados = this.form.getRawValue();
    try {
      await this.auth.cadastrar(dados);
      this.sessao.entrar(await this.auth.login(dados.cpf, dados.senha));
      await this.router.navigateByUrl('/gestao', { replaceUrl: true });
    } catch (e) {
      this.erro.set(mensagemDeErro(e));
    }
  }
}
