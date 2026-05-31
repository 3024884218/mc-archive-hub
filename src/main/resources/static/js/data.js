/**
 * MC Archive Hub v2 — 数据层
 * 常量定义 + 全局状态管理 + 工具函数
 */
window.MC = window.MC || {};

// ===== 固定分类 =====
MC.CATEGORIES = [
  { id: 'all',      name: '全部',       icon: '📂', color: '#6b7280' },
  { id: 'survival', name: '生存存档',    icon: '🏕️', color: '#4caf50' },
  { id: 'build',    name: '建筑作品',    icon: '🏰', color: '#f59e0b' },
  { id: 'redstone', name: '红石机械',    icon: '⚙️', color: '#ef4444' },
  { id: 'adventure',name: '冒险地图',    icon: '🗺️', color: '#8b5cf6' },
  { id: 'minigame', name: '小游戏',      icon: '🎮', color: '#ec4899' },
  { id: 'modpack',  name: '模组整合',    icon: '📦', color: '#3b82f6' },
  { id: 'skyblock', name: '空岛',        icon: '🏝️', color: '#06b6d4' },
  { id: 'other',    name: '其他',        icon: '📁', color: '#9ca3af' },
];

// ===== Mod 加载器映射 =====
MC.LOADER_LABELS = {
  'vanilla':  '原版 Vanilla',
  'fabric':   'Fabric',
  'forge':    'Forge',
  'neoforge': 'NeoForge',
  'quilt':    'Quilt',
};

// ===== 加载器图标 =====
MC.LOADER_ICONS = {
  'vanilla':  '🟢',
  'fabric':   '🧶',
  'forge':    '🔨',
  'neoforge': '⚡',
  'quilt':    '🟪',
};

// ===== 排序选项 =====
MC.SORT_OPTIONS = [
  { id: 'popular', label: '🔥 最热', icon: '🔥' },
  { id: 'newest',  label: '🕐 最新', icon: '🕐' },
  { id: 'downloads', label: '⬇ 最多下载', icon: '⬇' },
];

/** ===== 全局状态 ===== */
MC.State = {
  // 用户
  currentUser: null,

  // 路由
  currentPage: 'home',

  // 筛选
  activeCategory: 'all',
  activeMcVersion: 'all',
  activeModLoader: 'all',
  sortMode: 'popular',

  // 搜索
  searchQuery: '',

  // 数据缓存
  archives: [],
  archiveDetail: null,
  totalElements: 0,

  // 作者页
  authorId: null,
  authorName: '',

  // 动态元数据（从后端获取）
  meta: { categories: [], mcVersions: [], modLoaders: [] },

  // UI 状态
  loading: false,
  darkMode: false,
  viewMode: 'card',      // 'card' | 'list'
  pageNum: 0,            // 分页页码（与路由 currentPage 分离，避免类型冲突）
  totalPages: 0,
  pageSize: 12,

  // 动画防抖
  _cardAnimTimer: null,
};

/** ===== 主题管理 ===== */
MC.Theme = {
  init() {
    const saved = localStorage.getItem('mc-theme');
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
    const isDark = saved ? saved === 'dark' : prefersDark;
    MC.State.darkMode = isDark;
    MC.Theme.apply(isDark);

    // 监听系统主题变化
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', (e) => {
      if (localStorage.getItem('mc-theme') === null) {
        MC.Theme.apply(e.matches);
      }
    });
  },

  apply(isDark) {
    MC.State.darkMode = isDark;
    document.documentElement.setAttribute('data-theme', isDark ? 'dark' : 'light');
    document.querySelector('meta[name="theme-color"]')
      .setAttribute('content', isDark ? '#1e1e2e' : '#3a8f3a');

    // 切换图标
    const sunIcon = document.getElementById('theme-icon-sun');
    const moonIcon = document.getElementById('theme-icon-moon');
    if (sunIcon && moonIcon) {
      sunIcon.classList.toggle('hidden', isDark);
      moonIcon.classList.toggle('hidden', !isDark);
    }

    localStorage.setItem('mc-theme', isDark ? 'dark' : 'light');
  },

  toggle() {
    MC.Theme.apply(!MC.State.darkMode);
  },
};

/** ===== 工具函数 ===== */
MC.Utils = {
  truncate(str, len) {
    if (!str) return '';
    return str.length > len ? str.substring(0, len) + '...' : str;
  },

  pluralize(count, singular, plural) {
    return count === 1 ? singular : (plural || singular + 's');
  },

  debounce(fn, delay) {
    let timer;
    return function(...args) {
      clearTimeout(timer);
      timer = setTimeout(() => fn.apply(this, args), delay);
    };
  },
};
