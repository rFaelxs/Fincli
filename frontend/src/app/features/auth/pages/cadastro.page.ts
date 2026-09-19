import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { lerErro } from '../../../core/api/problema';
import { SessaoService } from '../../../core/auth/sessao.service';
import { ErroFormularioComponent } from '../../../shared/ui/erro-formulario.component';
import { AuthService } from '../data/auth.service';

@Component({
  selector: 'app-cadastro-page',
  imports: [ReactiveFormsModule, RouterLink, ErroFormularioComponent],
  styleUrl: '../auth.css',
  template: `
    <div class="auth-plane">
      <form class="auth card" [formGroup]="form" (ngSubmit)="cadastrar()" ngNativeValidate>
        <div class="marca"><img src="/assets/img/fincli-logo.png" alt="FinCLI" /></div>
        <div class="auth-title">
          <div class="t">Criar conta</div>
          <div class="sub">A Reserva de Emergência já vem criada.</div>
        </div>
        <app-erro-formulario [mensagem]="erro()" />
        <div class="field">
          <label for="nome">Nome</label>
          <input
            id="nome"
            placeholder="Seu nome"
            autocomplete="name"
            required
            maxlength="120"
            formControlName="nome"
          />
        </div>
        <div class="field">
          <label for="cpf">CPF</label>
          <input
            id="cpf"
            class="mono"
            inputmode="numeric"
            placeholder="000.000.000-00"
            required
            formControlName="cpf"
          />
        </div>
        <div class="field">
          <label for="senha">Senha <span class="muted">(mínimo 8 caracteres)</span></label>
          <input
            id="senha"
            type="password"
            placeholder="••••••••"
            autocomplete="new-password"
            required
            minlength="8"
            maxlength="100"
            formControlName="senha"
          />
        </div>
        <button class="btn" type="submit">Cadastrar e entrar</button>
        <div class="note-open">
          <b>Atenção:</b> ainda não existe recuperação de senha — guarde-a com cuidado.
        </div>
        <div class="auth-alt">Já tem conta? <a routerLink="/login">Entrar</a></div>
      </form>
    </div>
  `,
})
export class CadastroPage {
  private readonly auth = inject(AuthService);
  private readonly sessao = inject(SessaoService);
  private readonly router = inject(Router);

  protected readonly form = inject(FormBuilder).nonNullable.group({
    nome: '',
    cpf: '',
    senha: '',
  });
  protected readonly erro = signal('');

  /** Cadastra e já entra com as mesmas credenciais. */
  protected async cadastrar(): Promise<void> {
    this.erro.set('');
    const { nome, cpf, senha } = this.form.getRawValue();
    try {
      await this.auth.cadastrar({ nome, cpf, senha });
      this.sessao.entrar(await this.auth.entrar({ cpf, senha }));
      await this.router.navigateByUrl('/gestao', { replaceUrl: true });
    } catch (e) {
      this.erro.set(lerErro(e));
    }
  }
}
