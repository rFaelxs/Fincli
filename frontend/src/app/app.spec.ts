import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';

describe('App', () => {
  it('monta o outlet das rotas e o toast', async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([])],
    }).compileComponents();

    const fixture = TestBed.createComponent(App);
    const el: HTMLElement = fixture.nativeElement;

    expect(el.querySelector('router-outlet')).not.toBeNull();
    expect(el.querySelector('[role="status"]')).not.toBeNull();
  });
});
