import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';
import { ToastService } from './shared/ui/toast.component';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([])],
    }).compileComponents();
  });

  it('cria a aplicação', () => {
    const fixture = TestBed.createComponent(App);
    expect(fixture.componentInstance).toBeTruthy();
  });

  // Item 15 da spec 01
  it('o toast é uma região de status que some sozinha', async () => {
    vi.useFakeTimers();
    const fixture = TestBed.createComponent(App);
    const toast = () => (fixture.nativeElement as HTMLElement).querySelector('.toast')!;

    TestBed.inject(ToastService).mostrar('Transação excluída.');
    fixture.detectChanges();
    expect(toast().getAttribute('role')).toBe('status');
    expect(toast().classList.contains('on')).toBe(true);
    expect(toast().textContent?.trim()).toBe('Transação excluída.');

    vi.advanceTimersByTime(3200);
    fixture.detectChanges();
    expect(toast().classList.contains('on')).toBe(false);
    vi.useRealTimers();
  });
});
