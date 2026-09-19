import { Pipe, PipeTransform } from '@angular/core';
import { diaMes, moeda, moedaAbs, pct } from './formatos';

@Pipe({ name: 'moeda' })
export class MoedaPipe implements PipeTransform {
  transform(valor: number): string {
    return moeda(valor);
  }
}

/** Valor sem sinal; o sinal vem da tela, junto do rótulo. */
@Pipe({ name: 'moedaAbs' })
export class MoedaAbsPipe implements PipeTransform {
  transform(valor: number): string {
    return moedaAbs(valor);
  }
}

@Pipe({ name: 'pct' })
export class PctPipe implements PipeTransform {
  transform(valor: number): string {
    return pct(valor);
  }
}

@Pipe({ name: 'diaMes' })
export class DiaMesPipe implements PipeTransform {
  transform(iso: string): string {
    return diaMes(iso);
  }
}
