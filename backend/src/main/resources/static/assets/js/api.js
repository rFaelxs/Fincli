/* ═══ FinCLI · núcleo compartilhado: API, formatação, tema, guarda de sessão ═══ */
'use strict';

window.App = (function () {

  /* ── Formatação ── */

  const fmtBRL = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
  const moeda = v => fmtBRL.format(v);
  const pct = v => Number(v).toFixed(1).replace('.', ',') + '%';
  const esc = t => String(t ?? '').replace(/[&<>"']/g,
      c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  const qs = nome => new URLSearchParams(location.search).get(nome);

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

  /** Páginas públicas (login/cadastro): quem já tem sessão vai direto ao dashboard. */
  async function initPublico() {
    await fetch('/api/csrf');
    try {
      await api('/me', { silencioso: true });
      location.replace('/dashboard/');
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
      lista.push({
        valor: d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0'),
        rotulo: d.toLocaleDateString('pt-BR', { month: 'long', year: 'numeric' })
      });
    }
    return lista;
  }

  function preencherMeses(sel, n = 6) {
    sel.innerHTML = ultimosMeses(n)
      .map(m => `<option value="${m.valor}">${esc(m.rotulo)}</option>`).join('');
  }

  /* ── Tema ── */

  function aplicarTema(modo) {
    const root = document.documentElement;
    if (modo === 'light' || modo === 'dark') root.setAttribute('data-theme', modo);
    else { modo = 'auto'; root.removeAttribute('data-theme'); }
    document.querySelectorAll('[data-theme-set]').forEach(b =>
      b.setAttribute('aria-pressed', String(b.dataset.themeSet === modo)));
    try { localStorage.setItem('fincli-theme', modo); } catch (e) { /* sem storage */ }
  }

  (function initTema() {
    let salvo = null;
    try { salvo = localStorage.getItem('fincli-theme'); } catch (e) { /* sem storage */ }
    aplicarTema(salvo || 'auto');
  })();

  return { api, moeda, pct, esc, qs, toast, guardar, initPublico, erroForm, limparErro, preencherMeses, aplicarTema };
})();
