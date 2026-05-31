/**
 * MC Archive Hub v2 — UI 渲染层
 * 卡片组件、骨架屏、Hero、侧边栏、空状态
 */
window.MC = window.MC || {};

MC.UI = {
  // ===== 基础工具 =====
  esc(str) {
    if (!str) return '';
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
  },

  fmtDate(ds) {
    if (!ds) return '';
    const d = new Date(ds);
    const now = new Date();
    const diff = now - d;
    const days = Math.floor(diff / 86400000);
    if (days === 0) return '今天';
    if (days === 1) return '昨天';
    if (days < 7) return days + '天前';
    if (days < 30) return Math.floor(days / 7) + '周前';
    if (days < 365) return Math.floor(days / 30) + '个月前';
    return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0');
  },

  fmtSize(bytes) {
    if (!bytes) return '0 B';
    const units = ['B', 'KB', 'MB', 'GB'];
    let i = 0;
    let size = bytes;
    while (size >= 1024 && i < units.length - 1) { size /= 1024; i++; }
    return size.toFixed(1) + ' ' + units[i];
  },

  catName(id) {
    return MC.CATEGORIES.find(c => c.id === id)?.name || id;
  },

  catIcon(id) {
    return MC.CATEGORIES.find(c => c.id === id)?.icon || '';
  },

  catColor(id) {
    return MC.CATEGORIES.find(c => c.id === id)?.color || '#6b7280';
  },

  loaderLabel(id) {
    return MC.LOADER_LABELS[id] || id;
  },

  loaderIcon(id) {
    return MC.LOADER_ICONS[id] || '';
  },

  // ===== 存档卡片 =====
  archiveCard(a, index) {
    const cover = a.images?.[0]?.url;
    const imgPart = cover
      ? `<div class="card-image-wrap">
           <img class="card-image" src="${cover}" alt="${this.esc(a.title)}" loading="lazy">
           <div class="card-image-overlay"></div>
         </div>`
      : `<div class="card-image-wrap">
           <div class="card-placeholder">🧱</div>
         </div>`;

    // 动画延迟
    const animDelay = (index || 0) * 0.05;

    return `<article class="archive-card" data-id="${a.id}" onclick="MC.navigate('detail',${a.id})" style="animation-delay:${animDelay}s">
      ${imgPart}
      <div class="card-body">
        <div class="card-tags">
          <span class="card-category">${this.catIcon(a.category)} ${this.catName(a.category)}</span>
          <span class="card-meta-tag">MC ${this.esc(a.mcVersion || '?')}</span>
          <span class="card-meta-tag">${this.loaderIcon(a.modLoader)} ${this.esc(this.loaderLabel(a.modLoader))}</span>
        </div>
        <h3 class="card-title">${this.esc(a.title)}</h3>
        <p class="card-desc">${this.esc((a.description || '').substring(0, 120))}</p>
        <div class="card-footer">
          <div class="card-author">
            <div class="card-author-avatar" onclick="event.stopPropagation();MC.navigateAuthor('${a.authorId || ''}','${this.esc(a.authorName || '匿名')}')" title="查看作者主页">${this.esc((a.authorName || '?')[0])}</div>
            <a class="card-author-name" href="#" onclick="event.stopPropagation();MC.navigateAuthor('${a.authorId || ''}','${this.esc(a.authorName || '匿名')}')" title="查看作者所有存档">${this.esc(a.authorName || '匿名')}</a>
          </div>
          <div class="card-actions">
            <span class="card-dl-count" title="下载次数">
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4M7 10l5 5 5-5M12 15V3"/></svg>
              ${a.downloadCount || 0}
            </span>
            <button class="card-action-btn ${a.liked ? 'liked' : ''}"
              onclick="MC.handleLike(${a.id},event)" title="点赞" aria-label="点赞">
              <svg><use href="#icon-heart"/></svg>
              <span>${a.likeCount || 0}</span>
            </button>
            <button class="card-action-btn ${a.bookmarked ? 'bookmarked' : ''}"
              onclick="MC.handleBookmark(${a.id},event)" title="收藏" aria-label="收藏">
              <svg><use href="#icon-bookmark"/></svg>
            </button>
          </div>
        </div>
      </div>
    </article>`;
  },

  // ===== 骨架屏 =====
  skeletonGrid(count) {
    let html = '';
    for (let i = 0; i < count; i++) {
      html += `<div class="skeleton-card" style="animation-delay:${i * 0.05}s">
        <div class="skeleton sk-img" style="border-radius:0"></div>
        <div class="sk-body">
          <div class="skeleton sk-line" style="width:50px;height:18px;margin-bottom:8px"></div>
          <div class="skeleton sk-line" style="width:85%"></div>
          <div class="skeleton sk-line" style="width:95%"></div>
          <div class="skeleton sk-line" style="width:40%"></div>
        </div>
      </div>`;
    }
    return html;
  },

  // ===== 侧边栏 =====
  sidebarFilter(title, icon, items, active, stateKey) {
    const rows = items.map(it => {
      const id = Array.isArray(it) ? it[0] : it.id;
      const name = Array.isArray(it) ? it[1] || id : it.name || id;
      const count = Array.isArray(it) ? it[2] : it.count;
      return `<div class="sidebar-item ${active === id ? 'active' : ''}"
          onclick="MC.State.${stateKey}='${id}';MC.State.pageNum=0;MC.renderHome(false)"
          role="button" tabindex="0"
          aria-pressed="${active === id}">
        <span>${name}</span>
        <span class="sidebar-count">${count || ''}</span>
      </div>`;
    }).join('');

    return `<div class="sidebar-section">
      <div class="sidebar-section-title">
        <svg><use href="#icon-category"/></svg>
        ${title}
      </div>
      ${rows}
    </div>`;
  },

  sidebarCategories(active) {
    return this.sidebarFilter('存档类型', '📂',
      MC.CATEGORIES.filter(c => c.id !== 'all').map(c => {
        const entry = (MC.State.meta.categories || []).find(e => e[0] === c.id);
        return [c.id, c.icon + ' ' + c.name, entry ? entry[1] : 0];
      }), active, 'activeCategory');
  },

  sidebarVersions(active) {
    const items = (MC.State.meta.mcVersions || []).map(([v, c]) => [v, 'MC ' + v, c]);
    return items.length ? this.sidebarFilter('MC 版本', '🎮', items, active, 'activeMcVersion') : '';
  },

  sidebarLoaders(active) {
    const items = (MC.State.meta.modLoaders || []).map(([l, c]) =>
      [l, this.loaderIcon(l) + ' ' + this.loaderLabel(l), c]);
    return items.length ? this.sidebarFilter('Mod 加载器', '⚙️', items, active, 'activeModLoader') : '';
  },

  // ===== Hero 区块 =====
  renderHero() {
    const totalArchives = MC.State.meta.totalArchives || 0;
    const totalUsers = MC.State.meta.totalUsers || 0;
    const totalDownloads = MC.State.meta.totalDownloads || 0;

    return `<section class="hero">
      <div class="hero-inner app-container">
        <div class="hero-badge">
          <span class="hero-badge-dot"></span>
          Minecraft 存档分享社区
        </div>
        <h1 class="hero-title">
          发现 & 分享<br><span class="highlight">Minecraft 存档</span>
        </h1>
        <p class="hero-subtitle">
          浏览来自全球玩家的精美生存、建筑、红石存档。上传你的作品，与社区一同成长。
        </p>
        <div class="hero-actions">
          <button class="btn btn-primary btn-lg" onclick="MC.State.searchQuery='';MC.renderHome(true)">
            探索存档
          </button>
          <button class="btn btn-accent btn-lg" onclick="MC.openUploadModal()">
            <svg width="16" height="16"><use href="#icon-plus"/></svg>
            上传存档
          </button>
          <button class="btn btn-ghost btn-lg" onclick="MC.openSponsorModal()" title="支持网站">
            <svg width="16" height="16"><use href="#icon-coin"/></svg>
            支持网站
          </button>
        </div>
        <div class="hero-stats">
          <div class="stat-item">
            <div class="stat-number">${totalArchives || 0}</div>
            <div class="stat-label">存档总数</div>
          </div>
          <div class="stat-item">
            <div class="stat-number">${totalUsers || 0}</div>
            <div class="stat-label">社区成员</div>
          </div>
          <div class="stat-item">
            <div class="stat-number">${totalDownloads || 0}</div>
            <div class="stat-label">累计下载</div>
          </div>
        </div>
      </div>
    </section>`;
  },

  // ===== 空状态 =====
  emptyState(icon, title, desc, action) {
    return `<div class="empty-state">
      <div class="empty-icon">${icon}</div>
      <div class="empty-title">${title}</div>
      <div class="empty-desc">${desc}</div>
      ${action || ''}
    </div>`;
  },

  // ===== Header 更新 =====
  updateHeader() {
    const u = MC.State.currentUser;
    const authBtns = document.getElementById('auth-buttons');
    const userMenu = document.getElementById('user-menu');
    const navProfile = document.getElementById('nav-profile');

    if (authBtns) authBtns.classList.toggle('hidden', !!u);
    if (userMenu) userMenu.classList.toggle('hidden', !u);
    if (navProfile) navProfile.style.display = u ? '' : 'none';

    if (u) {
      const uname = document.getElementById('header-username');
      const avatar = document.getElementById('header-avatar');
      if (uname) uname.textContent = u.username;
      if (avatar) {
        if (u.avatarUrl) {
          avatar.style.backgroundImage = `url('${u.avatarUrl}')`;
          avatar.style.backgroundSize = 'cover';
          avatar.textContent = '';
        } else {
          avatar.style.backgroundImage = '';
          avatar.textContent = u.username[0];
        }
      }
    }
  },

  // ===== 获取页面标题 =====
  getPageTitle() {
    if (MC.State.searchQuery) return '搜索: "' + MC.State.searchQuery + '"';
    if (MC.State.activeMcVersion !== 'all') return 'MC ' + MC.State.activeMcVersion;
    if (MC.State.activeModLoader !== 'all') return this.loaderLabel(MC.State.activeModLoader);
    if (MC.State.activeCategory !== 'all') return this.catName(MC.State.activeCategory);
    return '全部存档';
  },
};
