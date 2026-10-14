/* New intake: validated form, live AI check, backend depot picker, receipt on save. */
(function () {
  'use strict';
  if (window.ZuriBlocked) return;
  var Z = window.Zuri, U = window.ZuriUI, esc = Z.esc, nf = Z.nf;
  var $ = function (s) { return document.querySelector(s); };
  var form = $('#intake-form');
  var state = { depot: 'ELD-DEP-01', lastId: null };

  var val = {
    name: function () { return $('#f-name').value.trim(); },
    phone: function () { return $('#f-phone').value.replace(/[\s-]/g, ''); },
    sub: function () { return $('#f-sub').value; },
    crop: function () {
      var checked = form.querySelector('input[name="produce"]:checked');
      return checked ? checked.value : 'Grade 1 White Maize';
    },
    bags: function () { return parseInt($('#f-bags').value, 10); },
    moist: function () { return parseFloat($('#f-moist').value); }
  };

  /* ---------- validation ---------- */
  var rules = {
    name: function () { var v = val.name(); return v.length < 3 || !/[a-zA-Z]/.test(v) ? 'Enter the farmer’s full name.' : ''; },
    phone: function () { return /^(07|01|\+254)\d{8,9}$/.test(val.phone()) ? '' : 'Enter a 10-digit number (e.g. 0712345678).'; },
    sub: function () { return val.sub() ? '' : 'Choose the farmer’s sub-county.'; },
    bags: function () { var v = val.bags(); return v >= 1 && v <= 50000 ? '' : 'Enter a whole number of bags (1 or more).'; },
    moist: function () { var v = val.moist(); return v >= 5 && v <= 30 ? '' : 'Enter a moisture reading between 5% and 30%.'; }
  };

  function check(key) {
    var f = form.querySelector('[data-field="' + key + '"]');
    if (!f) return true;
    var msg = rules[key]();
    f.classList.toggle('invalid', !!msg);
    var err = f.querySelector('.field-err');
    if (err) err.textContent = msg;
    var c = f.querySelector('.control');
    if (c) c.setAttribute('aria-invalid', msg ? 'true' : 'false');
    return !msg;
  }

  Object.keys(rules).forEach(function (k) {
    var f = form.querySelector('[data-field="' + k + '"]');
    if (!f) return;
    var el = f.querySelector('.control');
    if (!el) return;
    el.addEventListener('blur', function () { check(k); });
    el.addEventListener('input', function () { if (el.closest('.invalid')) check(k); });
  });

  /* ---------- grain types & sub-counties ---------- */
  function renderGrains() {
    var seg = form.querySelector('.seg');
    if (!seg || !Z.GRAIN_TYPES || !Z.GRAIN_TYPES.length) return;
    seg.innerHTML = Z.GRAIN_TYPES.map(function (g, i) {
      var id = 'p-' + g.code.toLowerCase();
      var label = g.name.replace(/^(Grade 1 |Hard Red Winter |Yellow |White |Long Grain )/, '');
      return '<div><input type="radio" name="produce" id="' + id + '" value="' + esc(g.name) + '"' + (i === 0 ? ' checked' : '') + '>' +
        '<label for="' + id + '">' + esc(label) + '</label></div>';
    }).join('');
  }

  function renderSubCounties() {
    var subEl = $('#f-sub');
    if (!subEl) return;
    var subs = Z.DEPOTS.map(function (d) { return d.name; }).concat(['Cherangany', 'Kiminini', 'Kwanza', 'Saboti', 'Endebess']);
    var unique = Array.from(new Set(subs));
    subEl.innerHTML = '<option value="">Choose sub-county / depot area</option>' + unique.map(function (s) {
      return '<option value="' + esc(s) + '">' + esc(s) + '</option>';
    }).join('');
  }

  /* ---------- depot picker ---------- */
  function renderDepots() {
    var D = Z.DEPOTS;
    var bags = val.bags() || 0;
    var optsEl = $('#depot-opts');
    if (!optsEl) return;
    optsEl.innerHTML = D.map(function (d) {
      var m = Z.metrics(d), s = Z.ST[m.st], free = Math.max(0, d.cap - d.stored), tooSmall = bags > free;
      var isChecked = state.depot === d.id || state.depot === d.code;
      return '<div><input type="radio" name="depot" id="d-' + d.id + '" value="' + d.id + '"' + (isChecked ? ' checked' : '') + '>' +
        '<label for="d-' + d.id + '" class="' + s.cls + (tooSmall ? ' is-full' : '') + '">' +
        '<span class="opt-top"><span>' + esc(d.name) + '</span><span class="pill ' + s.cls + '">' + s.t + '</span></span>' +
        '<span class="mini-bar"><i style="width:' + Math.min(100, Math.round(m.util * 100)) + '%"></i></span>' +
        '<span class="opt-meta mono">' + nf(free) + ' bags free' + (tooSmall ? ' · too small' : '') + '</span></label></div>';
    }).join('');
  }

  $('#depot-opts').addEventListener('change', function (e) {
    if (e.target.name === 'depot') {
      state.depot = e.target.value;
      renderCheck();
    }
  });

  /* ---------- live AI check ---------- */
  function scaleHTML(m) {
    var p = Math.max(0, Math.min(1, (m - 11) / 9)) * 100;
    return '<div class="scale" aria-hidden="true"><i style="left:' + p.toFixed(1) + '%"></i></div><div class="scale-l"><span>11%</span><span>13.5% safe limit</span><span>20%</span></div>';
  }

  function renderCheck() {
    var m = val.moist(), bags = val.bags(), dep = Z.depotById(state.depot), h = '';
    if (!dep) return;
    if (m > 0) {
      var sc = Z.score(m, 14), L = Z.LV[Z.level(sc)];
      h += '<div class="check-sec"><div class="label">Spoilage risk after 14 days in store</div>' +
        '<div class="check-top"><span class="big mono">' + sc + '</span><span class="pill ' + L.cls + '">' + L.t + ' risk</span></div>' +
        '<p style="margin-top:12px">' + (m > Z.LIMIT ? 'At <strong class="mono">' + m.toFixed(1) + '%</strong> moisture this lot is above the safe limit. ' : 'At <strong class="mono">' + m.toFixed(1) + '%</strong> moisture this lot is within the safe limit. ') +
        '<strong>' + L.act + '.</strong></p>' + scaleHTML(m) + '</div>';
    } else {
      h += '<div class="check-sec"><p class="muted">Enter the moisture reading to see the spoilage forecast.</p></div>';
    }
    var free = Math.max(0, dep.cap - dep.stored);
    h += '<div class="check-sec"><div class="label">Depot space</div><p style="margin-top:8px"><strong>' + esc(dep.name) + '</strong> has <span class="mono">' + nf(free) + '</span> bags free.</p>';
    if (bags > 0) {
      if (bags > free) {
        var alt = Z.DEPOTS.filter(function (x) { return x.id !== dep.id; }).sort(function (a, b) { return (b.cap - b.stored) - (a.cap - a.stored); })[0];
        h += '<p style="margin-top:10px"><span class="pill s-crit">Over capacity by ' + nf(bags - free) + ' bags</span></p>' +
          (alt ? '<p style="margin-top:10px">Try <strong>' + esc(alt.name) + '</strong>, which has ' + nf(Math.max(0, alt.cap - alt.stored)) + ' bags free.</p>' : '');
      } else {
        var after = dep.cap > 0 ? (dep.stored + bags) / dep.cap : 0;
        h += '<p style="margin-top:10px"><span class="pill ' + (after >= .9 ? 's-warn' : 's-ok') + '">' + Math.round(after * 100) + '% full after this delivery</span></p>';
      }
    }
    $('#check').innerHTML = h + '</div>';
  }

  function updateKg() {
    var b = val.bags();
    $('#bags-kg').textContent = b > 0 ? '= ' + nf(b * Z.KG) + ' kg' : 'Each bag is 90 kg';
  }

  ['f-bags', 'f-moist'].forEach(function (id) {
    var el = $('#' + id);
    if (el) el.addEventListener('input', function () { updateKg(); renderDepots(); renderCheck(); });
  });

  /* ---------- submit ---------- */
  form.addEventListener('submit', function (e) {
    e.preventDefault();
    var box = $('#form-error');
    var ok = true;
    Object.keys(rules).forEach(function (k) { if (!check(k)) ok = false; });
    if (!ok) {
      box.textContent = 'Fix the highlighted fields and save again.';
      box.classList.add('show');
      var first = form.querySelector('.invalid .control');
      if (first) first.focus();
      return;
    }

    var dep = Z.depotById(state.depot), bags = val.bags();
    var free = Math.max(0, dep.cap - dep.stored);
    if (free > 0 && bags > free) {
      box.textContent = dep.name + ' depot has only ' + nf(free) + ' bags of space. Choose another depot.';
      box.classList.add('show');
      $('#depot-opts').scrollIntoView({ block: 'center' });
      return;
    }
    box.classList.remove('show');

    var submitBtn = form.querySelector('button[type="submit"]');
    if (submitBtn) {
      submitBtn.disabled = true;
      submitBtn.textContent = 'Recording deposit…';
    }

    var intakeData = {
      farmer: val.name(),
      phone: val.phone(),
      sub: val.sub(),
      depot: state.depot,
      crop: val.crop(),
      bags: bags,
      m: val.moist(),
      farmerRecordId: state.farmerRecordId || null
    };

    Z.recordIntake(intakeData)
      .then(function (d) {
        state.lastId = d.id;
        $('#success-ticket').innerHTML = '<div class="ticket-wrap">' + U.ticket(d) + '</div>';
        $('#intake-form-wrap').hidden = true;
        $('#intake-success').hidden = false;
        window.scrollTo(0, 0);
        U.toast('Deposit recorded successfully');
      })
      .catch(function (err) {
        box.textContent = 'Error recording deposit: ' + (err.message || 'Server error');
        box.classList.add('show');
      })
      .finally(function () {
        if (submitBtn) {
          submitBtn.disabled = false;
          submitBtn.textContent = 'Record deposit';
        }
      });
  });

  document.addEventListener('click', function (e) {
    var t = e.target.closest('[data-act]');
    if (!t) return;
    if (t.dataset.act === 'another') {
      state.farmerRecordId = null;
      var sel = $('#f-registered-select');
      if (sel) sel.value = '';
      form.querySelectorAll('[data-field="name"] .control, [data-field="phone"] .control').forEach(function (c) { c.value = ''; });
      Object.keys(rules).forEach(function (k) {
        var f = form.querySelector('[data-field="' + k + '"]');
        if (f) f.classList.remove('invalid');
      });
      $('#intake-success').hidden = true;
      $('#intake-form-wrap').hidden = false;
      renderDepots();
      renderCheck();
      $('#f-name').focus();
    }
    if (t.dataset.act === 'view' && state.lastId) {
      Z.setLookup(state.lastId);
      location.href = 'deposits.html';
    }
    if (t.dataset.act === 'clear') {
      state.farmerRecordId = null;
      form.reset();
      Object.keys(rules).forEach(function (k) {
        var f = form.querySelector('[data-field="' + k + '"]');
        if (f) f.classList.remove('invalid');
      });
      $('#form-error').classList.remove('show');
      state.depot = (Z.DEPOTS[0] && Z.DEPOTS[0].id) || 'ELD-DEP-01';
      updateKg();
      renderDepots();
      renderCheck();
    }
  });

  function setupRegisteredFarmers() {
    var sel = $('#f-registered-select');
    if (!sel || !window.ZuriAPI) return;

    window.ZuriAPI.get('/farmers?status=ACTIVE&size=100')
      .then(function (res) {
        var farmers = res.content || [];
        state.loadedFarmers = farmers;
        if (!farmers.length) return;

        sel.innerHTML = '<option value="">-- Choose registered farmer or enter below --</option>' +
          farmers.map(function (f) {
            return '<option value="' + f.id + '">' + esc(f.fullName) + ' (' + esc(f.farmerCode) + ' · ' + esc(f.phoneNumber) + ')</option>';
          }).join('');

        sel.addEventListener('change', function () {
          var chosenId = sel.value;
          if (!chosenId) {
            state.farmerRecordId = null;
            return;
          }
          var match = state.loadedFarmers.filter(function (x) { return String(x.id) === String(chosenId); })[0];
          if (match) {
            state.farmerRecordId = match.id;
            $('#f-name').value = match.fullName;
            $('#f-phone').value = match.phoneNumber;
            if (match.subCounty) $('#f-sub').value = match.subCounty;
            form.querySelectorAll('[data-field]').forEach(function (f) { f.classList.remove('invalid'); });
          }
        });
      })
      .catch(function () {
        /* Ignore if unauthenticated or offline */
      });
  }

  Z.ready(function () {
    if (Z.DEPOTS.length) state.depot = Z.DEPOTS[0].id;
    renderGrains();
    renderSubCounties();
    updateKg();
    renderDepots();
    renderCheck();
    setupRegisteredFarmers();
  });
})();
