import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ExtratoPage } from './extrato.page';

describe('tela Extrato', () => {
  let fixture: ComponentFixture<ExtratoPage>;
  let http: HttpTestingController;
  let el: HTMLElement;

  async function tick() {
    await new Promise((r) => setTimeout(r, 0));
    fixture.detectChanges();
  }

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [ExtratoPage],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ExtratoPage);
    el = fixture.nativeElement;
    fixture.autoDetectChanges();
    await tick();
  });

  it('lista plana: cabeçalho do dia seguido dos itens, título e rótulo separados', async () => {
    http.expectOne('/api/extrato').flush([
      {
        origem: 'TRANSACAO',
        data: '2026-09-16',
        valor: 120,
        tipo: 'SAIDA',
        categoria: 'Alimentação',
        descricao: 'Mercado',
      },
    ]);
    await tick();

    const feed = el.querySelector('.feed')!;
    expect([...feed.children].map((c) => c.className)).toEqual(['feed-day', 'item']);
    expect(el.querySelector('.item .body')!.textContent).toBe('Mercado Saída · Alimentação');
  });

  it('CA-EXT-08: sem lançamentos', async () => {
    http.expectOne('/api/extrato').flush([]);
    await tick();
    expect(el.querySelector('.vazio')!.textContent).toBe('Nada por aqui ainda.');
  });
});
