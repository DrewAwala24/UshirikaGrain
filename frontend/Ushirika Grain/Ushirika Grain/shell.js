/* ==========================================================================
   Ushirika Grain: shared page behaviour
   Mobile menu, log in / sign out in the header, access control for the
   Dashboard / Intake / Deposits pages, toast messages, receipt (ticket) markup.
   ========================================================================== */
(function () {
  'use strict';
  var Z = window.Zuri;
  var main = document.querySelector('[data-page]');
  var page = main ? main.getAttribute('data-page') : '';

  /* Pages marked data-protected need a signed-in user. */
  if (main && main.hasAttribute('data-protected') && !Z.session.get()) {
    window.ZuriBlocked = true;
    Z.session.setNext((location.pathname.split('/').pop() || 'index.html'));
    location.replace('login.html');
    return;
  }

  /* ---------- icons (same set the static pages use) ---------- */
  var ICONS = {
    check: '<path d="M5 12.5l4.5 4.5L19 7.500"/>',
    shield: '<path d="M12 3l7 3v5c0 4.500-3 8-7 10-4-2-7-5.500-7-10V6z"/><path d="M9 12l2 2 4-4"/>',
    gauge: '<path d="M4 16a8 8 0 1 1 16 0"/><path d="M12 16l4-5"/><circle cx="12" cy="16" r="1.2"/>',
    droplet: '<path d="M12 3s6 6.200 6 10.500A6 6 0 0 1 6 13.500C6 9.200 12 3 12 3z"/>',
    clip: '<rect x="6" y="4" width="12" height="17" rx="2"/><path d="M9 4h6v3H9z"/><path d="M9 12h6M9 16h4"/>',
    chart: '<path d="M4 20V4"/><path d="M4 20h16"/><path d="M8 15l4-4 3 3 5-6"/>',
    search: '<circle cx="11" cy="11" r="6"/><path d="M20 20l-4.200-4.200"/>',
    info: '<circle cx="12" cy="12" r="9"/><path d="M12 11v5"/><path d="M12 8h.01"/>',
    bag: '<path d="M6 8h12l1 12H5z"/><path d="M9 8V6a3 3 0 0 1 6 0v2"/>',
    alert: '<path d="M12 4l9 16H3z"/><path d="M12 10v4"/><path d="M12 17h.01"/>'
  };
  function icon(name, size) {
    return '<svg class="ic" viewBox="0 0 24 24" aria-hidden="true"' + (size ? ' style="width:' + size + 'px;height:' + size + 'px"' : '') + '>' + (ICONS[name] || '') + '</svg>';
  }

  /* ---------- toast ---------- */
  var toastEl, toastTimer;
  function toast(msg) {
    if (!toastEl) {
      toastEl = document.createElement('div');
      toastEl.id = 'toast';
      toastEl.setAttribute('role', 'status');
      toastEl.innerHTML = '<span></span>';
      document.body.appendChild(toastEl);
    }
    toastEl.firstChild.textContent = msg;
    toastEl.classList.add('show');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(function () { toastEl.classList.remove('show'); }, 2600);
  }

  /* ---------- copy to clipboard with a visible fallback ---------- */
  function copy(text, btn, fallbackHost) {
    var done = function () { toast('Copied'); if (btn) btn.textContent = 'Copied'; };
    var fail = function () {
      if (fallbackHost && !fallbackHost.firstChild) {
        var i = document.createElement('input');
        i.className = 'control fallback'; i.readOnly = true; i.value = text;
        fallbackHost.appendChild(i); i.focus(); i.select();
      }
      if (btn) btn.textContent = 'Select and copy';
    };
    try { navigator.clipboard.writeText(text).then(done, fail); } catch (e) { fail(); }
  }

  /* ---------- receipt ticket ---------- */
  function riskPill(r) {
    var L = Z.LV[r.lv];
    return '<span class="pill ' + L.cls + '">' + L.t + ' · ' + r.s + '</span>';
  }
  function ticket(d) {
    var r = Z.riskOf(d), L = Z.LV[r.lv], ok = Z.chainOk(d);
    return '<article class="ticket"><div class="ticket-main">' +
      '<div class="ticket-top"><span class="ticket-brand">Ushirika Grain · Grain receipt</span><span class="pill ' + (ok ? 's-ok' : 's-crit') + '">' + (ok ? 'Signed' : 'Altered') + '</span></div>' +
      '<div class="ticket-id">' + Z.esc(d.id) + '</div>' +
      '<dl class="ticket-grid">' +
      '<div><dt>Farmer</dt><dd>' + Z.esc(d.farmer) + '</dd></div>' +
      '<div><dt>Phone</dt><dd class="mono">07•• ••• ' + Z.esc(d.phone) + '</dd></div>' +
      '<div><dt>Depot</dt><dd>' + Z.depotName(d.depot) + '</dd></div>' +
      '<div><dt>Produce</dt><dd>' + Z.esc(d.crop) + '</dd></div>' +
      '<div><dt>Weight</dt><dd class="mono">' + Z.nf(d.bags) + ' bags · ' + Z.nf(d.bags * Z.KG) + ' kg</dd></div>' +
      '<div><dt>Moisture</dt><dd class="mono">' + d.m.toFixed(1) + '%</dd></div>' +
      '<div><dt>Received</dt><dd>' + Z.fmtDate(d.ts) + '</dd></div>' +
      '<div><dt>Payout</dt><dd><span class="pill ' + (Z.PAY[d.pay] || 's-warn') + '">' + d.pay + '</span></dd></div>' +
      '</dl></div>' +
      '<div class="ticket-stub"><div><span class="label">Signature</span><span class="mono">' + d.hash.slice(0, 10) + '</span></div>' +
      '<div style="text-align:right"><span class="label">Spoilage risk</span><span class="pill ' + L.cls + '" style="margin-top:2px">' + L.t + ' · ' + r.s + '</span></div></div></article>';
  }

  /* ---------- header: menu and account ---------- */
  var header = document.querySelector('.site-header');
  var toggle = document.getElementById('nav-toggle');
  if (header && toggle) {
    toggle.addEventListener('click', function () {
      var open = header.classList.toggle('nav-open');
      toggle.setAttribute('aria-expanded', String(open));
    });
    document.addEventListener('keydown', function (e) {
      if (e.key === 'Escape' && header.classList.contains('nav-open')) {
        header.classList.remove('nav-open'); toggle.setAttribute('aria-expanded', 'false'); toggle.focus();
      }
    });
  }
  function renderAuth() {
    var slot = document.getElementById('auth-slot');
    var navLinks = document.querySelector('.nav-links');
    var u = Z.session.get();

    if (navLinks && u && u.roles && (u.roles.includes('ROLE_ADMIN') || u.roles.includes('ROLE_DEPOT_MANAGER'))) {
      if (!document.getElementById('nav-farmers-link')) {
        var liF = document.createElement('li');
        liF.id = 'nav-farmers-link';
        liF.innerHTML = '<a href="farmers.html"' + (page === 'farmers' ? ' aria-current="page"' : '') + '>Farmers</a>';
        navLinks.appendChild(liF);
      }
      if (!document.getElementById('nav-admin-link')) {
        var liA = document.createElement('li');
        liA.id = 'nav-admin-link';
        liA.innerHTML = '<a href="admin.html"' + (page === 'admin' ? ' aria-current="page"' : '') + '>Admin</a>';
        navLinks.appendChild(liA);
      }
    }

    if (!slot) return;
    if (!u) {
      slot.innerHTML = '<a class="btn btn-outline btn-sm" href="register.html" style="margin-right:8px">Register</a><a class="btn btn-primary btn-sm" href="login.html">Log in</a>';
      return;
    }
    var ini = (u.name || u.username || 'U').split(' ').map(function (p) { return p.charAt(0); }).slice(0, 2).join('').toUpperCase();
    slot.innerHTML = '<span class="user"><span class="avatar" aria-hidden="true">' + Z.esc(ini) + '</span><span>' + Z.esc(u.name || u.username) + '</span></span>' +
      '<button class="btn btn-outline btn-sm" id="signout" type="button">Sign out</button>';
    document.getElementById('signout').addEventListener('click', function () {
      if (window.ZuriAPI) window.ZuriAPI.logout();
      else Z.session.clear();
      toast('You have been signed out');
      setTimeout(function () { location.href = 'index.html'; }, 700);
    });
  }
  renderAuth();

  var yr = document.getElementById('year');
  if (yr) yr.textContent = new Date().getFullYear();

  window.ZuriUI = { icon: icon, toast: toast, copy: copy, ticket: ticket, riskPill: riskPill, page: page };
})();
