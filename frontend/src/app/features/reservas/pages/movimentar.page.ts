import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Reserva } from '../../../core/api/modelos';
import { mensagemDeErro } from '../../../core/api/problema';
import { ErroFormularioComponent } from '../../../shared/ui/erro-formulario.component';
import { ResumoReservaComponent } from '../components/resumo-reserva.component';
import { ReservasService } from '../data/reservas.service';
import { SaldoService } from '../data/saldo.service';

export type Operacao = 'alocar' | 'sacar';

/** Os textos de cada operação (spec 07, itens 22 e 23). */
const TEXTOS: Record<Operacao, { titulo: string; subtitulo: string; botao: string }> = {
  alocar: {
    titulo: 'Alocar na reserva',
    subtitulo: 'Move dinheiro do saldo disponível para a reserva.',
    botao: 'Alocar',
  },
  sacar: {
    titulo: 'Sacar da reserva',
    subtitulo: 'Devolve dinheiro da reserva ao saldo disponível.',
    botao: 'Sacar',
  },
};

type Carga =
  | { tipo: 'carregando' }
  | { tipo: 'falhou'; mensagem: string }
  | { tipo: 'pronta'; reserva: Reserva };

/**
 * `/reservas/:id/alocar` e `/reservas/:id/sacar`: uma tela só, decidida pela rota (spec 07, itens
 * 22 a 28). Só a de alocar lê o saldo disponível.
 */
@Component({
  selector: 'app-movimentar',
  imports: [ReactiveFormsModule, ErroFormularioComponent, ResumoReservaComponent],
  template: `<div class="pagina">
    <header class="head">
      <div>
        <h1>{{ textos().titulo }}</h1>
        <p class="sub" data-cy="reserva-subtitulo">{{ subtitulo() }}</p>
      </div>
    </header>
    @if (carga(); as c) {
      @if (c.tipo === 'pronta') {
        <form class="card form-card" ngNativeValidate [formGroup]="form" (ngSubmit)="enviar()">
          <app-erro-formulario [mensagem]="erro()" />
          <app-resumo-reserva [nome]="c.reserva.nome" [guardado]="c.reserva.saldoAtual"
            [disponivel]="disponivel()" />
          <div class="field">
            <label for="valor">Valor (R$)</label>
            <input id="valor" data-cy="reserva-movimento-valor" type="number"
              formControlName="valor" step="0.01" min="0.01" inputmode="decimal" required>
          </div>
          <div class="form-acts">
            <button class="btn" type="submit" data-cy="reserva-movimento-enviar">{{
              textos().botao }}</button>
          </div>
        </form>
      }
    }
  </div>`,
})
export class MovimentarPage implements OnInit {
  private readonly reservas = inject(ReservasService);
  private readonly saldo = inject(SaldoService);
  private readonly router = inject(Router);

  /** Vem do parâmetro da rota. */
  readonly id = input<string>();
  /** Vem do `data` da rota. */
  readonly operacao = input.required<Operacao>();

  protected readonly textos = computed(() => TEXTOS[this.operacao()]);
  protected readonly carga = signal<Carga>({ tipo: 'carregando' });
  /** Indefinido na tela de sacar, que não mostra a linha; nulo se a leitura falhou. */
  protected readonly disponivel = signal<number | null | undefined>(undefined);
  protected readonly erro = signal('');
  protected readonly form = new FormGroup({ valor: new FormControl<number | null>(null) });

  protected readonly subtitulo = computed(() => {
    const c = this.carga();
    if (c.tipo === 'falhou') {
      return c.mensagem;
    }
    return this.textos().subtitulo;
  });

  async ngOnInit(): Promise<void> {
    const id = this.id();
    if (!id) {
      await this.router.navigateByUrl('/reservas', { replaceUrl: true });
      return;
    }
    if (this.operacao() === 'alocar') {
      void this.lerSaldo();
    }
    try {
      this.carga.set({ tipo: 'pronta', reserva: await this.reservas.buscar(id) });
    } catch (e) {
      this.carga.set({ tipo: 'falhou', mensagem: mensagemDeErro(e) });
    }
  }

  /** Sucesso troca a rota; erro fica no topo e a tela não sai do lugar (itens 27 e CA-RES-18). */
  protected async enviar(): Promise<void> {
    this.erro.set('');
    const valor = Number(this.form.getRawValue().valor);
    try {
      if (this.operacao() === 'alocar') {
        await this.reservas.alocar(this.id()!, valor);
      } else {
        await this.reservas.sacar(this.id()!, valor);
      }
      await this.router.navigateByUrl('/reservas', { replaceUrl: true });
    } catch (e) {
      this.erro.set(mensagemDeErro(e));
    }
  }

  private async lerSaldo(): Promise<void> {
    try {
      this.disponivel.set(await this.saldo.disponivel());
    } catch {
      this.disponivel.set(null);
    }
  }
}
