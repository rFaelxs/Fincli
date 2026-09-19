import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs';
import { BarraLateralComponent } from './components/barra-lateral.component';
import { GuardadoService } from './data/guardado.service';

const ATALHOS: Record<string, string> = { g: '/gestao', r: '/relatorio' };

/**
 * Moldura das rotas autenticadas: barra lateral à esquerda, tela da rota à direita.
 *
 * O link de pulo para a barra de comando mora aqui, antes da barra lateral, para ser o primeiro
 * elemento focável da Gestão.
 */
@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, BarraLateralComponent],
  template: `
    @if (naGestao()) {
      <a class="pular" href="/gestao#comando" (click)="pularParaComando($event)">
        Pular para a barra de comando
      </a>
    }
    <div class="shell">
      <aside class="side"><app-barra-lateral /></aside>
      <main><router-outlet /></main>
    </div>
  `,
  host: { '(document:keydown)': 'atalho($event)' },
})
export class ShellComponent {
  private readonly router = inject(Router);

  private readonly url = toSignal(
    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd),
      map((e) => e.urlAfterRedirects),
    ),
    { initialValue: this.router.url },
  );

  protected readonly naGestao = computed(() => this.url().startsWith('/gestao'));

  constructor() {
    // Cada tela aberta recarrega o card "GUARDADO", como fazia cada página do front antigo.
    const guardado = inject(GuardadoService);
    this.router.events
      .pipe(filter((e) => e instanceof NavigationEnd))
      .subscribe(() => void guardado.recarregar());
  }

  /**
   * G para Gestão, R para Relatório.
   *
   * Ignorados enquanto o foco está num campo de texto — senão digitar "guardar" na barra de
   * comando navegaria para outra tela.
   */
  protected atalho(e: KeyboardEvent): void {
    if (e.ctrlKey || e.metaKey || e.altKey || digitando(e.target)) {
      return;
    }
    const destino = ATALHOS[e.key.toLowerCase()];
    if (destino && !this.router.url.startsWith(destino)) {
      void this.router.navigateByUrl(destino);
    }
  }

  protected pularParaComando(e: Event): void {
    e.preventDefault();
    document.getElementById('comando')?.focus();
  }
}

/** @returns true se o alvo do evento aceita texto */
export function digitando(alvo: EventTarget | null): boolean {
  if (!(alvo instanceof HTMLElement)) {
    return false;
  }
  const tag = alvo.tagName;
  return tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT' || alvo.isContentEditable;
}
