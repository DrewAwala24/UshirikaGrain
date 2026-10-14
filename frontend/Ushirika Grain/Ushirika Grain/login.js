/* Log in: Authenticates against ZuriAgro Spring Boot JWT API. */
(function () {
  'use strict';
  var Z = window.Zuri, U = window.ZuriUI, API = window.ZuriAPI;
  if (!U || !API) return;
  var $ = function (s) { return document.querySelector(s); };
  var form = $('#login-form');
  var email = $('#l-email'), pass = $('#l-pass');

  if (Z.session.get()) { location.replace(Z.session.takeNext() || 'dashboard.html'); return; }

  function setErr(input, msg) {
    var f = input.closest('.field');
    if (!f) return !msg;
    f.classList.toggle('invalid', !!msg);
    var errEl = f.querySelector('.field-err');
    if (errEl) errEl.textContent = msg;
    input.setAttribute('aria-invalid', msg ? 'true' : 'false');
    return !msg;
  }

  function setGlobalErr(msg) {
    var existing = $('#login-global-err');
    if (!existing) {
      existing = document.createElement('div');
      existing.id = 'login-global-err';
      existing.className = 'notice s-crit';
      existing.style.marginBottom = '16px';
      form.insertBefore(existing, form.firstChild);
    }
    if (msg) {
      existing.style.display = 'flex';
      existing.innerHTML = '<svg class="ic" viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="9"/><path d="M12 8v4M12 16h.01"/></svg><span>' + Z.esc(msg) + '</span>';
    } else {
      existing.style.display = 'none';
    }
  }

  var checks = {
    email: function () {
      var val = email.value.trim();
      return setErr(email, val.length > 0 ? '' : 'Enter your username or email address.');
    },
    pass: function () {
      return setErr(pass, pass.value.length >= 6 ? '' : 'Your password must be at least 6 characters.');
    }
  };

  email.addEventListener('blur', checks.email);
  pass.addEventListener('blur', checks.pass);
  email.addEventListener('input', function () { if (email.closest('.invalid')) checks.email(); });
  pass.addEventListener('input', function () { if (pass.closest('.invalid')) checks.pass(); });

  var revealBtn = $('#reveal');
  if (revealBtn) {
    revealBtn.addEventListener('click', function (e) {
      var show = pass.type === 'password';
      pass.type = show ? 'text' : 'password';
      e.currentTarget.textContent = show ? 'Hide' : 'Show';
      e.currentTarget.setAttribute('aria-pressed', String(show));
    });
  }

  form.addEventListener('submit', function (e) {
    e.preventDefault();
    setGlobalErr('');
    var a = checks.email(), b = checks.pass();
    if (!(a && b)) { (a ? pass : email).focus(); return; }

    var btn = $('#login-btn');
    var remember = $('#l-remember') ? $('#l-remember').checked : true;
    btn.disabled = true;
    btn.textContent = 'Signing in…';

    API.login(email.value.trim(), pass.value, remember)
      .then(function () {
        if (U.toast) U.toast('Signed in successfully');
        var dest = Z.session.takeNext() || 'dashboard.html';
        location.href = dest;
      })
      .catch(function (err) {
        btn.disabled = false;
        btn.textContent = 'Log in';
        var errorMsg = err.message || 'Invalid username or password.';
        setGlobalErr(errorMsg);
      });
  });
})();
