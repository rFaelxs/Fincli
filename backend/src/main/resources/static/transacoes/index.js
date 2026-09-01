/* ═══ Transações · listagem, busca e exclusão ═══ */
'use strict';

const $ = s => document.querySelector(s);

async function carregar() {
  const mes = $('#f-mes').value;
  const tipo = $('#f-tipo').value;
  const busca = $('#f-busca').value.trim().toLowerCase();

  let txs = await App.api('/transacoes');
  if (mes) txs = txs.filter(t => t.data.startsWith(mes));
  if (tipo) txs = txs.filter(t => t.tipo === tipo);
  if (busca) {
    txs = txs.filter(t =>
      (t.descricao || '').toLowerCase().includes(busca) ||
      t.categoria.toLowerCase().includes(busca));
  }

  $('#asof').textContent = txs.length + ' lançamento(s)';
  const corpo = $('#corpo');

  if (!txs.length) {
    corpo.innerHTML = '<tr><td colspan="6" class="vazio">Nenhuma transação encontrada.</td></tr>';
    $('#total').textContent = '—';
    return;
  }

  corpo.innerHTML = txs.map(t => {
    const entrada = t.tipo === 'ENTRADA';
    const [ano, mesTx, dia] = t.data.split('-');
    return `<tr>
      <td class="date">${dia}/${mesTx}</td>
      <td class="desc">${App.esc(t.descricao) || '<span class="muted">—</span>'}</td>
      <td class="muted">${App.esc(t.categoria)}</td>
      <td>
        <span class="chip ${entrada ? 'in' : 'out'}">${entrada ? '↑ entrada' : '↓ saída'}</span>
        ${t.essencial && !entrada ? '<span class="chip neutral">essencial</span>' : ''}
      </td>
      <td class="num ${entrada ? 'money-in' : 'money-out'}">${entrada ? '+' : '−'}${Number(t.valor).toLocaleString('pt-BR', { minimumFractionDigits: 2 })}</td>
      <td class="acts">
        <a class="btn danger" title="Editar" href="/transacoes/update.html?id=${t.id}">✎</a>
        <button class="btn danger" title="Excluir" onclick="excluir('${t.id}')">×</button>
      </td>
    </tr>`;
  }).join('');

  const total = txs.reduce((s, t) => s + (t.tipo === 'ENTRADA' ? 1 : -1) * Number(t.valor), 0);
  $('#total').textContent = (total >= 0 ? '+' : '−') +
    Math.abs(total).toLocaleString('pt-BR', { minimumFractionDigits: 2 });
}

window.excluir = async id => {
  if (!confirm('Excluir esta transação?')) return;
  try {
    await App.api('/transacoes/' + id, { method: 'DELETE' });
    App.toast('Transação excluída.');
    await carregar();
  } catch (e) { App.toast(e.message); }
};

(async () => {
  const eu = await App.guardar();
  App.montarShell(eu, 'transacoes');
  App.preencherMeses($('#f-mes'));

  const recarregar = () => carregar().catch(e => App.toast(e.message));
  $('#f-mes').addEventListener('change', recarregar);
  $('#f-tipo').addEventListener('change', recarregar);
  $('#f-busca').addEventListener('input', recarregar);

  await carregar();
})().catch(e => App.toast(e.message));
