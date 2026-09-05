/* ═══ Tela Relatório ═══ */
'use strict';

const $ = s => document.querySelector(s);

/** Acordeão aberto por padrão: a primeira pergunta. */
const abertas = { q1: true, q2: false, q3: false };

/* ── Barra de fluxo ── */

function pintarFluxo(d) {
  const partes = [
    { chave: 'essenciais', rotulo: 'essenciais', valor: Number(d.essenciais) },
    { chave: 'superfluas', rotulo: 'supérfluas', valor: Number(d.superfluas) },
    { chave: 'reservas', rotulo: 'reservas', valor: Math.max(0, Number(d.aportes)) },
    { chave: 'sobra', rotulo: 'sobra', valor: Math.max(0, Number(d.sobra)) }
  ].filter(p => p.valor > 0);

  const total = partes.reduce((s, p) => s + p.valor, 0);
  const barra = $('#fluxo');

  if (!total) {
    barra.innerHTML = '<div class="faixa-seg sobra" style="flex:1">nada lançado neste mês</div>';
    barra.setAttribute('aria-label', 'Nada lançado neste mês.');
  } else {
    barra.innerHTML = partes.map(p =>
      `<div class="faixa-seg ${p.chave}" style="flex:${p.valor}">${p.rotulo}</div>`).join('');
    // A barra é decorativa para quem lê a tela; o texto abaixo dela tem os mesmos números.
    barra.setAttribute('aria-label', partes
      .map(p => `${p.rotulo} ${App.moeda(p.valor)}`).join('. ') + '.');
  }

  $('#totais').innerHTML = [
    ['ENTROU', '+' + App.moedaAbs(d.entradas), 'entrada'],
    ['ESSENCIAIS', '−' + App.moedaAbs(d.essenciais), 'saida'],
    ['SUPÉRFLUAS', '−' + App.moedaAbs(d.superfluas), 'saida'],
    ['GUARDEI', (Number(d.aportes) < 0 ? '−' : '↳ ') + App.moedaAbs(d.aportes), 'aporte'],
    ['SOBRA', (Number(d.sobra) < 0 ? '−' : '') + App.moedaAbs(d.sobra), '']
  ].map(([rot, val, cls]) =>
    `<div><span class="rot">${rot}</span><span class="val ${cls}">${val}</span></div>`).join('');
}

/* ── Histórico de sobra ── */

function pintarHistorico(historico, mesAtual) {
  // A altura é proporcional ao maior valor absoluto: um mês no vermelho também precisa
  // aparecer, e comparar só os positivos esconderia justamente o mês que interessa.
  const maior = Math.max(...historico.map(h => Math.abs(Number(h.sobra))), 1);

  $('#historico').innerHTML = historico.map(h => {
    const v = Number(h.sobra);
    const altura = Math.max(2, Math.round((Math.abs(v) / maior) * 100));
    const classes = ['barra'];
    if (h.mes === mesAtual) classes.push('atual');
    if (v < 0) classes.push('negativa');
    return `<div class="col">
        <span class="rotulo">${v < 0 ? '−' : ''}${App.moedaCurta(v)}</span>
        <div class="${classes.join(' ')}" style="height:${altura}%"></div>
      </div>`;
  }).join('');

  $('#historico-meses').innerHTML = historico
    .map(h => `<span>${App.mesCurto(h.mes)}</span>`).join('');
}

/* ── Perguntas ── */

function pintarPerguntas(d) {
  const saidas = Number(d.saidas);
  const superfluas = Number(d.superfluas);
  const maiorCat = d.topCategorias.length ? Number(d.topCategorias[0].total) : 1;

  const comMeta = d.reservas.filter(r => Number(r.meta) > 0);
  const noRitmo = comMeta.filter(r => r.noRitmo).length;

  const p = d.projecao;
  const base = Math.max(Number(d.entradas), 1);

  const perguntas = [
    {
      chave: 'q1',
      titulo: 'O que puxou meu mês pra baixo?',
      destaque: App.moeda(superfluas) + ' em supérfluas',
      texto: saidas > 0
        ? `É ${Math.round((superfluas / saidas) * 100)}% de tudo que saiu no mês.`
          + ' As categorias abaixo são as que mais pesaram.'
        : 'Nada saiu neste mês, então não há o que explicar.',
      linhas: d.topCategorias.map(c => ({
        rotulo: c.nome,
        valor: App.moeda(c.total),
        largura: (Number(c.total) / maiorCat) * 100
      }))
    },
    {
      chave: 'q2',
      titulo: 'Estou no ritmo das minhas metas?',
      destaque: comMeta.length ? `${noRitmo} de ${comMeta.length} no ritmo` : 'nenhuma meta ainda',
      texto: comMeta.length
        ? 'A previsão usa o aporte médio dos últimos 3 meses. Reserva parada não recebe'
          + ' previsão — sem aporte não há como estimar quando ela fecha.'
        : 'Defina uma meta em Reservas para acompanhar o ritmo por aqui.',
      linhas: d.reservas.map(r => ({
        rotulo: r.nome,
        valor: r.previsao ? 'meta em ' + App.mesAno(r.previsao)
          : (Number(r.meta) > 0 ? 'sem ritmo' : 'sem meta'),
        largura: Math.min(100, Number(r.progresso))
      }))
    },
    {
      chave: 'q3',
      titulo: 'Como fecho o mês se seguir assim?',
      destaque: (Number(p.sobraPrevista) < 0 ? '−' : '+') + App.moedaAbs(p.sobraPrevista)
        + (Number(p.sobraPrevista) < 0 ? ' no vermelho' : ' livres'),
      texto: `Projeção com o que já saiu e as contas que ainda vencem `
        + `(${App.moeda(p.aindaVence)}).`,
      linhas: [
        { rotulo: 'Já saiu', valor: App.moeda(p.jaSaiu), largura: (Number(p.jaSaiu) / base) * 100 },
        { rotulo: 'Ainda vence', valor: App.moeda(p.aindaVence), largura: (Number(p.aindaVence) / base) * 100 },
        { rotulo: 'Sobra prevista', valor: App.moeda(p.sobraPrevista), largura: (Math.abs(Number(p.sobraPrevista)) / base) * 100 }
      ]
    }
  ];

  $('#perguntas').innerHTML = perguntas.map(q => {
    const aberta = abertas[q.chave];
    return `<div class="pergunta">
      <button type="button" data-pergunta="${q.chave}" aria-expanded="${aberta}"
              aria-controls="resp-${q.chave}">
        <span>${App.esc(q.titulo)}</span>
        <span class="seta" aria-hidden="true">${aberta ? '▴' : '▾'}</span>
      </button>
      ${aberta ? `<div class="resposta" id="resp-${q.chave}">
        <span class="destaque">${App.esc(q.destaque)}</span>
        <p>${App.esc(q.texto)}</p>
        ${q.linhas.map(l => `
          <div class="linha-barra">
            <span class="rot">${App.esc(l.rotulo)}</span>
            <div class="trilho"><i style="width:${Math.max(0, Math.min(100, l.largura)).toFixed(1)}%"></i></div>
            <span class="val">${App.esc(l.valor)}</span>
          </div>`).join('')}
      </div>` : ''}
    </div>`;
  }).join('');

  $('#perguntas').querySelectorAll('[data-pergunta]').forEach(b =>
    b.addEventListener('click', () => {
      abertas[b.dataset.pergunta] = !abertas[b.dataset.pergunta];
      pintarPerguntas(d);
      // O foco tem de acompanhar o botão que o usuário acabou de clicar, já que o bloco é
      // reconstruído inteiro.
      $(`[data-pergunta="${b.dataset.pergunta}"]`).focus();
    }));
}

/* ── Carga ── */

async function carregar() {
  const mes = $('#mes').value;
  const d = await App.api('/relatorio?mes=' + mes);

  $('#eyebrow').textContent = 'RELATÓRIO · ' + App.mesPorExtenso(d.mes).toUpperCase();
  pintarFluxo(d);
  pintarHistorico(d.historico, d.mes);
  pintarPerguntas(d);
}

(async () => {
  const eu = await App.guardar();
  App.montarShell(eu, 'relatorio');

  App.preencherMeses($('#mes'), 12);
  $('#mes').addEventListener('change', () => carregar().catch(e => App.toast(e.message)));

  await carregar();
})().catch(e => App.toast(e.message));
