/* ==========================================================================
   ZuriAgro / Ushirika Grain: REST API Client
   Communicates with Spring Boot backend at http://localhost:8080/api/v1
   ========================================================================== */
(function (w) {
  'use strict';

  var BASE_URL = 'http://localhost:8080/api/v1';
  var TOKEN_KEY = 'zuri.token.v1';

  function getToken() {
    try {
      return localStorage.getItem(TOKEN_KEY) || sessionStorage.getItem(TOKEN_KEY);
    } catch (e) {
      return null;
    }
  }

  function setToken(token, remember) {
    try {
      localStorage.removeItem(TOKEN_KEY);
      sessionStorage.removeItem(TOKEN_KEY);
      if (token) {
        (remember ? localStorage : sessionStorage).setItem(TOKEN_KEY, token);
      }
    } catch (e) {
      /* ignore */
    }
  }

  function clearToken() {
    try {
      localStorage.removeItem(TOKEN_KEY);
      sessionStorage.removeItem(TOKEN_KEY);
    } catch (e) {
      /* ignore */
    }
  }

  function request(path, options) {
    options = options || {};
    var url = path.startsWith('http') ? path : BASE_URL + (path.startsWith('/') ? path : '/' + path);
    var headers = Object.assign({}, options.headers || {});
    
    if (!headers['Content-Type'] && options.body && typeof options.body === 'string') {
      headers['Content-Type'] = 'application/json';
    }

    var token = getToken();
    if (token && !headers['Authorization']) {
      headers['Authorization'] = 'Bearer ' + token;
    }

    options.headers = headers;

    return fetch(url, options).then(function (res) {
      if (res.status === 401 && !path.includes('/auth/login')) {
        clearToken();
        if (w.Zuri && w.Zuri.session) {
          w.Zuri.session.clear();
        }
        if (!location.pathname.endsWith('login.html')) {
          location.replace('login.html');
        }
        return Promise.reject(new Error('Session expired. Please log in again.'));
      }

      var contentType = res.headers.get('content-type') || '';
      var isJson = contentType.includes('application/json') || contentType.includes('application/problem+json');

      if (!res.ok) {
        if (isJson) {
          return res.json().then(function (errBody) {
            var msg = errBody.detail || errBody.message || errBody.title || ('HTTP error ' + res.status);
            if (errBody.errors && Array.isArray(errBody.errors)) {
              msg = errBody.errors.map(function (e) { return e.defaultMessage || e; }).join(', ');
            }
            var err = new Error(msg);
            err.status = res.status;
            err.data = errBody;
            return Promise.reject(err);
          });
        }
        return res.text().then(function (txt) {
          var err = new Error(txt || ('HTTP error ' + res.status));
          err.status = res.status;
          return Promise.reject(err);
        });
      }

      if (res.status === 204) return Promise.resolve(null);
      return isJson ? res.json() : res.text();
    });
  }

  function get(path) {
    return request(path, { method: 'GET' });
  }

  function post(path, body) {
    return request(path, {
      method: 'POST',
      body: typeof body === 'object' ? JSON.stringify(body) : body
    });
  }

  function patch(path, body) {
    return request(path, {
      method: 'PATCH',
      body: typeof body === 'object' ? JSON.stringify(body) : body
    });
  }

  function put(path, body) {
    return request(path, {
      method: 'PUT',
      body: typeof body === 'object' ? JSON.stringify(body) : body
    });
  }

  function del(path) {
    return request(path, { method: 'DELETE' });
  }

  function login(usernameOrEmail, password, remember) {
    var payload = {
      username: usernameOrEmail.trim(),
      password: password
    };
    return post('/auth/login', payload).then(function (res) {
      setToken(res.accessToken, remember);
      if (w.Zuri && w.Zuri.session) {
        w.Zuri.session.set({
          id: res.id,
          username: res.username,
          email: res.email,
          name: res.fullName || res.username,
          roles: res.roles || []
        }, remember);
      }
      return res;
    });
  }

  function logout() {
    clearToken();
    if (w.Zuri && w.Zuri.session) {
      w.Zuri.session.clear();
    }
  }

  w.ZuriAPI = {
    BASE_URL: BASE_URL,
    getToken: getToken,
    setToken: setToken,
    clearToken: clearToken,
    request: request,
    get: get,
    post: post,
    patch: patch,
    put: put,
    delete: del,
    login: login,
    logout: logout
  };
})(window);
