/* Dashboard: capacity, forecast, alerts and the batches to act on first. */
(function () {
  'use strict';
  if (window.ZuriBlocked) return;
  var Z = window.Zuri, U = window.ZuriUI, KG = Z.KG, nf = Z.nf, esc = Z.esc;
  var $ = function (s) { return document.querySelector(s); };

  var state = { depot: null, summary: null };

  function renderHead() {
    var u = Z.session.get(), h = new Date().getHours();
    var g = h < 12 ? 'Good morning' : h < 17 ? 'Good afternoon' : 'Good evening';
    var namePart = u ? (u.name || u.username || '').split(' ')[0] : '';
    $('#greet').textContent = g + (namePart ? ', ' + namePart : '');
    $('#today-line').textContent = new Date().toLocaleDateString('en-GB', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' }) +
      '. Here is how depots look today.';
  }

  function kpi(label, val, small, note, ic) {
    return '<div class="kpi"><div class="icon-badge">' + U.icon(ic) + '</div><div class="label">' + label + '</div>' +
      '<div class="val mono">' + val + (small ? '<small>' + small + '</small>' : '') + '</div><div class="note">' + note + '</div></div>';
  }

  function renderKPIs() {
    var D = Z.DEPOTS;
    var stored = Z.sum(D, function (d) { return d.stored; });
    var cap = Z.sum(D, function (d) { return d.cap; });
    var redirect = D.filter(function (d) { return Z.metrics(d).st === 'crit'; }).length;
    var high = Z.DEPOSITS.filter(function (d) { return Z.riskOf(d).lv === 'high'; }).length;

    var todayCount = 0;
    var todayBags = 0;
    if (state.summary && state.summary.todayDeposits !== undefined) {
      todayCount = state.summary.todayDeposits;
    }
    var todayStr = new Date().toDateString();
    var todayDeposits = Z.DEPOSITS.filter(function (d) { return new Date(d.ts).toDateString() === todayStr; });
    if (todayDeposits.length) {
      todayCount = todayDeposits.length;
      todayBags = Z.sum(todayDeposits, function (d) { return d.bags; });
    }

    var utilPercent = cap > 0 ? Math.round(stored / cap * 100) : 0;

    $('#kpis').innerHTML =
      kpi('In store', nf(stored), 'bags', nf(stored * KG / 1000) + ' t, ' + utilPercent + '% of ' + nf(cap) + ' capacity', 'bag') +
      kpi('Depots to redirect', redirect, 'of ' + D.length, '90% full, or under 3 days of space', 'gauge') +
      kpi('High-risk batches', high, '', 'Wet grain that has sat in store', 'droplet') +
      kpi('Received today', todayCount, 'deposits', nf(todayBags) + ' bags', 'clip');
  }

  function renderDepots() {
    var D = Z.DEPOTS;
    if (!D.length) {
      $('#depots').innerHTML = '<p class="empty" style="padding:16px">No depots found.</p>';
      return;
    }
    $('#depots').innerHTML = D.map(function (d) {
      var m = Z.metrics(d), s = Z.ST[m.st];
      return '<button class="depot ' + s.cls + '" data-depot="' + d.id + '" aria-pressed="' + (state.depot === d.id) + '">' +
        '<div class="d-head"><span class="d-name">' + esc(d.name) + '</span><span class="pill ' + s.cls + '">' + s.t + '</span></div>' +
        '<div class="bar"><i style="width:' + Math.min(100, Math.round(m.util * 100)) + '%"></i></div>' +
        '<div class="d-meta"><span class="mono">' + nf(d.stored) + ' / ' + nf(d.cap) + ' bags · ' + Math.round(m.util * 100) + '%</span><span>Full in ' + Z.daysTxt(m.days) + '</span></div></button>';
    }).join('');
  }

  function renderChart() {
    var dep = Z.depotById(state.depot);
    if (!dep) return;
    var m = Z.metrics(dep), hist = Z.history(dep), box = $('#chart');
    if (!box) return;
    var W = Math.max(300, Math.floor(box.clientWidth - 20) || 520), H = 250, L = 50, R = 14, T = 16, B = 30;
    var iw = W - L - R, ih = H - T - B, ymax = Math.max(1000, dep.cap * 1.1);
    var X = function (d) { return L + (d + 13) / 27 * iw; };
    var Y = function (v) { return T + ih - v / ymax * ih; };
    var s = '<svg viewBox="0 0 ' + W + ' ' + H + '" role="img" aria-label="Stock at ' + esc(dep.name) + ' over the last 14 days with a 14 day projection">';
    [0, .25, .5, .75, 1].forEach(function (f) {
      var v = dep.cap * f;
      s += '<line class="' + (f === 1 ? 'cl' : 'gl') + '" x1="' + L + '" x2="' + (W - R) + '" y1="' + Y(v).toFixed(1) + '" y2="' + Y(v).toFixed(1) + '"/>' +
        '<text x="' + (L - 6) + '" y="' + (Y(v) + 4).toFixed(1) + '" text-anchor="end">' + nf(v) + '</text>';
    });
    var pts = hist.map(function (v, i) { return X(i - 13).toFixed(1) + ' ' + Y(v).toFixed(1); });
    s += '<path class="area" d="M' + pts.join('L') + 'L' + X(0).toFixed(1) + ' ' + Y(0).toFixed(1) + 'L' + X(-13).toFixed(1) + ' ' + Y(0).toFixed(1) + 'Z"/>';
    s += '<path class="act" d="M' + pts.join('L') + '"/>';
    var dFull = m.rate > 0 ? (m.free / m.rate) : 999;
    var endD = Math.min(14, dFull), endV = dep.stored + m.rate * endD;
    s += '<line class="tl" x1="' + X(0).toFixed(1) + '" x2="' + X(0).toFixed(1) + '" y1="' + T + '" y2="' + (T + ih) + '"/>';
    s += '<path class="proj" d="M' + X(0).toFixed(1) + ' ' + Y(dep.stored).toFixed(1) + 'L' + X(endD).toFixed(1) + ' ' + Y(endV).toFixed(1) + '"/>';
    s += '<circle class="nm" cx="' + X(0).toFixed(1) + '" cy="' + Y(dep.stored).toFixed(1) + '" r="4.5"/>';
    if (dFull <= 14) {
      var fx = X(dFull);
      s += '<circle class="fm" cx="' + fx.toFixed(1) + '" cy="' + Y(dep.cap).toFixed(1) + '" r="5"/>';
      s += '<text class="fl" x="' + (fx - 9).toFixed(1) + '" y="' + (Y(dep.cap) - 8).toFixed(1) + '" text-anchor="end">Full ' + Z.fmtDay(Z.NOW + dFull * Z.DAY) + '</text>';
    }
    s += '<text x="' + X(-13) + '" y="' + (H - 8) + '" text-anchor="start">13 d ago</text><text x="' + X(0).toFixed(1) + '" y="' + (H - 8) + '" text-anchor="middle">Today</text><text x="' + X(14) + '" y="' + (H - 8) + '" text-anchor="end">+14 d</text></svg>';
    box.innerHTML = s;
    $('#chart-title').textContent = dep.name + ' depot';
    $('#cap-legend').textContent = 'Capacity ' + nf(dep.cap);
    $('#chart-note').innerHTML = 'At <strong class="mono">' + nf(m.rate) + '</strong> bags a day, this depot has <strong class="mono">' + nf(m.free) + '</strong> bags of space left and ' +
      (dFull <= 14 ? 'fills in ' + Z.daysTxt(dFull) + ' (<strong>' + Z.fmtDay(Z.NOW + dFull * Z.DAY) + '</strong>).' : 'will not fill within the next 14 days.');
  }

  function renderAlerts() {
    var D = Z.DEPOTS;
    var items = [];
    D.slice().sort(function (a, b) { return Z.metrics(a).days - Z.metrics(b).days; }).forEach(function (d) {
      var m = Z.metrics(d);
      if (m.st === 'ok') return;
      var alt = D.filter(function (x) { return x.id !== d.id && Z.metrics(x).st === 'ok'; })
        .sort(function (a, b) { return Z.metrics(b).free - Z.metrics(a).free; })[0];
      items.push({
        cls: Z.ST[m.st].cls,
        html: '<strong>' + esc(d.name) + '</strong> is ' + Math.round(m.util * 100) + '% full and fills in ' + Z.daysTxt(m.days) + '. ' +
          (alt ? 'Send new deliveries to ' + esc(alt.name) + ' (' + nf(Z.metrics(alt).free) + ' bags free).' : 'No depot has spare room, so plan a dispatch.'),
        sms: alt ? 'Ushirika Grain: ' + d.name + ' depot is almost full. Please deliver to ' + alt.name + ' depot instead.' : null
      });
    });
    var hi = Z.DEPOSITS.filter(function (d) { return Z.riskOf(d).lv === 'high'; });
    if (hi.length) {
      items.push({ cls: 's-crit', html: '<strong>' + hi.length + ' batches</strong> are at high spoilage risk. Dry or dispatch the wettest lots first (see the table below).', sms: null });
    }
    $('#alerts').innerHTML = items.length ? items.map(function (a, i) {
      return '<div class="alert ' + a.cls + '"><span class="dot"></span><div class="txt">' + a.html + '<div data-fb="' + i + '"></div></div>' +
        (a.sms ? '<button class="btn btn-outline btn-sm" data-copy="' + esc(a.sms) + '" type="button">Copy SMS</button>' : '') + '</div>';
    }).join('') : '<p class="empty" style="padding:16px">No alerts. Every depot has room and no batch is at risk.</p>';
  }

  function renderRisk() {
    var list = Z.DEPOSITS.slice().sort(function (a, b) { return Z.riskOf(b).s - Z.riskOf(a).s; }).slice(0, 5);
    if (!list.length) {
      $('#risk-table').innerHTML = '<p class="empty" style="padding:24px">No grain batches recorded yet. Deliveries will appear here with automated moisture risk ratings.</p>';
      return;
    }
    $('#risk-table').innerHTML = '<table><thead><tr><th>Tracking ID</th><th>Farmer</th><th>Depot</th><th class="r">Moisture</th><th class="r">Days in store</th><th>Risk</th><th>Recommended action</th></tr></thead><tbody>' +
      list.map(function (d) {
        var r = Z.riskOf(d);
        var dep = Z.depotById(d.depot);
        var depName = dep ? dep.name : d.depot;
        return '<tr><td><button class="idlink" data-id="' + esc(d.id) + '" type="button">' + esc(d.id) + '</button></td><td>' + esc(d.farmer) + '</td><td>' + esc(depName) +
          '</td><td class="r mono">' + d.m.toFixed(1) + '%</td><td class="r mono">' + Math.floor(r.days) + '</td><td>' + U.riskPill(r) + '</td><td class="act-cell">' + Z.LV[r.lv].act + '</td></tr>';
      }).join('') + '</tbody></table>';
  }

  document.addEventListener('click', function (e) {
    var t = e.target.closest('button');
    if (!t) return;
    if (t.dataset.depot) {
      state.depot = t.dataset.depot;
      renderDepots();
      renderChart();
      return;
    }
    if (t.dataset.copy !== undefined) {
      U.copy(t.dataset.copy, t, t.parentNode.querySelector('[data-fb]'));
      return;
    }
    if (t.dataset.id) {
      Z.setLookup(t.dataset.id);
      location.href = 'deposits.html';
    }
  });

  Z.ready(function (data) {
    if (data && data.summary) state.summary = data.summary;
    var D = Z.DEPOTS;
    if (D.length) {
      var urgent = D.slice().sort(function (a, b) { return Z.metrics(a).days - Z.metrics(b).days; })[0];
      state.depot = urgent ? urgent.id : D[0].id;
    }
    renderHead();
    renderKPIs();
    renderDepots();
    renderChart();
    renderAlerts();
    renderRisk();
    try { new ResizeObserver(renderChart).observe($('#chart')); } catch (e) { window.addEventListener('resize', renderChart); }
  });
})();
