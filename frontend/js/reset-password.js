/**
 * reset-password.js - Handle password reset confirmation
 */

document.addEventListener('DOMContentLoaded', () => {
  const urlParams = new URLSearchParams(window.location.search);
  const token = urlParams.get('token');

  const form = document.getElementById('reset-form');
  const inputPassword = document.getElementById('new-password');
  const inputConfirmPassword = document.getElementById('confirm-password');
  const errorPassword = document.getElementById('error-password');
  const errorConfirmPassword = document.getElementById('error-confirm-password');
  const btnSubmit = document.getElementById('btn-submit');
  const btnText = document.getElementById('btn-text');
  const btnSpinner = document.getElementById('btn-spinner');
  const alertContainer = document.getElementById('alert-container');
  const alertText = document.getElementById('alert-text');

  function showAlert(msg, isSuccess = false) {
    alertText.textContent = msg;
    alertContainer.className = isSuccess ? 'alert alert-success' : 'alert alert-danger';
    alertContainer.style.display = 'flex';
  }

  function hideAlert() {
    alertContainer.style.display = 'none';
  }

  if (!token) {
    showAlert('Mã xác nhận (token) đặt lại mật khẩu không tìm thấy hoặc không hợp lệ.');
    btnSubmit.disabled = true;
    return;
  }

  inputPassword.addEventListener('input', () => {
    inputPassword.classList.remove('has-error');
    errorPassword.classList.remove('show');
    hideAlert();
  });

  inputConfirmPassword.addEventListener('input', () => {
    inputConfirmPassword.classList.remove('has-error');
    errorConfirmPassword.classList.remove('show');
    hideAlert();
  });

  form.addEventListener('submit', async (e) => {
    e.preventDefault();

    const newPassword = inputPassword.value;
    const confirmPassword = inputConfirmPassword.value;

    let hasError = false;

    if (!newPassword || newPassword.length < 8) {
      inputPassword.classList.add('has-error');
      errorPassword.textContent = 'Mật khẩu phải có độ dài tối thiểu 8 ký tự và chứa cả chữ lẫn số.';
      errorPassword.classList.add('show');
      hasError = true;
    }

    if (newPassword !== confirmPassword) {
      inputConfirmPassword.classList.add('has-error');
      errorConfirmPassword.textContent = 'Xác nhận mật khẩu không trùng khớp.';
      errorConfirmPassword.classList.add('show');
      hasError = true;
    }

    if (hasError) return;

    btnSubmit.disabled = true;
    btnText.textContent = 'Đang cập nhật…';
    btnSpinner.style.display = 'inline-block';
    hideAlert();

    try {
      const data = await apiRequest('/api/auth/reset-password', {
        method: 'POST',
        body: JSON.stringify({
          token,
          newPassword
        })
      });

      showAlert(data.message + ' Đang chuyển hướng về trang đăng nhập...', true);
      setTimeout(() => {
        window.location.href = 'login.html';
      }, 2000);
    } catch (err) {
      showAlert(err.message || 'Token đã hết hạn hoặc không hợp lệ.');
      btnSubmit.disabled = false;
      btnText.textContent = 'Đặt lại mật khẩu';
      btnSpinner.style.display = 'none';
    }
  });
});
