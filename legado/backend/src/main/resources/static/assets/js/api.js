/* ═══ FinCLI · núcleo compartilhado: API, formatação e guarda de sessão ═══ */
'use strict';

window.App = (function () {

  /* ── Formatação ── */

  const fmtBRL = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
  const fmtInt = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 0 });

  const moeda = v => fmtBRL.format(v);

  /** Valor sem sinal: quem mostra o `+`/`−` é a tela, junto com o rótulo textual. */
  const moedaAbs = v => fmtBRL.format(Math.abs(Number(v)));

  const inteiro = v => fmtInt.format(v);
  const pct = v => Number(v).toFixed(1).replace('.', ',') + '%';

  /** Forma compacta para rótulo de barra: 3210 → "3,2k". */
  function moedaCurta(v) {
    const n = Math.abs(Number(v));
    if (n >= 1000) return (n / 1000).toFixed(1).replace('.', ',') + 'k';
    return fmtInt.format(n);
  }

  const esc = t => String(t ?? '').replace(/[&<>"']/g,
      c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  const qs = nome => new URLSearchParams(location.search).get(nome);

  /* ── Datas ──
     As datas da API vêm em ISO puro (yyyy-MM-dd). `new Date('2026-09-16')` é interpretada como
     UTC e vira 15/09 em fuso negativo, então as partes são lidas da string. */

  const MESES = ['janeiro', 'fevereiro', 'março', 'abril', 'maio', 'junho',
    'julho', 'agosto', 'setembro', 'outubro', 'novembro', 'dezembro'];
  const MESES_CURTOS = ['jan', 'fev', 'mar', 'abr', 'mai', 'jun',
    'jul', 'ago', 'set', 'out', 'nov', 'dez'];

  /** '2026-09-16' → '16/09' */
  const diaMes = iso => iso.slice(8, 10) + '/' + iso.slice(5, 7);

  /** '2026-09-16' → '16 de setembro' */
  const diaPorExtenso = iso => Number(iso.slice(8, 10)) + ' de ' + MESES[Number(iso.slice(5, 7)) - 1];

  /** '2026-09' → 'setembro de 2026' */
  const mesPorExtenso = iso => MESES[Number(iso.slice(5, 7)) - 1] + ' de ' + iso.slice(0, 4);

  /** '2026-09' → 'setembro' — sem o ano, para uso no meio de uma frase */
  const soMes = iso => MESES[Number(iso.slice(5, 7)) - 1];

  /** '2026-09' → 'set' */
  const mesCurto = iso => MESES_CURTOS[Number(iso.slice(5, 7)) - 1];

  /** '2026-09' → 'set/26' — usado onde o ano importa (previsão de meta) */
  const mesAno = iso => MESES_CURTOS[Number(iso.slice(5, 7)) - 1] + '/' + iso.slice(2, 4);

  /* ── Toast ── */

  function toast(msg) {
    let el = document.getElementById('toast');
    if (!el) {
      el = document.createElement('div');
      el.id = 'toast';
      el.setAttribute('role', 'status');
      document.body.appendChild(el);
    }
    el.textContent = msg;
    el.classList.add('on');
    clearTimeout(el._t);
    el._t = setTimeout(() => el.classList.remove('on'), 3200);
  }

  /* ── API ── */

  function cookie(nome) {
    const m = document.cookie.match(new RegExp('(?:^|; )' + nome + '=([^;]*)'));
    return m ? decodeURIComponent(m[1]) : '';
  }

  async function api(caminho, opts = {}) {
    const res = await fetch('/api' + caminho, {
      method: opts.method || 'GET',
      headers: {
        ...(opts.body ? { 'Content-Type': 'application/json' } : {}),
        'X-XSRF-TOKEN': cookie('XSRF-TOKEN')
      },
      body: opts.body ? JSON.stringify(opts.body) : undefined
    });
    if (res.status === 401 && !opts.silencioso) {
      location.replace('/login/');
      throw new Error('Sessão expirada.');
    }
    if (!res.ok) {
      let msg = 'Erro ' + res.status;
      try {
        const p = await res.json();
        msg = p.detail || msg;
        if (p.campos) msg += ' ' + Object.values(p.campos).join(' ');
      } catch (e) { /* corpo não-JSON */ }
      const erro = new Error(msg);
      erro.status = res.status;
      throw erro;
    }
    if (res.status === 204) return null;
    return res.json().catch(() => null);
  }

  /* ── Sessão ── */

  /**
   * Guarda de página autenticada: emite o cookie CSRF e valida a sessão.
   * Sem sessão, redireciona para o login. Devolve o usuário logado.
   */
  async function guardar() {
    await fetch('/api/csrf');
    try {
      return await api('/me', { silencioso: true });
    } catch (e) {
      location.replace('/login/');
      throw e;
    }
  }

  /** Páginas públicas (login/cadastro): quem já tem sessão vai direto para Hoje. */
  async function initPublico() {
    await fetch('/api/csrf');
    try {
      await api('/me', { silencioso: true });
      location.replace('/hoje/');
    } catch (e) { /* sem sessão — permanece na página pública */ }
  }

  /* ── Formulários ── */

  function erroForm(id, msg) {
    const el = document.querySelector(id);
    el.textContent = msg;
    el.classList.add('on');
  }
  function limparErro(id) { document.querySelector(id).classList.remove('on'); }

  /* ── Meses (filtros) ── */

  function ultimosMeses(n) {
    const agora = new Date(), lista = [];
    for (let i = 0; i < n; i++) {
      const d = new Date(agora.getFullYear(), agora.getMonth() - i, 1);
      const valor = d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0');
      lista.push({ valor, rotulo: mesPorExtenso(valor) });
    }
    return lista;
  }

  function preencherMeses(sel, n = 6) {
    sel.innerHTML = ultimosMeses(n)
      .map(m => `<option value="${m.valor}">${esc(m.rotulo)}</option>`).join('');
  }

  return {
    api, guardar, initPublico, toast, esc, qs,
    moeda, moedaAbs, moedaCurta, inteiro, pct,
    diaMes, diaPorExtenso, mesPorExtenso, soMes, mesCurto, mesAno,
    erroForm, limparErro, preencherMeses, ultimosMeses
  };
})();
