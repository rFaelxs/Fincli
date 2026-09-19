import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Reserva } from '../../../core/api/modelos';
import { lerErro } from '../../../core/api/problema';
import { ErroFormularioComponent } from '../../../shared/ui/erro-formulario.component';
import { ResumoReservaComponent } from '../components/resumo-reserva.component';
import { Movimento, ReservasService } from '../data/reservas.service';
import { SaldoService } from '../data/saldo.service';

const TEXTOS = {
  alocar: {
    titulo: 'Alocar na reserva',
    sub: 'Move dinheiro do saldo disponível para a reserva.',
    ok: 'Alocar',
  },
  sacar: {
    titulo: 'Sacar da reserva',
    sub: 'Devolve dinheiro da reserva ao saldo disponível.',
    ok: 'Sacar',
  },
} as const;

/** Alocar e sacar: a mesma tela, parametrizada pela rota. */
@Component({
  selector: 'app-movimentar-page',
  imports: [ReactiveFormsModule, RouterLink, ErroFormularioComponent, ResumoReservaComponent],
  host: { class: 'pagina' },
  template: `
    <div class="head">
      <div>
        <h1>{{ textos().titulo }}</h1>
        <span class="sub">{{ subtitulo() }}</span>
      </div>
    </div>

    @if (reserva(); as r) {
      <form class="card form-card" [formGroup]="form" (ngSubmit)="confirmar()" ngNativeValidate>
        <app-erro-formulario [mensagem]="erro()" />
        <app-resumo-reserva [nome]="r.nome" [saldo]="r.saldoAtual" [disponivel]="disponivel()" />
        <div class="field">
          <label for="valor">Valor (R$)</label>
          <input
            id="valor"
            class="num"
            type="number"
            step="0.01"
            min="0.01"
            placeholder="0,00"
            required
            formControlName="valor"
          />
        </div>
        <div class="form-acts">
          <a class="btn ghost" routerLink="/reservas">Cancelar</a>
          <button class="btn" type="submit">{{ textos().ok }}</button>
        </div>
      </form>
    }
  `,
})
export class MovimentarPage implements OnInit {
  private readonly servico = inject(ReservasService);
  private readonly saldo = inject(SaldoService);
  private readonly router = inject(Router);

  /** Da rota `/reservas/:id/alocar` ou `/reservas/:id/sacar`. */
  readonly id = input.required<string>();
  /** Do `data` da rota. */
  readonly acao = input.required<Movimento>();

  protected readonly textos = computed(() => TEXTOS[this.acao()]);
  protected readonly reserva = signal<Reserva | null>(null);
  protected readonly disponivel = signal<{ saldo: number } | null>(null);
  protected readonly subtitulo = signal('Carregando…');
  protected readonly erro = signal('');
  protected readonly form = inject(FormBuilder).group({ valor: [null as number | null] });

  async ngOnInit(): Promise<void> {
    try {
      const r = await this.servico.buscar(this.id());
      // O teto de quem aloca é o saldo disponível — mostrado para orientar antes do erro.
      if (this.acao() === 'alocar') {
        this.disponivel.set({ saldo: await this.saldo.disponivel() });
      }
      this.reserva.set(r);
      this.subtitulo.set(this.textos().sub);
    } catch (e) {
      this.subtitulo.set(lerErro(e));
    }
  }

  protected async confirmar(): Promise<void> {
    this.erro.set('');
    try {
      await this.servico.movimentar(this.id(), this.acao(), Number(this.form.getRawValue().valor));
      await this.router.navigateByUrl('/reservas', { replaceUrl: true });
    } catch (e) {
      this.erro.set(lerErro(e));
    }
  }
}
