/* ═══ Tela Hoje ═══ */
'use strict';

const $ = s => document.querySelector(s);

const SUGESTOES = ['guardar 300 viagem', 'uber 32 hoje', 'freela 450'];

/** Último lançamento salvo, para o "desfazer" da faixa. */
let ultimo = null;

/* ── Herói ── */

function pintarHeroi(d) {
  const livre = Number(d.livre);
  const negativo = livre < 0;
  const vazio = Number(d.entradas) === 0 && Number(d.saidas) === 0 && Number(d.aportes) === 0;

  $('#heroi-rotulo').textContent = negativo
    ? 'NO VERMELHO · ATÉ ' + App.diaPorExtenso(d.fimDoMes).toUpperCase()
    : 'DÁ PRA GASTAR · ATÉ ' + App.diaPorExtenso(d.fimDoMes).toUpperCase();

  const valor = $('#livre');
  valor.textContent = App.moedaAbs(livre);
  valor.classList.toggle('negativo', negativo);
  valor.classList.remove('esqueleto');

  const pilula = $('#ritmo');
  pilula.classList.toggle('negativo', negativo);
  pilula.textContent = negativo
    ? App.moedaAbs(livre) + ' no vermelho'
    : App.moeda(d.porDia) + ' por dia · ' + d.diasRestantes
      + (d.diasRestantes === 1 ? ' dia' : ' dias');

  const mes = App.mesPorExtenso(d.mes);
  if (vazio) {
    $('#heroi-rotulo').textContent = 'SEM LANÇAMENTOS AINDA';
    valor.textContent = App.moeda(0);
    pilula.textContent = mes + ' está em branco';
    $('#heroi-texto').textContent =
      'Lance o primeiro gasto na barra abaixo — é só escrever o que aconteceu.';
    $('.comando-secao').classList.add('destaque');
    return;
  }

  $('.comando-secao').classList.remove('destaque');
  $('#heroi-texto').textContent = negativo
    ? `As contas que ainda vencem e o que já saiu passam do que entrou em ${mes}. `
      + 'Cortar aqui é escolher o que não vai acontecer.'
    : 'Já descontei as contas que ainda vencem no mês e o aporte das reservas. '
      + `É dinheiro que você pode gastar sem estragar ${mes}.`;
}

/* ── Três números do mês ── */

function pintarKpis(d) {
  const entradas = Number(d.entradas);
  const proporcao = v => (entradas > 0 ? Math.min(100, (Number(v) / entradas) * 100) : 0);

  // O sinal vem sempre junto do valor: entrada e saída não podem se distinguir só pela cor.
  definir('#kpi-entradas', '+' + App.moedaAbs(d.entradas));
  definir('#kpi-saidas', '−' + App.moedaAbs(d.saidas));
  definir('#kpi-aportes', (Number(d.aportes) < 0 ? '−' : '↳ ') + App.moedaAbs(d.aportes));

  $('#bar-entradas').style.width = entradas > 0 ? '100%' : '0%';
  $('#bar-saidas').style.width = proporcao(d.saidas) + '%';
  $('#bar-aportes').style.width = proporcao(Math.abs(Number(d.aportes))) + '%';
}

function definir(seletor, texto) {
  const el = $(seletor);
  el.textContent = texto;
  el.classList.remove('esqueleto');
}

/* ── Lançamentos ── */

const SINAL = { ENTRADA: '+', SAIDA: '−', APORTE: '↳ ', SAQUE: '↰ ' };
const CLASSE = { ENTRADA: 'entrada', SAIDA: 'saida', APORTE: 'aporte', SAQUE: 'aporte' };
const LEITURA = { ENTRADA: 'entrada', SAIDA: 'saída', APORTE: 'guardado', SAQUE: 'sacado' };

function pintarLancamentos(itens) {
  const lista = $('#lancamentos');
  if (!itens.length) {
    lista.innerHTML = '<div class="vazio-lista">Nenhum lançamento neste mês ainda.</div>';
    return;
  }
  lista.innerHTML = itens.map(l => `
    <div class="linha">
      <span class="dia">${App.diaMes(l.data)}</span>
      <span class="desc">${App.esc(l.descricao)}</span>
      <span class="cat">${App.esc(l.categoria)}</span>
      ${l.essencial ? '<span class="chip">essencial</span>' : ''}
      <span class="valor ${CLASSE[l.tipo]}">
        <span class="visualmente-oculto">${LEITURA[l.tipo]} de </span>${SINAL[l.tipo]}${App.moedaAbs(l.valor)}
      </span>
    </div>`).join('');
}

/* ── Contas a vencer ── */

function pintarPrevistas(previstas, mes) {
  $('#titulo-previstas').textContent = 'Ainda vence em ' + App.mesPorExtenso(mes).split(' de ')[0];
  const alvo = $('#previstas');

  if (!previstas.length) {
    alvo.innerHTML = '<p class="vazio">Nenhuma conta prevista para o resto do mês. '
      + 'Escreva <code>prever energia 210 dia 22</code> na barra para adicionar uma.</p>';
    return;
  }

  alvo.innerHTML = previstas.map(p => `
    <div class="prevista">
      <button class="tirar" data-tirar="${p.id}"
              aria-label="Remover ${App.esc(p.nome)}" title="Remover">×</button>
      <span class="nome">${App.esc(p.nome)}</span>
      <span class="valor">${App.moeda(p.valor)}</span>
      <span class="vence">vence ${App.diaMes(p.vencimento)}</span>
    </div>`).join('');

  alvo.querySelectorAll('[data-tirar]').forEach(b => {
    const conta = previstas.find(p => p.id === b.dataset.tirar);
    b.addEventListener('click', () => removerPrevista(conta));
  });
}

async function removerPrevista(conta) {
  try {
    await App.api('/previstas/' + conta.id, { method: 'DELETE' });
    ultimo = { tipo: 'PREVISTA_REMOVIDA', conta };
    mostrarFaixa('Removido ' + conta.nome, 'desfazer');
    await carregar();
  } catch (e) {
    mostrarErro(e.message);
  }
}

/* ── Barra de comando ── */

function pintarPreview() {
  const alvo = $('#preview');
  const sugestoes = $('#sugestoes');
  const p = App.Comando.parse($('#comando').value);

  if (!p) {
    alvo.innerHTML = '';
    sugestoes.hidden = false;
    return;
  }
  sugestoes.hidden = true;

  const titulo = { APORTE: 'vai guardar', ENTRADA: 'vai receber',
    PREVISTA: 'vai prever', SAIDA: 'vai lançar' }[p.tipo];
  const rotuloValor = { APORTE: 'aporte ', ENTRADA: 'entrada ',
    PREVISTA: 'conta ', SAIDA: 'saída ' }[p.tipo];

  const fracos = p.tipo === 'PREVISTA'
    ? [`vence dia ${p.diaVencimento}`, p.recorrente ? 'todo mês' : 'só este mês']
    : [p.categoria.toLowerCase(), App.diaMes(iso(p.data)),
       p.tipo === 'SAIDA' ? (p.essencial ? 'essencial' : 'supérflua') : null];

  alvo.innerHTML = `
    <span class="rotulo">${titulo}</span>
    <span class="chip-forte">${rotuloValor}${App.moeda(p.valor)}</span>
    ${fracos.filter(Boolean).map(t => `<span class="chip-fraco">${App.esc(t)}</span>`).join('')}
    <span class="dica">enter para salvar</span>`;
}

function pintarSugestoes() {
  $('#sugestoes').innerHTML = '<span class="rotulo">tente:</span>'
    + SUGESTOES.map(s => `<button type="button" data-sugestao="${App.esc(s)}">${App.esc(s)}</button>`).join('');

  $('#sugestoes').querySelectorAll('[data-sugestao]').forEach(b =>
    b.addEventListener('click', () => {
      $('#comando').value = b.dataset.sugestao;
      $('#comando').focus();
      pintarPreview();
    }));
}

async function salvar() {
  const campo = $('#comando');
  const texto = campo.value.trim();
  if (!texto || !App.Comando.parse(texto)) return;

  campo.disabled = true;
  try {
    const r = await App.api('/comando', { method: 'POST', body: { texto } });
    campo.value = '';
    ultimo = { tipo: r.tipo, id: r.id, reservaId: r.reservaId, valor: r.valor };
    mostrarFaixa(r.mensagem, 'desfazer');
    pintarPreview();
    await carregar();
  } catch (e) {
    mostrarErro(e.message);
  } finally {
    campo.disabled = false;
    campo.focus();
  }
}

/**
 * Desfaz o último lançamento.
 *
 * Entrada e saída somem. Aporte volta como saque de mesmo valor — a movimentação original
 * fica no extrato, porque ela aconteceu; fingir o contrário seria mentir no histórico.
 */
async function desfazer() {
  if (!ultimo) return;
  try {
    switch (ultimo.tipo) {
      case 'ENTRADA':
      case 'SAIDA':
        await App.api('/transacoes/' + ultimo.id, { method: 'DELETE' });
        break;
      case 'APORTE':
        await App.api('/reservas/' + ultimo.reservaId + '/sacar',
          { method: 'POST', body: { valor: ultimo.valor } });
        break;
      case 'PREVISTA':
        await App.api('/previstas/' + ultimo.id, { method: 'DELETE' });
        break;
      case 'PREVISTA_REMOVIDA':
        await App.api('/previstas', { method: 'POST', body: {
          nome: ultimo.conta.nome,
          valor: ultimo.conta.valor,
          diaVencimento: ultimo.conta.diaVencimento,
          mesReferencia: ultimo.conta.recorrente ? null : dadosMes
        } });
        break;
      default:
        return;
    }
    const era = ultimo.tipo;
    ultimo = null;
    esconderFaixa();
    await carregar();
    if (era === 'APORTE') App.toast('Aporte desfeito — o saque ficou registrado no extrato.');
  } catch (e) {
    mostrarErro(e.message);
  }
}

/* ── Faixa de aviso ── */

function mostrarFaixa(texto, acao) {
  const faixa = $('#faixa');
  faixa.className = 'faixa';
  faixa.hidden = false;
  faixa.innerHTML = `<span class="texto">${App.esc(texto)}</span>`
    + (acao ? `<button class="acao" type="button" id="btn-desfazer">${acao}</button>` : '');
  if (acao) $('#btn-desfazer').addEventListener('click', desfazer);
  $('#sugestoes').hidden = true;
}

function mostrarErro(texto) {
  const faixa = $('#faixa');
  faixa.className = 'faixa erro';
  faixa.hidden = false;
  faixa.innerHTML = `<span class="texto">${App.esc(texto)}</span>`
    + '<button class="acao" type="button" id="btn-retry">tentar de novo</button>';
  $('#btn-retry').addEventListener('click', () => {
    esconderFaixa();
    carregar().catch(erro => mostrarErro(erro.message));
  });
  $('#sugestoes').hidden = true;
}

/** Some com a faixa e devolve as sugestões, que só aparecem com o campo vazio e sem aviso. */
function esconderFaixa() {
  $('#faixa').hidden = true;
  $('#faixa').innerHTML = '';
  $('#sugestoes').hidden = !!App.Comando.parse($('#comando').value);
}

/* ── Carga ── */

let dadosMes = null;

async function carregar() {
  const d = await App.api('/hoje');
  dadosMes = d.mes;
  pintarHeroi(d);
  pintarKpis(d);
  pintarLancamentos(d.ultimosLancamentos);
  pintarPrevistas(d.previstas, d.mes);
  App.carregarGuardado();
}

/** Data local em ISO — `toISOString` converteria para UTC e poderia voltar um dia. */
function iso(data) {
  return data.getFullYear() + '-'
    + String(data.getMonth() + 1).padStart(2, '0') + '-'
    + String(data.getDate()).padStart(2, '0');
}

(async () => {
  const eu = await App.guardar();
  App.montarShell(eu, 'hoje');
  pintarSugestoes();

  // A tecla mostrada tem de ser a que funciona na máquina de quem olha.
  const mac = /Mac|iPhone|iPad/.test(navigator.platform || navigator.userAgent);
  document.querySelector('.comando .tecla').textContent = mac ? '⌘K' : 'Ctrl K';

  const campo = $('#comando');
  campo.addEventListener('input', () => { esconderFaixa(); pintarPreview(); });
  campo.addEventListener('keydown', e => {
    if (e.key === 'Enter') { e.preventDefault(); salvar(); }
    if (e.key === 'Escape') { campo.value = ''; pintarPreview(); }
  });

  // ⌘K / Ctrl+K foca a barra de qualquer lugar da tela.
  document.addEventListener('keydown', e => {
    if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k') {
      e.preventDefault();
      campo.focus();
      campo.select();
    }
  });

  await carregar();
  campo.focus();
})().catch(e => {
  mostrarErro(e.message);
  App.toast(e.message);
});
