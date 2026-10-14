/* ==========================================================================
   Admin & Depot Operations: Pending Approvals, Balances, and Stock Adjustments
   ========================================================================== */
(function () {
  'use strict';
  if (window.ZuriBlocked) return;
  var Z = window.Zuri, U = window.ZuriUI, API = window.ZuriAPI, esc = Z.esc, nf = Z.nf;
  var $ = function (s) { return document.querySelector(s); };

  var user = Z.session.get();
  var isManager = user && user.roles && (user.roles.includes('ROLE_ADMIN') || user.roles.includes('ROLE_DEPOT_MANAGER'));

  if (!isManager) {
    $('#admin-content').style.display = 'none';
    $('#admin-access-denied').style.display = 'flex';
    return;
  }

  /* ---------- Section 1: Pending Approvals Queue ---------- */
  function loadPendingDeposits() {
    var tbody = $('#pending-tbody');
    var countBadge = $('#pending-count');

    API.get('/deposits?status=PENDING&size=100')
      .then(function (res) {
        var items = res.content || [];
        countBadge.textContent = items.length + ' pending';

        if (!items.length) {
          tbody.innerHTML = '<tr><td colspan="9" class="empty" style="padding:24px;text-align:center">No pending deposits awaiting approval.</td></tr>';
          return;
        }

        tbody.innerHTML = items.map(function (d) {
          var kg = Number(d.quantity || 0);
          var bags = Math.max(1, Math.round(kg / Z.KG));
          var m = Number(d.moisturePercentage || 13.0);
          var riskScore = Z.score(m, 0);
          var riskLevel = Z.level(riskScore);
          var L = Z.LV[riskLevel];
          var dateStr = Z.fmtDate(new Date(d.createdAt).getTime());

          return '<tr>' +
            '<td class="mono"><strong>' + esc(d.depositReference) + '</strong></td>' +
            '<td>' + esc(d.farmerName || ('Farmer #' + d.farmerId)) + '<br><small class="mono muted">' + esc(d.farmerPhone || '') + '</small></td>' +
            '<td>' + esc(d.depotName || d.depotCode) + '</td>' +
            '<td>' + esc(d.grainTypeName || 'Grain') + '</td>' +
            '<td class="r mono">' + nf(kg) + ' kg<br><small class="muted">' + nf(bags) + ' bags</small></td>' +
            '<td class="r mono">' + m.toFixed(1) + '%</td>' +
            '<td><span class="pill ' + L.cls + '">' + L.t + '</span></td>' +
            '<td class="mono" style="font-size:13px">' + dateStr + '</td>' +
            '<td style="text-align:center;white-space:nowrap">' +
            '<button class="btn btn-primary btn-sm" style="padding:4px 10px;font-size:12.5px;margin-right:6px" data-act="accept" data-bid="' + d.id + '" type="button">Accept</button>' +
            '<button class="btn btn-outline btn-sm" style="padding:4px 10px;font-size:12.5px;color:var(--crit,#c00)" data-act="reject" data-bid="' + d.id + '" type="button">Reject</button>' +
            '</td>' +
            '</tr>';
        }).join('');
      })
      .catch(function (err) {
        tbody.innerHTML = '<tr><td colspan="9" class="empty s-crit" style="padding:24px;text-align:center">Failed to load pending deposits: ' + esc(err.message) + '</td></tr>';
      });
  }

  /* ---------- Section 2: Real-time Balances ---------- */
  function loadBalances() {
    var tbody = $('#balances-tbody');
    tbody.innerHTML = '<tr><td colspan="7" class="empty" style="padding:24px;text-align:center">Loading inventory balances…</td></tr>';

    API.get('/depots')
      .then(function (depots) {
        if (!depots || !depots.length) {
          tbody.innerHTML = '<tr><td colspan="7" class="empty" style="padding:24px;text-align:center">No active depots found.</td></tr>';
          return;
        }

        var balancePromises = depots.map(function (depot) {
          return API.get('/inventory/depots/' + depot.id)
            .then(function (balances) {
              return { depot: depot, balances: balances };
            })
            .catch(function () {
              return { depot: depot, balances: [] };
            });
        });

        return Promise.all(balancePromises).then(function (results) {
          var rows = [];
          results.forEach(function (res) {
            var dep = res.depot;
            var balances = res.balances;
            var capKg = Number(dep.capacityMetricTonnes || 0) * 1000;

            if (!balances || !balances.length) {
              rows.push('<tr>' +
                '<td><strong>' + esc(dep.name) + '</strong><br><small class="muted">' + esc(dep.location) + '</small></td>' +
                '<td class="muted">No grain stored yet</td>' +
                '<td class="r mono">0 kg</td>' +
                '<td class="r mono">0 bags</td>' +
                '<td class="r mono">' + nf(capKg) + ' kg</td>' +
                '<td class="r mono">0%</td>' +
                '<td class="muted">-</td>' +
                '</tr>');
            } else {
              balances.forEach(function (b) {
                var stockKg = Number(b.quantity || 0);
                var bags = Math.round(stockKg / Z.KG);
                var util = capKg > 0 ? (stockKg / capKg * 100).toFixed(1) + '%' : '0%';
                var updated = b.lastUpdated ? Z.fmtDate(new Date(b.lastUpdated).getTime()) : '-';

                rows.push('<tr>' +
                  '<td><strong>' + esc(b.depotName || dep.name) + '</strong></td>' +
                  '<td>' + esc(b.grainTypeName) + '</td>' +
                  '<td class="r mono">' + nf(stockKg) + ' kg</td>' +
                  '<td class="r mono">' + nf(bags) + ' bags</td>' +
                  '<td class="r mono">' + nf(capKg) + ' kg</td>' +
                  '<td class="r mono">' + util + '</td>' +
                  '<td class="mono" style="font-size:13px">' + updated + '</td>' +
                  '</tr>');
              });
            }
          });

          tbody.innerHTML = rows.join('');
        });
      })
      .catch(function (err) {
        tbody.innerHTML = '<tr><td colspan="7" class="empty s-crit" style="padding:24px;text-align:center">Failed to load balances: ' + esc(err.message) + '</td></tr>';
      });
  }

  /* ---------- Section 3: Stock Adjustment Form ---------- */
  function setupAdjustmentForm() {
    var depotSelect = $('#adj-depot');
    var grainSelect = $('#adj-grain');
    var qtyInput = $('#adj-qty');
    var bagsHint = $('#adj-bags-hint');
    var form = $('#adjustment-form');
    var errBox = $('#adj-error');

    API.get('/depots').then(function (depots) {
      depotSelect.innerHTML = depots.map(function (d) {
        return '<option value="' + d.id + '">' + esc(d.name) + ' (' + esc(d.code) + ')</option>';
      }).join('');
    });

    API.get('/grain-types').then(function (grains) {
      grainSelect.innerHTML = grains.map(function (g) {
        return '<option value="' + g.id + '">' + esc(g.name) + '</option>';
      }).join('');
    });

    qtyInput.addEventListener('input', function () {
      var v = parseFloat(qtyInput.value) || 0;
      bagsHint.textContent = v > 0 ? '= ' + nf(v / Z.KG) + ' bags (' + nf(v) + ' kg)' : 'Each bag is 90 kg';
    });

    form.addEventListener('submit', function (e) {
      e.preventDefault();
      errBox.classList.remove('show');

      var depotId = parseInt(depotSelect.value, 10);
      var grainTypeId = parseInt(grainSelect.value, 10);
      var movementType = $('#adj-type').value;
      var quantity = parseFloat(qtyInput.value);
      var reason = $('#adj-reason').value.trim();
      var refNumber = $('#adj-ref').value.trim();

      if (!quantity || quantity <= 0) {
        errBox.textContent = 'Enter a valid positive quantity in KG.';
        errBox.classList.add('show');
        qtyInput.focus();
        return;
      }

      if (!reason) {
        errBox.textContent = 'Mandatory audit reason must be provided.';
        errBox.classList.add('show');
        $('#adj-reason').focus();
        return;
      }

      var btn = $('#adj-btn');
      btn.disabled = true;
      btn.textContent = 'Submitting adjustment…';

      var payload = {
        depotId: depotId,
        grainTypeId: grainTypeId,
        movementType: movementType,
        quantity: quantity,
        reason: reason,
        referenceNumber: refNumber || null
      };

      API.post('/inventory/adjustments', payload)
        .then(function () {
          U.toast('Stock adjustment logged successfully');
          qtyInput.value = '';
          $('#adj-reason').value = '';
          $('#adj-ref').value = '';
          bagsHint.textContent = 'Each bag is 90 kg';
          loadBalances();
        })
        .catch(function (err) {
          errBox.textContent = 'Adjustment failed: ' + (err.message || 'Server error');
          errBox.classList.add('show');
        })
        .finally(function () {
          btn.disabled = false;
          btn.textContent = 'Submit Stock Adjustment';
        });
    });
  }

  /* ---------- Action event listeners ---------- */
  document.addEventListener('click', function (e) {
    var t = e.target.closest('button');
    if (!t) return;

    var act = t.dataset.act;
    var bid = t.dataset.bid;

    if (act === 'accept' && bid) {
      t.disabled = true;
      t.textContent = '…';
      API.patch('/deposits/' + bid + '/accept', {})
        .then(function () {
          U.toast('Deposit accepted');
          loadPendingDeposits();
          loadBalances();
        })
        .catch(function (err) {
          t.disabled = false;
          t.textContent = 'Accept';
          alert('Failed to accept deposit: ' + (err.message || 'Error'));
        });
    }

    if (act === 'reject' && bid) {
      var reason = prompt('Please specify the reason for deposit rejection:', 'High moisture content or quality criteria not met');
      if (!reason) return;
      t.disabled = true;
      t.textContent = '…';
      API.patch('/deposits/' + bid + '/reject', { rejectionReason: reason })
        .then(function () {
          U.toast('Deposit rejected');
          loadPendingDeposits();
        })
        .catch(function (err) {
          t.disabled = false;
          t.textContent = 'Reject';
          alert('Failed to reject deposit: ' + (err.message || 'Error'));
        });
    }

    if (t.id === 'btn-refresh-balances') {
      loadBalances();
      U.toast('Balances refreshed');
    }
  });

  Z.ready(function () {
    loadPendingDeposits();
    loadBalances();
    setupAdjustmentForm();
  });
})();
