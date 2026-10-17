/* ═══ Reservas · listagem ═══ */
'use strict';

function cardReserva(r) {
  const largura = Math.min(100, Number(r.progresso));
  return `<div class="card">
    <div class="res-head">
      <span class="res-nome">${App.esc(r.nome)}</span>
      ${r.emergencia
        ? '<span class="chip neutral">protegida</span>'
        : `<button class="btn danger" title="Excluir" onclick="excluir('${r.id}')">×</button>`}
    </div>
    <div class="meter-row" style="margin-top:.5rem">
      <span class="meter-pct num">${App.pct(r.progresso)}</span>
      <span class="sub num">${App.moeda(r.saldoAtual)} / ${App.moeda(r.metaValor)}</span>
    </div>
    <div class="meter"><i style="width:${largura}%"></i></div>
    <div class="res-acts">
      <a class="btn" href="/reservas/movimentar.html?id=${r.id}&acao=alocar">Alocar</a>
      <a class="btn ghost" href="/reservas/movimentar.html?id=${r.id}&acao=sacar">Sacar</a>
      <a class="btn ghost" href="/reservas/update.html?id=${r.id}">Meta</a>
    </div>
  </div>`;
}

async function carregar() {
  const rs = await App.api('/reservas');
  const total = rs.reduce((s, r) => s + Number(r.saldoAtual), 0);
  document.getElementById('asof').textContent = App.moeda(total) + ' guardados no total';
  document.getElementById('grid').innerHTML = rs.map(cardReserva).join('');
}

window.excluir = async id => {
  if (!confirm('Excluir esta reserva?')) return;
  try {
    await App.api('/reservas/' + id, { method: 'DELETE' });
    App.toast('Reserva excluída.');
    await carregar();
  } catch (e) { App.toast(e.message); }
};

(async () => {
  const eu = await App.guardar();
  App.montarShell(eu, 'reservas');
  await carregar();
})().catch(e => App.toast(e.message));
