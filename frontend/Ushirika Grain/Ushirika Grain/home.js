/* Home page: farmer receipt lookup (public, no log in needed). */
(function () {
  'use strict';
  var Z = window.Zuri, U = window.ZuriUI, API = window.ZuriAPI;
  var form = document.getElementById('lookup-form');
  if (!form || !U) return;
  var input = document.getElementById('lookup-id');
  var out = document.getElementById('lookup-result');
  var err = document.getElementById('lookup-err');
  var field = input.closest('.field');

  function empty() {
    out.innerHTML = '<div class="lookup-empty">' + U.icon('search') + '<p><strong>Your receipt will show here.</strong><br>Enter the tracking ID printed on your ticket or sent by SMS.</p></div>';
  }

  function show(d) {
    out.innerHTML = '<div class="ticket-wrap">' + U.ticket(d) + '</div>';
  }

  function run(id) {
    id = String(id || '').trim();
    if (!id) {
      field.classList.add('invalid');
      err.textContent = 'Enter the tracking ID from your ticket.';
      empty();
      return;
    }

    var localMatch = Z.find(id);
    if (localMatch) {
      field.classList.remove('invalid');
      show(localMatch);
      return;
    }

    // Query backend directly
    if (API) {
      API.get('/deposits/reference/' + encodeURIComponent(id))
        .then(function (res) {
          field.classList.remove('invalid');
          var d = {
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
          show(d);
        })
        .catch(function () {
          field.classList.add('invalid');
          err.textContent = 'No deposit found with tracking ID "' + id + '". Check the ID and try again.';
          empty();
        });
    } else {
      field.classList.add('invalid');
      err.textContent = 'No deposit has that ID. Check the ID on your ticket and try again.';
      empty();
    }
  }

  form.addEventListener('submit', function (e) {
    e.preventDefault();
    run(input.value);
  });

  input.addEventListener('input', function () {
    field.classList.remove('invalid');
  });

  var sampleBtn = document.getElementById('lookup-sample');
  if (sampleBtn) {
    sampleBtn.addEventListener('click', function () {
      if (Z.DEPOSITS.length) {
        input.value = Z.DEPOSITS[0].id;
        run(Z.DEPOSITS[0].id);
      } else {
        input.value = 'DEP-';
        input.focus();
      }
    });
  }

  empty();
})();
