/**
 * api.js - Disaster Rescue HTTP Client with Automatic Token Refresh
 */

const API_BASE_URL = window.API_BASE_URL || 'http://localhost:8080';

const TokenStorage = {
  getAccessToken() {
    return localStorage.getItem('access_token') || sessionStorage.getItem('access_token');
  },
  getRefreshToken() {
    return localStorage.getItem('refresh_token') || sessionStorage.getItem('refresh_token');
  },
  getUser() {
    const raw = localStorage.getItem('user_info') || sessionStorage.getItem('user_info');
    try {
      return raw ? JSON.parse(raw) : null;
    } catch {
      return null;
    }
  },
  setTokens(accessToken, refreshToken, user, rememberMe) {
    const storage = rememberMe ? localStorage : sessionStorage;
    // Clear both first to avoid stale cross-storage issues
    this.clear();

    storage.setItem('access_token', accessToken);
    if (refreshToken) {
      storage.setItem('refresh_token', refreshToken);
    }
    if (user) {
      storage.setItem('user_info', JSON.stringify(user));
    }
  },
  updateTokens(accessToken, refreshToken, user) {
    // Keep in whatever storage is currently holding the tokens
    const isLocal = !!localStorage.getItem('access_token');
    const storage = isLocal ? localStorage : sessionStorage;

    storage.setItem('access_token', accessToken);
    if (refreshToken) {
      storage.setItem('refresh_token', refreshToken);
    }
    if (user) {
      storage.setItem('user_info', JSON.stringify(user));
    }
  },
  clear() {
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
    localStorage.removeItem('user_info');
    sessionStorage.removeItem('access_token');
    sessionStorage.removeItem('refresh_token');
    sessionStorage.removeItem('user_info');
  }
};

let isRefreshing = false;
let failedQueue = [];

const processQueue = (error, token = null) => {
  failedQueue.forEach(prom => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

/**
 * Universal API Request with automatic 401 recovery and Bearer header insertion
 */
async function apiRequest(endpoint, options = {}) {
  const url = endpoint.startsWith('http') ? endpoint : `${API_BASE_URL}${endpoint}`;
  const headers = {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
    ...(options.headers || {})
  };

  const accessToken = TokenStorage.getAccessToken();
  if (accessToken && !headers['Authorization']) {
    headers['Authorization'] = `Bearer ${accessToken}`;
  }

  const config = {
    ...options,
    headers
  };

  try {
    let response = await fetch(url, config);

    // If 401 UNAUTHORIZED and not calling login/refresh/logout/forgot-password/reset-password
    const isAuthRoute = endpoint.includes('/api/auth/login') ||
                        endpoint.includes('/api/auth/refresh') ||
                        endpoint.includes('/api/auth/logout') ||
                        endpoint.includes('/api/auth/forgot-password') ||
                        endpoint.includes('/api/auth/reset-password');

    if (response.status === 401 && !isAuthRoute) {
      const refreshToken = TokenStorage.getRefreshToken();
      if (!refreshToken) {
        handleSessionExpired();
        throw new Error('Hết phiên làm việc');
      }

      if (isRefreshing) {
        // Wait for current refresh to complete
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        }).then(newToken => {
          headers['Authorization'] = `Bearer ${newToken}`;
          return fetch(url, { ...options, headers }).then(res => res.json());
        });
      }

      isRefreshing = true;

      try {
        const refreshResponse = await fetch(`${API_BASE_URL}/api/auth/refresh`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ refreshToken })
        });

        if (!refreshResponse.ok) {
          throw new Error('Refresh token invalid');
        }

        const data = await refreshResponse.json();
        TokenStorage.updateTokens(data.accessToken, data.refreshToken, data.user);
        processQueue(null, data.accessToken);

        // Replay original request with fresh access token
        headers['Authorization'] = `Bearer ${data.accessToken}`;
        return fetch(url, { ...options, headers }).then(res => res.json());
      } catch (refreshErr) {
        processQueue(refreshErr, null);
        handleSessionExpired();
        throw refreshErr;
      } finally {
        isRefreshing = false;
      }
    }

    const data = await response.json().catch(() => ({}));
    if (!response.ok) {
      const error = new Error(data.message || 'Đã xảy ra lỗi khi kết nối tới máy chủ');
      error.status = response.status;
      error.code = data.code || 'UNKNOWN_ERROR';
      error.fieldErrors = data.fieldErrors || [];
      throw error;
    }

    return data;
  } catch (err) {
    if (!err.status && (err.name === 'TypeError' || err.message === 'Failed to fetch')) {
      const netError = new Error('Không thể kết nối đến máy chủ. Vui lòng kiểm tra kết nối mạng.');
      netError.code = 'NETWORK_ERROR';
      netError.status = 0;
      throw netError;
    }
    throw err;
  }
}

function handleSessionExpired() {
  TokenStorage.clear();
  const currentPath = window.location.pathname + window.location.search;
  const redirectParam = encodeURIComponent(currentPath);
  if (!window.location.pathname.includes('login.html')) {
    window.location.href = `login.html?redirect=${redirectParam}`;
  }
}

// Dark / Light Theme Manager
function initTheme() {
  const savedTheme = localStorage.getItem('theme') ||
    (window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light');
  document.documentElement.setAttribute('data-theme', savedTheme);

  const toggleBtn = document.getElementById('theme-toggle-btn');
  if (toggleBtn) {
    toggleBtn.textContent = savedTheme === 'dark' ? '☀️' : '🌙';
    toggleBtn.setAttribute('aria-label', savedTheme === 'dark' ? 'Chuyển sang chế độ sáng' : 'Chuyển sang chế độ tối');

    toggleBtn.addEventListener('click', () => {
      const current = document.documentElement.getAttribute('data-theme');
      const next = current === 'dark' ? 'light' : 'dark';
      document.documentElement.setAttribute('data-theme', next);
      localStorage.setItem('theme', next);
      toggleBtn.textContent = next === 'dark' ? '☀️' : '🌙';
      toggleBtn.setAttribute('aria-label', next === 'dark' ? 'Chuyển sang chế độ sáng' : 'Chuyển sang chế độ tối');
    });
  }
}

document.addEventListener('DOMContentLoaded', initTheme);
