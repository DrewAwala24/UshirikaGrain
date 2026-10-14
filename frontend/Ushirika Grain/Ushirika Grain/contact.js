/* Contact: validated message form. No message is sent until a backend endpoint is connected. */
(function () {
  'use strict';
  var form = document.getElementById('contact-form');
  if (!form) return;
  var $ = function (s) { return document.querySelector(s); };
  var rules = {
    name: function (v) { return v.trim().length < 2 ? 'Enter your name.' : ''; },
    email: function (v) { return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v.trim()) ? '' : 'Enter a valid email address.'; },
    phone: function (v) { v = v.replace(/[\s-]/g, ''); return !v || /^(\+254|0)(7|1)\d{8}$/.test(v) ? '' : 'Use a Kenyan number, such as 0712 345 678.'; },
    topic: function (v) { return v ? '' : 'Choose what your message is about.'; },
    message: function (v) { return v.trim().length < 15 ? 'Write at least a sentence so we can help.' : ''; }
  };
  function check(k) {
    var el = $('#c-' + k), f = el.closest('.field'), msg = rules[k](el.value);
    f.classList.toggle('invalid', !!msg);
    f.querySelector('.field-err').textContent = msg;
    el.setAttribute('aria-invalid', msg ? 'true' : 'false');
    return !msg;
  }
  Object.keys(rules).forEach(function (k) {
    var el = $('#c-' + k);
    el.addEventListener('blur', function () { check(k); });
    el.addEventListener('input', function () { if (el.closest('.invalid')) check(k); });
  });
  var count = $('#c-count'), msgEl = $('#c-message');
  msgEl.addEventListener('input', function () { count.textContent = msgEl.value.length + ' / 600'; });

  form.addEventListener('submit', function (e) {
    e.preventDefault();
    var ok = true;
    Object.keys(rules).forEach(function (k) { if (!check(k)) ok = false; });
    var consent = $('#c-consent'), cf = consent.closest('.field');
    cf.classList.toggle('invalid', !consent.checked);
    if (!consent.checked) ok = false;
    if (!ok) { var first = form.querySelector('.invalid .control, .invalid input'); if (first) first.focus(); return; }
    $('#contact-name').textContent = $('#c-name').value.trim().split(' ')[0];
    $('#contact-form-wrap').hidden = true;
    $('#contact-success').hidden = false;
    $('#contact-success').scrollIntoView({ block: 'center' });
  });
  document.getElementById('contact-again').addEventListener('click', function () {
    form.reset(); count.textContent = '0 / 600';
    $('#contact-success').hidden = true; $('#contact-form-wrap').hidden = false;
    $('#c-name').focus();
  });
})();
