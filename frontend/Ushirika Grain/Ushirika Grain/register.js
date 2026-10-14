/* Register: Client validation and API integration */
(function () {
  'use strict';
  var Z = window.Zuri, U = window.ZuriUI, API = window.ZuriAPI;
  if (!U || !API) return;
  var $ = function (s) { return document.querySelector(s); };

  var form = $('#register-form');
  var fullName = $('#r-name');
  var username = $('#r-username');
  var phone = $('#r-phone');
  var email = $('#r-email');
  var pass = $('#r-pass');
  var passConfirm = $('#r-pass-confirm');
  var agree = $('#r-agree');
  var btn = $('#reg-btn');
  var globalErr = $('#reg-global-err');
  var successBox = $('#reg-success');

  function setErr(input, msg) {
    var f = input.closest('.field');
    if (!f) return !msg;
    f.classList.toggle('invalid', !!msg);
    var err = f.querySelector('.field-err');
    if (err) err.textContent = msg;
    input.setAttribute('aria-invalid', msg ? 'true' : 'false');
    return !msg;
  }

  var checks = {
    fullName: function () {
      var v = fullName.value.trim();
      return setErr(fullName, v.length >= 2 ? '' : 'Enter your full legal name.');
    },
    username: function () {
      var v = username.value.trim();
      return setErr(username, /^[a-zA-Z0-9_-]{3,50}$/.test(v) ? '' : 'Username must be 3-50 letters, numbers, or dashes.');
    },
    phone: function () {
      var v = phone.value.replace(/[\s-]/g, '');
      return setErr(phone, /^(07|01|\+254)\d{8,9}$/.test(v) ? '' : 'Enter a valid 10-digit mobile number.');
    },
    email: function () {
      var v = email.value.trim();
      return setErr(email, /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v) ? '' : 'Enter a valid email address.');
    },
    pass: function () {
      return setErr(pass, pass.value.length >= 6 ? '' : 'Password must be at least 6 characters.');
    },
    passConfirm: function () {
      return setErr(passConfirm, passConfirm.value === pass.value ? '' : 'Passwords do not match.');
    }
  };

  fullName.addEventListener('blur', checks.fullName);
  username.addEventListener('blur', checks.username);
  phone.addEventListener('blur', checks.phone);
  email.addEventListener('blur', checks.email);
  pass.addEventListener('blur', checks.pass);
  passConfirm.addEventListener('blur', checks.passConfirm);

  form.addEventListener('submit', function (e) {
    e.preventDefault();
    globalErr.style.display = 'none';
    successBox.style.display = 'none';

    var valid = true;
    Object.keys(checks).forEach(function (k) {
      if (!checks[k]()) valid = false;
    });

    if (!agree.checked) {
      globalErr.textContent = 'Please accept the cooperative terms to continue.';
      globalErr.style.display = 'flex';
      return;
    }

    if (!valid) return;

    btn.disabled = true;
    btn.textContent = 'Creating account…';

    var payload = {
      username: username.value.trim(),
      email: email.value.trim().toLowerCase(),
      password: pass.value,
      fullName: fullName.value.trim(),
      phoneNumber: phone.value.replace(/[\s-]/g, '')
    };

    API.post('/auth/register', payload)
      .then(function () {
        successBox.textContent = 'Account created successfully! Logging you in…';
        successBox.style.display = 'flex';
        // Auto-login with the new credentials
        return API.login(payload.username, payload.password, true);
      })
      .then(function () {
        if (U.toast) U.toast('Welcome to Ushirika Grain');
        setTimeout(function () {
          location.href = 'dashboard.html';
        }, 800);
      })
      .catch(function (err) {
        btn.disabled = false;
        btn.textContent = 'Create account';
        globalErr.textContent = err.message || 'Registration failed. Please check your information.';
        globalErr.style.display = 'flex';
      });
  });
})();
