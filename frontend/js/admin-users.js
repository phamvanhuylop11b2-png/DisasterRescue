/**
 * admin-users.js - User Management Module for Administrators
 */

document.addEventListener('DOMContentLoaded', async () => {
  // Ensure user is authenticated and has ADMIN role
  if (!AuthService.requireAuth(['ADMIN'])) {
    return;
  }

  const currentUser = AuthService.getCurrentUser();
  const navUserName = document.getElementById('nav-user-name');
  if (navUserName) {
    navUserName.textContent = currentUser.fullName || currentUser.username;
  }

  const btnLogout = document.getElementById('btn-logout');
  if (btnLogout) {
    btnLogout.addEventListener('click', () => AuthService.logout());
  }

  // Filters state
  let state = {
    q: '',
    role: '',
    status: '',
    page: 0,
    size: 10,
    totalPages: 1
  };

  let rolesList = [];

  // Elements
  const inputSearch = document.getElementById('search-query');
  const selectRole = document.getElementById('filter-role');
  const selectStatus = document.getElementById('filter-status');
  const tableBody = document.getElementById('users-table-body');
  const btnPrevPage = document.getElementById('btn-prev-page');
  const btnNextPage = document.getElementById('btn-next-page');
  const pageInfo = document.getElementById('page-info');

  // Modals elements
  const modalCreate = document.getElementById('modal-create');
  const modalEdit = document.getElementById('modal-edit');
  const modalResetPass = document.getElementById('modal-reset-pass');
  const btnOpenCreate = document.getElementById('btn-open-create');

  // Escape HTML helper to prevent XSS
  function escapeHtml(str) {
    if (!str) return '';
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  // Load Roles
  async function loadRoles() {
    try {
      rolesList = await apiRequest('/api/roles');
      populateRoleSelects();
    } catch (err) {
      console.error('Failed to load roles:', err);
    }
  }

  function populateRoleSelects() {
    const createRoleSelect = document.getElementById('create-role-id');
    const editRoleSelect = document.getElementById('edit-role-id');

    const options = rolesList.map(r => `<option value="${r.id}">${escapeHtml(r.name)} (${r.code})</option>`).join('');
    if (createRoleSelect) createRoleSelect.innerHTML = options;
    if (editRoleSelect) editRoleSelect.innerHTML = options;
  }

  // Load Users List
  async function loadUsers() {
    try {
      tableBody.innerHTML = `<tr><td colspan="7" style="text-align:center; padding: 2rem;">Đang tải danh sách người dùng…</td></tr>`;

      const params = new URLSearchParams({
        page: state.page,
        size: state.size
      });
      if (state.q) params.append('q', state.q);
      if (state.role) params.append('role', state.role);
      if (state.status) params.append('status', state.status);

      const data = await apiRequest(`/api/users?${params.toString()}`);
      state.totalPages = data.totalPages;

      renderUsersTable(data.content);
      renderPagination(data);
    } catch (err) {
      tableBody.innerHTML = `<tr><td colspan="7" style="text-align:center; color: var(--color-danger); padding: 2rem;">Lỗi tải dữ liệu: ${escapeHtml(err.message)}</td></tr>`;
    }
  }

  function renderUsersTable(users) {
    if (!users || users.length === 0) {
      tableBody.innerHTML = `<tr><td colspan="7" style="text-align:center; padding: 2rem; color: var(--text-muted);">Không tìm thấy người dùng nào phù hợp.</td></tr>`;
      return;
    }

    tableBody.innerHTML = users.map(user => {
      const isSelf = currentUser && currentUser.id === user.id;

      let roleBadgeClass = 'badge-citizen';
      if (user.role === 'ADMIN') roleBadgeClass = 'badge-admin';
      else if (user.role === 'OPERATOR') roleBadgeClass = 'badge-operator';
      else if (user.role === 'RESCUER') roleBadgeClass = 'badge-rescuer';

      let statusBadgeClass = 'badge-active';
      let statusText = 'Hoạt động';
      if (user.status === 'LOCKED') {
        statusBadgeClass = 'badge-locked';
        statusText = 'Bị khóa';
      } else if (user.status === 'PENDING') {
        statusBadgeClass = 'badge-pending';
        statusText = 'Chờ duyệt';
      }

      return `
        <tr>
          <td><strong>#${user.id}</strong></td>
          <td>
            <div style="font-weight:600;">${escapeHtml(user.fullName)}</div>
            <div style="font-size:0.75rem; color:var(--text-muted);">@${escapeHtml(user.username)}</div>
          </td>
          <td>${escapeHtml(user.email)}</td>
          <td><span class="badge ${roleBadgeClass}">${escapeHtml(user.roleName || user.role)}</span></td>
          <td><span class="badge ${statusBadgeClass}">${statusText}</span></td>
          <td style="font-size:0.8rem; color:var(--text-muted);">
            ${user.lastLoginAt ? new Date(user.lastLoginAt).toLocaleString('vi-VN') : 'Chưa đăng nhập'}
          </td>
          <td>
            <div class="action-group">
              <button class="btn btn-secondary btn-icon" onclick="openEditModal(${user.id}, '${escapeHtml(user.email)}', '${escapeHtml(user.fullName)}', '${user.role}')" title="Sửa thông tin">
                ✏️ Sửa
              </button>
              <button class="btn btn-secondary btn-icon" onclick="openResetPassModal(${user.id}, '${escapeHtml(user.username)}')" title="Cấp lại mật khẩu">
                🔑 Đổi MK
              </button>
              ${!isSelf ? `
                <button class="btn ${user.status === 'LOCKED' ? 'btn-primary' : 'btn-danger'} btn-icon" onclick="toggleStatus(${user.id}, '${user.status}')" title="${user.status === 'LOCKED' ? 'Mở khóa tài khoản' : 'Khóa tài khoản'}">
                  ${user.status === 'LOCKED' ? '🔓 Mở khóa' : '🔒 Khóa'}
                </button>
              ` : `
                <span style="font-size:0.75rem; color:var(--text-muted);">(Tài khoản hiện tại)</span>
              `}
            </div>
          </td>
        </tr>
      `;
    }).join('');
  }

  function renderPagination(data) {
    pageInfo.textContent = `Trang ${data.page + 1} / ${Math.max(1, data.totalPages)} (Tổng số ${data.totalElements} tài khoản)`;
    btnPrevPage.disabled = data.page === 0;
    btnNextPage.disabled = data.page >= data.totalPages - 1;
  }

  // Filter change handlers
  inputSearch.addEventListener('input', debounce(() => {
    state.q = inputSearch.value.trim();
    state.page = 0;
    loadUsers();
  }, 400));

  selectRole.addEventListener('change', () => {
    state.role = selectRole.value;
    state.page = 0;
    loadUsers();
  });

  selectStatus.addEventListener('change', () => {
    state.status = selectStatus.value;
    state.page = 0;
    loadUsers();
  });

  btnPrevPage.addEventListener('click', () => {
    if (state.page > 0) {
      state.page--;
      loadUsers();
    }
  });

  btnNextPage.addEventListener('click', () => {
    if (state.page < state.totalPages - 1) {
      state.page++;
      loadUsers();
    }
  });

  // Toggle user status (Lock / Unlock)
  window.toggleStatus = async (userId, currentStatus) => {
    const nextStatus = currentStatus === 'LOCKED' ? 'ACTIVE' : 'LOCKED';
    const confirmMsg = nextStatus === 'LOCKED' ?
      'Bạn có chắc chắn muốn khóa tài khoản này? Mọi phiên đăng nhập của người dùng sẽ bị hủy ngay lập tức.' :
      'Mở khóa tài khoản này để người dùng có thể đăng nhập lại?';

    if (!confirm(confirmMsg)) return;

    try {
      await apiRequest(`/api/users/${userId}/status`, {
        method: 'PATCH',
        body: JSON.stringify({ status: nextStatus })
      });
      loadUsers();
    } catch (err) {
      alert(err.message || 'Không thể cập nhật trạng thái');
    }
  };

  // Create User Handler
  const formCreate = document.getElementById('form-create-user');
  btnOpenCreate.addEventListener('click', () => {
    modalCreate.classList.add('active');
  });

  formCreate.addEventListener('submit', async (e) => {
    e.preventDefault();
    const username = document.getElementById('create-username').value.trim();
    const email = document.getElementById('create-email').value.trim();
    const fullName = document.getElementById('create-fullname').value.trim();
    const password = document.getElementById('create-password').value;
    const roleId = Number(document.getElementById('create-role-id').value);

    try {
      await apiRequest('/api/users', {
        method: 'POST',
        body: JSON.stringify({ username, email, fullName, password, roleId })
      });
      closeModal(modalCreate);
      formCreate.reset();
      loadUsers();
      alert('Tạo người dùng mới thành công!');
    } catch (err) {
      alert(err.message || 'Lỗi khi tạo người dùng');
    }
  });

  // Edit User Handler
  let activeEditUserId = null;
  window.openEditModal = (userId, email, fullName, roleCode) => {
    activeEditUserId = userId;
    document.getElementById('edit-email').value = email;
    document.getElementById('edit-fullname').value = fullName;

    const matchedRole = rolesList.find(r => r.code === roleCode);
    if (matchedRole) {
      document.getElementById('edit-role-id').value = matchedRole.id;
    }

    modalEdit.classList.add('active');
  };

  const formEdit = document.getElementById('form-edit-user');
  formEdit.addEventListener('submit', async (e) => {
    e.preventDefault();
    if (!activeEditUserId) return;

    const email = document.getElementById('edit-email').value.trim();
    const fullName = document.getElementById('edit-fullname').value.trim();
    const roleId = Number(document.getElementById('edit-role-id').value);

    try {
      await apiRequest(`/api/users/${activeEditUserId}`, {
        method: 'PUT',
        body: JSON.stringify({ email, fullName, roleId })
      });
      closeModal(modalEdit);
      loadUsers();
      alert('Cập nhật thông tin thành công!');
    } catch (err) {
      alert(err.message || 'Lỗi khi cập nhật');
    }
  });

  // Reset Password Handler
  let activeResetUserId = null;
  window.openResetPassModal = (userId, username) => {
    activeResetUserId = userId;
    document.getElementById('reset-pass-target').textContent = username;
    document.getElementById('reset-new-password').value = '';
    modalResetPass.classList.add('active');
  };

  const formResetPass = document.getElementById('form-reset-password');
  formResetPass.addEventListener('submit', async (e) => {
    e.preventDefault();
    if (!activeResetUserId) return;

    const newPassword = document.getElementById('reset-new-password').value;
    if (newPassword.length < 8) {
      alert('Mật khẩu phải tối thiểu 8 ký tự, có cả chữ và số.');
      return;
    }

    try {
      await apiRequest(`/api/users/${activeResetUserId}/reset-password`, {
        method: 'POST',
        body: JSON.stringify({ newPassword })
      });
      closeModal(modalResetPass);
      alert('Đã cấp lại mật khẩu thành công!');
    } catch (err) {
      alert(err.message || 'Lỗi cấp lại mật khẩu');
    }
  });

  // Modal helpers
  window.closeModal = (modalElement) => {
    modalElement.classList.remove('active');
  };

  document.querySelectorAll('.modal-overlay').forEach(overlay => {
    overlay.addEventListener('click', (e) => {
      if (e.target === overlay) {
        closeModal(overlay);
      }
    });
  });

  function debounce(fn, delay) {
    let timer = null;
    return (...args) => {
      clearTimeout(timer);
      timer = setTimeout(() => fn.apply(this, args), delay);
    };
  }

  // Initialize
  await loadRoles();
  await loadUsers();
});
