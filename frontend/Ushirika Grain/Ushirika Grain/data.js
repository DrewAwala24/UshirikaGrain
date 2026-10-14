/* ==========================================================================
   Ushirika Grain / ZuriAgro: Data layer and business logic
   Integrated with Spring Boot REST API (/api/v1) with offline fallback.
   ========================================================================== */
(function (w) {
  'use strict';

  var KG = 90;          // kilograms in one bag
  var LIMIT = 13.5;     // safe storage moisture limit, percent
  var DAY = 864e5;
  var NOW = Math.floor(Date.now() / 36e5) * 36e5;

  /* ---------- small helpers ---------- */
  var pad = function (n) { return String(n).padStart(2, '0'); };
  var nf = function (n) { return Math.round(n).toLocaleString('en-US'); };
  var esc = function (s) {
    return String(s || '').replace(/[&<>"']/g, function (c) {
      return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
    });
  };
  var fmtDay = function (ts) { return new Date(ts).toLocaleDateString('en-GB', { day: 'numeric', month: 'short' }); };
  var fmtDate = function (ts) { var d = new Date(ts); return fmtDay(ts) + ', ' + pad(d.getHours()) + ':' + pad(d.getMinutes()); };
  var sum = function (a, f) { return a.reduce(function (t, x) { return t + f(x); }, 0); };

  function makeStore(kind) {
    var real = null, mem = {};
    try { real = w[kind]; real.setItem('__z', '1'); real.removeItem('__z'); } catch (e) { real = null; }
    return {
      get: function (k) { try { return real ? real.getItem(k) : (k in mem ? mem[k] : null); } catch (e) { return null; } },
      set: function (k, v) { try { if (real) real.setItem(k, v); else mem[k] = v; } catch (e) { /* ignore */ } },
      remove: function (k) { try { if (real) real.removeItem(k); else delete mem[k]; } catch (e) { /* ignore */ } }
    };
  }
  var local = makeStore('localStorage'), sess = makeStore('sessionStorage');

  /* ---------- depots (fallback baseline) ---------- */
  var DEPOTS = [
    { backendId: 1, id: 'ELD-DEP-01', code: 'ELD-DEP-01', name: 'Eldoret North Grain Terminal', cap: 833333, stored: 0, rate: 380 },
    { backendId: 2, id: 'NAK-DEP-01', code: 'NAK-DEP-01', name: 'Nakuru Rift Valley Silo', cap: 500000, stored: 0, rate: 310 },
    { backendId: 3, id: 'NAI-DEP-01', code: 'NAI-DEP-01', name: 'Nairobi Central Strategic Grain Depot', cap: 666667, stored: 0, rate: 260 }
  ];

  var GRAIN_TYPES = [
    { id: 1, code: 'MAIZE', name: 'Grade 1 White Maize' },
    { id: 2, code: 'WHEAT', name: 'Hard Red Winter Wheat' },
    { id: 3, code: 'SOYBEAN', name: 'Yellow Soybean' },
    { id: 4, code: 'SORGHUM', name: 'White Sorghum' },
    { id: 5, code: 'RICE', name: 'Long Grain Milled Rice' }
  ];

  function rng(a) {
    return function () {
      a |= 0; a = a + 0x6D2B79F5 | 0;
      var t = Math.imul(a ^ a >>> 15, 1 | a);
      t = t + Math.imul(t ^ t >>> 7, 61 | t) ^ t;
      return ((t ^ t >>> 14) >>> 0) / 4294967296;
    };
  }

  function initDepotProjections(d, i) {
    var r = rng((d.backendId || i) * 977 + 13);
    d.intake = Array.from({ length: 14 }, function () { return Math.round(d.rate * (0.7 + 0.6 * r())); });
  }
  DEPOTS.forEach(initDepotProjections);

  var depotById = function (id) {
    if (!id) return DEPOTS[0];
    id = String(id).toUpperCase();
    return DEPOTS.filter(function (d) {
      return String(d.id).toUpperCase() === id ||
             String(d.code).toUpperCase() === id ||
             String(d.backendId) === id ||
             d.name.toLowerCase().indexOf(id.toLowerCase()) > -1;
    })[0] || DEPOTS[0];
  };

  var depotName = function (id) {
    var d = depotById(id);
    return d ? d.name : 'Central Depot';
  };

  function history(d) {
    var out = new Array(14), v = d.stored;
    var intake = d.intake || Array.from({ length: 14 }, function () { return d.rate; });
    for (var i = 13; i >= 0; i--) { out[i] = Math.max(0, v); v -= (intake[i] || 0); }
    return out;
  }

  function metrics(d) {
    var intake = d.intake || [d.rate];
    var rate = Math.max(1, intake.slice(-7).reduce(function (a, b) { return a + b; }, 0) / 7);
    var free = Math.max(0, d.cap - d.stored), util = d.cap > 0 ? (d.stored / d.cap) : 0, days = free / rate, st = 'ok';
    if (util >= 0.9 || days <= 3) st = 'crit'; else if (util >= 0.8 || days <= 10) st = 'warn';
    return { rate: rate, free: free, util: util, days: days, st: st };
  }

  var ST = { ok: { t: 'Open', cls: 's-ok' }, warn: { t: 'Filling', cls: 's-warn' }, crit: { t: 'Redirect', cls: 's-crit' } };
  function daysTxt(d) {
    if (d < 1) return 'under a day';
    if (d < 2) return 'about 1 day';
    if (d > 60) return 'over 60 days';
    return 'about ' + Math.round(d) + ' days';
  }

  /* ---------- AI: spoilage risk (rule-based) ---------- */
  function score(m, days) {
    var base = Math.max(0, m - 12.5) * 14, time = days * (m > LIMIT ? 2.2 : 0.4);
    return Math.min(100, Math.round(base + time));
  }
  var level = function (s) { return s >= 65 ? 'high' : s >= 35 ? 'med' : 'low'; };
  var LV = {
    high: { t: 'High', cls: 's-crit', act: 'Dry to 13.5% or dispatch first' },
    med: { t: 'Medium', cls: 's-warn', act: 'Re-test moisture in 3 days and aerate' },
    low: { t: 'Low', cls: 's-ok', act: 'Store as normal' }
  };
  function riskOf(d) {
    var days = Math.max(0, (Date.now() - d.ts) / DAY), s = score(d.m, days);
    return { s: s, lv: level(s), days: days };
  }

  /* ---------- ledger with a hash chain (tamper-evident) ---------- */
  function cyrb53(str) {
    var h1 = 0xdeadbeef, h2 = 0x41c6ce57;
    for (var i = 0, ch; i < str.length; i++) {
      ch = str.charCodeAt(i);
      h1 = Math.imul(h1 ^ ch, 2654435761);
      h2 = Math.imul(h2 ^ ch, 1597334677);
    }
    h1 = Math.imul(h1 ^ (h1 >>> 16), 2246822507) ^ Math.imul(h2 ^ (h2 >>> 13), 3266489909);
    h2 = Math.imul(h2 ^ (h2 >>> 16), 2246822507) ^ Math.imul(h1 ^ (h1 >>> 13), 3266489909);
    return 4294967296 * (2097151 & h2) + (h1 >>> 0);
  }
  var hashOf = function (d) {
    return cyrb53([d.prev, d.id, d.farmer, d.depot, d.bags, d.m, d.ts].join('|')).toString(16).padStart(14, '0');
  };

  var DEPOSITS = [];

  function mapBackendDeposit(item) {
    var m = Number(item.moisturePercentage !== null && item.moisturePercentage !== undefined ? item.moisturePercentage : 13.0);
    var kg = Number(item.quantity || 0);
    var bags = Math.max(1, Math.round(kg / KG));
    var phone = item.farmerPhone || '';
    var phoneLast3 = phone.length >= 3 ? phone.slice(-3) : '000';
    var payStatus = item.status === 'ACCEPTED' ? 'Paid' : (item.status === 'REJECTED' ? 'Rejected' : 'Pending');

    return {
      backendId: item.id,
      id: item.depositReference,
      farmer: item.farmerName || ('Farmer #' + item.farmerId),
      phone: phoneLast3,
      fullPhone: phone,
      sub: item.depotName || '',
      depot: item.depotCode || 'ELD-DEP-01',
      depotId: item.depotId,
      crop: item.grainTypeName || 'Maize',
      grainTypeId: item.grainTypeId,
      bags: bags,
      kg: kg,
      m: m,
      ts: item.createdAt ? new Date(item.createdAt).getTime() : Date.now(),
      status: item.status,
      pay: payStatus,
      rejectionReason: item.rejectionReason,
      notes: item.notes,
      prev: 'GENESIS'
    };
  }

  function rebuildChain() {
    for (var i = 0; i < DEPOSITS.length; i++) {
      DEPOSITS[i].prev = i ? DEPOSITS[i - 1].hash : 'GENESIS';
      DEPOSITS[i].hash = hashOf(DEPOSITS[i]);
    }
  }

  var chainOk = function (d) { return hashOf(d) === d.hash; };
  function verifyLedger() {
    for (var i = 0; i < DEPOSITS.length; i++) {
      var d = DEPOSITS[i], prev = i ? DEPOSITS[i - 1].hash : 'GENESIS';
      if (d.prev !== prev || !chainOk(d)) return { ok: false, at: d };
    }
    return { ok: true, n: DEPOSITS.length };
  }

  function find(id) {
    id = String(id || '').trim().toUpperCase();
    return DEPOSITS.filter(function (d) { return String(d.id).toUpperCase() === id; })[0] || null;
  }

  /* ---------- async initialization from backend ---------- */
  var initPromise = null;

  function initData() {
    if (initPromise) return initPromise;
    var API = w.ZuriAPI;
    if (!API) {
      return Promise.resolve({ depots: DEPOTS, deposits: DEPOSITS });
    }

    var token = API.getToken();
    var pDashboard = token ? API.get('/dashboard/summary').catch(function () { return null; }) : Promise.resolve(null);
    var pGrains = token ? API.get('/grain-types').catch(function () { return null; }) : Promise.resolve(null);
    var pDeposits = token ? API.get('/deposits?size=100').catch(function () { return null; }) : Promise.resolve(null);

    initPromise = Promise.all([pDashboard, pGrains, pDeposits])
      .then(function (results) {
        var summary = results[0];
        var grains = results[1];
        var depositPage = results[2];

        if (summary && summary.depots && summary.depots.length) {
          DEPOTS.length = 0;
          summary.depots.forEach(function (d, i) {
            var capBags = Math.round((d.capacityMetricTonnes * 1000) / KG);
            var storedBags = Math.round((Number(d.currentStockKg) || 0) / KG);
            var rate = Math.max(50, Math.round(capBags * 0.02));
            var dep = {
              backendId: d.id,
              id: d.code,
              code: d.code,
              name: d.name,
              location: d.location,
              cap: capBags,
              stored: storedBags,
              rate: rate
            };
            initDepotProjections(dep, i);
            DEPOTS.push(dep);
          });
        }

        if (grains && grains.length) {
          GRAIN_TYPES.length = 0;
          grains.forEach(function (g) { GRAIN_TYPES.push(g); });
        }

        if (depositPage && depositPage.content && depositPage.content.length) {
          DEPOSITS.length = 0;
          depositPage.content.forEach(function (item) {
            DEPOSITS.push(mapBackendDeposit(item));
          });
          rebuildChain();
        }

        return { depots: DEPOTS, deposits: DEPOSITS, grainTypes: GRAIN_TYPES, summary: summary };
      })
      .catch(function (e) {
        console.warn('API init error, using local fallback:', e);
        return { depots: DEPOTS, deposits: DEPOSITS, grainTypes: GRAIN_TYPES };
      });

    return initPromise;
  }

  function ready(fn) {
    return initData().then(fn);
  }

  /* ---------- intake submission ---------- */
  function recordIntake(o) {
    var API = w.ZuriAPI;
    var dep = depotById(o.depot);
    var sessUser = session.get();

    // Map grain type to ID
    var grainTypeId = 1;
    for (var i = 0; i < GRAIN_TYPES.length; i++) {
      if (GRAIN_TYPES[i].name.toLowerCase().indexOf(String(o.crop).toLowerCase()) > -1 ||
          GRAIN_TYPES[i].code.toLowerCase() === String(o.crop).toLowerCase()) {
        grainTypeId = GRAIN_TYPES[i].id;
        break;
      }
    }

    var payload = {
      farmerId: (sessUser && sessUser.id) ? sessUser.id : 1,
      depotId: dep.backendId || 1,
      grainTypeId: grainTypeId,
      quantity: o.bags * KG,
      unitOfMeasure: 'KG',
      moisturePercentage: o.m,
      farmerName: o.farmer,
      farmerPhone: o.phone,
      farmerRecordId: o.farmerRecordId || null,
      notes: o.sub ? ('Sub-county: ' + o.sub) : 'Produce intake'
    };

    if (API && API.getToken()) {
      return API.post('/deposits', payload).then(function (res) {
        var d = mapBackendDeposit(res);
        d.prev = DEPOSITS.length ? DEPOSITS[DEPOSITS.length - 1].hash : 'GENESIS';
        d.hash = hashOf(d);
        DEPOSITS.push(d);
        dep.stored += d.bags;
        return d;
      });
    }

    // Fallback if offline
    var ts = Date.now(), dt = new Date(ts);
    var fakeId = 'ZA-' + dep.id + '-' + String(dt.getFullYear()).slice(2) + pad(dt.getMonth() + 1) + pad(dt.getDate()) + '-' + String(DEPOSITS.length + 1).padStart(4, '0');
    var d = {
      id: fakeId,
      farmer: o.farmer,
      phone: String(o.phone).slice(-3),
      fullPhone: o.phone,
      sub: o.sub,
      depot: dep.id,
      crop: o.crop,
      bags: o.bags,
      kg: o.bags * KG,
      m: o.m,
      ts: ts,
      pay: 'Pending',
      status: 'PENDING',
      prev: DEPOSITS.length ? DEPOSITS[DEPOSITS.length - 1].hash : 'GENESIS'
    };
    d.hash = hashOf(d);
    DEPOSITS.push(d);
    dep.stored += d.bags;
    return Promise.resolve(d);
  }

  /* ---------- session management ---------- */
  var SKEY = 'zuri.session.v1';
  function nameFromEmail(e) {
    var base = e.split('@')[0].replace(/[._-]+/g, ' ').replace(/\d+/g, '').trim() || 'Officer';
    return base.replace(/\b\w/g, function (c) { return c.toUpperCase(); });
  }

  var session = {
    get: function () {
      var raw = local.get(SKEY) || sess.get(SKEY);
      if (!raw) return null;
      try { return JSON.parse(raw); } catch (e) { return null; }
    },
    set: function (user, remember) {
      var s = JSON.stringify(user);
      local.remove(SKEY); sess.remove(SKEY);
      (remember ? local : sess).set(SKEY, s);
    },
    clear: function () {
      local.remove(SKEY);
      sess.remove(SKEY);
      if (w.ZuriAPI) w.ZuriAPI.clearToken();
    },
    setNext: function (p) { sess.set('zuri.next', p); },
    takeNext: function () { var p = sess.get('zuri.next'); sess.remove('zuri.next'); return p; },
    nameFromEmail: nameFromEmail
  };

  var setLookup = function (id) { sess.set('zuri.lookup', id); };
  var takeLookup = function () { var id = sess.get('zuri.lookup'); sess.remove('zuri.lookup'); return id; };

  w.Zuri = {
    KG: KG, LIMIT: LIMIT, DAY: DAY, NOW: NOW,
    DEPOTS: DEPOTS, DEPOSITS: DEPOSITS, GRAIN_TYPES: GRAIN_TYPES,
    depotById: depotById, depotName: depotName, history: history, metrics: metrics, ST: ST, daysTxt: daysTxt,
    score: score, level: level, LV: LV, riskOf: riskOf,
    hashOf: hashOf, chainOk: chainOk, verifyLedger: verifyLedger,
    recordIntake: recordIntake, find: find, init: initData, ready: ready,
    session: session, setLookup: setLookup, takeLookup: takeLookup,
    nf: nf, esc: esc, pad: pad, fmtDay: fmtDay, fmtDate: fmtDate, sum: sum,
    PAY: { Paid: 's-ok', Approved: 's-info', Pending: 's-warn', Rejected: 's-crit' }
  };
})(window);
