/**
 * login.js - Authentication Form Logic & Validation
 */

document.addEventListener('DOMContentLoaded', () => {
  // If already logged in, redirect straight to appropriate page
  if (AuthService.isAuthenticated()) {
    const user = AuthService.getCurrentUser();
    if (user && user.role === 'ADMIN') {
      window.location.href = 'admin-users.html';
    } else {
      window.location.href = 'dashboard.html';
    }
    return;
  }

  // Parse redirect query parameter (Internal path only)
  const urlParams = new URLSearchParams(window.location.search);
  const rawRedirect = urlParams.get('redirect');
  const sanitizedRedirectUrl = AuthService.sanitizeRedirect(rawRedirect);

  // UI Elements
  const form = document.getElementById('login-form');
  const inputLogin = document.getElementById('username-or-email');
  const inputPassword = document.getElementById('password');
  const checkRememberMe = document.getElementById('remember-me');
  const btnTogglePass = document.getElementById('btn-toggle-password');
  const btnSubmit = document.getElementById('btn-login');
  const btnText = document.getElementById('btn-login-text');
  const btnSpinner = document.getElementById('btn-login-spinner');
  const alertContainer = document.getElementById('login-alert');
  const alertMessage = document.getElementById('login-alert-text');

  const errorLogin = document.getElementById('error-username-or-email');
  const errorPassword = document.getElementById('error-password');

  // Local UI State
  let showPassword = false;
  let isLoading = false;

  // Toggle Password Visibility
  if (btnTogglePass) {
    btnTogglePass.addEventListener('click', () => {
      showPassword = !showPassword;
      inputPassword.type = showPassword ? 'text' : 'password';
      btnTogglePass.setAttribute('aria-label', showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu');
      btnTogglePass.innerHTML = showPassword ?
        `<svg width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path><line x1="1" y1="1" x2="23" y2="23"></line></svg>` :
        `<svg width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path><circle cx="12" cy="12" r="3"></circle></svg>`;
    });
  }

  // Clear field errors on input
  inputLogin.addEventListener('input', () => {
    inputLogin.classList.remove('has-error');
    errorLogin.classList.remove('show');
    hideAlert();
  });

  inputPassword.addEventListener('input', () => {
    inputPassword.classList.remove('has-error');
    errorPassword.classList.remove('show');
    hideAlert();
  });

  function showAlert(message) {
    alertMessage.textContent = message;
    alertContainer.style.display = 'flex';
  }

  function hideAlert() {
    alertContainer.style.display = 'none';
    alertMessage.textContent = '';
  }

  function setLoading(loading) {
    isLoading = loading;
    btnSubmit.disabled = loading;
    if (loading) {
      btnText.textContent = 'Đang đăng nhập…';
      btnSpinner.style.display = 'inline-block';
    } else {
      btnText.textContent = 'Đăng nhập';
      btnSpinner.style.display = 'none';
    }
  }

  // Form Submit Handler
  form.addEventListener('submit', async (e) => {
    e.preventDefault();

    // BR-LOGIN-08: Block duplicate submission
    if (isLoading) return;

    hideAlert();

    const usernameOrEmail = inputLogin.value.trim();
    const password = inputPassword.value;
    const rememberMe = checkRememberMe.checked;

    let hasValidationError = false;

    // BR-LOGIN-01: Tên đăng nhập không được để trống
    if (!usernameOrEmail) {
      inputLogin.classList.add('has-error');
      errorLogin.textContent = 'Vui lòng nhập tên đăng nhập hoặc email.';
      errorLogin.classList.add('show');
      hasValidationError = true;
    }

    // BR-LOGIN-02: Mật khẩu không được để trống
    if (!password) {
      inputPassword.classList.add('has-error');
      errorPassword.textContent = 'Vui lòng nhập mật khẩu.';
      errorPassword.classList.add('show');
      hasValidationError = true;
    }

    if (hasValidationError) {
      return;
    }

    setLoading(true);

    try {
      const data = await apiRequest('/api/auth/login', {
        method: 'POST',
        body: JSON.stringify({
          usernameOrEmail,
          password,
          rememberMe
        })
      });

      // Save access & refresh tokens
      TokenStorage.setTokens(data.accessToken, data.refreshToken, data.user, rememberMe);

      // BR-LOGIN-06: Điều hướng theo vai trò (hoặc redirectParam nếu hợp lệ)
      const targetDestination = sanitizedRedirectUrl || (
        data.user.role === 'ADMIN' ? 'admin-users.html' : 'dashboard.html'
      );

      window.location.href = targetDestination;
    } catch (err) {
      setLoading(false);

      if (err.code === 'INVALID_CREDENTIALS') {
        showAlert('Tên đăng nhập/email hoặc mật khẩu không chính xác.');
      } else if (err.code === 'ACCOUNT_LOCKED') {
        showAlert('Tài khoản của bạn đã bị khóa. Vui lòng liên hệ Quản trị viên hệ thống.');
      } else if (err.code === 'ACCOUNT_PENDING') {
        showAlert('Tài khoản của bạn đang chờ kích hoạt. Vui lòng liên hệ Quản trị viên.');
      } else if (err.code === 'VALIDATION_ERROR' && err.fieldErrors && err.fieldErrors.length > 0) {
        err.fieldErrors.forEach(fe => {
          if (fe.field === 'usernameOrEmail') {
            inputLogin.classList.add('has-error');
            errorLogin.textContent = fe.message;
            errorLogin.classList.add('show');
          } else if (fe.field === 'password') {
            inputPassword.classList.add('has-error');
            errorPassword.textContent = fe.message;
            errorPassword.classList.add('show');
          }
        });
      } else {
        showAlert(err.message || 'Đăng nhập không thành công. Vui lòng thử lại sau.');
      }
    }
  });
});
