import { Component, ElementRef, inject, viewChild } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
import { BarraLateralComponent } from './components/barra-lateral.component';

/** Atalhos de teclado do shell (spec 01, item 13). */
const ATALHOS: Record<string, string> = { g: '/gestao', r: '/relatorio' };

/** Campo em que a letra digitada é texto, e não atalho. */
function ehCampoDeTexto(alvo: EventTarget | null): boolean {
  return (
    alvo instanceof HTMLElement &&
    (alvo.isContentEditable || ['INPUT', 'TEXTAREA', 'SELECT'].includes(alvo.tagName))
  );
}

/** A moldura de toda rota autenticada: barra lateral, conteúdo, link de pulo e atalhos. */
@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, BarraLateralComponent],
  template: `<a class="pular" href="#conteudo" (click)="pular($event)" data-cy="shell-pular"
      >Pular para o conteúdo</a>
    <div class="shell">
      <app-barra-lateral />
      <main id="conteudo" tabindex="-1" #conteudo><router-outlet /></main>
    </div>`,
  host: { '(document:keydown)': 'atalho($event)' },
})
export class ShellComponent {
  private readonly router = inject(Router);
  private readonly conteudo = viewChild.required<ElementRef<HTMLElement>>('conteudo');

  /**
   * Leva o foco ao conteúdo. O `href` fica para a semântica de link, mas a navegação por âncora
   * resolveria contra o `<base href="/">` e sairia da rota atual.
   */
  protected pular(evento: Event): void {
    evento.preventDefault();
    this.conteudo().nativeElement.focus();
  }

  /** G vai para a Gestão e R para o Relatório, exceto com modificador, em campo de texto ou já lá. */
  protected atalho(evento: KeyboardEvent): void {
    if (evento.ctrlKey || evento.metaKey || evento.altKey || ehCampoDeTexto(evento.target)) {
      return;
    }
    const destino = ATALHOS[evento.key.toLowerCase()];
    if (!destino || this.router.url.startsWith(destino)) {
      return;
    }
    evento.preventDefault();
    void this.router.navigateByUrl(destino);
  }
}
