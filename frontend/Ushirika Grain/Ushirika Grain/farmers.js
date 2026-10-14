/* Farmers Directory and Profile Management */
(function () {
  'use strict';
  if (window.ZuriBlocked) return;
  var Z = window.Zuri, U = window.ZuriUI, API = window.ZuriAPI, esc = Z.esc, nf = Z.nf;
  var $ = function (s) { return document.querySelector(s); };

  var user = Z.session.get();
  var isStaff = user && user.roles && (user.roles.includes('ROLE_ADMIN') || user.roles.includes('ROLE_DEPOT_MANAGER'));

  if (!isStaff) {
    $('#farmers-content').style.display = 'none';
    $('#farmers-access-denied').style.display = 'flex';
    return;
  }

  var state = {
    search: '',
    status: 'ACTIVE',
    selectedFarmer: null
  };

  /* ---------- Load Farmers ---------- */
  function loadFarmers() {
    var tbody = $('#farmers-tbody');
    var countBadge = $('#farmers-count');

    var url = '/farmers?page=0&size=50';
    if (state.search) url += '&search=' + encodeURIComponent(state.search);
    if (state.status) url += '&status=' + encodeURIComponent(state.status);

    API.get(url)
      .then(function (res) {
        var farmers = res.content || [];
        countBadge.textContent = res.totalElements + ' producers';

        if (!farmers.length) {
          tbody.innerHTML = '<tr><td colspan="9" class="empty" style="padding:28px;text-align:center">No farmers found matching the criteria. Click "+ Register Farmer" to add one.</td></tr>';
          return;
        }

        tbody.innerHTML = farmers.map(function (f) {
          var isAct = f.status === 'ACTIVE';
          var statusPill = isAct ? '<span class="pill s-ok">Active</span>' : '<span class="pill s-crit">Inactive</span>';
          var locParts = [f.subCounty, f.county].filter(Boolean).join(', ') || '-';
          var depositsCount = f.totalDeposits || 0;
          var totalKg = f.totalQuantityDepositedKg ? Number(f.totalQuantityDepositedKg) : 0;

          return '<tr>' +
            '<td class="mono"><strong>' + esc(f.farmerCode) + '</strong></td>' +
            '<td><strong>' + esc(f.fullName) + '</strong></td>' +
            '<td class="mono muted">' + esc(f.nationalId || '-') + '</td>' +
            '<td class="mono">' + esc(f.phoneNumber) + '</td>' +
            '<td>' + esc(locParts) + '</td>' +
            '<td class="r mono">' + nf(depositsCount) + '</td>' +
            '<td class="r mono">' + nf(totalKg) + ' kg</td>' +
            '<td>' + statusPill + '</td>' +
            '<td style="text-align:center;white-space:nowrap">' +
            '<button class="btn btn-outline btn-sm" style="padding:4px 8px;font-size:12px;margin-right:4px" data-act="view-profile" data-id="' + f.id + '" type="button">View</button>' +
            '<button class="btn btn-outline btn-sm" style="padding:4px 8px;font-size:12px;' + (isAct ? 'color:var(--crit,#c00)' : 'color:var(--ok,#0F766E)') + '" data-act="toggle-status" data-id="' + f.id + '" data-current="' + f.status + '" type="button">' +
            (isAct ? 'Deactivate' : 'Activate') + '</button>' +
            '</td>' +
            '</tr>';
        }).join('');
      })
      .catch(function (err) {
        tbody.innerHTML = '<tr><td colspan="9" class="empty s-crit" style="padding:28px;text-align:center">Failed to load directory: ' + esc(err.message) + '</td></tr>';
      });
  }

  /* ---------- Search & Filters ---------- */
  var searchTimer = null;
  $('#farmer-search').addEventListener('input', function (e) {
    clearTimeout(searchTimer);
    searchTimer = setTimeout(function () {
      state.search = e.target.value.trim();
      loadFarmers();
    }, 300);
  });

  $('#farmer-status-filter').addEventListener('change', function (e) {
    state.status = e.target.value;
    loadFarmers();
  });

  /* ---------- Profile View Drawer ---------- */
  function openProfile(id) {
    var card = $('#farmer-profile-card');
    card.style.display = 'block';
    card.scrollIntoView({ behavior: 'smooth', block: 'start' });

    API.get('/farmers/' + id)
      .then(function (res) {
        var f = res.farmer;
        state.selectedFarmer = f;

        $('#prof-code').textContent = f.farmerCode;
        $('#prof-name').textContent = f.fullName;
        $('#prof-phone').textContent = f.phoneNumber;
        $('#prof-natid').textContent = f.nationalId || 'Not provided';
        $('#prof-email').textContent = f.email || 'None';
        $('#prof-location').textContent = [f.location, f.ward, f.subCounty, f.county].filter(Boolean).join(', ') || '-';
        $('#prof-regby').textContent = f.registeredByUsername || 'System';
        $('#prof-date').textContent = f.createdAt ? Z.fmtDate(new Date(f.createdAt).getTime()) : '-';

        var totalKg = f.totalQuantityDepositedKg ? Number(f.totalQuantityDepositedKg) : 0;
        $('#prof-total-kg').textContent = nf(totalKg) + ' kg (' + nf(totalKg / Z.KG) + ' bags)';
        $('#prof-total-deps').textContent = (f.totalDeposits || 0) + ' deposits';

        var toggleBtn = $('#prof-toggle-status');
        toggleBtn.textContent = f.status === 'ACTIVE' ? 'Deactivate Farmer' : 'Activate Farmer';
        toggleBtn.style.color = f.status === 'ACTIVE' ? 'var(--crit,#c00)' : 'var(--ok,#0F766E)';

        var depTbody = $('#prof-deposits-tbody');
        var deposits = res.recentDeposits || [];
        if (!deposits.length) {
          depTbody.innerHTML = '<tr><td colspan="7" class="empty" style="padding:16px;text-align:center">No deposits recorded for this producer yet.</td></tr>';
          return;
        }

        depTbody.innerHTML = deposits.map(function (d) {
          var kg = Number(d.quantity || 0);
          var bags = Math.max(1, Math.round(kg / Z.KG));
          var m = Number(d.moisturePercentage || 13.0);
          var dateStr = d.createdAt ? Z.fmtDate(new Date(d.createdAt).getTime()) : '-';
          var payStatus = d.status === 'ACCEPTED' ? 'Paid' : (d.status === 'REJECTED' ? 'Rejected' : 'Pending');

          return '<tr>' +
            '<td class="mono"><strong>' + esc(d.depositReference) + '</strong></td>' +
            '<td>' + esc(d.depotName || d.depotCode) + '</td>' +
            '<td>' + esc(d.grainTypeName) + '</td>' +
            '<td class="r mono">' + nf(kg) + ' kg (' + nf(bags) + ' bags)</td>' +
            '<td class="r mono">' + m.toFixed(1) + '%</td>' +
            '<td><span class="pill ' + (Z.PAY[payStatus] || 's-warn') + '">' + esc(payStatus) + '</span></td>' +
            '<td class="mono">' + dateStr + '</td>' +
            '</tr>';
        }).join('');
      })
      .catch(function (err) {
        alert('Failed to load profile: ' + err.message);
      });
  }

  /* ---------- Register Farmer Form ---------- */
  var modal = $('#farmer-modal');
  var form = $('#farmer-reg-form');
  var errBox = $('#farmer-form-err');
  var submitBtn = $('#fm-submit-btn');

  $('#btn-open-register').addEventListener('click', function () {
    modal.style.display = 'block';
    errBox.style.display = 'none';
    form.reset();
    modal.scrollIntoView({ behavior: 'smooth', block: 'start' });
    $('#fm-name').focus();
  });

  $('#btn-close-form').addEventListener('click', function () {
    modal.style.display = 'none';
  });

  $('#prof-close').addEventListener('click', function () {
    $('#farmer-profile-card').style.display = 'none';
  });

  form.addEventListener('submit', function (e) {
    e.preventDefault();
    errBox.style.display = 'none';

    var nameVal = $('#fm-name').value.trim();
    var phoneVal = $('#fm-phone').value.trim();

    if (nameVal.length < 2) {
      errBox.textContent = 'Enter the farmer’s full legal name.';
      errBox.style.display = 'flex';
      $('#fm-name').focus();
      return;
    }
    if (!/^(07|01|\+254)\d{8,9}$/.test(phoneVal.replace(/[\s-]/g, ''))) {
      errBox.textContent = 'Enter a valid 10-digit phone number (e.g. 0711000003).';
      errBox.style.display = 'flex';
      $('#fm-phone').focus();
      return;
    }

    submitBtn.disabled = true;
    submitBtn.textContent = 'Registering…';

    var payload = {
      fullName: nameVal,
      nationalId: $('#fm-natid').value.trim() || null,
      phoneNumber: phoneVal.replace(/[\s-]/g, ''),
      email: $('#fm-email').value.trim().toLowerCase() || null,
      county: $('#fm-county').value.trim() || 'Trans Nzoia',
      subCounty: $('#fm-subcounty').value.trim() || null,
      ward: $('#fm-ward').value.trim() || null,
      location: $('#fm-loc').value.trim() || null
    };

    API.post('/farmers', payload)
      .then(function (res) {
        U.toast('Farmer registered: ' + res.farmerCode);
        modal.style.display = 'none';
        loadFarmers();
        openProfile(res.id);
      })
      .catch(function (err) {
        errBox.textContent = err.message || 'Registration failed. Check for duplicate phone or national ID.';
        errBox.style.display = 'flex';
      })
      .finally(function () {
        submitBtn.disabled = false;
        submitBtn.textContent = 'Save Farmer Record';
      });
  });

  /* ---------- Action Buttons Handler ---------- */
  document.addEventListener('click', function (e) {
    var t = e.target.closest('button');
    if (!t) return;

    var act = t.dataset.act;
    var id = t.dataset.id;

    if (act === 'view-profile' && id) {
      openProfile(id);
    }

    if (act === 'toggle-status' && id) {
      var cur = t.dataset.current;
      var next = cur === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
      var confirmMsg = next === 'INACTIVE'
        ? 'Are you sure you want to deactivate this farmer? Inactive farmers cannot submit new deposits.'
        : 'Reactivate this farmer account?';

      if (!confirm(confirmMsg)) return;

      t.disabled = true;
      API.patch('/farmers/' + id + '/status?status=' + next, {})
        .then(function () {
          U.toast('Farmer status updated to ' + next);
          loadFarmers();
          if (state.selectedFarmer && String(state.selectedFarmer.id) === String(id)) {
            openProfile(id);
          }
        })
        .catch(function (err) {
          t.disabled = false;
          alert('Failed to update status: ' + err.message);
        });
    }

    if (t.id === 'prof-toggle-status' && state.selectedFarmer) {
      var f = state.selectedFarmer;
      var nextStatus = f.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
      if (!confirm('Change status of ' + f.fullName + ' to ' + nextStatus + '?')) return;
      t.disabled = true;
      API.patch('/farmers/' + f.id + '/status?status=' + nextStatus, {})
        .then(function () {
          U.toast('Farmer status updated to ' + nextStatus);
          loadFarmers();
          openProfile(f.id);
        })
        .catch(function (err) {
          t.disabled = false;
          alert('Failed to update status: ' + err.message);
        });
    }
  });

  Z.ready(function () {
    loadFarmers();
  });
})();
