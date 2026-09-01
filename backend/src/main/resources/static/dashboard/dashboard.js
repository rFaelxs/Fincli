/* ═══ Dashboard ═══ */
'use strict';

const $ = s => document.querySelector(s);

function cardReserva(r) {
  const largura = Math.min(100, Number(r.progresso));
  return `<div class="card" style="box-shadow:none">
    <div class="res-head">
      <span class="res-nome">${App.esc(r.nome)}</span>
      ${r.emergencia ? '<span class="chip neutral">protegida</span>' : ''}
    </div>
    <div class="meter"><i style="width:${largura}%"></i></div>
    <div class="meter-legend">
      <span><b class="num">${App.moeda(r.saldoAtual)}</b></span>
      <span class="num">/ ${App.moeda(r.metaValor)} · ${App.pct(r.progresso)}</span>
    </div>
  </div>`;
}

async function carregar() {
  const mes = $('#mes').value;
  const d = await App.api('/dashboard?mes=' + mes);

  $('#asof').textContent = d.mes + ' · atualizado ' + new Date().toLocaleTimeString('pt-BR');
  $('#saldo').textContent = App.moeda(d.saldoDisponivel);
  $('#entradas').textContent = '+' + App.moeda(d.entradasMes);
  $('#saidas').textContent = '−' + App.moeda(d.saidasMes);
  $('#selic').textContent = d.selicMetaAnual != null
    ? Number(d.selicMetaAnual).toFixed(2).replace('.', ',') + '%'
    : 'Indisponível';

  const emerg = d.reservas.find(r => r.emergencia);
  if (emerg) {
    $('#emerg-pct').textContent = App.pct(emerg.progresso);
    $('#emerg-bar').style.width = Math.min(100, Number(emerg.progresso)) + '%';
    $('#emerg-saldo').textContent = App.moeda(emerg.saldoAtual);
    $('#emerg-meta').textContent = App.moeda(emerg.metaValor);
    $('#emerg-acts').innerHTML = `
      <a class="btn" href="/reservas/movimentar.html?id=${emerg.id}&acao=alocar">Alocar</a>
      <a class="btn ghost" href="/reservas/movimentar.html?id=${emerg.id}&acao=sacar">Sacar</a>
      <a class="btn ghost" href="/reservas/update.html?id=${emerg.id}">Editar meta</a>`;
  }

  $('#reservas').innerHTML = d.reservas.map(cardReserva).join('');

  // Essencial × supérfluo, calculado das transações do mês.
  const txs = await App.api('/transacoes');
  const saidasMes = txs.filter(t => t.tipo === 'SAIDA' && t.data.startsWith(mes));
  const corpo = $('#split-corpo');
  if (!saidasMes.length) {
    corpo.innerHTML = '<div class="vazio">Sem saídas no mês.</div>';
    return;
  }
  const ess = saidasMes.filter(t => t.essencial).reduce((s, t) => s + Number(t.valor), 0);
  const sup = saidasMes.filter(t => !t.essencial).reduce((s, t) => s + Number(t.valor), 0);
  const tot = ess + sup;
  corpo.innerHTML = `
    <div class="split" role="img" aria-label="Essenciais ${App.moeda(ess)}. Supérfluas ${App.moeda(sup)}.">
      <i class="a" style="width:${(ess / tot * 100).toFixed(1)}%"></i>
      <i class="b" style="width:${(sup / tot * 100).toFixed(1)}%"></i>
    </div>
    <div class="split-legend">
      <span><i class="swatch a"></i>Essenciais <b class="num">${App.moeda(ess)}</b></span>
      <span><i class="swatch b"></i>Supérfluas <b class="num">${App.moeda(sup)}</b></span>
    </div>`;
}

(async () => {
  const eu = await App.guardar();
  App.montarShell(eu, 'dashboard');
  App.preencherMeses($('#mes'));
  $('#mes').addEventListener('change', () => carregar().catch(e => App.toast(e.message)));
  await carregar();
})().catch(e => App.toast(e.message));
