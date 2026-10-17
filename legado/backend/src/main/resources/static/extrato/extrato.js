/* ═══ Extrato ═══ */
'use strict';

async function carregar() {
  const itens = await App.api('/extrato');
  const feed = document.getElementById('feed');

  if (!itens.length) {
    feed.innerHTML = '<div class="vazio">Nada por aqui ainda.</div>';
    return;
  }

  let diaAtual = '', html = '';
  for (const it of itens) {
    if (it.data !== diaAtual) {
      diaAtual = it.data;
      const d = new Date(it.data + 'T00:00:00');
      html += `<div class="feed-day">${d.toLocaleDateString('pt-BR',
          { day: '2-digit', month: 'short', year: 'numeric' })}</div>`;
    }
    const transacao = it.origem === 'TRANSACAO';
    const entrada = it.tipo === 'ENTRADA';
    const classe = transacao ? (entrada ? 'in' : 'out') : 'mov';
    const glifo = transacao ? (entrada ? '↑' : '↓') : '◎';
    const rotulo = { ENTRADA: 'Entrada', SAIDA: 'Saída', ALOCACAO: 'Alocação', SAQUE: 'Saque' }[it.tipo] || it.tipo;
    const valor = transacao
      ? `<span class="amt num ${entrada ? 'money-in' : 'money-out'}">${entrada ? '+' : '−'}${App.moeda(it.valor)}</span>`
      : `<span class="amt num">${App.moeda(it.valor)}</span>`;

    html += `<div class="item">
      <span class="badge ${classe}" aria-hidden="true">${glifo}</span>
      <span class="body">
        <span class="t">${App.esc(it.descricao) || App.esc(it.categoria)}</span>
        <span class="s">${rotulo} · ${App.esc(it.categoria)}</span>
      </span>
      ${valor}
    </div>`;
  }
  feed.innerHTML = html;
}

(async () => {
  const eu = await App.guardar();
  App.montarShell(eu, 'extrato');
  await carregar();
})().catch(e => App.toast(e.message));
