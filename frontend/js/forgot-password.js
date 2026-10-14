/**
 * forgot-password.js - Handle forgot password request
 */

document.addEventListener('DOMContentLoaded', () => {
  const form = document.getElementById('forgot-form');
  const inputEmail = document.getElementById('email');
  const errorEmail = document.getElementById('error-email');
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

  inputEmail.addEventListener('input', () => {
    inputEmail.classList.remove('has-error');
    errorEmail.classList.remove('show');
    hideAlert();
  });

  form.addEventListener('submit', async (e) => {
    e.preventDefault();

    const email = inputEmail.value.trim();
    if (!email) {
      inputEmail.classList.add('has-error');
      errorEmail.textContent = 'Vui lòng nhập địa chỉ email.';
      errorEmail.classList.add('show');
      return;
    }

    btnSubmit.disabled = true;
    btnText.textContent = 'Đang gửi…';
    btnSpinner.style.display = 'inline-block';
    hideAlert();

    try {
      const data = await apiRequest('/api/auth/forgot-password', {
        method: 'POST',
        body: JSON.stringify({ email })
      });

      showAlert(data.message || 'Yêu cầu đặt lại mật khẩu đã được xử lý thành công.', true);
      inputEmail.value = '';
    } catch (err) {
      showAlert(err.message || 'Đã xảy ra lỗi khi gửi yêu cầu. Vui lòng thử lại.');
    } finally {
      btnSubmit.disabled = false;
      btnText.textContent = 'Gửi liên kết đặt lại mật khẩu';
      btnSpinner.style.display = 'none';
    }
  });
});
