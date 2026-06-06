/**
 * MC Archive Hub v3 · API 层
 * 封装所有后端请求，自动注入 CSRF Token
 */
window.MC = window.MC || {};

MC.API = {
  BASE: '',

  /**
   * 从 Cookie 中读取 CSRF Token
   * Spring Security CookieCsrfTokenRepository 将 token 存在 "XSRF-TOKEN" cookie 中
   */
  _getCsrfToken() {
    const name = 'XSRF-TOKEN=';
    const decodedCookie = decodeURIComponent(document.cookie);
    const ca = decodedCookie.split(';');
    for (let i = 0; i < ca.length; i++) {
      let c = ca[i].trim();
      if (c.indexOf(name) === 0) {
        return c.substring(name.length, c.length);
      }
    }
    return '';
  },

  async _fetch(url, options = {}) {
    // 合并 headers，对 POST/PUT/DELETE 请求注入 CSRF Token
    const headers = { ...options.headers };

    if (!headers['Content-Type'] && !(options.body instanceof FormData)) {
      headers['Content-Type'] = 'application/json';
    }

    // 对非 GET 请求自动添加 CSRF Token
    const method = (options.method || 'GET').toUpperCase();
    if (method !== 'GET' && method !== 'HEAD') {
      const csrf = this._getCsrfToken();
      if (csrf) {
        headers['X-XSRF-TOKEN'] = csrf;
      }
    }

    const res = await fetch(url, {
      credentials: 'same-origin',
      headers,
      ...options,
    });

    // 尝试解析 JSON，失败则返回纯文本
    let data;
    const contentType = res.headers.get('content-type');
    if (contentType && contentType.includes('application/json')) {
      data = await res.json();
    } else {
      data = await res.text();
    }

    if (!res.ok) {
      const msg = typeof data === 'object' ? (data.error || data.message || '请求失败') : (data || '请求失败');
      throw new Error(msg);
    }
    return data;
  },

  // ===== 分页参数辅助 =====
  _pageParams(page, size) {
    const p = new URLSearchParams();
    if (page !== undefined) p.set('page', page);
    if (size !== undefined) p.set('size', size);
    return p.toString();
  },

  // ===== Auth =====
  async checkAuth()    { return this._fetch('/api/auth/me'); },
  async login(u, p)    { return this._fetch('/api/auth/login',    { method:'POST', body:JSON.stringify({username:u, password:p}) }); },
  async register(u, p, email) { return this._fetch('/api/auth/register', { method:'POST', body:JSON.stringify({username:u, password:p, email: email || ''}) }); },
  async logout()       { return this._fetch('/api/auth/logout', { method:'POST' }); },

  // ===== Email =====
  async bindEmail(email)       { return this._fetch('/api/auth/bind-email',       { method:'POST', body:JSON.stringify({email}) }); },
  async updateProfile(nickname){ return this._fetch('/api/auth/update-profile',   { method:'POST', body:JSON.stringify({nickname}) }); },
  async changePassword(oldPwd, newPwd) { return this._fetch('/api/auth/change-password', { method:'POST', body:JSON.stringify({oldPassword:oldPwd, newPassword:newPwd}) }); },
  async uploadAvatar(fd)   { return this._fetch('/api/auth/upload-avatar', { method:'POST', body:fd }); },
  async uploadQrCode(type, fd) { return this._fetch('/api/auth/upload-qrcode?type=' + type, { method:'POST', body:fd }); },
  async updateContactEmail(email) { return this._fetch('/api/auth/update-contact-email', { method:'POST', body:JSON.stringify({email}) }); },
  async forgotPassword(email)  { return this._fetch('/api/auth/forgot-password',  { method:'POST', body:JSON.stringify({email}) }); },
  async resetPassword(token, password) { return this._fetch('/api/auth/reset-password', { method:'POST', body:JSON.stringify({token, password}) }); },

  // ===== 关注 =====
  async toggleFollow(userId) { return this._fetch('/api/auth/follow/' + userId, { method:'POST' }); },
  async getFollowing()       { return this._fetch('/api/auth/following'); },
  async getFollowers()       { return this._fetch('/api/auth/followers'); },

  // ===== 用户搜索 =====
  async searchUsers(q)       { return this._fetch('/api/auth/search?q=' + encodeURIComponent(q)); },
  async getUserProfile(id)   { return this._fetch('/api/auth/user/' + id); },

  // ===== Archives =====
  async listArchives(filters = {}) {
    const p = new URLSearchParams();
    Object.entries(filters).forEach(([k,v]) => { if (v && v !== 'all') p.set(k, v); });
    // 默认分页参数
    if (!p.has('page')) p.set('page', filters.page !== undefined ? filters.page : 0);
    if (!p.has('size')) p.set('size', filters.size !== undefined ? filters.size : 12);
    return this._fetch('/api/archives' + (p.toString() ? '?' + p : ''));
  },
  async getMetadata() { return this._fetch('/api/archives/metadata'); },
  async searchArchives(q, page, size)   {
    if (!q || !q.trim()) return this.listArchives({ page: page || 0, size: size || 12 });
    var url = '/api/archives/search?q=' + encodeURIComponent(q.trim());
    if (page !== undefined) url += '&page=' + page;
    if (size !== undefined) url += '&size=' + size;
    return this._fetch(url);
  },
  async getArchivesByAuthor(authorId, page, size) {
    var url = '/api/archives/author/' + authorId;
    var sep = '?';
    if (page !== undefined) { url += sep + 'page=' + page; sep = '&'; }
    if (size !== undefined) { url += sep + 'size=' + size; sep = '&'; }
    return this._fetch(url);
  },
  async getRelatedArchives(id) { return this._fetch('/api/archives/' + id + '/related'); },
  async getArchive(id)      { return this._fetch('/api/archives/' + id); },
  async createArchive(fd)   {
    return this._fetch('/api/archives', { method:'POST', body:fd });
  },
  async toggleLike(id)      { return this._fetch('/api/archives/' + id + '/like',     { method:'POST' }); },
  async toggleBookmark(id)  { return this._fetch('/api/archives/' + id + '/bookmark', { method:'POST' }); },
  async toggleDislike(id)   { return this._fetch('/api/archives/' + id + '/dislike', { method:'POST' }); },
  async getComments(id)     { return this._fetch('/api/archives/' + id + '/comments'); },
  async addComment(id, content) { return this._fetch('/api/archives/' + id + '/comments', { method:'POST', body:JSON.stringify({content}) }); },
  async deleteComment(commentId) { return this._fetch('/api/comments/' + commentId, { method:'DELETE' }); },
  async getMyArchives()     { return this._fetch('/api/archives/my/list'); },
  async getMyBookmarks()    { return this._fetch('/api/archives/my/bookmarks'); },
};
