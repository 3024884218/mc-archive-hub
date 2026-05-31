/**
 * MC Archive Hub v2 — App 控制器
 * 路由分发、状态变更、事件处理、主题管理
 * 依赖: api.js, data.js, ui.js 先于本文件加载
 */
window.MC = window.MC || {};

// ================================================================
// Toast 通知
// ================================================================
MC.Toast = {
  icons: {
    success: '✓',
    error: '✕',
    info: 'ℹ',
  },

  show(msg, type) {
    const container = document.getElementById('toast-container');
    const toast = document.createElement('div');
    const icon = this.icons[type] || '';

    toast.className = 'toast ' + (type || '');
    toast.innerHTML = icon
      ? `<span class="toast-icon">${icon}</span><span>${msg}</span>`
      : `<span>${msg}</span>`;

    container.appendChild(toast);

    // 自动消失
    setTimeout(() => {
      toast.style.opacity = '0';
      toast.style.transform = 'translateX(30px)';
      toast.style.transition = 'all .25s ease';
      setTimeout(() => toast.remove(), 250);
    }, 3000);
  },
};

// ================================================================
// Modal 管理
// ================================================================
MC.Modal = {
  open(id) {
    const el = document.getElementById(id);
    if (!el) return;
    el.classList.remove('hidden');
    // 聚焦第一个输入框
    const firstInput = el.querySelector('input:not([type="hidden"])');
    if (firstInput) setTimeout(() => firstInput.focus(), 100);
  },
  close(id) {
    const el = document.getElementById(id);
    if (el) el.classList.add('hidden');
  },
  closeAll() {
    document.querySelectorAll('.modal-overlay:not(.hidden)').forEach(m => m.classList.add('hidden'));
  },
};

// 点击遮罩关闭
document.addEventListener('click', e => {
  if (e.target.classList.contains('modal-overlay')) {
    e.target.classList.add('hidden');
  }
});

// ESC 关闭弹窗
document.addEventListener('keydown', e => {
  if (e.key === 'Escape') {
    MC.closeLightbox();
    MC.Modal.closeAll();
  }
});

// ================================================================
// 主题切换
// ================================================================
MC.toggleTheme = function() {
  MC.Theme.toggle();
};

// 移动端导航菜单
MC.toggleMobileNav = function() {
  var nav = document.getElementById('nav-links');
  var btn = document.getElementById('hamburger-btn');
  if (nav) nav.classList.toggle('nav-open');
  if (btn) btn.classList.toggle('open');
};

// ================================================================
// 灯箱放大（支持多图导航）
// ================================================================
MC._lightboxImages = [];
MC._lightboxIndex = 0;

MC.openLightbox = function(src, images) {
  var img = document.getElementById('lightbox-img');
  var overlay = document.getElementById('lightbox');
  if (!img || !overlay) return;

  // 支持传入图片数组或单张图片
  if (images && images.length > 0) {
    MC._lightboxImages = images;
    MC._lightboxIndex = images.indexOf(src);
    if (MC._lightboxIndex < 0) MC._lightboxIndex = 0;
  } else {
    MC._lightboxImages = [src];
    MC._lightboxIndex = 0;
  }

  MC._updateLightboxImage();
  overlay.classList.remove('hidden');
};

MC._updateLightboxImage = function() {
  var img = document.getElementById('lightbox-img');
  if (img && MC._lightboxImages[MC._lightboxIndex]) {
    img.src = MC._lightboxImages[MC._lightboxIndex];
  }
  // 更新导航按钮
  var prevBtn = document.getElementById('lightbox-prev');
  var nextBtn = document.getElementById('lightbox-next');
  if (prevBtn) prevBtn.style.display = MC._lightboxImages.length > 1 ? '' : 'none';
  if (nextBtn) nextBtn.style.display = MC._lightboxImages.length > 1 ? '' : 'none';
};

MC.lightboxPrev = function(e) {
  e.stopPropagation();
  if (MC._lightboxImages.length <= 1) return;
  MC._lightboxIndex = (MC._lightboxIndex - 1 + MC._lightboxImages.length) % MC._lightboxImages.length;
  MC._updateLightboxImage();
};

MC.lightboxNext = function(e) {
  e.stopPropagation();
  if (MC._lightboxImages.length <= 1) return;
  MC._lightboxIndex = (MC._lightboxIndex + 1) % MC._lightboxImages.length;
  MC._updateLightboxImage();
};

MC.closeLightbox = function() {
  var overlay = document.getElementById('lightbox');
  if (overlay) overlay.classList.add('hidden');
};

// 全局事件代理：所有 .sponsor-qr-img 点击放大
document.addEventListener('click', function(e) {
  var img = e.target.closest('.sponsor-qr-img');
  if (img && img.src) {
    MC.openLightbox(img.src);
  }
});

// ================================================================
// 赞助充电
// ================================================================
MC.openSponsorModal = function(wxUrl, zfbUrl, authorName) {
  var wxSrc = wxUrl || '/images/wx.jpg';
  var zfbSrc = zfbUrl || '/images/zfb.jpg';
  var title = authorName ? '⚡ 为 ' + MC.UI.esc(authorName) + ' 充电' : '⚡ 支持网站';
  var subtitle = authorName
    ? '如果你喜欢这个存档，可以请作者喝杯咖啡 ☕'
    : '如果你觉得本站对你有帮助，可以请站长喝杯咖啡 ☕';

  var content = document.getElementById('auth-modal-content');
  content.innerHTML = '<div class="modal-header">' +
    '<h2 class="modal-title">' + title + '</h2>' +
    '<button class="modal-close" onclick="MC.Modal.close(\'auth-modal\')" aria-label="关闭">&times;</button>' +
    '</div>' +
    '<div class="modal-body" style="text-align:center">' +
    '<p style="font-size:var(--fs-sm);color:var(--c-text-secondary);margin-bottom:var(--sp-4)">' + subtitle + '</p>' +
    '<div class="sponsor-qr-row">' +
    '<div class="sponsor-qr-card">' +
    '<img class="sponsor-qr-img" src="' + wxSrc + '" alt="微信收款码" loading="lazy" style="cursor:zoom-in" title="点击放大">' +
    '<div class="sponsor-qr-label"><span style="color:#07c160;font-weight:var(--fw-semibold)">微信</span> 扫一扫</div>' +
    '</div>' +
    '<div class="sponsor-qr-card">' +
    '<img class="sponsor-qr-img" src="' + zfbSrc + '" alt="支付宝收款码" loading="lazy" style="cursor:zoom-in" title="点击放大">' +
    '<div class="sponsor-qr-label"><span style="color:#1677ff;font-weight:var(--fw-semibold)">支付宝</span> 扫一扫</div>' +
    '</div>' +
    '</div>' +
    '<p style="font-size:var(--fs-xs);color:var(--c-text-tertiary);margin-top:var(--sp-3)">感谢你的支持 ❤️</p>' +
    '</div>';

  MC.Modal.open('auth-modal');
};

// ================================================================
// 通知系统（后端待实现，前端暂时禁用）
// ================================================================
MC.updateNotifBadge = function() {};
MC.openNotifications = function() {};
    MC.updateNotifBadge();

    var content = document.getElementById('auth-modal-content');
    content.innerHTML = '<div class="modal-header">' +
      '<h2 class="modal-title">🔔 通知</h2>' +
      '<button class="modal-close" onclick="MC.Modal.close(\'auth-modal\')" aria-label="关闭">&times;</button>' +
      '</div>' +
      '<div class="modal-body">' +
      (notifs.length === 0
        ? '<p style="text-align:center;color:var(--c-text-tertiary);padding:var(--sp-6)">暂无通知</p>'
        : notifs.map(function(n) {
            var icon = n.type === 'comment' ? '💬' : '❤️';
            var text = n.type === 'comment'
              ? '<strong>' + MC.UI.esc(n.actorName) + '</strong> 评论了你的存档 <strong>' + MC.UI.esc(n.archiveTitle) + '</strong>'
              : '<strong>' + MC.UI.esc(n.actorName) + '</strong> 点赞了你的存档 <strong>' + MC.UI.esc(n.archiveTitle) + '</strong>';
            return '<div class="notif-item' + (!n.read ? ' unread' : '') + '" onclick="MC.Modal.close(\'auth-modal\');MC.navigate(\'detail\',' + n.archiveId + ')" style="cursor:pointer;padding:12px;border-bottom:1px solid var(--c-border-light);transition:background var(--t-fast)">' +
              '<div style="display:flex;align-items:flex-start;gap:10px">' +
              '<span style="font-size:20px">' + icon + '</span>' +
              '<div style="flex:1;min-width:0">' +
              '<div style="font-size:var(--fs-sm);line-height:1.4">' + text + '</div>' +
              '<div style="font-size:var(--fs-xs);color:var(--c-text-tertiary);margin-top:4px">' + MC.UI.fmtDate(n.createdAt) + '</div>' +
              '</div>' +
              (!n.read ? '<span style="width:8px;height:8px;border-radius:50%;background:var(--c-primary);flex-shrink:0;margin-top:6px"></span>' : '') +
              '</div></div>';
          }).join('')
      ) +
      '</div>';
    MC.Modal.open('auth-modal');
  } catch (e) {
    MC.Toast.show('加载通知失败', 'error');
  }
};

// ================================================================
// 认证弹窗
// ================================================================
MC.openAuthModal = function(mode) {
  const isLogin = mode === 'login';
  const content = document.getElementById('auth-modal-content');

  content.innerHTML = `
    <div class="modal-header">
      <h2 class="modal-title">${isLogin ? '登录' : '创建账号'}</h2>
      <button class="modal-close" onclick="MC.Modal.close('auth-modal')" aria-label="关闭">&times;</button>
    </div>
    <div class="modal-body">
      <div class="form-group">
        <label class="form-label">用户名</label>
        <input class="form-input" id="auth-username" type="text"
          placeholder="输入用户名" maxlength="30" autocomplete="username"
          oninput="this.classList.remove('error')">
      </div>
      ${!isLogin ? `<div class="form-group">
        <label class="form-label">邮箱</label>
        <input class="form-input" id="auth-email" type="email"
          placeholder="your@email.com" autocomplete="email"
          oninput="this.classList.remove('error')">
      </div>
      <div class="form-group">
        <label class="form-label">密码</label>
        <input class="form-input" id="auth-password" type="password"
          placeholder="输入密码（至少8位）" autocomplete="new-password"
          oninput="this.classList.remove('error')">
      </div>
      <div class="form-group">
        <label class="form-label">确认密码</label>
        <input class="form-input" id="auth-password2" type="password"
          placeholder="再次输入密码" autocomplete="new-password"
          oninput="this.classList.remove('error')">
      </div>` : `<div class="form-group">
        <label class="form-label">密码</label>
        <input class="form-input" id="auth-password" type="password"
          placeholder="输入密码" autocomplete="current-password"
          oninput="this.classList.remove('error')">
      </div>`}
      <div class="form-error" id="auth-error">
        <span class="form-error-icon">⚠️</span>
        <span id="auth-error-msg"></span>
      </div>
      <button class="btn btn-primary" style="width:100%;justify-content:center;margin-top:8px" id="auth-submit">
        ${isLogin ? '登录' : '注册'}
      </button>
      <p style="text-align:center;margin-top:16px;font-size:14px;color:var(--c-text-secondary)">
        ${isLogin ? '还没有账号？' : '已有账号？'}
        <button style="background:none;border:none;color:var(--c-primary);cursor:pointer;font-size:14px;font-weight:500"
          onclick="MC.openAuthModal('${isLogin ? 'register' : 'login'}')">
          ${isLogin ? '立即注册' : '去登录'}
        </button>
        ${isLogin ? '<span style="margin:0 8px;color:var(--c-border)">|</span><button style="background:none;border:none;color:var(--c-text-tertiary);cursor:pointer;font-size:14px" onclick="MC.openForgotPasswordModal()">忘记密码</button>' : ''}
      </p>
    </div>`;

  MC.Modal.open('auth-modal');

  // 提交按钮
  const submitBtn = document.getElementById('auth-submit');
  submitBtn.onclick = () => {
    const uEl = document.getElementById('auth-username');
    const pEl = document.getElementById('auth-password');
    const u = uEl.value.trim();
    const p = pEl.value;
    const errEl = document.getElementById('auth-error');
    const errMsg = document.getElementById('auth-error-msg');

    const setErr = (msg, fieldId) => {
      errMsg.textContent = msg;
      errEl.classList.add('show');
      if (fieldId) {
        document.getElementById(fieldId).classList.add('error');
      }
    };

    // 清除之前的错误
    errEl.classList.remove('show');
    [uEl, pEl].forEach(el => el.classList.remove('error'));

    if (!u) return setErr('请输入用户名', 'auth-username');
    if (u.length > 30) return setErr('用户名最多 30 个字符', 'auth-username');
    if (!p) return setErr('请输入密码', 'auth-password');
    if (p.length < 8) return setErr('密码至少 8 位', 'auth-password');
    let email = '';
    if (!isLogin) {
      const p2El = document.getElementById('auth-password2');
      p2El.classList.remove('error');
      const p2 = p2El.value;
      if (!p2) return setErr('请再次输入密码', 'auth-password2');
      if (p !== p2) return setErr('两次密码不一致', 'auth-password2');

      // 邮箱校验
      const eEl = document.getElementById('auth-email');
      eEl.classList.remove('error');
      email = eEl.value.trim();
      if (!email) return setErr('请输入邮箱', 'auth-email');
      if (!email.match(/^[\w.-]+@[\w.-]+\.[a-zA-Z]{2,}$/))
        return setErr('邮箱格式不正确', 'auth-email');
    }

    submitBtn.disabled = true;
    submitBtn.textContent = '处理中...';

    const fn = isLogin ? MC.handleLogin : MC.handleRegister;
    fn(u, p, email).finally(() => {
      submitBtn.disabled = false;
      submitBtn.textContent = isLogin ? '登录' : '注册';
    });
  };

  // Enter 提交
  content.querySelectorAll('input').forEach(inp => {
    inp.addEventListener('keydown', e => {
      if (e.key === 'Enter') document.getElementById('auth-submit').click();
    });
    // 输入时清除该字段错误
    inp.addEventListener('input', () => inp.classList.remove('error'));
  });
};

MC.handleLogin = async function(u, p) {
  try {
    const r = await MC.API.login(u, p);
    MC.State.currentUser = r.user;
    MC.Modal.close('auth-modal');
    MC.UI.updateHeader();
    MC.Toast.show('登录成功！欢迎回来 👋', 'success');
    MC.renderPage();
  } catch (e) {
    // 如果是邮箱未验证，显示引导重发
    const msg = e.message || '';
    if (msg.includes('邮箱尚未验证')) {
      _showAuthError(msg, true);
    } else {
      _showAuthError(msg);
    }
  }
};

MC.handleRegister = async function(u, p, email) {
  try {
    const r = await MC.API.register(u, p, email);
    // 注册成功但需要验证邮箱
    MC.Modal.close('auth-modal');
    MC.Toast.show(r.message, 'success');
  } catch (e) {
    _showAuthError(e.message);
  }
};

/** 在认证弹窗中显示错误消息 */
function _showAuthError(msg, showRetry) {
  const errEl = document.getElementById('auth-error');
  const errMsg = document.getElementById('auth-error-msg');
  if (errEl && errMsg) {
    errMsg.textContent = msg;
    if (showRetry) {
      // 邮箱未验证时追加重发按钮
      errMsg.innerHTML = msg +
        '<br><a href="#" style="color:var(--c-primary);font-weight:600;margin-top:4px;display:inline-block" onclick="MC.openForgotPasswordModal();return false">重新发送验证邮件</a>';
    }
    errEl.classList.add('show');
    const pwdEl = document.getElementById('auth-password');
    if (pwdEl) pwdEl.classList.add('error');
  }
}

MC.handleLogout = async function() {
  try { await MC.API.logout(); } catch {}
  MC.State.currentUser = null;
  MC.UI.updateHeader();
  MC.navigate('home');
  MC.Toast.show('已退出登录', 'info');
};

// ================================================================
// 上传弹窗
// ================================================================

let _uploadedImages = [];

MC.openUploadModal = function() {
  if (!MC.State.currentUser) return MC.openAuthModal('login');

  _uploadedImages = [];
  const content = document.getElementById('upload-modal-content');

  content.innerHTML = `
    <div class="modal-header">
      <h2 class="modal-title">上传存档</h2>
      <button class="modal-close" onclick="MC.Modal.close('upload-modal')" aria-label="关闭">&times;</button>
    </div>
    <div class="modal-body">
      <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px">
        <div class="form-group">
          <label class="form-label" for="up-title">存档名称 *</label>
          <input class="form-input" id="up-title" type="text"
            placeholder="给你的存档起个名字" maxlength="100">
        </div>
        <div class="form-group">
          <label class="form-label" for="up-cat">分类 *</label>
          <select class="form-select" id="up-cat">
            ${MC.CATEGORIES.filter(c => c.id !== 'all').map(c =>
              `<option value="${c.id}">${c.icon} ${c.name}</option>`).join('')}
          </select>
        </div>
        <div class="form-group">
          <label class="form-label" for="up-ver">MC 版本 *</label>
          <input class="form-input" id="up-ver" type="text"
            placeholder="如 1.21.4、1.20.1" list="ver-list" maxlength="20">
          <datalist id="ver-list">
            ${(MC.State.meta.mcVersions || []).map(([v]) => `<option value="${v}">`).join('')}
          </datalist>
        </div>
        <div class="form-group">
          <label class="form-label" for="up-loader">Mod 加载器 *</label>
          <select class="form-select" id="up-loader">
            ${Object.entries(MC.LOADER_LABELS).map(([k, v]) =>
              `<option value="${k}">${MC.UI.loaderIcon(k)} ${v}</option>`).join('')}
          </select>
        </div>
      </div>
      <div class="form-group">
        <label class="form-label" for="up-desc">存档介绍 *</label>
        <textarea class="form-textarea" id="up-desc"
          placeholder="描述你的存档内容、特色、适用版本等..."
          maxlength="2000"></textarea>
        <div class="form-hint">至少 10 个字</div>
      </div>
      <div class="form-group">
        <label class="form-label">存档文件</label>
        <input type="file" id="up-file" accept=".zip,.rar,.7z"
          style="font-size:14px">
        <div class="form-hint">支持 .zip/.rar/.7z，最大 500MB</div>
      </div>
      <div class="form-group">
        <label class="form-label">展示图片（可选，最多 5 张）</label>
        <div class="image-upload-area" onclick="document.getElementById('up-imgs').click()">
          <div class="upload-icon">🖼️</div>
          <div class="upload-text">点击上传图片（JPG/PNG）</div>
        </div>
        <input type="file" id="up-imgs" accept="image/*" multiple
          style="display:none" onchange="MC.handleImageSelect(event)">
        <div class="image-preview-list" id="img-preview"></div>
        <div class="form-hint">第一张将作为封面展示</div>
      </div>
      <div class="form-error" id="up-error">
        <span class="form-error-icon">⚠️</span>
        <span id="up-error-msg"></span>
      </div>
      <button class="btn btn-primary btn-lg" style="width:100%;justify-content:center"
        id="up-submit" onclick="MC.handleUpload()">
        发布存档
      </button>
    </div>`;

  MC.Modal.open('upload-modal');

  // 恢复草稿
  setTimeout(function() {
    var draft = MC._loadDraft();
    if (draft) {
      if (draft.title) { var tEl = document.getElementById('up-title'); if (tEl) tEl.value = draft.title; }
      if (draft.cat) { var cEl = document.getElementById('up-cat'); if (cEl) cEl.value = draft.cat; }
      if (draft.ver) { var vEl = document.getElementById('up-ver'); if (vEl) vEl.value = draft.ver; }
      if (draft.loader) { var lEl = document.getElementById('up-loader'); if (lEl) lEl.value = draft.loader; }
      if (draft.desc) { var dEl = document.getElementById('up-desc'); if (dEl) dEl.value = draft.desc; }
      MC.Toast.show('已恢复上次未完成的表单 📝', 'info');
    }

    // 绑定自动保存
    ['up-title', 'up-cat', 'up-ver', 'up-loader', 'up-desc'].forEach(function(id) {
      var el = document.getElementById(id);
      if (el) el.addEventListener('input', MC._saveDraft);
    });
  }, 150);
};

MC._saveDraft = function() {
  var draft = {
    title: (document.getElementById('up-title') || {}).value || '',
    cat: (document.getElementById('up-cat') || {}).value || '',
    ver: (document.getElementById('up-ver') || {}).value || '',
    loader: (document.getElementById('up-loader') || {}).value || '',
    desc: (document.getElementById('up-desc') || {}).value || '',
  };
  if (draft.title || draft.desc) {
    sessionStorage.setItem('mc-upload-draft', JSON.stringify(draft));
  }
};

MC._loadDraft = function() {
  try {
    var raw = sessionStorage.getItem('mc-upload-draft');
    return raw ? JSON.parse(raw) : null;
  } catch (e) { return null; }
};

MC._clearDraft = function() {
  sessionStorage.removeItem('mc-upload-draft');
};

MC.handleImageSelect = function(e) {
  Array.from(e.target.files).forEach(f => {
    if (_uploadedImages.length < 5) _uploadedImages.push(f);
  });
  e.target.value = '';

  const container = document.getElementById('img-preview');
  if (!container) return;

  container.innerHTML = _uploadedImages.map((f, i) =>
    `<div class="image-preview-item">
      <img src="${URL.createObjectURL(f)}" alt="预览图 ${i + 1}">
      <button class="image-preview-remove"
        onclick="_uploadedImages.splice(${i},1);MC.handleImageSelect({target:{files:[]}})"
        aria-label="删除图片">&times;</button>
     </div>`
  ).join('');
};

MC.handleUpload = async function() {
  const title = document.getElementById('up-title').value.trim();
  const desc = document.getElementById('up-desc').value.trim();
  const errEl = document.getElementById('up-error');
  const errMsg = document.getElementById('up-error-msg');
  const submitBtn = document.getElementById('up-submit');

  const setErr = msg => {
    if (errMsg) errMsg.textContent = msg;
    errEl.classList.add('show');
  };
  if (errEl) errEl.classList.remove('show');

  if (!title) return setErr('请输入存档名称');
  if (!desc || desc.length < 10) return setErr('介绍至少 10 个字');

  submitBtn.disabled = true;
  submitBtn.textContent = '发布中...';

  const fd = new FormData();
  fd.append('title', title);
  fd.append('category', document.getElementById('up-cat').value);
  fd.append('mcVersion', document.getElementById('up-ver').value);
  fd.append('modLoader', document.getElementById('up-loader').value);
  fd.append('description', desc);

  const file = document.getElementById('up-file').files[0];
  if (file) fd.append('file', file);

  _uploadedImages.forEach(img => fd.append('images', img));

  try {
    await MC.API.createArchive(fd);
    MC._clearDraft();
    MC.Modal.close('upload-modal');
    MC.Toast.show('存档发布成功！🎉', 'success');
    MC.navigate('home');
  } catch (e) {
    setErr(e.message);
  } finally {
    submitBtn.disabled = false;
    submitBtn.textContent = '发布存档';
  }
};

// ================================================================
// 点赞 / 收藏 / 下载
// ================================================================
MC.handleLike = async function(id, evt) {
  if (evt) evt.stopPropagation();
  if (!MC.State.currentUser) return MC.openAuthModal('login');

  try {
    const r = await MC.API.toggleLike(id);
    // 即时更新详情页
    const likeBtn = document.getElementById('detail-like-btn');
    if (likeBtn) {
      likeBtn.classList.toggle('liked', r.liked);
      const countEl = document.getElementById('detail-like-count');
      if (countEl) countEl.textContent = r.likeCount;
    }
    // 即时更新卡片上的点赞按钮
    var card = document.querySelector('.archive-card[data-id="' + id + '"]');
    if (card) {
      var btn = card.querySelector('.card-action-btn');
      if (btn) {
        btn.classList.toggle('liked', r.liked);
        var span = btn.querySelector('span');
        if (span) span.textContent = r.likeCount || 0;
      }
    }
    MC.Toast.show(r.liked ? '已点赞 ❤️' : '已取消点赞', r.liked ? 'success' : 'info');
  } catch (e) {
    MC.Toast.show(e.message, 'error');
  }
};

MC.handleBookmark = async function(id, evt) {
  if (evt) evt.stopPropagation();
  if (!MC.State.currentUser) return MC.openAuthModal('login');

  try {
    const r = await MC.API.toggleBookmark(id);
    const bmBtn = document.getElementById('detail-bookmark-btn');
    if (bmBtn) bmBtn.classList.toggle('bookmarked', r.bookmarked);
    // 即时更新卡片上的收藏按钮
    var card = document.querySelector('.archive-card[data-id="' + id + '"]');
    if (card) {
      var bmCardBtn = card.querySelector('.card-action-btn:last-of-type');
      if (bmCardBtn) bmCardBtn.classList.toggle('bookmarked', r.bookmarked);
    }
    MC.Toast.show(r.bookmarked ? '已加入收藏 ⭐' : '已取消收藏', r.bookmarked ? 'success' : 'info');
  } catch (e) {
    MC.Toast.show(e.message, 'error');
  }
};

MC.handleDislike = async function(id, evt) {
  if (evt) evt.stopPropagation();
  if (!MC.State.currentUser) return MC.openAuthModal('login');

  try {
    const r = await MC.API.toggleDislike(id);
    if (r.deleted) {
      MC.Toast.show(r.message, 'info');
      MC.navigate('home');
      return;
    }
    const btn = document.getElementById('detail-dislike-btn');
    if (btn) {
      btn.classList.toggle('disliked', r.disliked);
      btn.innerHTML = '<svg><use href="#icon-thumbs-down"/></svg> 踩 ' + (r.dislikeCount || 0);
    }
    MC.Toast.show(r.disliked ? '已踩 👎' : '已取消踩', 'info');
  } catch (e) {
    MC.Toast.show(e.message, 'error');
  }
};

MC.handleAddComment = async function(archiveId) {
  var input = document.getElementById('comment-input');
  var btn = document.getElementById('comment-submit');
  if (!input) return;
  var content = input.value.trim();
  if (!content) return;
  btn.disabled = true;
  try {
    await MC.API.addComment(archiveId, content);
    input.value = '';
    MC.Toast.show('评论已发表', 'success');
    // 刷新详情页评论
    MC.navigate('detail', archiveId);
  } catch (e) {
    MC.Toast.show(e.message, 'error');
  } finally {
    btn.disabled = false;
  }
};

MC.handleDeleteComment = async function(commentId) {
  if (!confirm('确定删除这条评论吗？')) return;
  try {
    await MC.API.deleteComment(commentId);
    MC.Toast.show('评论已删除', 'info');
    MC.navigate('detail', MC.State.archiveDetail.id);
  } catch (e) {
    MC.Toast.show(e.message, 'error');
  }
};

MC.handleDownload = function(id) {
  window.open('/api/archives/' + id + '/download', '_blank');
  MC.Toast.show('下载开始！', 'success');
};

// ================================================================
// 路由
// ================================================================
MC.navigate = function(page, data) {
  MC.State.currentPage = page;
  MC.State.searchQuery = '';
  MC.State.archiveDetail = null;

  // 清空搜索
  const inp = document.getElementById('search-input');
  if (inp) inp.value = '';
  const clearBtn = document.getElementById('search-clear');
  if (clearBtn) clearBtn.classList.add('hidden');

  // 导航高亮
  document.querySelectorAll('.nav-link').forEach(el =>
    el.classList.toggle('active', el.dataset.nav === page));

  // 更新浏览器 URL
  var url = '/';
  if (page === 'detail' && data) {
    url = '/archive/' + data;
  } else if (page === 'profile') {
    url = '/profile';
  }
  if (window.location.pathname !== url) {
    history.pushState({ page: page, data: data }, '', url);
  }

  MC.renderPage(data);
};

// 从 URL 解析路由状态
MC._parseRoute = function() {
  var path = window.location.pathname;
  if (path === '/profile') {
    return { page: 'profile', data: null };
  }
  var archiveMatch = path.match(/^\/archive\/(\d+)$/);
  if (archiveMatch) {
    return { page: 'detail', data: parseInt(archiveMatch[1]) };
  }
  var authorMatch = path.match(/^\/author\/(\d+)$/);
  if (authorMatch) {
    var id = parseInt(authorMatch[1]);
    MC.State.authorId = id;
    MC.State.currentPage = 0;
    return { page: 'author', data: { id: id } };
  }
  return { page: 'home', data: null };
};

// 监听浏览器前进/后退
window.addEventListener('popstate', function(e) {
  if (e.state && e.state.page) {
    MC.State.currentPage = e.state.page;
    MC.State.searchQuery = '';
    MC.State.archiveDetail = null;
    var inp = document.getElementById('search-input');
    if (inp) inp.value = '';
    var clearBtn = document.getElementById('search-clear');
    if (clearBtn) clearBtn.classList.add('hidden');
    document.querySelectorAll('.nav-link').forEach(function(el) {
      el.classList.toggle('active', el.dataset.nav === e.state.page);
    });
    MC.renderPage(e.state.data);
  } else {
    // 页面首次加载或手动输入 URL
    var route = MC._parseRoute();
    MC.State.currentPage = route.page;
    document.querySelectorAll('.nav-link').forEach(function(el) {
      el.classList.toggle('active', el.dataset.nav === route.page);
    });
    MC.renderPage(route.data);
  }
});

// 导航到作者存档列表
MC.navigateAuthor = function(authorId, authorName) {
  if (!authorId) return;
  MC.State.currentPage = 'author';
  MC.State.authorId = authorId;
  MC.State.authorName = authorName;
  MC.State.searchQuery = '';
  MC.State.currentPage = 0;
  var inp = document.getElementById('search-input');
  if (inp) inp.value = '';
  var clearBtn = document.getElementById('search-clear');
  if (clearBtn) clearBtn.classList.add('hidden');
  document.querySelectorAll('.nav-link').forEach(function(el) {
    el.classList.remove('active');
  });
  history.pushState({ page: 'author', data: { id: authorId, name: authorName } }, '', '/author/' + authorId);
  MC.renderAuthorPage();
};

// ================================================================
// 搜索
// ================================================================
MC.doSearch = function() {
  const q = document.getElementById('search-input').value.trim();
  MC.State.searchQuery = q;
  MC.State.currentPage = 0;
  document.getElementById('search-clear').classList.toggle('hidden', !q);

  if (q) {
    MC.State.activeCategory = 'all';
    MC.State.activeMcVersion = 'all';
    MC.State.activeModLoader = 'all';
  }

  MC.renderHome(true);
};

MC.clearSearch = function() {
  document.getElementById('search-input').value = '';
  MC.State.searchQuery = '';
  document.getElementById('search-clear').classList.add('hidden');
  MC.renderHome(false);
};

// ================================================================
// 页面渲染入口
// ================================================================
MC.renderPage = function(data) {
  const p = MC.State.currentPage;
  if (p === 'home') MC.renderHome();
  else if (p === 'profile') MC.renderProfile();
  else if (p === 'detail') MC.renderDetail(data || MC.State.archiveDetail?.id);
  else if (p === 'author') MC.renderAuthorPage();
  else MC.renderHome(); // fallback
};

// ================================================================
// 首页
// ================================================================
MC.renderHome = async function(fullRender) {
  const main = document.getElementById('main-content');

  if (fullRender !== false) {
    // 显示 Hero（仅在非搜索状态）
    const showHero = !MC.State.searchQuery;
    main.innerHTML = `
      ${showHero ? MC.UI.renderHero() : ''}
      <div class="app-container">
        <div class="home-layout">
          <aside class="sidebar" id="sidebar">
            <div style="text-align:center;padding:30px 0;color:var(--c-text-tertiary);font-size:13px">
              加载筛选中...
            </div>
          </aside>
          <div class="home-main">
            <div class="home-topbar">
              <h2 class="section-title" id="section-title">${MC.UI.getPageTitle()}</h2>
              <div class="sort-toggle" id="sort-toggle">
                ${MC.SORT_OPTIONS.map(s =>
                  `<button class="sort-btn ${MC.State.sortMode === s.id ? 'active' : ''}"
                    onclick="MC.State.sortMode='${s.id}';MC.State.currentPage=0;MC.renderHome(false)">${s.label}</button>`
                ).join('')}
              </div>
            </div>
            <div class="archive-grid" id="archive-grid">
              ${MC.UI.skeletonGrid(6)}
            </div>
            <div class="pagination" id="pagination"></div>
          </div>
        </div>
      </div>`;
  }

  // 加载元数据
  if (fullRender !== false) {
    try { MC.State.meta = await MC.API.getMetadata(); } catch {}
  }

  // 加载存档数据
  try {
    var result;
    if (MC.State.searchQuery) {
      result = await MC.API.searchArchives(MC.State.searchQuery, MC.State.currentPage, MC.State.pageSize);
    } else {
      result = await MC.API.listArchives({
        category: MC.State.activeCategory,
        mcVersion: MC.State.activeMcVersion,
        modLoader: MC.State.activeModLoader,
        sort: MC.State.sortMode,
        page: MC.State.currentPage,
        size: MC.State.pageSize,
      });
    }
    // 处理分页响应格式: { content, totalElements, totalPages, currentPage, size }
    if (result && result.content !== undefined) {
      MC.State.archives = result.content;
      MC.State.totalPages = result.totalPages || 0;
      MC.State.currentPage = result.currentPage || 0;
      MC.State.totalElements = result.totalElements || 0;
    } else {
      // 兼容旧格式（数组直接返回）
      MC.State.archives = Array.isArray(result) ? result : [];
      MC.State.totalPages = 1;
      MC.State.currentPage = 0;
    }
  } catch (e) {
    MC.State.archives = [];
    if (fullRender !== false) MC.Toast.show('加载失败: ' + e.message, 'error');
  }

  // 更新侧边栏
  const sidebar = document.getElementById('sidebar');
  if (sidebar) {
    sidebar.innerHTML = `
      <div class="sidebar-item all-item ${MC.State.activeCategory === 'all' &&
        MC.State.activeMcVersion === 'all' && MC.State.activeModLoader === 'all' ? 'active' : ''}"
        onclick="MC.State.activeCategory='all';MC.State.activeMcVersion='all';MC.State.activeModLoader='all';MC.State.currentPage=0;MC.renderHome(false)"
        role="button" tabindex="0">
        ✨ 展示全部
      </div>
      ${MC.UI.sidebarCategories(MC.State.activeCategory)}
      ${MC.UI.sidebarVersions(MC.State.activeMcVersion)}
      ${MC.UI.sidebarLoaders(MC.State.activeModLoader)}
    `;
  }

  // 更新标题
  const titleEl = document.getElementById('section-title');
  if (titleEl) titleEl.textContent = MC.UI.getPageTitle();

  // 更新排序按钮
  const sortToggle = document.getElementById('sort-toggle');
  if (sortToggle) {
    sortToggle.innerHTML = MC.SORT_OPTIONS.map(s =>
      `<button class="sort-btn ${MC.State.sortMode === s.id ? 'active' : ''}"
        onclick="MC.State.sortMode='${s.id}';MC.State.currentPage=0;MC.renderHome(false)">${s.label}</button>`
    ).join('');
  }

  // 渲染卡片网格
  const grid = document.getElementById('archive-grid');
  if (grid) {
    if (MC.State.archives.length === 0) {
      grid.innerHTML = MC.UI.emptyState(
        '📭',
        MC.State.searchQuery ? '未找到相关存档' : '暂无存档',
        MC.State.searchQuery ? '试试其他关键词？' : '换个筛选条件，或上传第一个存档吧！',
        !MC.State.searchQuery
          ? '<button class="btn btn-primary" onclick="MC.openUploadModal()">上传第一个存档</button>'
          : ''
      );
    } else {
      grid.innerHTML = MC.State.archives.map((a, i) => MC.UI.archiveCard(a, i)).join('');
    }
  }

  // 渲染分页
  MC.renderPagination();

  // 搜索模式下隐藏 hero
  if (MC.State.searchQuery && fullRender !== false) {
    const hero = document.querySelector('.hero');
    if (hero) hero.style.display = 'none';
  }
};

// ================================================================
// 详情页
// ================================================================
MC.renderDetail = async function(id) {
  if (!id) return MC.navigate('home');

  const main = document.getElementById('main-content');
  main.innerHTML = `<div class="app-container" style="text-align:center;padding:80px">
    <p style="color:var(--c-text-tertiary)">加载中...</p>
  </div>`;

  try {
    MC.State.archiveDetail = await MC.API.getArchive(id);
  } catch {
    MC.Toast.show('存档不存在或已被删除', 'error');
    MC.navigate('home');
    return;
  }

  const a = MC.State.archiveDetail;
  const cover = a.images?.[0]?.url;

  main.innerHTML = `<div class="app-container">
    <!-- 返回按钮 -->
    <div class="detail-back">
      <button class="btn btn-ghost btn-sm" onclick="MC.navigate('home')">
        <svg width="14" height="14"><use href="#icon-arrow-left"/></svg>
        返回列表
      </button>
    </div>

    <div class="detail-header">
      <!-- 图片画廊 -->
      <div class="detail-gallery">
        ${cover
          ? `<img class="detail-main-image" id="detail-main-img"
               src="${cover}" alt="${MC.UI.esc(a.title)}">`
          : `<div class="detail-main-placeholder">🧱</div>`
        }
        ${a.images && a.images.length > 1 ? `
          <div class="detail-thumbnails">
            ${a.images.map((img, i) =>
              `<img class="detail-thumb ${i === 0 ? 'active' : ''}"
                src="${img.url}" alt="截图 ${i + 1}"
                onclick="var el=document.getElementById('detail-main-img');el.src=this.src;this.parentElement.querySelectorAll('.detail-thumb').forEach(t=>t.classList.remove('active'));this.classList.add('active')">`
            ).join('')}
          </div>` : ''}
      </div>

      <!-- 详情信息 -->
      <div class="detail-info">
        <div class="detail-tags">
          <span class="detail-category">${MC.UI.catIcon(a.category)} ${MC.UI.catName(a.category)}</span>
          <span class="detail-meta-tag">MC ${MC.UI.esc(a.mcVersion || '?')}</span>
          <span class="detail-meta-tag">${MC.UI.loaderIcon(a.modLoader)} ${MC.UI.esc(MC.UI.loaderLabel(a.modLoader))}</span>
        </div>

        <h1 class="detail-title">${MC.UI.esc(a.title)}</h1>

        <div class="detail-author-row">
          <div class="detail-author-avatar">${MC.UI.esc((a.authorName || '?')[0])}</div>
          <div>
            <div class="detail-author-name">${MC.UI.esc(a.authorName || '匿名')}</div>
            <div class="detail-date">发布于 ${MC.UI.fmtDate(a.createdAt)}</div>
          </div>
        </div>

        <div class="detail-actions">
          <button class="detail-action-btn ${a.liked ? 'liked' : ''}" id="detail-like-btn"
            onclick="MC.handleLike(${a.id})">
            <svg><use href="#icon-heart"/></svg>
            点赞 <span id="detail-like-count">${a.likeCount || 0}</span>
          </button>
          <button class="detail-action-btn ${a.bookmarked ? 'bookmarked' : ''}" id="detail-bookmark-btn"
            onclick="MC.handleBookmark(${a.id})">
            <svg><use href="#icon-bookmark"/></svg>
            收藏 ${a.bookmarked ? '✓' : ''}
          </button>
          <button class="btn btn-primary btn-lg" onclick="MC.handleDownload(${a.id})">
            <svg width="16" height="16"><use href="#icon-download"/></svg>
            下载存档 (${a.downloadCount || 0})
          </button>
        </div>

        <div class="detail-desc-section">
          <div class="detail-desc md-content">${MC.md(a.description || '暂无介绍')}</div>
        </div>
      </div>
    </div>
  </div>`;
};

// ================================================================
// 作者主页
// ================================================================
MC.renderAuthorPage = async function() {
  var authorId = MC.State.authorId;
  var authorName = MC.State.authorName || '作者';
  var main = document.getElementById('main-content');
  main.innerHTML = '<div class="app-container" style="text-align:center;padding:80px"><p style="color:var(--c-text-tertiary)">加载中...</p></div>';

  try {
    var result = await MC.API.getArchivesByAuthor(authorId, MC.State.currentPage, MC.State.pageSize);
    var archives = result.content || [];
    MC.State.totalPages = result.totalPages || 0;
  } catch (e) {
    MC.Toast.show('加载失败: ' + e.message, 'error');
    MC.navigate('home');
    return;
  }

  main.innerHTML = '<div class="app-container">' +
    '<div class="detail-back">' +
      '<button class="btn btn-ghost btn-sm" onclick="MC.navigate(\'home\')">' +
        '<svg width="14" height="14"><use href="#icon-arrow-left"/></svg>返回列表' +
      '</button>' +
    '</div>' +
    '<div class="home-topbar">' +
      '<h2 class="section-title">' + MC.UI.esc(authorName) + ' 的存档</h2>' +
      '<span style="font-size:var(--fs-sm);color:var(--c-text-tertiary)">共 ' + (MC.State.totalElements || archives.length) + ' 个</span>' +
    '</div>' +
    '<div class="archive-grid" id="archive-grid">' +
      (archives.length === 0
        ? MC.UI.emptyState('📭', '暂无存档', '该作者还没有发布任何存档')
        : archives.map(function(a, i) { return MC.UI.archiveCard(a, i); }).join('')
      ) +
    '</div>' +
    '<div class="pagination" id="pagination"></div>' +
  '</div>';

  MC.renderPagination();
};

// ================================================================
// 个人主页
// ================================================================
MC.renderProfile = async function() {
  if (!MC.State.currentUser) return MC.openAuthModal('login');

  const main = document.getElementById('main-content');
  main.innerHTML = `<div class="app-container" style="text-align:center;padding:80px">
    <p style="color:var(--c-text-tertiary)">加载中...</p>
  </div>`;

  // 刷新用户信息（获取最新 email 状态）
  try { MC.State.currentUser = await MC.API.checkAuth(); } catch {}

  let myArchives = [], myBookmarks = [], myDownloads = [];
  try { myArchives = await MC.API.getMyArchives(); } catch {}
  try { myBookmarks = await MC.API.getMyBookmarks(); } catch {}
  try { myDownloads = await MC.API.getMyDownloads(); } catch {}

  const u = MC.State.currentUser;
  const hasEmail = !!u.email;
  const emailVerified = u.emailVerified;

  main.innerHTML = `<div class="app-container">
    <!-- 个人信息卡片 -->
    <div class="profile-header">
      <div class="profile-avatar"
        style="${u.avatarUrl ? `background-image:url('${u.avatarUrl}');background-size:cover` : ''}">
        ${u.avatarUrl ? '' : MC.UI.esc(u.username[0])}
      </div>
      <div class="profile-info">
        <h1>${MC.UI.esc(u.nickname || u.username)}</h1>
        <p>用户名: ${MC.UI.esc(u.username)} · 已发布 ${myArchives.length} 个存档 · 收藏 ${myBookmarks.length} 个存档</p>
      </div>
      <div style="display:flex;gap:8px;margin-left:auto">
        <button class="btn btn-ghost btn-sm" onclick="MC.openEditProfileModal()">编辑资料</button>
        <button class="btn btn-ghost btn-sm" onclick="MC.handleLogout()">退出登录</button>
      </div>
    </div>

    <!-- 账号安全卡片 -->
    <div style="background:var(--c-surface);border:1px solid var(--c-border-light);border-radius:var(--r-lg);padding:var(--sp-6);margin-bottom:var(--sp-8)">
      <h3 style="font-size:var(--fs-lg);font-weight:var(--fw-semibold);margin-bottom:var(--sp-4)">账号安全</h3>
      <div style="display:flex;align-items:center;justify-content:space-between;padding:12px 0;border-bottom:1px solid var(--c-border-light)">
        <div>
          <div style="font-size:var(--fs-sm);font-weight:var(--fw-medium)">绑定邮箱</div>
          <div style="font-size:var(--fs-xs);color:var(--c-text-tertiary);margin-top:2px">用于找回密码</div>
        </div>
        <div style="display:flex;align-items:center;gap:8px">
          ${hasEmail
            ? `<span style="font-size:var(--fs-sm);color:var(--c-text-secondary)">${MC.UI.esc(u.email)}</span>
               ${emailVerified
                 ? '<span style="font-size:var(--fs-xs);color:var(--c-success);background:rgba(34,197,94,0.1);padding:2px 8px;border-radius:var(--r-pill)">已验证</span>'
                 : '<span style="font-size:var(--fs-xs);color:var(--c-warning);background:rgba(245,158,11,0.1);padding:2px 8px;border-radius:var(--r-pill);cursor:pointer" onclick="MC.openBindEmailModal()" title="点击重新发送验证邮件">未验证（点击重发）</span>'
               }
               <button class="btn btn-ghost btn-sm" onclick="MC.openBindEmailModal()">更换</button>`
            : `<span style="font-size:var(--fs-sm);color:var(--c-text-tertiary)">未绑定</span>
               <button class="btn btn-primary btn-sm" onclick="MC.openBindEmailModal()">绑定邮箱</button>`
          }
        </div>
      </div>
      <div style="display:flex;align-items:center;justify-content:space-between;padding:12px 0">
        <div>
          <div style="font-size:var(--fs-sm);font-weight:var(--fw-medium)">修改密码</div>
          <div style="font-size:var(--fs-xs);color:var(--c-text-tertiary);margin-top:2px">定期更换密码更安全</div>
        </div>
        <button class="btn btn-ghost btn-sm" onclick="MC.openChangePasswordModal()">修改</button>
      </div>
    </div>

    <!-- 收款码设置 -->
    <div style="background:var(--c-surface);border:1px solid var(--c-border-light);border-radius:var(--r-lg);padding:var(--sp-6);margin-bottom:var(--sp-8)">
      <h3 style="font-size:var(--fs-lg);font-weight:var(--fw-semibold);margin-bottom:var(--sp-4)">收款码设置</h3>
      <p style="font-size:var(--fs-xs);color:var(--c-text-tertiary);margin-bottom:var(--sp-4)">设置后，你的存档详情页「为作者充电」将展示你自己的收款码</p>
      <div class="sponsor-qr-row">
        <div class="sponsor-qr-card" style="position:relative;cursor:pointer" onclick="document.getElementById('profile-qr-wx').click()">
          ${u.wechatQrCodeUrl
            ? '<img class="sponsor-qr-img" src="' + u.wechatQrCodeUrl + '" alt="微信收款码">'
            : '<div class="sponsor-qr-img" style="display:flex;align-items:center;justify-content:center;background:var(--c-bg);aspect-ratio:1;border-radius:var(--r-md);border:1px dashed var(--c-border)"><span style="color:var(--c-text-tertiary);font-size:var(--fs-xs)">点击上传</span></div>'}
          <div style="margin-top:4px;font-size:var(--fs-xs);color:#07c160;text-align:center">微信</div>
          <input type="file" id="profile-qr-wx" accept="image/*" style="display:none" onchange="MC.handleQrUpload('wx',event)">
        </div>
        <div class="sponsor-qr-card" style="position:relative;cursor:pointer" onclick="document.getElementById('profile-qr-ali').click()">
          ${u.alipayQrCodeUrl
            ? '<img class="sponsor-qr-img" src="' + u.alipayQrCodeUrl + '" alt="支付宝收款码">'
            : '<div class="sponsor-qr-img" style="display:flex;align-items:center;justify-content:center;background:var(--c-bg);aspect-ratio:1;border-radius:var(--r-md);border:1px dashed var(--c-border)"><span style="color:var(--c-text-tertiary);font-size:var(--fs-xs)">点击上传</span></div>'}
          <div style="margin-top:4px;font-size:var(--fs-xs);color:#1677ff;text-align:center">支付宝</div>
          <input type="file" id="profile-qr-ali" accept="image/*" style="display:none" onchange="MC.handleQrUpload('ali',event)">
        </div>
      </div>
    </div>

    <!-- 联系邮箱设置 -->
    <div style="background:var(--c-surface);border:1px solid var(--c-border-light);border-radius:var(--r-lg);padding:var(--sp-6);margin-bottom:var(--sp-8)">
      <h3 style="font-size:var(--fs-lg);font-weight:var(--fw-semibold);margin-bottom:var(--sp-4)">联系邮箱</h3>
      <p style="font-size:var(--fs-xs);color:var(--c-text-tertiary);margin-bottom:var(--sp-3)">此邮箱将公开显示在你的存档详情页，供其他用户联系你</p>
      <div style="display:flex;gap:8px;align-items:center">
        <input class="form-input" id="profile-contact-email" type="email"
          value="${MC.UI.esc(u.contactEmail || '')}" placeholder="your@email.com"
          style="flex:1;max-width:320px">
        <button class="btn btn-primary btn-sm" onclick="MC.handleUpdateContactEmail()">保存</button>
        ${u.contactEmail ? '<button class="btn btn-ghost btn-sm" onclick="MC.handleUpdateContactEmail(true)" style="color:var(--c-error)">清除</button>' : ''}
      </div>
    </div>

    <!-- 我的存档 -->
    <div class="home-topbar">
      <h2 class="section-title">我的存档</h2>
      <span style="font-size:var(--fs-sm);color:var(--c-text-tertiary)">共 ${myArchives.length} 个</span>
    </div>
    <div class="archive-grid" id="my-archives-grid">
      ${myArchives.length === 0
        ? MC.UI.emptyState('📭', '还没有发布存档', '点击右上角「上传存档」开始分享吧！',
            '<button class="btn btn-primary" onclick="MC.openUploadModal()">上传第一个存档</button>')
        : myArchives.map((a, i) => MC.UI.archiveCard(a, i)).join('')
      }
    </div>

    <!-- 我的收藏 -->
    <div class="home-topbar" style="margin-top:40px">
      <h2 class="section-title">我的收藏</h2>
      <span style="font-size:var(--fs-sm);color:var(--c-text-tertiary)">共 ${myBookmarks.length} 个</span>
    </div>
    <div class="archive-grid" id="my-bookmarks-grid">
      ${myBookmarks.length === 0
        ? MC.UI.emptyState('⭐', '还没有收藏', '浏览存档时点击收藏按钮即可保存')
        : myBookmarks.map((a, i) => MC.UI.archiveCard(a, i)).join('')
      }
    </div>

    <!-- 下载历史 -->
    <div class="home-topbar" style="margin-top:40px">
      <h2 class="section-title">下载历史</h2>
      <span style="font-size:var(--fs-sm);color:var(--c-text-tertiary)">共 ${myDownloads.length} 个</span>
    </div>
    <div class="archive-grid" id="my-downloads-grid">
      ${myDownloads.length === 0
        ? MC.UI.emptyState('⬇️', '还没有下载记录', '浏览存档时点击下载即可记录')
        : myDownloads.map((a, i) => MC.UI.archiveCard(a, i)).join('')
      }
    </div>
  </div>`;
};

// ================================================================
// 绑定邮箱弹窗
// ================================================================
MC.openBindEmailModal = function() {
  if (!MC.State.currentUser) return;

  const content = document.getElementById('auth-modal-content');
  const u = MC.State.currentUser;

  content.innerHTML = `
    <div class="modal-header">
      <h2 class="modal-title">绑定邮箱</h2>
      <button class="modal-close" onclick="MC.Modal.close('auth-modal')" aria-label="关闭">&times;</button>
    </div>
    <div class="modal-body">
      ${u.email
        ? `<p style="font-size:var(--fs-sm);color:var(--c-text-secondary);margin-bottom:var(--sp-4)">
             当前邮箱：<strong>${MC.UI.esc(u.email)}</strong>
             ${u.emailVerified ? '（已验证）' : '（<span style="color:var(--c-warning)">未验证</span>）'}
           </p>`
        : ''
      }
      <div class="form-group">
        <label class="form-label" for="bind-email">${u.email ? '新邮箱' : '邮箱地址'}</label>
        <input class="form-input" id="bind-email" type="email"
          placeholder="your@email.com" autocomplete="email">
        <div class="form-hint">绑定后可用于找回密码</div>
      </div>
      <div class="form-error" id="bind-error">
        <span class="form-error-icon">⚠️</span>
        <span id="bind-error-msg"></span>
      </div>
      <button class="btn btn-primary" style="width:100%;justify-content:center"
        id="bind-submit" onclick="MC.handleBindEmail()">
        发送验证邮件
      </button>
    </div>`;

  MC.Modal.open('auth-modal');
};

MC.handleBindEmail = async function() {
  const email = document.getElementById('bind-email').value.trim();
  const errEl = document.getElementById('bind-error');
  const errMsg = document.getElementById('bind-error-msg');
  const btn = document.getElementById('bind-submit');

  const setErr = m => { errMsg.textContent = m; errEl.classList.add('show'); };

  if (!email) return setErr('请输入邮箱');
  if (!email.match(/^[\w.-]+@[\w.-]+\.[a-zA-Z]{2,}$/)) return setErr('邮箱格式不正确');

  btn.disabled = true;
  btn.textContent = '发送中...';

  try {
    const r = await MC.API.bindEmail(email);
    MC.Modal.close('auth-modal');
    MC.Toast.show(r.message, 'success');
    // 刷新用户信息
    MC.State.currentUser = await MC.API.checkAuth();
    MC.renderProfile();
  } catch (e) {
    setErr(e.message);
  } finally {
    btn.disabled = false;
    btn.textContent = '发送验证邮件';
  }
};

// ================================================================
// 忘记密码弹窗
// ================================================================
MC.openForgotPasswordModal = function() {
  MC.Modal.close('auth-modal');

  const content = document.getElementById('auth-modal-content');
  content.innerHTML = `
    <div class="modal-header">
      <h2 class="modal-title">找回密码</h2>
      <button class="modal-close" onclick="MC.Modal.close('auth-modal')" aria-label="关闭">&times;</button>
    </div>
    <div class="modal-body">
      <p style="font-size:var(--fs-sm);color:var(--c-text-secondary);margin-bottom:var(--sp-4)">
        输入已绑定且验证过的邮箱，我们将发送重置链接。
      </p>
      <div class="form-group">
        <label class="form-label" for="reset-email">邮箱</label>
        <input class="form-input" id="reset-email" type="email"
          placeholder="your@email.com" autocomplete="email">
      </div>
      <div class="form-error" id="reset-error">
        <span class="form-error-icon">⚠️</span>
        <span id="reset-error-msg"></span>
      </div>
      <button class="btn btn-primary" style="width:100%;justify-content:center"
        id="reset-submit" onclick="MC.handleForgotPassword()">
        发送重置链接
      </button>
      <p style="text-align:center;margin-top:16px;font-size:14px;color:var(--c-text-secondary)">
        <button style="background:none;border:none;color:var(--c-primary);cursor:pointer;font-size:14px;font-weight:500"
          onclick="MC.openAuthModal('login')">← 返回登录</button>
      </p>
    </div>`;

  MC.Modal.open('auth-modal');
};

MC.handleForgotPassword = async function() {
  const email = document.getElementById('reset-email').value.trim();
  const errEl = document.getElementById('reset-error');
  const errMsg = document.getElementById('reset-error-msg');
  const btn = document.getElementById('reset-submit');

  const setErr = m => { errMsg.textContent = m; errEl.classList.add('show'); };
  errEl.classList.remove('show');

  if (!email) return setErr('请输入邮箱');

  btn.disabled = true;
  btn.textContent = '发送中...';

  try {
    const r = await MC.API.forgotPassword(email);
    MC.Modal.close('auth-modal');
    MC.Toast.show(r.message, 'success');
  } catch (e) {
    setErr(e.message);
  } finally {
    btn.disabled = false;
    btn.textContent = '发送重置链接';
  }
};

// ================================================================
// 编辑资料弹窗
// ================================================================
MC.openEditProfileModal = function() {
  if (!MC.State.currentUser) return;
  const u = MC.State.currentUser;
  const content = document.getElementById('auth-modal-content');
  const avatarSrc = u.avatarUrl || '';

  content.innerHTML = `
    <div class="modal-header">
      <h2 class="modal-title">编辑资料</h2>
      <button class="modal-close" onclick="MC.Modal.close('auth-modal')" aria-label="关闭">&times;</button>
    </div>
    <div class="modal-body" style="text-align:center">
      <!-- 头像 -->
      <div style="position:relative;display:inline-block;cursor:pointer;margin-bottom:20px"
        onclick="document.getElementById('avatar-file').click()">
        <div id="avatar-preview" style="width:80px;height:80px;border-radius:50%;overflow:hidden;margin:0 auto;
          background:linear-gradient(135deg,var(--c-primary),var(--c-primary-dark));
          display:flex;align-items:center;justify-content:center;font-size:28px;font-weight:var(--fw-bold);color:white;
          ${avatarSrc ? `background-image:url('${avatarSrc}');background-size:cover` : ''}">
          ${avatarSrc ? '' : MC.UI.esc(u.username[0])}
        </div>
        <div style="position:absolute;bottom:0;right:calc(50% - 40px);background:var(--c-text);color:white;
          width:24px;height:24px;border-radius:50%;font-size:12px;display:flex;align-items:center;justify-content:center;
          border:2px solid var(--c-surface)">+</div>
      </div>
      <input type="file" id="avatar-file" accept="image/*" style="display:none"
        onchange="MC.handleAvatarSelect(event)">
      <div class="form-hint" style="margin-bottom:16px">点击上传头像（JPG/PNG，≤5MB）</div>

      <div class="form-group" style="text-align:left">
        <label class="form-label">用户名</label>
        <input class="form-input" type="text" value="${MC.UI.esc(u.username)}" disabled
          style="background:var(--c-surface-secondary);color:var(--c-text-tertiary)">
        <div class="form-hint">用户名不可修改</div>
      </div>
      <div class="form-group" style="text-align:left">
        <label class="form-label" for="edit-nickname">昵称</label>
        <input class="form-input" id="edit-nickname" type="text"
          value="${MC.UI.esc(u.nickname || u.username)}" maxlength="30"
          placeholder="输入昵称">
      </div>
      <div class="form-error" id="edit-error">
        <span class="form-error-icon">⚠️</span>
        <span id="edit-error-msg"></span>
      </div>
      <button class="btn btn-primary" style="width:100%;justify-content:center"
        onclick="MC.handleUpdateProfile()">保存</button>
    </div>`;

  MC.Modal.open('auth-modal');
};

let _avatarFile = null;
MC.handleAvatarSelect = function(e) {
  const file = e.target.files[0];
  if (!file) return;
  _avatarFile = file;

  // 预览
  const reader = new FileReader();
  reader.onload = ev => {
    const preview = document.getElementById('avatar-preview');
    if (preview) {
      preview.style.backgroundImage = `url('${ev.target.result}')`;
      preview.style.backgroundSize = 'cover';
      preview.textContent = '';
    }
  };
  reader.readAsDataURL(file);
};

MC.handleQrUpload = async function(type, e) {
  const file = e.target.files[0];
  if (!file) return;
  const fd = new FormData();
  fd.append('file', file);
  try {
    await MC.API.uploadQrCode(type, fd);
    // 刷新用户状态
    MC.State.currentUser = await MC.API.checkAuth();
    MC.Toast.show((type === 'wx' ? '微信' : '支付宝') + '收款码已更新', 'success');
    MC.renderProfile();
  } catch (err) {
    MC.Toast.show('上传失败: ' + err.message, 'error');
  }
};

MC.handleUpdateContactEmail = async function(clear) {
  var email = clear ? '' : document.getElementById('profile-contact-email').value.trim();
  try {
    await MC.API.updateContactEmail(email);
    MC.State.currentUser = await MC.API.checkAuth();
    MC.Toast.show(clear ? '联系邮箱已清除' : '联系邮箱已更新', 'success');
    MC.renderProfile();
  } catch (err) {
    MC.Toast.show(err.message, 'error');
  }
};

MC.handleUpdateProfile = async function() {
  const nickname = document.getElementById('edit-nickname').value.trim();
  const errEl = document.getElementById('edit-error');
  const errMsg = document.getElementById('edit-error-msg');
  const setErr = m => { errMsg.textContent = m; errEl.classList.add('show'); };

  if (!nickname) return setErr('昵称不能为空');

  try {
    // 先上传头像（如果有选）
    if (_avatarFile) {
      const fd = new FormData();
      fd.append('avatar', _avatarFile);
      const r = await MC.API.uploadAvatar(fd);
      MC.State.currentUser = r.user;
    }
    // 再更新昵称
    const r = await MC.API.updateProfile(nickname);
    MC.State.currentUser = r.user;
    MC.Modal.close('auth-modal');
    MC.UI.updateHeader();
    MC.Toast.show('资料已更新', 'success');
    _avatarFile = null;
    MC.renderProfile();
  } catch (e) {
    setErr(e.message);
  }
};

// ================================================================
// 修改密码弹窗
// ================================================================
MC.openChangePasswordModal = function() {
  if (!MC.State.currentUser) return;
  const content = document.getElementById('auth-modal-content');

  content.innerHTML = `
    <div class="modal-header">
      <h2 class="modal-title">修改密码</h2>
      <button class="modal-close" onclick="MC.Modal.close('auth-modal')" aria-label="关闭">&times;</button>
    </div>
    <div class="modal-body">
      <div class="form-group">
        <label class="form-label" for="ch-old-pwd">当前密码</label>
        <input class="form-input" id="ch-old-pwd" type="password" placeholder="输入当前密码">
      </div>
      <div class="form-group">
        <label class="form-label" for="ch-new-pwd">新密码</label>
        <input class="form-input" id="ch-new-pwd" type="password" placeholder="至少8位">
      </div>
      <div class="form-group">
        <label class="form-label" for="ch-new-pwd2">确认新密码</label>
        <input class="form-input" id="ch-new-pwd2" type="password" placeholder="再次输入新密码">
      </div>
      <div class="form-error" id="chpwd-error">
        <span class="form-error-icon">⚠️</span>
        <span id="chpwd-error-msg"></span>
      </div>
      <button class="btn btn-primary" style="width:100%;justify-content:center"
        id="chpwd-submit" onclick="MC.handleChangePassword()">修改密码</button>
    </div>`;

  MC.Modal.open('auth-modal');
};

MC.handleChangePassword = async function() {
  const oldPwd = document.getElementById('ch-old-pwd').value;
  const newPwd = document.getElementById('ch-new-pwd').value;
  const newPwd2 = document.getElementById('ch-new-pwd2').value;
  const errEl = document.getElementById('chpwd-error');
  const errMsg = document.getElementById('chpwd-error-msg');
  const btn = document.getElementById('chpwd-submit');
  const setErr = m => { errMsg.textContent = m; errEl.classList.add('show'); };

  errEl.classList.remove('show');
  if (!oldPwd) return setErr('请输入当前密码');
  if (newPwd.length < 8) return setErr('新密码至少8位');
  if (newPwd !== newPwd2) return setErr('两次密码不一致');

  btn.disabled = true;
  btn.textContent = '修改中...';

  try {
    await MC.API.changePassword(oldPwd, newPwd);
    MC.Modal.close('auth-modal');
    MC.Toast.show('密码已修改', 'success');
  } catch (e) {
    setErr(e.message);
  } finally {
    btn.disabled = false;
    btn.textContent = '修改密码';
  }
};

// ================================================================
// 初始化
// ================================================================
MC.init = async function() {
  // 初始化主题
  MC.Theme.init();

  // 检查登录状态
  try {
    const u = await MC.API.checkAuth();
    MC.State.currentUser = u;
  } catch {}

  MC.UI.updateHeader();

  // 邮箱验证成功跳转回来，显示提示
  if (window.location.search.includes('verified=ok')) {
    MC.Toast.show('邮箱验证成功！现在可以登录了 🎉', 'success');
    // 清除 URL 参数
    window.history.replaceState({ page: 'home', data: null }, '', '/');
  }

  // 通知轮询（每30秒检查一次）
  if (MC.State.currentUser) {
    MC.updateNotifBadge();
    setInterval(MC.updateNotifBadge, 30000);
  }

  // 从 URL 解析初始路由状态
  var route = MC._parseRoute();
  MC.State.currentPage = route.page;
  // 用 replaceState 确保初始状态可被 popstate 使用
  var url = '/';
  if (route.page === 'detail' && route.data) {
    url = '/archive/' + route.data;
  } else if (route.page === 'profile') {
    url = '/profile';
  }
  history.replaceState({ page: route.page, data: route.data }, '', url);

  document.querySelectorAll('.nav-link').forEach(function(el) {
    el.classList.toggle('active', el.dataset.nav === route.page);
  });

  MC.renderPage(route.data);
};

// 启动应用
MC.init();
