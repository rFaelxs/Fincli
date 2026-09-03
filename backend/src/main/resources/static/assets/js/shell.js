/* ═══ FinCLI · sidebar compartilhada das páginas autenticadas ═══ */
'use strict';

/**
 * Monta a sidebar no elemento #sidebar.
 *
 * @param eu    usuário da sessão ({ nome, cpf })
 * @param ativa página ativa: dashboard | transacoes | reservas | extrato
 */
App.montarShell = function (eu, ativa) {
  const itens = [
    { id: 'dashboard',  rotulo: 'Dashboard',  ico: '◧' },
    { id: 'transacoes', rotulo: 'Transações', ico: '≡' },
    { id: 'reservas',   rotulo: 'Reservas',   ico: '◎' },
    { id: 'extrato',    rotulo: 'Extrato',    ico: '⇅' }
  ];

  const iniciais = eu.nome.trim().split(/\s+/).slice(0, 2).map(p => p[0]).join('').toUpperCase();

  document.getElementById('sidebar').innerHTML = `
    <a class="wordmark" href="/dashboard/"><span class="prompt">❯</span>fincli</a>

    <nav aria-label="Seções">
      ${itens.map(i => `
        <a href="/${i.id}/" ${i.id === ativa ? 'aria-current="page"' : ''}>
          <span class="ico">${i.ico}</span>${i.rotulo}
        </a>`).join('')}
    </nav>

    <div class="side-foot">
      <div class="user">
        <div class="avatar">${App.esc(iniciais)}</div>
        <div style="min-width:0">
          <div class="who">${App.esc(eu.nome)}</div>
          <div class="cpf mono">${App.esc(eu.cpf)}</div>
        </div>
      </div>
      <div class="seg" role="group" aria-label="Tema">
        <button data-theme-set="auto">Auto</button>
        <button data-theme-set="light">Claro</button>
        <button data-theme-set="dark">Escuro</button>
      </div>
      <button class="btn ghost" id="btn-sair" style="width:100%">Sair</button>
    </div>`;

  document.querySelectorAll('[data-theme-set]').forEach(b =>
    b.addEventListener('click', () => App.aplicarTema(b.dataset.themeSet)));

  let temaAtual = 'auto';
  try { temaAtual = localStorage.getItem('fincli-theme') || 'auto'; } catch (e) { /* sem storage */ }
  App.aplicarTema(temaAtual);

  document.getElementById('btn-sair').addEventListener('click', async () => {
    try { await App.api('/logout', { method: 'POST' }); } catch (e) { /* sessão já encerrada */ }
    location.replace('/login/');
  });
};
