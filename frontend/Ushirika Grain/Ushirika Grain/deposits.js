/* Deposits: receipt lookup, searchable ledger, tamper-evidence demo, and manager actions. */
(function () {
  'use strict';
  if (window.ZuriBlocked) return;
  var Z = window.Zuri, U = window.ZuriUI, API = window.ZuriAPI, esc = Z.esc, nf = Z.nf;
  var $ = function (s) { return document.querySelector(s); };

  var user = Z.session.get();
  var isManager = user && user.roles && (user.roles.includes('ROLE_ADMIN') || user.roles.includes('ROLE_DEPOT_MANAGER'));

  var state = {
    q: '',
    fd: '',
    fr: '',
    lookup: Z.takeLookup() || '',
    tamper: null,
    verify: null
  };

  function renderReceipt(msg) {
    var field = $('#lookup-field');
    $('#lookup-id').value = state.lookup || '';

    if (!state.lookup) {
      field.classList.remove('invalid');
      $('#receipt').innerHTML = '';
      return;
    }

    var d = Z.find(state.lookup);
    if (d) {
      field.classList.remove('invalid');
      $('#receipt').innerHTML = '<div class="ticket-wrap" style="margin:0 22px 22px">' + U.ticket(d) + '</div>';
      return;
    }

    // Try fetching from backend if not currently in memory
    if (API) {
      API.get('/deposits/reference/' + encodeURIComponent(state.lookup))
        .then(function (res) {
          field.classList.remove('invalid');
          var mapped = {
            id: res.depositReference,
            farmer: res.farmerName || ('Farmer #' + res.farmerId),
            phone: (res.farmerPhone || '000').slice(-3),
            sub: res.depotName || '',
            depot: res.depotCode,
            crop: res.grainTypeName,
            bags: Math.round(Number(res.quantity) / Z.KG),
            m: Number(res.moisturePercentage || 13.0),
            ts: new Date(res.createdAt).getTime(),
            status: res.status,
            pay: res.status === 'ACCEPTED' ? 'Paid' : (res.status === 'REJECTED' ? 'Rejected' : 'Pending'),
            hash: '00000000000000'
          };
          $('#receipt').innerHTML = '<div class="ticket-wrap" style="margin:0 22px 22px">' + U.ticket(mapped) + '</div>';
        })
        .catch(function () {
          field.classList.add('invalid');
          $('#lookup-err').textContent = 'No deposit has that ID. Check the ID on the farmer’s ticket and try again.';
          $('#receipt').innerHTML = '';
        });
    } else {
      field.classList.add('invalid');
      $('#lookup-err').textContent = 'No deposit has that ID. Check the ID on the farmer’s ticket and try again.';
      $('#receipt').innerHTML = '';
    }
  }

  function filtered() {
    var q = state.q.trim().toLowerCase();
    return Z.DEPOSITS.slice().reverse().filter(function (d) {
      if (state.fd && d.depot !== state.fd && d.depotId !== Number(state.fd)) return false;
      if (state.fr && Z.riskOf(d).lv !== state.fr) return false;
      return !q || (d.id + ' ' + d.farmer + ' ' + Z.depotName(d.depot)).toLowerCase().indexOf(q) > -1;
    });
  }

  function renderLedger() {
    var list = filtered();
    $('#ledger-count').textContent = list.length + ' of ' + Z.DEPOSITS.length + ' deposits';
    if (!list.length) {
      $('#ledger').innerHTML = '<p class="empty" style="padding:24px">No deposits match. Record a new deposit on the Intake page.</p>';
      return;
    }

    var html = '<table><thead><tr>' +
      '<th>Tracking ID</th>' +
      '<th>Farmer</th>' +
      '<th>Depot</th>' +
      '<th class="r">Bags</th>' +
      '<th class="r">Moisture</th>' +
      '<th>Risk</th>' +
      '<th>Status</th>' +
      '<th>Received</th>' +
      (isManager ? '<th style="text-align:center">Actions</th>' : '') +
      '</tr></thead><tbody>';

    html += list.map(function (d) {
      var r = Z.riskOf(d);
      var depotInfo = Z.depotById(d.depot);
      var depotDisplayName = depotInfo ? depotInfo.name : d.depot;

      var actionsHtml = '';
      if (isManager && d.backendId) {
        if (d.status === 'PENDING') {
          actionsHtml = '<td style="text-align:center;white-space:nowrap">' +
            '<button class="btn btn-primary btn-sm" style="padding:4px 8px;font-size:12px;margin-right:4px" data-act="accept" data-bid="' + d.backendId + '" type="button">Accept</button>' +
            '<button class="btn btn-outline btn-sm" style="padding:4px 8px;font-size:12px;color:var(--crit,#c00)" data-act="reject" data-bid="' + d.backendId + '" type="button">Reject</button>' +
            '</td>';
        } else {
          actionsHtml = '<td style="text-align:center;font-size:12px;color:var(--muted)">' + (d.status === 'ACCEPTED' ? 'Approved' : 'Rejected') + '</td>';
        }
      } else if (isManager) {
        actionsHtml = '<td style="text-align:center;color:var(--muted);font-size:12px">-</td>';
      }

      return '<tr>' +
        '<td><button class="idlink" data-id="' + esc(d.id) + '" type="button">' + esc(d.id) + '</button></td>' +
        '<td>' + esc(d.farmer) + '</td>' +
        '<td>' + esc(depotDisplayName) + '</td>' +
        '<td class="r mono">' + nf(d.bags) + '</td>' +
        '<td class="r mono">' + (d.m ? d.m.toFixed(1) + '%' : '-') + '</td>' +
        '<td>' + U.riskPill(r) + '</td>' +
        '<td><span class="pill ' + (Z.PAY[d.pay] || 's-warn') + '">' + esc(d.pay) + '</span></td>' +
        '<td class="mono">' + Z.fmtDate(d.ts) + '</td>' +
        actionsHtml +
        '</tr>';
    }).join('');

    html += '</tbody></table>';
    $('#ledger').innerHTML = html;
  }

  function renderVerify() {
    var v = state.verify, el = $('#verify-msg');
    var tamperBtn = $('#tamper-btn');
    if (tamperBtn) tamperBtn.textContent = state.tamper ? 'Undo the edit' : 'Simulate an edit';
    if (!v) { el.innerHTML = ''; return; }
    el.innerHTML = v.ok ?
      '<span class="v-ok"><strong>Chain intact.</strong> ' + v.n + ' records checked, each linked to the one before it.</span>' :
      '<span class="v-bad"><strong>Chain broken.</strong> <span class="mono">' + esc(v.at.id) + '</span> no longer matches its signature. Its details changed after it was saved.</span>';
  }

  function renderDepotFilter() {
    var fd = $('#fd');
    if (!fd) return;
    fd.innerHTML = '<option value="">All depots</option>' + Z.DEPOTS.map(function (d) {
      return '<option value="' + esc(d.id) + '">' + esc(d.name) + '</option>';
    }).join('');
  }

  $('#lookup-form').addEventListener('submit', function (e) {
    e.preventDefault();
    state.lookup = $('#lookup-id').value.trim().toUpperCase();
    renderReceipt();
  });

  $('#lookup-id').addEventListener('input', function () {
    $('#lookup-field').classList.remove('invalid');
  });

  $('#q').addEventListener('input', function (e) {
    state.q = e.target.value;
    renderLedger();
  });

  $('#fd').addEventListener('change', function (e) {
    state.fd = e.target.value;
    renderLedger();
  });

  $('#fr').addEventListener('change', function (e) {
    state.fr = e.target.value;
    renderLedger();
  });

  document.addEventListener('click', function (e) {
    var t = e.target.closest('button');
    if (!t) return;

    if (t.dataset.id) {
      state.lookup = t.dataset.id;
      renderReceipt();
      var calm = window.matchMedia && matchMedia('(prefers-reduced-motion: reduce)').matches;
      var lookupPanel = $('#lookup-panel');
      if (lookupPanel) lookupPanel.scrollIntoView({ behavior: calm ? 'auto' : 'smooth', block: 'start' });
      return;
    }

    var a = t.dataset.act;
    if (a === 'verify') {
      state.verify = Z.verifyLedger();
      renderVerify();
    }
    if (a === 'tamper') {
      if (!Z.DEPOSITS.length) return;
      if (state.tamper) {
        state.tamper.d.bags = state.tamper.bags;
        state.tamper = null;
      } else {
        var d = Z.DEPOSITS[0];
        state.tamper = { d: d, bags: d.bags };
        d.bags += 20;
      }
      state.verify = Z.verifyLedger();
      renderVerify();
      renderReceipt();
      renderLedger();
    }
    if (a === 'reset') {
      U.toast('Refreshing data from server…');
      location.reload();
    }

    // Manager actions: Accept or Reject deposit
    if (a === 'accept' && t.dataset.bid) {
      var bid = t.dataset.bid;
      t.disabled = true;
      t.textContent = '…';
      API.patch('/deposits/' + bid + '/accept', {})
        .then(function () {
          U.toast('Deposit accepted and stock updated');
          var found = Z.DEPOSITS.filter(function (x) { return String(x.backendId) === String(bid); })[0];
          if (found) {
            found.status = 'ACCEPTED';
            found.pay = 'Paid';
          }
          renderLedger();
        })
        .catch(function (err) {
          t.disabled = false;
          t.textContent = 'Accept';
          alert('Failed to accept deposit: ' + (err.message || 'Error'));
        });
    }

    if (a === 'reject' && t.dataset.bid) {
      var bid = t.dataset.bid;
      var reason = prompt('Please enter the reason for rejection:', 'Excessive moisture or failed quality check');
      if (!reason) return;
      t.disabled = true;
      t.textContent = '…';
      API.patch('/deposits/' + bid + '/reject', { rejectionReason: reason })
        .then(function () {
          U.toast('Deposit rejected');
          var found = Z.DEPOSITS.filter(function (x) { return String(x.backendId) === String(bid); })[0];
          if (found) {
            found.status = 'REJECTED';
            found.pay = 'Rejected';
            found.rejectionReason = reason;
          }
          renderLedger();
        })
        .catch(function (err) {
          t.disabled = false;
          t.textContent = 'Reject';
          alert('Failed to reject deposit: ' + (err.message || 'Error'));
        });
    }
  });

  Z.ready(function () {
    renderDepotFilter();
    if (!state.lookup && Z.DEPOSITS.length) {
      var topRisk = Z.DEPOSITS.slice().sort(function (a, b) { return Z.riskOf(b).s - Z.riskOf(a).s; })[0];
      if (topRisk) state.lookup = topRisk.id;
    }
    renderReceipt();
    renderLedger();
    renderVerify();
  });
})();
