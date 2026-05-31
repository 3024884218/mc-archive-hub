/**
 * MC Archive Hub v2 — 增强补丁
 * 分页 / 编辑删除 / 进度条 / 拖拽 / Markdown / 相关推荐 / 视图切换
 */
(function() {

// ===== 分页渲染 =====
MC.renderGrid = function() {
  var grid = document.getElementById('archive-grid');
  if (!grid) return;
  var archives = MC.State.archives;
  grid.className = MC.State.viewMode === 'list' ? 'archive-grid archive-list' : 'archive-grid';
  if (!archives.length) return;
  grid.innerHTML = archives.map(function(a, i) { return MC.UI.archiveCard(a, i); }).join('');
};

MC.renderPagination = function() {
  var el = document.getElementById('pagination');
  if (!el) return;
  var total = MC.State.totalPages;
  var cur = MC.State.currentPage;
  if (total <= 1) { el.innerHTML = ''; return; }
  var html = '';
  html += '<button class="pagination-btn" ' + (cur===0?'disabled':'') + ' onclick="MC.goPage('+(cur-1)+')">◀</button>';
  for (var i = 0; i < total && i < 10; i++) {
    html += '<button class="pagination-btn ' + (i===cur?'active':'') + '" onclick="MC.goPage('+i+')">'+(i+1)+'</button>';
  }
  html += '<button class="pagination-btn" ' + (cur>=total-1?'disabled':'') + ' onclick="MC.goPage('+(cur+1)+')">▶</button>';
  el.innerHTML = html;
};

MC.goPage = function(page) {
  MC.State.currentPage = Math.max(0, Math.min(page, Math.max(0, MC.State.totalPages - 1)));
  MC.renderHome(false);
  // 滚动到顶部
  window.scrollTo({ top: 0, behavior: 'smooth' });
};

// ===== Markdown 简易渲染 =====
MC.md = function(text) {
  if (!text) return '';
  return text
    .replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;')
    .replace(/^### (.+)$/gm,'<h3>$1</h3>')
    .replace(/^## (.+)$/gm,'<h2>$1</h2>')
    .replace(/^# (.+)$/gm,'<h1>$1</h1>')
    .replace(/\*\*(.+?)\*\*/g,'<strong>$1</strong>')
    .replace(/\*(.+?)\*/g,'<em>$1</em>')
    .replace(/`(.+?)`/g,'<code>$1</code>')
    .replace(/^- (.+)$/gm,'<li>$1</li>')
    .replace(/(<li>.*<\/li>\n?)+/g,'<ul>$&</ul>')
    .replace(/\n\n/g,'</p><p>')
    .replace(/^(.+)$/gm, function(m) {
      if (m.match(/^<[huol]/)) return m;
      return m;
    });
};

// ===== 上传进度条 + 拖拽 =====
MC.openUploadModalEnhanced = function() {
  MC.openUploadModal();
  // 添加进度条和拖拽支持
  setTimeout(function() {
    var area = document.querySelector('.image-upload-area');
    if (!area) return;
    // 拖拽
    area.addEventListener('dragover', function(e) { e.preventDefault(); area.classList.add('dragover'); });
    area.addEventListener('dragleave', function() { area.classList.remove('dragover'); });
    area.addEventListener('drop', function(e) {
      e.preventDefault(); area.classList.remove('dragover');
      var files = e.dataTransfer.files;
      if (files.length) {
        Array.from(files).forEach(function(f) {
          if (_uploadedImages.length < 5) _uploadedImages.push(f);
        });
        MC.handleImageSelect({target:{files:[]}});
      }
    });
    // 进度条
    var bar = document.createElement('div');
    bar.className = 'upload-progress';
    bar.id = 'upload-progress';
    bar.innerHTML = '<div class="upload-progress-bar"><div class="upload-progress-fill" id="up-progress-fill"></div></div><div class="upload-progress-text" id="up-progress-text"></div>';
    area.parentNode.insertBefore(bar, area.nextSibling);
  }, 100);
};

// 覆盖原上传方法，加入进度条
var _origUpload = MC.openUploadModal;
MC.openUploadModal = function() {
  _origUpload.call(MC);
  setTimeout(function() {
    var area = document.querySelector('.image-upload-area');
    if (!area) return;
    area.addEventListener('dragover', function(e) { e.preventDefault(); area.classList.add('dragover'); });
    area.addEventListener('dragleave', function() { area.classList.remove('dragover'); });
    area.addEventListener('drop', function(e) {
      e.preventDefault(); area.classList.remove('dragover');
      Array.from(e.dataTransfer.files).forEach(function(f) {
        if (_uploadedImages.length < 5) _uploadedImages.push(f);
      });
      MC.handleImageSelect({target:{files:[]}});
    });
    // 进度条
    if (!document.getElementById('upload-progress')) {
      var bar = document.createElement('div');
      bar.className = 'upload-progress';
      bar.id = 'upload-progress';
      bar.innerHTML = '<div class="upload-progress-bar"><div class="upload-progress-fill" id="up-progress-fill"></div></div><div class="upload-progress-text" id="up-progress-text"></div>';
      var areaParent = area.parentNode;
      areaParent.insertBefore(bar, area.nextSibling);
    }
  }, 100);
};

// 包装 handleUpload 加入进度模拟
var _origHandleUpload = MC.handleUpload;
MC.handleUpload = function() {
  var prog = document.getElementById('upload-progress');
  var fill = document.getElementById('up-progress-fill');
  var text = document.getElementById('up-progress-text');
  if (prog) {
    prog.classList.add('show');
    // 模拟进度（真实进度需要 XMLHttpRequest）
    var w = 0;
    var timer = setInterval(function() {
      w += Math.random() * 20;
      if (w > 90) { w = 90; clearInterval(timer); }
      fill.style.width = w + '%';
      text.textContent = '上传中... ' + Math.floor(w) + '%';
    }, 300);
  }
  return _origHandleUpload.call(MC).finally(function() {
    if (fill) fill.style.width = '100%';
    if (text) text.textContent = '上传完成！';
    setTimeout(function() {
      if (prog) prog.classList.remove('show');
    }, 1500);
  });
};

// ===== 删除存档 =====
MC.deleteArchive = function(id, evt) {
  if (evt) evt.stopPropagation();
  if (!confirm('确定要删除这个存档吗？此操作不可撤销！')) return;
  MC.API._fetch('/api/archives/' + id, { method: 'DELETE' }).then(function() {
    MC.Toast.show('存档已删除', 'success');
    MC.State.currentPage = 0;
    MC.renderHome(true);
  }).catch(function(e) {
    MC.Toast.show(e.message, 'error');
  });
};

// ===== 编辑存档 =====
MC.openEditArchiveModal = function(id) {
  if (!MC.State.currentUser) return;
  var a = MC.State.archiveDetail;
  if (!a) return;
  _uploadedImages = [];
  var content = document.getElementById('upload-modal-content');
  content.innerHTML = '<div class="modal-header"><h2 class="modal-title">编辑存档</h2><button class="modal-close" onclick="MC.Modal.close(\'upload-modal\')">&times;</button></div>' +
    '<div class="modal-body">' +
    '<div class="form-group"><label class="form-label">存档名称 *</label><input class="form-input" id="up-title" type="text" value="' + MC.UI.esc(a.title) + '" maxlength="100"></div>' +
    '<div class="form-group"><label class="form-label">分类 *</label><select class="form-select" id="up-cat">' +
      MC.CATEGORIES.filter(function(c) { return c.id !== 'all'; }).map(function(c) { return '<option value="' + c.id + '" ' + (a.category === c.id ? 'selected' : '') + '>' + c.icon + ' ' + c.name + '</option>'; }).join('') +
    '</select></div>' +
    '<div class="form-group"><label class="form-label">MC 版本 *</label><input class="form-input" id="up-ver" type="text" value="' + MC.UI.esc(a.mcVersion) + '" list="ver-list" maxlength="20"></div>' +
    '<div class="form-group"><label class="form-label">Mod 加载器 *</label><select class="form-select" id="up-loader">' +
      Object.entries(MC.LOADER_LABELS).map(function(e) { return '<option value="' + e[0] + '" ' + (a.modLoader === e[0] ? 'selected' : '') + '>' + MC.UI.loaderIcon(e[0]) + ' ' + e[1] + '</option>'; }).join('') +
    '</select></div>' +
    '<div class="form-group"><label class="form-label">存档介绍 *</label><textarea class="form-textarea" id="up-desc" maxlength="2000">' + MC.UI.esc(a.description) + '</textarea></div>' +
    '<div class="form-error" id="up-error"><span class="form-error-icon">⚠️</span><span id="up-error-msg"></span></div>' +
    '<button class="btn btn-primary" style="width:100%;justify-content:center" id="up-submit">保存修改</button>' +
    '</div>';
  MC.Modal.open('upload-modal');
  document.getElementById('up-submit').onclick = function() {
    MC.handleEditArchive(id);
  };
};

MC.handleEditArchive = async function(id) {
  var t = document.getElementById('up-title').value.trim();
  var d = document.getElementById('up-desc').value.trim();
  var err = document.getElementById('up-error-msg');
  var errEl = document.getElementById('up-error');
  var setErr = function(m) { if (err) err.textContent = m; if (errEl) errEl.classList.add('show'); };
  if (!t) return setErr('请输入名称');
  if (!d || d.length < 10) return setErr('介绍至少10字');
  var fd = new FormData();
  fd.append('title', t); fd.append('category', document.getElementById('up-cat').value);
  fd.append('mcVersion', document.getElementById('up-ver').value); fd.append('modLoader', document.getElementById('up-loader').value);
  fd.append('description', d);
  try {
    await MC.API._fetch('/api/archives/' + id, { method: 'PUT', body: fd });
    MC.Modal.close('upload-modal');
    MC.Toast.show('存档已更新', 'success');
    MC.navigate('detail', id);
  } catch(e) {
    setErr(e.message);
  }
};

// ===== 详情页增强：编辑/删除按钮 + 相关存档 + Markdown =====
var _origRenderDetail = MC.renderDetail;
MC.renderDetail = async function(id) {
  await _origRenderDetail.call(MC, id);
  if (!MC.State.archiveDetail) return;
  var a = MC.State.archiveDetail;

  // 等待 DOM
  setTimeout(function() {
    // 主图点击放大
    var mainImg = document.getElementById('detail-main-img');
    if (mainImg && a.images && a.images.length > 0) {
      mainImg.style.cursor = 'zoom-in';
      var allUrls = a.images.map(function(img) { return img.url; });
      mainImg.onclick = function() { MC.openLightbox(mainImg.src, allUrls); };
      // 缩略图点击也打开灯箱
      var thumbs = document.querySelectorAll('.detail-thumb');
      thumbs.forEach(function(thumb, i) {
        thumb.onclick = function(e) {
          e.stopPropagation();
          MC.openLightbox(thumb.src, allUrls);
        };
      });
    }

    // 作者本人显示编辑/删除
    if (MC.State.currentUser && MC.State.currentUser.id === a.authorId) {
      var actions = document.querySelector('.detail-actions');
      if (actions) {
        actions.insertAdjacentHTML('beforeend',
          '<button class="detail-action-btn" onclick="MC.openEditArchiveModal(' + a.id + ')" style="color:var(--c-info)">✏️ 编辑</button>' +
          '<button class="detail-action-btn" onclick="MC.deleteArchive(' + a.id + ',event)" style="color:var(--c-error)">🗑 删除</button>'
        );
      }
    }

    // 赞助按钮（所有人可见），传递作者收款码
    var actions2 = document.querySelector('.detail-actions');
    if (actions2) {
      var wxUrl = a.wechatQrCodeUrl || null;
      var zfbUrl = a.alipayQrCodeUrl || null;
      var authorName = a.authorName || '';
      actions2.insertAdjacentHTML('beforeend',
        '<button class="detail-sponsor-btn" onclick="MC.openSponsorModal(' +
        (wxUrl ? "'" + wxUrl + "'" : 'null') + ', ' +
        (zfbUrl ? "'" + zfbUrl + "'" : 'null') + ', ' +
        "'" + authorName.replace(/'/g, "\\'") + "'" +
        ')">' +
        '<svg><use href="#icon-coin"/></svg>为作者充电</button>'
      );
    }

    // 踩按钮
    var actions3 = document.querySelector('.detail-actions');
    if (actions3) {
      var disliked = a.disliked || false;
      actions3.insertAdjacentHTML('beforeend',
        '<button class="detail-dislike-btn ' + (disliked ? 'disliked' : '') + '" id="detail-dislike-btn" onclick="MC.handleDislike(' + a.id + ',event)">' +
        '<svg><use href="#icon-thumbs-down"/></svg> 踩 ' + (a.dislikeCount || 0) + '</button>'
      );
    }

    // 作者联系邮箱
    if (a.contactEmail) {
      var authorRow = document.querySelector('.detail-author-row');
      if (authorRow) {
        authorRow.insertAdjacentHTML('afterend',
          '<div style="margin-bottom:12px"><a class="contact-email-link" href="mailto:' + a.contactEmail + '">' +
          '<svg width="14" height="14"><use href="#icon-mail"/></svg> 联系作者: ' + MC.UI.esc(a.contactEmail) + '</a></div>'
        );
      }
    }

    // 作者名可点击
    if (a.authorId) {
      var authorNameEl = document.querySelector('.detail-author-name');
      if (authorNameEl) {
        authorNameEl.style.cursor = 'pointer';
        authorNameEl.style.color = 'var(--c-primary)';
        authorNameEl.title = '查看作者所有存档';
        authorNameEl.onclick = function() { MC.navigateAuthor(a.authorId, a.authorName); };
      }
    }

    // 文件大小
    if (a.fileSize) {
      var downloadBtn = document.querySelector('.detail-actions .btn-primary');
      if (downloadBtn) {
        downloadBtn.insertAdjacentHTML('beforeend', ' <span style="font-size:11px;opacity:0.8">(' + MC.UI.fmtSize(a.fileSize) + ')</span>');
      }
    }

    // 浏览量显示
    if (a.viewCount !== undefined) {
      var date = document.querySelector('.detail-date');
      if (date) {
        date.textContent += ' · 👁 ' + a.viewCount + ' 次浏览 · ⬇ ' + (a.downloadCount || 0) + ' 次下载' + 
          ' · 👎 ' + (a.dislikeCount || 0);
      }
    }

    // 评论加载
    MC.API.getComments(a.id).then(function(comments) {
      var info = document.querySelector('.detail-info');
      if (!info) return;
      var sec = document.createElement('div');
      sec.className = 'comments-section';
      sec.id = 'comments-section';
      sec.innerHTML = '<h3 class="comments-title">💬 评论 (' + comments.length + ')</h3>' +
        (comments.length === 0
          ? '<p style="font-size:var(--fs-sm);color:var(--c-text-tertiary);text-align:center;padding:var(--sp-4)">暂无评论，来发表第一条吧</p>'
          : comments.map(function(c) {
              return '<div class="comment-item">' +
                '<div class="comment-avatar" style="' + (c.authorAvatar ? 'background-image:url(' + c.authorAvatar + ');background-size:cover' : '') + '">' +
                  (c.authorAvatar ? '' : MC.UI.esc((c.authorName || '?')[0])) +
                '</div>' +
                '<div class="comment-body">' +
                  '<div><span class="comment-author">' + MC.UI.esc(c.authorName || '匿名') + '</span>' +
                  '<span class="comment-time">' + MC.UI.fmtDate(c.createdAt) + '</span></div>' +
                  '<div class="comment-content">' + MC.UI.esc(c.content) + '</div>' +
                '</div>' +
                (MC.State.currentUser && MC.State.currentUser.id === c.authorId
                  ? '<button class="comment-delete" onclick="MC.handleDeleteComment(' + c.id + ')">删除</button>' : '') +
              '</div>';
            }).join('')
        ) +
        (MC.State.currentUser
          ? '<div class="comment-form">' +
            '<input class="comment-input" id="comment-input" type="text" placeholder="发表评论..." maxlength="2000" onkeydown="if(event.key===\'Enter\')MC.handleAddComment(' + a.id + ')">' +
            '<button class="comment-submit" id="comment-submit" onclick="MC.handleAddComment(' + a.id + ')">发送</button>' +
            '</div>'
          : '<p style="font-size:var(--fs-sm);color:var(--c-text-tertiary);text-align:center;margin-top:var(--sp-3)"><a href="#" onclick="MC.openAuthModal(\'login\')" style="color:var(--c-primary)">登录</a>后即可评论</p>');
      info.appendChild(sec);
    }).catch(function(){});

    // 加载相关存档
    MC.API._fetch('/api/archives/' + a.id + '/related').then(function(related) {
	      if (!related || related.length === 0) return;
	      var info2 = document.querySelector('.detail-info');
	      if (!info2) return;
	      var relSec = document.createElement('div');
	      relSec.className = 'related-section';
	      relSec.innerHTML = '<h3 class="comments-title">📦 相关推荐</h3><div class="archive-grid" style="grid-template-columns:repeat(auto-fill, minmax(200px, 1fr));gap:12px">' +
	        related.map(function(r, i) { return MC.UI.archiveCard(r, i); }).join('') +
	        '</div>';
	      info2.appendChild(relSec);
	    }).catch(function(){});
	  }, 200);
};

})();
