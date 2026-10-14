/**
 * auth.js - Authentication State, Route Guards & Role Permissions
 */

const AuthService = {
  isAuthenticated() {
    return !!TokenStorage.getAccessToken();
  },

  getCurrentUser() {
    return TokenStorage.getUser();
  },

  hasRole(expectedRole) {
    const user = this.getCurrentUser();
    return user && user.role === expectedRole;
  },

  async logout() {
    const refreshToken = TokenStorage.getRefreshToken();
    try {
      await apiRequest('/api/auth/logout', {
        method: 'POST',
        body: JSON.stringify({ refreshToken })
      });
    } catch (e) {
      console.warn('Logout API call failed or offline:', e.message);
    } finally {
      TokenStorage.clear();
      window.location.href = 'login.html';
    }
  },

  /**
   * Route Guard: Verifies login and optional required roles
   */
  requireAuth(allowedRoles = []) {
    if (!this.isAuthenticated()) {
      const currentUrl = window.location.pathname + window.location.search;
      window.location.href = `login.html?redirect=${encodeURIComponent(currentUrl)}`;
      return false;
    }

    const user = this.getCurrentUser();
    if (allowedRoles.length > 0 && (!user || !allowedRoles.includes(user.role))) {
      alert('Bạn không có quyền truy cập vào trang này.');
      window.location.href = 'dashboard.html';
      return false;
    }

    return true;
  },

  /**
   * Validates internal redirect path (must start with a single '/')
   */
  sanitizeRedirect(path) {
    if (!path || typeof path !== 'string') return null;
    const trimmed = path.trim();
    if (trimmed.startsWith('/') && !trimmed.startsWith('//') && !trimmed.includes('://')) {
      return trimmed;
    }
    return null;
  }
};
