// ============================================================
// 宠物管家 — 前端工具
// ============================================================

// ---- JWT Token Management ----
function getToken() {
    return localStorage.getItem('token');
}

function setToken(token) {
    localStorage.setItem('token', token);
    document.cookie = `token=${token}; path=/; max-age=604800; SameSite=Lax`;
}

function clearToken() {
    localStorage.removeItem('token');
    document.cookie = 'token=; path=/; max-age=0';
}

// ---- Centralized API Request ----
async function apiRequest(url, method = 'GET', data = null, isFormData = false) {
    const headers = {};
    const token = getToken();
    if (token) headers['Authorization'] = 'Bearer ' + token;
    if (!isFormData) headers['Content-Type'] = 'application/json';

    const options = { method, headers };
    if (data) options.body = isFormData ? data : JSON.stringify(data);

    const response = await fetch(url, options);
    if (response.status === 401) {
        clearToken();
        window.location.href = '/auth/login';
        throw new Error('Unauthorized');
    }
    return response.json();
}

// ---- Toast Notifications ----
function showToast(message, type = 'info') {
    const container = document.getElementById('toastContainer');
    if (!container) return;

    const configs = {
        success: { bg: 'rgba(5,150,105,0.92)', icon: 'bi-check-circle-fill' },
        error: { bg: 'rgba(220,38,38,0.92)', icon: 'bi-x-circle-fill' },
        warning: { bg: 'rgba(217,119,6,0.92)', icon: 'bi-exclamation-triangle-fill' },
        info: { bg: 'rgba(249,115,22,0.92)', icon: 'bi-info-circle-fill' },
    };
    const cfg = configs[type] || configs.info;

    const toast = document.createElement('div');
    toast.className = 'toast align-items-center text-white border-0';
    toast.setAttribute('role', 'alert');
    toast.style.background = cfg.bg;
    toast.innerHTML = `
        <div class="d-flex">
            <div class="toast-body d-flex align-items-center gap-2">
                <i class="bi ${cfg.icon}"></i>${message}
            </div>
            <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
        </div>`;
    container.appendChild(toast);
    const bsToast = new bootstrap.Toast(toast, { delay: 3500 });
    bsToast.show();
    toast.addEventListener('hidden.bs.toast', () => toast.remove());
}

// ---- Confirm Dialog ----
function confirmDelete(message, callback) {
    if (confirm(message || '确定要删除吗？此操作不可恢复。')) {
        callback();
    }
}

// ---- Formatters ----
function formatDate(dateStr) {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleDateString('zh-CN');
}

function formatDateTime(dateStr) {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleString('zh-CN');
}

// ---- Image Preview ----
function setupImagePreview(inputEl, previewEl) {
    inputEl.addEventListener('change', function () {
        const file = this.files[0];
        if (file) {
            const reader = new FileReader();
            reader.onload = function (e) {
                previewEl.src = e.target.result;
                previewEl.style.display = 'block';
            };
            reader.readAsDataURL(file);
        }
    });
}

// ---- Drag & Drop Zone ----
function setupDragDrop(zoneEl, inputEl, onFileCallback) {
    ['dragenter', 'dragover'].forEach(evt => {
        zoneEl.addEventListener(evt, (e) => { e.preventDefault(); e.stopPropagation(); zoneEl.classList.add('drag-over'); });
    });
    ['dragleave', 'drop'].forEach(evt => {
        zoneEl.addEventListener(evt, (e) => { e.preventDefault(); e.stopPropagation(); zoneEl.classList.remove('drag-over'); });
    });
    zoneEl.addEventListener('click', () => inputEl.click());
    zoneEl.addEventListener('drop', (e) => {
        const file = e.dataTransfer.files[0];
        if (file && onFileCallback) onFileCallback(file);
    });
    inputEl.addEventListener('change', function () {
        if (this.files[0] && onFileCallback) onFileCallback(this.files[0]);
    });
}

// ---- Ripple Effect on Buttons ----
function addRippleEffect() {
    document.querySelectorAll('.btn').forEach(btn => {
        if (btn.dataset.ripple) return;
        btn.dataset.ripple = 'true';
        btn.addEventListener('click', function(e) {
            const ripple = document.createElement('span');
            ripple.className = 'ripple';
            const rect = btn.getBoundingClientRect();
            const size = Math.max(rect.width, rect.height);
            ripple.style.width = ripple.style.height = size + 'px';
            ripple.style.left = (e.clientX - rect.left - size / 2) + 'px';
            ripple.style.top = (e.clientY - rect.top - size / 2) + 'px';
            btn.appendChild(ripple);
            ripple.addEventListener('animationend', () => ripple.remove());
        });
    });
}

// ---- Count Up Animation for Stats ----
function countUp(el, target, duration = 800) {
    const start = 0;
    const startTime = performance.now();
    function update(now) {
        const elapsed = now - startTime;
        const progress = Math.min(elapsed / duration, 1);
        const eased = 1 - Math.pow(1 - progress, 2);
        const current = Math.round(start + (target - start) * eased);
        el.textContent = current;
        if (progress < 1) requestAnimationFrame(update);
        else el.textContent = target;
    }
    requestAnimationFrame(update);
}

// ---- Notification Dropdown ----
function loadNotificationDropdown() {
    const menu = document.getElementById('notificationMenu');
    if (!menu) return;
    fetch('/reminders/unread')
        .then(r => r.json())
        .then(data => {
            if (!data.reminders || data.reminders.length === 0) {
                menu.querySelector('div').textContent = '暂无未读提醒';
                return;
            }
            const items = data.reminders.map(r =>
                `<a class="dropdown-item d-flex justify-content-between align-items-center" href="/reminders">
                    <div>
                        <div class="small fw-bold">${r.title}</div>
                        <small class="text-muted">${r.reminder_date}${r.pet_name ? ' · ' + r.pet_name : ''}</small>
                    </div>
                    <i class="bi bi-chevron-right small text-muted"></i>
                </a>`
            ).join('');
            menu.innerHTML = `
                <li class="dropdown-header d-flex justify-content-between">
                    <span>最近提醒</span>
                    <a href="/reminders" class="small">查看全部</a>
                </li>
                <li><hr class="dropdown-divider mt-1"></li>
                ${items}`;
        })
        .catch(() => {
            menu.querySelector('div').textContent = '加载失败';
        });
}

// ---- Page Load Complete ----
document.addEventListener('DOMContentLoaded', () => {
    // Nav active state
    const currentPath = window.location.pathname;
    document.querySelectorAll('.nav-link').forEach(link => {
        const href = link.getAttribute('href');
        if (href === currentPath || (currentPath.startsWith(href) && href !== '/')) {
            link.classList.add('active');
        }
    });

    // Init effects
    addRippleEffect();

    // Count up stats on dashboard
    document.querySelectorAll('.stat-card .stat-value').forEach(el => {
        const target = parseInt(el.textContent);
        if (!isNaN(target) && target > 0) {
            countUp(el, target, 800);
        }
    });

    // Notification dropdown - load on bell click
    const bell = document.getElementById('notificationBell');
    if (bell) {
        bell.addEventListener('click', loadNotificationDropdown, {once: true});
    }
});
