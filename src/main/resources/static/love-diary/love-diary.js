/*!
 * love-diary.js — 恋爱日记主题模板 · 前端行为层
 * ==================================================================
 * 职责边界（很重要）：
 *   本文件**不生成任何业务 markup** —— 内容一律由服务端渲染。
 *   它只做四件事：① 跟随主题明暗；② 倒计时刷新；③ 富文本增强（灯箱 / 代码高亮）；
 *   ④ 解锁交互（提交 → 重新取回「已解锁」的那份 SSR HTML → 原地替换）。
 *
 * 为什么解锁要重新取 HTML 而不是前端拼：
 *   如果前端按接口数据自己拼一遍列表，四页模板就有两套实现（SSR + JS），
 *   字段一改就要改两处，且必然漂移。这里改为带着模块 token 再请求一次当前 URL
 *   （`X-UniHalo-Love-Token`），服务端据此直出内容，前端只做「换掉内容区」这一步。
 *   于是模板仍然只有一套，锁语义也仍在服务端裁决（前端删掉表单没用）。
 *
 * 依赖：window.__UNI_HALO_LOVE_DIARY__（由 LoveDiaryHeadProcessor 注入）
 * 无依赖、无构建、ES5 写法（Halo 主题环境不需要 polyfill，但也不假设 ES6 模块）。
 */
(function () {
  'use strict';

  var CFG = window.__UNI_HALO_LOVE_DIARY__ || {};
  var TOKEN_HEADER = 'X-UniHalo-Love-Token';
  var MODULE_TOKEN_PREFIX = 'uh-love:module:';
  var ALBUM_TOKEN_PREFIX = 'uh-love:album:';

  // ------------------------------------------------------------------
  // 基础工具
  // ------------------------------------------------------------------

  function $(sel, root) {
    return (root || document).querySelector(sel);
  }

  function $$(sel, root) {
    return Array.prototype.slice.call((root || document).querySelectorAll(sel));
  }

  function icon(name) {
    return '<svg class="uh-love-icon" aria-hidden="true"><use href="#' + name + '"/></svg>';
  }

  function pad2(number) {
    return (number < 10 ? '0' : '') + number;
  }

  function toast(message) {
    var el = $('[data-love-toast]');
    if (!el) {
      return;
    }
    el.textContent = message;
    el.classList.add('is-show');
    window.clearTimeout(el.__uhTimer);
    el.__uhTimer = window.setTimeout(function () {
      el.classList.remove('is-show');
    }, 2400);
  }

  function store(key, value) {
    try {
      window.sessionStorage.setItem(key, value);
    } catch (e) {
      /* 隐私模式下不可写：忽略即可，用户重新验证一次 */
    }
  }

  function read(key) {
    try {
      return window.sessionStorage.getItem(key);
    } catch (e) {
      return null;
    }
  }

  function drop(key) {
    try {
      window.sessionStorage.removeItem(key);
    } catch (e) {
      /* noop */
    }
  }

  function appendQuery(url, params) {
    var pairs = [];
    Object.keys(params || {}).forEach(function (key) {
      var value = params[key];
      if (value === null || value === undefined || value === '') {
        return;
      }
      pairs.push(encodeURIComponent(key) + '=' + encodeURIComponent(value));
    });
    if (!pairs.length) {
      return url;
    }
    return url + (url.indexOf('?') >= 0 ? '&' : '?') + pairs.join('&');
  }

  function extend(target, source) {
    var out = {};
    [target, source].forEach(function (item) {
      Object.keys(item || {}).forEach(function (key) {
        out[key] = item[key];
      });
    });
    return out;
  }

  /** 解析 yyyy-MM-dd（也接受 yyyy/MM/dd），返回本地零点时间；失败返回 null。 */
  function parseDate(text) {
    if (!text) {
      return null;
    }
    var parts = String(text).trim().replace(/[/.]/g, '-').split('-');
    if (parts.length !== 3) {
      return null;
    }
    var year = parseInt(parts[0], 10);
    var month = parseInt(parts[1], 10);
    var day = parseInt(parts[2], 10);
    if (!year || !month || !day) {
      return null;
    }
    return new Date(year, month - 1, day, 0, 0, 0, 0);
  }

  function fetchJson(url, options) {
    return window.fetch(url, options).then(function (res) {
      if (res.ok) {
        return res.json();
      }
      return res.json().catch(function () {
        return {};
      }).then(function (body) {
        var error = new Error('HTTP ' + res.status);
        error.status = res.status;
        error.body = body || {};
        throw error;
      });
    });
  }

  // ------------------------------------------------------------------
  // ① 明暗模式：探测结果写到 .uh-love-wrap[data-uh-color-scheme]
  //
  // ⚠️ 要认两种来源，缺一不可：
  //   ① 主题自己的 `data-theme`（如 cosolar / 很多第三方主题）：这是主题 JS
  //      按用户实时点选算出的**最终**取值，最权威 → 优先级最高；
  //   ② Halo 的 `data-color-scheme`（值 auto|dark|light，SSR 就写在 <html> 上）
  //      与 `color-scheme-*` class → 没有 ① 时用它。
  //   两者都没有才跟随系统。只认 Halo 标记的话，用第三方主题的站点在我们
  //   的页面上会永远停在浅色（主题切深色时插件区域不跟着变）。
  // ------------------------------------------------------------------

  function mediaPrefersDark() {
    return !!(window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches);
  }

  function detectScheme() {
    var html = document.documentElement;
    var theme = (html.getAttribute('data-theme') || '').toLowerCase();
    if (theme === 'dark' || theme === 'light') {
      return theme;
    }
    var marker = (html.getAttribute('data-color-scheme') || '').toLowerCase();
    if (marker === 'dark' || marker === 'light') {
      return marker;
    }
    var classes = (html.className || '') + ' '
      + (document.body ? document.body.className || '' : '');
    if (/\bdark\b/.test(classes) || classes.indexOf('color-scheme-dark') >= 0) {
      return 'dark';
    }
    if (classes.indexOf('color-scheme-light') >= 0) {
      return 'light';
    }
    // color-scheme-auto / 无标记 → 跟随系统
    return mediaPrefersDark() ? 'dark' : 'light';
  }

  function applyScheme() {
    var scheme = detectScheme();
    // 同时挂到 <html>：主题常把背景层放在 body/html 上（如 cosolar 的 .site-bg），
    // 我们的浮层、解锁遮罩是 fixed 定位，不在 .uh-love-wrap 里，需要这个全局钩子。
    document.documentElement.setAttribute('data-uh-color-scheme', scheme);
    $$('.uh-love-wrap').forEach(function (el) {
      el.setAttribute('data-uh-color-scheme', scheme);
    });
    if (document.body) {
      document.body.setAttribute('data-uh-color-scheme', scheme);
    }
  }

  function watchScheme() {
    applyScheme();
    if (window.MutationObserver) {
      var observer = new MutationObserver(applyScheme);
      observer.observe(document.documentElement, {
        attributes: true,
        attributeFilter: ['class', 'data-color-scheme', 'data-theme']
      });
      if (document.body) {
        observer.observe(document.body, { attributes: true, attributeFilter: ['class'] });
      }
    }
    if (window.matchMedia) {
      try {
        window.matchMedia('(prefers-color-scheme: dark)')
          .addEventListener('change', applyScheme);
      } catch (e) {
        /* 老浏览器：忽略 */
      }
    }
  }

  // ------------------------------------------------------------------
  // ② 恋爱计时：SSR 给初值，这里接管并每秒刷新
  // ------------------------------------------------------------------

  function startTimer() {
    var box = $('[data-love-timer]');
    if (!box) {
      return;
    }
    var start = parseDate(box.getAttribute('data-love-date'));
    if (!start) {
      return;
    }
    var daysEl = $('[data-love-days]');
    var hourEl = $('[data-love-h]', box);
    var minEl = $('[data-love-m]', box);
    var secEl = $('[data-love-s]', box);

    function tick() {
      var seconds = Math.floor((Date.now() - start.getTime()) / 1000);
      if (seconds < 0) {
        seconds = 0;
      }
      if (daysEl) {
        daysEl.textContent = Math.floor(seconds / 86400) + 1;
      }
      if (hourEl) {
        hourEl.textContent = pad2(Math.floor(seconds % 86400 / 3600));
      }
      if (minEl) {
        minEl.textContent = pad2(Math.floor(seconds % 3600 / 60));
      }
      if (secEl) {
        secEl.textContent = pad2(seconds % 60);
      }
    }

    tick();
    window.clearInterval(window.__uhTimerId);
    window.__uhTimerId = window.setInterval(tick, 1000);
  }

  // ------------------------------------------------------------------
  // ③ 富文本增强：图片灯箱 + 代码高亮
  //   （宽表格横向滚动已改由 rich-text.css 的 `table{display:block;overflow-x:auto}`
  //    处理，与主题同一套做法；不再用 JS 包一层 div —— 包 div 多一层 DOM，
  //    还会和主题自己的 `.prose table` 规则打架。）
  // ------------------------------------------------------------------

  function photoOf(node) {
    return node.getAttribute('data-src') || node.getAttribute('src') || '';
  }

  /**
   * 灯箱只绑「内容图片」——头像/图标不该点开大图。
   * 选择器覆盖：卡片图、正文图（.uh-love-content 同时承载主题 .prose 与插件兜底）、图集图。
   */
  function bindLightbox(root) {
    var selector = '.uh-love-pic img, .uh-love-story-thumbs img,'
      + ' .uh-love-content img, [data-aspect-ratio] img';
    $$(selector, root).forEach(function (img) {
      if (img.getAttribute('data-love-bound') === '1') {
        return;
      }
      img.setAttribute('data-love-bound', '1');
      img.addEventListener('click', function () {
        if (img.closest('[data-love-viewer]')) {
          return;
        }
        openViewer([photoOf(img)], 0, img.getAttribute('alt') || '图片', '');
      });
    });
  }

  function enhanceRichText(root) {
    bindLightbox(root);
    bindCopyCode(root);
  }

  /**
   * 给正文代码块挂「复制」按钮（样式见 hljs.css 的 .uh-love-copy-code）。
   * 明文读取用 `code.textContent`，所以高亮前后的文案一致，挂载顺序无所谓。
   */
  function bindCopyCode(root) {
    $$('pre > code', root || document).forEach(function (block) {
      var pre = block.parentNode;
      // 已挂过就跳过：解锁重取会替换 [data-love-page]，但抽屉里的克隆是新的
      if (!pre || pre.__uhCopyBound) {
        return;
      }
      pre.__uhCopyBound = true;

      var button = document.createElement('button');
      button.type = 'button';
      button.className = 'uh-love-copy-code';
      button.setAttribute('aria-label', '复制代码');
      button.innerHTML = icon('ri-file-copy-line') + '<span>复制</span>';

      button.addEventListener('click', function () {
        writeClipboard(block.textContent).then(function (ok) {
          button.innerHTML = icon(ok ? 'ri-check-line' : 'ri-error-warning-line')
            + '<span>' + (ok ? '已复制' : '复制失败') + '</span>';
          window.clearTimeout(button.__uhTimer);
          button.__uhTimer = window.setTimeout(function () {
            button.innerHTML = icon('ri-file-copy-line') + '<span>复制</span>';
          }, 1600);
        });
      });

      pre.appendChild(button);
    });
  }

  /** 非 HTTPS / 老内嵌 webview 里 clipboard API 不可用，必须留 textarea 回落 */
  function writeClipboard(text) {
    if (window.isSecureContext && window.navigator && window.navigator.clipboard) {
      return window.navigator.clipboard.writeText(text).then(
        function () {
          return true;
        },
        function () {
          return legacyCopy(text);
        }
      );
    }
    return Promise.resolve(legacyCopy(text));
  }

  function legacyCopy(text) {
    try {
      var area = document.createElement('textarea');
      area.value = text;
      area.setAttribute('readonly', 'readonly');
      area.style.position = 'fixed';
      area.style.top = '-1000px';
      area.style.opacity = '0';
      document.body.appendChild(area);
      area.select();
      var ok = document.execCommand('copy');
      document.body.removeChild(area);
      return !!ok;
    } catch (e) {
      return false;
    }
  }

  function loadScript(url) {
    return new Promise(function (resolve, reject) {
      var script = document.createElement('script');
      script.src = url;
      script.async = true;
      script.onload = resolve;
      script.onerror = function () {
        reject(new Error('load failed: ' + url));
      };
      document.head.appendChild(script);
    });
  }

  /**
   * 按需加载高亮库。v1.5 起**没有** `codeHighlight` 开关：
   * 主题只给 `.prose pre` 做外框、不给 token 配色，所以配色固定由插件提供，
   * 高亮能力也就成了标配 —— 不再有「关掉」这条路。
   */
  function ensureHighlighter() {
    if (window.hljs) {
      return Promise.resolve(window.hljs);
    }
    if (!CFG.hljsUrl) {
      return Promise.resolve(null);
    }
    // 页面已自带 Shiki/hljs 时不重复加载；万一包里没有（构建产物缺失）则静默降级为不着色
    return loadScript(CFG.hljsUrl).then(function () {
      return window.hljs || null;
    })['catch'](function () {
      return null;
    });
  }

  // hljs 的常用别名 → 规范名。正文里写的是 ```` ```js ```` 这种别名，
  // 包内注册的是规范名，两者先对齐再查注册表，否则 js 会被当成未知语言跳过。
  var LANG_ALIAS = {
    js: 'javascript', jsx: 'javascript', mjs: 'javascript', cjs: 'javascript',
    ts: 'typescript', tsx: 'typescript',
    html: 'xml', xhtml: 'xml', svg: 'xml', vue: 'xml',
    sh: 'bash', shell: 'bash', zsh: 'bash',
    yml: 'yaml', py: 'python', md: 'markdown', toml: 'ini',
    mysql: 'sql', less: 'css', scss: 'css'
  };

  function canonicalLang(name) {
    return LANG_ALIAS[name] || name;
  }

  function declaredLang(block) {
    var m = /(?:^|\s)language-([\w+#.-]+)/.exec(block.className || '');
    return m ? canonicalLang(m[1].toLowerCase()) : '';
  }

  function highlight(root) {
    var blocks = $$('pre code', root || document);
    if (!blocks.length) {
      return;
    }
    ensureHighlighter().then(function (hljs) {
      if (!hljs) {
        return;
      }
      // 产物是固定的 14 语言子集（见 scripts/build-hljs-bundle.mjs）。
      // 标了 language-xxx 但包里没注册 → 静默跳过（绝不抛错污染整页）；
      // 没标语言 → 交给 hljs 自动识别（其内部就在已注册语言里猜）。
      blocks.forEach(function (block) {
        try {
          var lang = declaredLang(block);
          if (lang && !hljs.getLanguage(lang)) {
            return;
          }
          hljs.highlightElement(block);
        } catch (e) {
          /* 单块失败不影响整页 */
        }
      });
    });
  }

  // ------------------------------------------------------------------
  // 全屏查看器（相册 / 灯箱共用）与故事抽屉
  // ------------------------------------------------------------------

  var viewerState = { list: [], index: 0, title: '' };

  function viewerElements() {
    return {
      box: $('[data-love-viewer]'),
      stage: $('[data-love-viewer-stage]'),
      photo: $('[data-love-viewer-photo]'),
      title: $('[data-love-viewer-title]'),
      count: $('[data-love-viewer-count]'),
      info: $('[data-love-viewer-info]'),
      prev: $('[data-love-viewer-prev]'),
      next: $('[data-love-viewer-next]')
    };
  }

  function resetViewerStage() {
    var el = viewerElements();
    if (el.stage) {
      $$('[data-love-lock-wrap]', el.stage).forEach(function (node) {
        node.parentNode.removeChild(node);
      });
    }
    if (el.photo) {
      el.photo.style.display = '';
      el.photo.removeAttribute('src');
    }
    if (el.info) {
      el.info.textContent = '';
    }
    return el;
  }

  function openViewer(list, index, title, info) {
    var el = viewerElements();
    if (!el.box || !el.stage) {
      return;
    }
    resetViewerStage();
    viewerState = { list: list || [], index: index || 0, title: title || '' };
    if (el.title) {
      el.title.textContent = viewerState.title || '照片';
    }
    if (el.info) {
      el.info.textContent = info || '';
    }
    el.box.classList.add('is-show');
    el.box.setAttribute('aria-hidden', 'false');
    document.documentElement.style.overflow = 'hidden';
    paintViewer();
  }

  function paintViewer() {
    var el = viewerElements();
    var total = viewerState.list.length;
    if (!total) {
      return;
    }
    viewerState.index = (viewerState.index + total) % total;
    if (el.photo) {
      el.photo.style.display = '';
      el.photo.src = viewerState.list[viewerState.index];
    }
    if (el.count) {
      el.count.textContent = (viewerState.index + 1) + ' / ' + total;
    }
    var single = total < 2;
    if (el.prev) {
      el.prev.style.display = single ? 'none' : '';
    }
    if (el.next) {
      el.next.style.display = single ? 'none' : '';
    }
  }

  function closeViewer() {
    var el = viewerElements();
    if (!el.box) {
      return;
    }
    el.box.classList.remove('is-show');
    el.box.setAttribute('aria-hidden', 'true');
    document.documentElement.style.overflow = '';
  }

  function openSheet(title, html) {
    var sheet = $('[data-love-sheet]');
    var mask = $('[data-love-mask]');
    var body = $('[data-love-sheet-body]');
    if (!sheet || !body) {
      return;
    }
    body.innerHTML = html || '';
    var titleEl = $('[data-love-sheet-title]');
    if (titleEl) {
      titleEl.textContent = title || '详情';
    }
    sheet.classList.add('is-show');
    sheet.setAttribute('aria-hidden', 'false');
    if (mask) {
      mask.classList.add('is-show');
    }
    document.documentElement.style.overflow = 'hidden';
    enhanceRichText(body);
    highlight(body);
  }

  function closeSheet() {
    var sheet = $('[data-love-sheet]');
    var mask = $('[data-love-mask]');
    if (sheet) {
      sheet.classList.remove('is-show');
      sheet.setAttribute('aria-hidden', 'true');
    }
    if (mask) {
      mask.classList.remove('is-show');
    }
    document.documentElement.style.overflow = '';
  }

  function bindOverlays() {
    $$('[data-love-sheet-close]').forEach(function (btn) {
      btn.addEventListener('click', closeSheet);
    });
    var mask = $('[data-love-mask]');
    if (mask) {
      mask.addEventListener('click', closeSheet);
    }
    var el = viewerElements();
    if (el.box) {
      var closeBtn = $('[data-love-viewer-close]');
      if (closeBtn) {
        closeBtn.addEventListener('click', closeViewer);
      }
      if (el.prev) {
        el.prev.addEventListener('click', function () {
          viewerState.index -= 1;
          paintViewer();
        });
      }
      if (el.next) {
        el.next.addEventListener('click', function () {
          viewerState.index += 1;
          paintViewer();
        });
      }
      el.box.addEventListener('click', function (event) {
        // 点空白处关闭（点图片/按钮不关）
        if (event.target === el.box || event.target === el.stage) {
          closeViewer();
        }
      });
    }
    document.addEventListener('keydown', function (event) {
      var key = event.key;
      if (key === 'Escape') {
        closeViewer();
        closeSheet();
      } else if (viewerState.list.length > 1
        && el.box && el.box.classList.contains('is-show')) {
        if (key === 'ArrowLeft') {
          viewerState.index -= 1;
          paintViewer();
        } else if (key === 'ArrowRight') {
          viewerState.index += 1;
          paintViewer();
        }
      }
    });
  }

  // ------------------------------------------------------------------
  // 故事详情（内容来自每篇卡片里的 <template>，不重新拼 markup）
  // ------------------------------------------------------------------

  function bindStories(root) {
    $$('[data-love-story-open]', root).forEach(function (btn) {
      btn.addEventListener('click', function () {
        var article = btn.closest('article');
        var template = article && article.querySelector('template[data-love-story-detail]');
        if (!template) {
          return;
        }
        var titleNode = template.content.querySelector('.uh-love-story-title');
        openSheet(titleNode ? titleNode.textContent : '故事详情', template.innerHTML);
      });
    });
  }

  // ------------------------------------------------------------------
  // 相册：详情与照片都按需取（加密相册的照片永不落地到 HTML）
  // ------------------------------------------------------------------

  function moduleToken(module) {
    return read(MODULE_TOKEN_PREFIX + module) || '';
  }

  function albumToken(name) {
    return read(ALBUM_TOKEN_PREFIX + name) || '';
  }

  function photoUrl(photo) {
    if (!photo) {
      return '';
    }
    if (typeof photo === 'string') {
      return photo;
    }
    return photo.url || photo.src || photo.permalink || '';
  }

  function fetchAlbum(name, token, title, info) {
    return fetchJson(appendQuery(CFG.loveAlbumsApi + '/' + encodeURIComponent(name), {
      albumToken: token || '',
      token: moduleToken('lovePhoto')
    })).then(function (album) {
      if (album && album.locked) {
        // 服务端在相册锁 token 缺失/过期时返回 locked=true 且 photos 被剥离
        // （不是 401）—— 这不是空相册：清掉旧 token，重新弹解锁表单
        drop(ALBUM_TOKEN_PREFIX + name);
        showAlbumLock(name, title);
        return;
      }
      var urls = ((album && album.photos) || []).map(photoUrl).filter(Boolean);
      if (!urls.length) {
        toast('这本相册还没有照片');
        return;
      }
      openViewer(urls, 0, title, (album && album.description) || info || '');
    });
  }

  function showAlbumLock(name, title) {
    var el = viewerElements();
    var template = document.querySelector('template[data-love-album-lock]');
    if (!el.box || !el.stage || !template) {
      toast('相册已加密，请在应用内查看');
      return;
    }
    resetViewerStage();
    if (el.photo) {
      el.photo.style.display = 'none';
    }
    if (el.title) {
      el.title.textContent = title || '加密相册';
    }
    if (el.prev) {
      el.prev.style.display = 'none';
    }
    if (el.next) {
      el.next.style.display = 'none';
    }
    var fragment = document.importNode(template.content, true);
    var card = fragment.querySelector('[data-love-lock]');
    if (card) {
      card.setAttribute('data-love-album', name);
      card.setAttribute('data-love-album-title', title || '');
      var subtitle = fragment.querySelector('[data-love-lock-sub]');
      if (subtitle) {
        subtitle.textContent = '「' + (title || '相册') + '」需要密码才能查看';
      }
    }
    el.stage.appendChild(fragment);
    el.box.classList.add('is-show');
    el.box.setAttribute('aria-hidden', 'false');
    document.documentElement.style.overflow = 'hidden';
    if (card) {
      wireLockCard(card, 'album');
    }
  }

  function bindAlbums(root) {
    $$('[data-love-album-open]', root).forEach(function (btn) {
      btn.addEventListener('click', function () {
        var name = btn.getAttribute('data-love-album');
        var title = btn.getAttribute('data-love-album-title') || '';
        var locked = btn.getAttribute('data-love-album-locked') === 'true';
        var token = albumToken(name);
        if (locked && !token) {
          showAlbumLock(name, title);
          return;
        }
        fetchAlbum(name, token, title).then(null, function (error) {
          if (error && error.status === 401) {
            drop(ALBUM_TOKEN_PREFIX + name);
            showAlbumLock(name, title);
            return;
          }
          toast('相册加载失败，请稍后重试');
        });
      });
    });
  }

  // ------------------------------------------------------------------
  // ④ 解锁：页内表单 → 服务端直出内容 → 原地替换（不刷新、不跳转）
  // ------------------------------------------------------------------

  function loadCaptcha(card) {
    var image = $('[data-love-captcha-img]', card);
    if (!image || !CFG.captchaApi) {
      return;
    }
    image.textContent = '加载中…';
    fetchJson(CFG.captchaApi).then(function (data) {
      var vo = (data && data.captcha) || data || {};
      var id = vo.id || '';
      var src = vo.imageBase64 || vo.image || '';
      image.setAttribute('data-love-captcha-id', id);
      if (src) {
        image.innerHTML = '<img src="' + src + '" alt="验证码图片" />';
      } else {
        image.textContent = '点击重试';
      }
    })['catch'](function () {
      image.textContent = '点击重试';
    });
  }

  function captchaParams(card) {
    var code = $('[data-love-captcha-code]', card);
    if (!code) {
      return {};
    }
    var image = $('[data-love-captcha-img]', card);
    return {
      captchaId: image ? image.getAttribute('data-love-captcha-id') || '' : '',
      captchaCode: code.value || ''
    };
  }

  /** 带凭证重新取回当前文档，用新的内容区替换旧的（服务端渲染的解锁版）。 */
  function reloadUnlocked(token) {
    var headers = {};
    headers[TOKEN_HEADER] = token;
    return window.fetch(window.location.pathname + window.location.search, {
      headers: headers,
      credentials: 'same-origin'
    }).then(function (res) {
      if (!res.ok) {
        throw new Error('HTTP ' + res.status);
      }
      return res.text();
    }).then(function (html) {
      var doc = new DOMParser().parseFromString(html, 'text/html');
      var fresh = doc.querySelector('[data-love-page]');
      var current = document.querySelector('[data-love-page]');
      if (!fresh || !current || !current.parentNode) {
        window.location.reload();
        return;
      }
      current.parentNode.replaceChild(document.importNode(fresh, true), current);
      init(document.querySelector('[data-love-page]'));
      toast('已解锁');
    });
  }

  function wireLockCard(card, kind) {
    if (!card || card.getAttribute('data-love-wired') === '1') {
      return;
    }
    card.setAttribute('data-love-wired', '1');
    kind = kind || card.getAttribute('data-love-lock-kind') || 'module';

    var form = $('[data-love-lock-form]', card);
    var password = $('[data-love-password]', card);
    var errBox = $('[data-love-err]', card);
    var submit = $('[data-love-submit]', card);
    var module = card.getAttribute('data-love-module') || 'loveDiary';
    var captchaRequired = card.getAttribute('data-love-captcha-required') === 'true';
    var captchaImage = $('[data-love-captcha-img]', card);

    if (!form || !password) {
      return;
    }

    function fail(message) {
      if (!errBox) {
        return;
      }
      errBox.innerHTML = message ? icon('ri-error-warning-line') + '<span>' + message + '</span>' : '';
    }

    function busy(state) {
      if (!submit) {
        return;
      }
      submit.disabled = state;
      submit.setAttribute('aria-busy', state ? 'true' : 'false');
    }

    var eye = $('[data-love-eye]', card);
    if (eye) {
      eye.addEventListener('click', function () {
        var show = password.type === 'password';
        password.type = show ? 'text' : 'password';
        eye.setAttribute('aria-pressed', show ? 'true' : 'false');
        var use = $('[data-love-eye-icon] use', card);
        var holder = $('[data-love-eye-icon]', card);
        if (use && holder) {
          use.setAttribute('href', show
            ? holder.getAttribute('data-icon-on')
            : holder.getAttribute('data-icon-off'));
        }
      });
    }

    if (captchaRequired && captchaImage) {
      captchaImage.addEventListener('click', function () {
        loadCaptcha(card);
      });
      loadCaptcha(card);
    }

    function handleError(error) {
      busy(false);
      var body = (error && error.body) || {};
      if (body.captcha) {
        // 403：验证码错误，接口顺带返回了新验证码 → 即时换图
        var vo = body.captcha || {};
        if (captchaImage) {
          captchaImage.setAttribute('data-love-captcha-id', vo.id || '');
          if (vo.imageBase64) {
            captchaImage.innerHTML = '<img src="' + vo.imageBase64 + '" alt="验证码图片" />';
          }
        }
        var codeInput = $('[data-love-captcha-code]', card);
        if (codeInput) {
          codeInput.value = '';
        }
        fail(body.message || '验证码不正确，请重试');
        return;
      }
      if (error && error.status === 401) {
        fail('验证已过期，请重新验证');
        return;
      }
      if (error && error.status === 400) {
        fail(body.message || '密码不正确');
        return;
      }
      fail(body.message || '解锁失败，请稍后重试');
    }

    form.addEventListener('submit', function (event) {
      event.preventDefault();
      fail('');
      var value = password.value;
      if (!value) {
        fail('请输入访问密码');
        password.focus();
        return;
      }
      var code = $('[data-love-captcha-code]', card);
      if (captchaRequired && code && !code.value.trim()) {
        fail('请输入验证码');
        code.focus();
        return;
      }
      if (kind === 'album') {
        submitAlbumUnlock(card, value);
      } else {
        submitModuleUnlock(card, value);
      }
    });

    function submitModuleUnlock(target, value) {
      busy(true);
      fetchJson(appendQuery(CFG.moduleUnlockApi, captchaParams(target)), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'same-origin',
        body: JSON.stringify({ module: module, password: value })
      }).then(function (data) {
        var token = data && data.token;
        if (!token) {
          throw new Error('missing token');
        }
        store(MODULE_TOKEN_PREFIX + module, token);
        return reloadUnlocked(token);
      }).then(function () {
        busy(false);
      })['catch'](handleError);
    }

    function submitAlbumUnlock(target, value) {
      var name = target.getAttribute('data-love-album') || '';
      var title = target.getAttribute('data-love-album-title') || '';
      busy(true);
      var url = appendQuery(
        (CFG.albumUnlockApiTemplate || '').replace('{name}', encodeURIComponent(name)),
        // 模块锁与相册锁是两层：模块 token 走 ?token=，相册解锁本身也要过模块校验
        extend(captchaParams(target), { token: moduleToken('lovePhoto') })
      );
      fetchJson(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'same-origin',
        body: JSON.stringify({ password: value })
      }).then(function (data) {
        var token = data && data.token;
        if (!token) {
          throw new Error('missing token');
        }
        store(ALBUM_TOKEN_PREFIX + name, token);
        var urls = ((data && data.photos) || []).map(photoUrl).filter(Boolean);
        if (!urls.length) {
          toast('这本相册还没有照片');
          closeViewer();
          return;
        }
        openViewer(urls, 0, title, '');
      }).then(function () {
        busy(false);
      })['catch'](handleError);
    }
  }

  // ------------------------------------------------------------------
  // 清单排序（纯前端增强；筛选走服务端 ?status=）
  // ------------------------------------------------------------------

  function bindSort(root) {
    var list = $('[data-love-daily-list]', root);
    if (!list) {
      return;
    }
    var buttons = $$('[data-love-sort]', root);
    buttons.forEach(function (button) {
      button.addEventListener('click', function () {
        var direction = button.getAttribute('data-love-sort') === 'asc' ? 1 : -1;
        buttons.forEach(function (other) {
          other.setAttribute('aria-pressed', other === button ? 'true' : 'false');
        });
        var items = $$('[data-love-daily-item]', list);
        items.sort(function (a, b) {
          var left = a.getAttribute('data-love-sort-key') || '';
          var right = b.getAttribute('data-love-sort-key') || '';
          if (left === right) {
            return 0;
          }
          return (left < right ? -1 : 1) * direction;
        });
        items.forEach(function (item) {
          list.appendChild(item);
        });
      });
    });
  }

  // ------------------------------------------------------------------
  // 启动
  // ------------------------------------------------------------------

  function init(root) {
    var scope = root || document;
    applyScheme();
    startTimer();
    enhanceRichText(scope);
    highlight(scope);
    bindStories(scope);
    bindAlbums(scope);
    bindSort(scope);
    $$('[data-love-lock]', scope).forEach(function (card) {
      wireLockCard(card);
    });
  }

  function boot() {
    // 浮层与全局快捷键只绑一次（解锁后替换的是内容区，不动浮层）
    bindOverlays();
    watchScheme();
    init(document);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', boot);
  } else {
    boot();
  }
})();
