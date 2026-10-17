/* ═══ FinCLI · parser da barra de comando (lado cliente) ═══
 *
 * Espelho de ComandoParser.java. Existe só para o preview instantâneo enquanto o usuário
 * digita: quem grava é o servidor, que reinterpreta o texto cru. Divergência entre os dois
 * causa um preview que promete o que não acontece — ao mexer aqui, mexa lá também, e nos
 * testes de ComandoParserTest.
 */
'use strict';

App.Comando = (function () {

  // [palavras-chave sem acento, categoria, essencial por padrão]
  const CATEGORIAS = [
    [/mercado|supermercado|feira|padaria|restaurante|ifood|lanche|almoco|jantar/, 'Alimentação', true],
    [/aluguel|condominio|luz|energia|agua|internet|fibra|gas/, 'Moradia', true],
    [/farmacia|academia|medico|dentista|remedio/, 'Saúde', true],
    [/uber|gasolina|onibus|metro|combustivel/, 'Transporte', true],
    [/show|cinema|streaming|netflix|spotify|bar|viagem|jogo/, 'Lazer', false],
    [/salario|freela|freelance|pix|bonus|venda/, 'Trabalho', false]
  ];

  const RX_PREVISTA = /^(prever|conta)\b/;
  const RX_APORTE = /^(guardar|poupar|alocar)\b/;
  const RX_ENTRADA = /^(entrada|recebi|\+)/;
  const RX_RENDA = /salario|freela|pix|bonus/;
  const RX_UMA_VEZ = /\buma vez\b|\bso (esse|este) mes\b/;
  const RX_VALOR = /\d[\d.]*(?:,\d+)?/;
  const RX_DIA = /^(\d{1,2})$/;

  const CONTROLE = new Set([
    'ontem', 'hoje', 'anteontem', 'essencial',
    'guardar', 'poupar', 'alocar', 'entrada', 'recebi', 'prever']);

  const CONTROLE_PREVISTA = new Set([
    'conta', 'dia', 'todo', 'todos', 'mes', 'meses', 'mensal', 'recorrente', 'uma', 'vez',
    'so', 'esse', 'este']);

  const LIGACAO = new Set(['de', 'do', 'da', 'no', 'na', 'em']);

  /** Tira acentos para que a tabela de palavras não precise repetir cada variante. */
  const semAcento = t => t.normalize('NFD').replace(/\p{M}+/gu, '');

  /**
   * Interpreta o texto digitado.
   *
   * @param texto o que está na barra
   * @param hoje  Date de referência
   * @returns {null|{tipo,valor,descricao,categoria,data,essencial,diaVencimento,recorrente}}
   *          null quando não há número — em dúvida, sem preview
   */
  function parse(texto, hoje = new Date()) {
    if (!texto || !texto.trim()) return null;

    const bruto = texto.trim();
    const normalizado = semAcento(bruto).toLowerCase();
    const tipo = tipoDe(normalizado);

    const { valor, dia, palavras } = analisar(bruto.split(/\s+/), tipo);
    if (valor === null) return null;

    const data = new Date(hoje);
    if (normalizado.includes('anteontem')) data.setDate(data.getDate() - 2);
    else if (normalizado.includes('ontem')) data.setDate(data.getDate() - 1);

    const descricao = descricaoDe(palavras, tipo);

    if (tipo === 'PREVISTA') {
      return {
        tipo, valor, descricao, categoria: 'Conta', data, essencial: false,
        diaVencimento: dia !== null ? dia : hoje.getDate(),
        recorrente: !RX_UMA_VEZ.test(normalizado)
      };
    }
    return {
      tipo, valor, descricao,
      categoria: categoriaDe(normalizado, tipo),
      data,
      essencial: essencialDe(normalizado, tipo),
      diaVencimento: null,
      recorrente: false
    };
  }

  function tipoDe(n) {
    if (RX_PREVISTA.test(n)) return 'PREVISTA';
    if (RX_APORTE.test(n)) return 'APORTE';
    if (RX_ENTRADA.test(n) || RX_RENDA.test(n)) return 'ENTRADA';
    return 'SAIDA';
  }

  /**
   * Uma passada pelos tokens separando valor, dia de vencimento e palavras da descrição.
   * O dia é lido antes do valor: em "prever energia 210 dia 22" o 22 é vencimento, não valor.
   */
  function analisar(tokens, tipo) {
    let valor = null, dia = null, esperandoDia = false;
    const palavras = [];

    for (const token of tokens) {
      const n = semAcento(token).toLowerCase();

      if (esperandoDia) {
        esperandoDia = false;
        const m = n.match(RX_DIA);
        if (m) { dia = parseInt(m[1], 10); continue; }
      }
      if (tipo === 'PREVISTA' && n === 'dia') { esperandoDia = true; continue; }

      const m = valor === null ? n.match(RX_VALOR) : null;
      if (m) {
        valor = paraNumero(m[0]);
        const resto = n.slice(0, m.index) + n.slice(m.index + m[0].length);
        if (/[a-z]/.test(resto)) {
          palavras.push(token.slice(0, m.index) + token.slice(m.index + m[0].length));
        }
        continue;
      }

      if (CONTROLE.has(n) || (tipo === 'PREVISTA' && CONTROLE_PREVISTA.has(n))) continue;
      palavras.push(token);
    }

    return { valor, dia, palavras };
  }

  /**
   * Com vírgula, o ponto é separador de milhar: 1.234,56 vale 1234,56. Sem vírgula, um ponto
   * com uma ou duas casas é decimal; qualquer outro é milhar.
   */
  function paraNumero(bruto) {
    let limpo;
    if (bruto.includes(',')) limpo = bruto.replace(/\./g, '').replace(',', '.');
    else if (/^\d+\.\d{1,2}$/.test(bruto)) limpo = bruto;
    else limpo = bruto.replace(/\./g, '');

    const n = parseFloat(limpo);
    return Number.isFinite(n) && n > 0 ? n : null;
  }

  function descricaoDe(palavras, tipo) {
    const restantes = palavras.slice();
    while (restantes.length && LIGACAO.has(semAcento(restantes[0]).toLowerCase())) {
      restantes.shift();
    }
    const d = restantes.join(' ').trim();
    if (!d) return tipo === 'APORTE' ? 'Aporte' : (tipo === 'PREVISTA' ? 'Conta' : 'Lançamento');
    return d.charAt(0).toUpperCase() + d.slice(1);
  }

  function categoriaDe(n, tipo) {
    if (tipo === 'APORTE') return 'Aporte';
    const achada = CATEGORIAS.find(([rx]) => rx.test(n));
    if (achada) return achada[1];
    return tipo === 'ENTRADA' ? 'Trabalho' : 'Outros';
  }

  function essencialDe(n, tipo) {
    if (tipo !== 'SAIDA') return false;
    if (n.includes('essencial')) return true;
    const achada = CATEGORIAS.find(([rx]) => rx.test(n));
    return achada ? achada[2] : false;
  }

  return { parse };
})();
