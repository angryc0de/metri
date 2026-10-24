/*
 * Metri Web · 主逻辑
 * 渲染策略：状态变化就整块重绘 .scroll 内部，但保留滚动位置；
 * 弹层和 toast 单独挂在 .app 上，绝对定位。事件全部走委托，重绘不会丢监听。
 */
(function () {
  'use strict';

  var app = document.getElementById('app');

  var S = {
    view: 'train',
    day: 0,
    month: null,
    collapsed: false,
    overlay: null,
    query: '',
    exQuery: '',
    draft: null,
    toast: null,
    busy: false
  };

  // ---------------------------------------------------------------- 基础

  function applyTheme(dark) {
    document.documentElement.setAttribute('data-theme', dark ? 'dark' : 'light');
    var meta = document.querySelector('meta[name="theme-color"]');
    if (meta) meta.setAttribute('content', dark ? '#0b0d10' : '#ffffff');
  }

  var toastTimer = null;
  function toast(msg) {
    S.toast = msg;
    paintOverlays();
    clearTimeout(toastTimer);
    toastTimer = setTimeout(function () {
      S.toast = null;
      paintOverlays();
    }, 2400);
  }

  /** 只重画弹层 / toast / 遮罩，不动主界面（避免丢失滚动位置和输入焦点）。 */
  function paintOverlays() {
    var old = app.querySelector('.sheet-mask, .toast, .busy-mask');
    while (old) {
      old.remove();
      old = app.querySelector('.sheet-mask, .toast, .busy-mask');
    }
    var html = (S.overlay ? SHEET.html(S) : '')
      + (S.busy ? busyHtml() : '')
      + (S.toast ? '<div class="toast">' + U.esc(S.toast) + '</div>' : '');
    if (html) app.insertAdjacentHTML('beforeend', html);
  }

  function busyHtml() {
    return '<div class="busy-mask"><div class="busy-box">'
      + '<div class="busy-dot"></div>'
      + '<div class="busy-t">正在拆解这一餐…</div>'
      + '<div class="busy-s">本地规则计算，很快</div>'
      + '</div></div>';
  }

  // ---------------------------------------------------------------- 主渲染

  function render() {
    var st = DB.get();
    applyTheme(st.darkTheme);

    if (!st.onboarded) {
      app.innerHTML = onboardHtml();
      return;
    }

    var sc = app.querySelector('.scroll');
    var keep = sc ? sc.scrollTop : 0;

    app.innerHTML = topbarHtml(st)
      + '<div class="scroll">' + scrollHtml(st) + '</div>'
      + tabbarHtml();
    paintOverlays();

    var sc2 = app.querySelector('.scroll');
    if (sc2) sc2.scrollTop = keep;
  }

  function scrollHtml(st) {
    return monthbarHtml()
      + calendarHtml()
      + legendHtml()
      + '<div class="content">' + contentHtml(st) + '</div>';
  }

  function topbarHtml(st) {
    var p = st.profile;
    return '<header class="topbar">'
      + '<div class="wordmark">Metri</div>'
      + '<div class="topbar-row">'
      + '<div class="who"><div class="who-name">' + U.esc(p.name || '你好') + '</div>'
      + '<div class="who-sub">今天 · ' + U.dayTitle(DB.today()) + '</div></div>'
      + '<div class="weight-pill" data-act="open-profile">' + U.fmtNum(p.weightKg) + '<span>kg</span></div>'
      + '</div></header>';
  }

  function monthbarHtml() {
    return '<div class="monthbar">'
      + '<button class="icon-btn" data-act="prev-month">' + U.chev('left') + '</button>'
      + '<div class="month-label">' + S.month.y + ' 年 ' + S.month.m + ' 月</div>'
      + '<button class="icon-btn" data-act="next-month">' + U.chev('right') + '</button>'
      + '<div class="spacer"></div>'
      + '<button class="icon-btn" data-act="toggle-collapse">' + U.chev(S.collapsed ? 'down' : 'up') + '</button>'
      + '</div>';
  }

  // ---------------------------------------------------------------- 日历

  function monthRange() {
    var y = S.month.y, m = S.month.m;
    var first = Date.UTC(y, m - 1, 1) / 86400000;
    var dow = (new Date(first * 86400000).getUTCDay() + 6) % 7;   // 0 = 周一
    var days = new Date(Date.UTC(y, m, 0)).getUTCDate();
    return { first: first, start: first - dow, days: days, rows: Math.ceil((dow + days) / 7) };
  }

  function calendarHtml() {
    var r = monthRange();
    var today = DB.today();
    var selRow = Math.floor((S.day - r.start) / 7);
    if (selRow < 0 || selRow >= r.rows) selRow = 0;

    var h = '<div class="weekdays"><div>一</div><div>二</div><div>三</div><div>四</div>'
      + '<div>五</div><div>六</div><div>日</div></div><div class="weeks">';

    for (var w = 0; w < r.rows; w++) {
      if (S.collapsed && w !== selRow) continue;
      h += '<div class="week' + (w === selRow ? ' pinned' : '') + '">';
      for (var d = 0; d < 7; d++) {
        var day = r.start + w * 7 + d;
        var o = DB.fromEpochDay(day);
        var inMonth = o.y === S.month.y && o.m === S.month.m;
        if (!inMonth) { h += '<div class="cell out"></div>'; continue; }
        h += cellHtml(day, o, today);
      }
      h += '</div>';
    }
    return h + '</div>';
  }

  function cellHtml(day, o, today) {
    var det = DB.dayDetail(day);
    var cls = 'cell';
    if (day > today) cls += ' future';
    if (day === S.day) cls += ' sel';

    var dcls = 'cell-day';
    if (det.status === 'DEFICIT') dcls += ' deficit';
    else if (det.status === 'SURPLUS') dcls += ' surplus';
    else if (det.status === 'PLAIN') dcls += ' plain';

    var mark = '';
    if (det.isRest) {
      mark = '<span class="rest">休</span>';
    } else if (det.hasStrength || det.hasCardio) {
      if (det.hasStrength) mark += '<i class="dot strength"></i>';
      if (det.hasCardio) mark += '<i class="dot cardio"></i>';
    }

    var sub = '';
    if (det.deficit != null
      && (det.status === 'DEFICIT' || det.status === 'SURPLUS' || det.status === 'EVEN')) {
      var v = det.deficit;
      var txt = v > 0 ? String(v) : v < 0 ? '+' + (-v) : '0';
      var sc = det.status === 'DEFICIT' ? 'deficit' : det.status === 'SURPLUS' ? 'surplus' : 'even';
      sub = '<div class="cell-sub ' + sc + '">' + txt + '</div>';
    }

    return '<div class="' + cls + '" data-act="select-day" data-d="' + day + '">'
      + '<div class="' + dcls + '">' + o.d + '</div>'
      + '<div class="cell-mark">' + mark + '</div>' + sub + '</div>';
  }

  function legendHtml() {
    return '<div class="legend">'
      + '<span class="legend-item"><i class="dot strength"></i>力量</span>'
      + '<span class="legend-item"><i class="dot cardio"></i>有氧</span>'
      + '<span class="legend-item"><span class="rest">休</span></span>'
      + '<span class="spacer"></span>'
      + '<span>数字颜色 = 热量缺口</span></div>';
  }

  // ---------------------------------------------------------------- 内容

  function contentHtml(st) {
    if (S.view === 'train') return trainHtml(st);
    if (S.view === 'diet') return dietHtml(st);
    return meHtml(st);
  }

  function dayCard(st, kind) {
    var det = DB.dayDetail(S.day);
    var tdee = det.tdee;
    var over = det.deficit != null && det.deficit < 0;
    var ws = DB.weekSummary();

    var big = det.deficit == null ? '—' : String(Math.abs(det.deficit));
    var color = det.deficit == null ? 'dim-text' : over ? 'red' : 'green';
    var label = over ? '热量超出' : '热量缺口';

    var tdeeNote = tdee.adaptive
      ? '基础消耗 ' + Math.round(tdee.value) + ' kcal · 自适应'
      : '基础消耗 ' + Math.round(tdee.value) + ' kcal';

    return '<div class="card">'
      + '<div class="card-head">'
      + '<div><div class="card-title">' + U.dayTitle(S.day) + '</div>'
      + '<div class="card-sub">' + tdeeNote + '</div></div>'
      + '<div style="text-align:right"><div class="card-sub">' + label + '</div>'
      + '<div class="big-num ' + color + '">' + big + '<span class="unit">kcal</span></div></div>'
      + '</div>'
      + (ws ? '<div class="week-strip"><span>本周 平均缺口 <b>' + (ws.avg == null ? '—' : ws.avg) + '</b></span>'
        + '<span>达标 <b>' + ws.hit + '/' + ws.days + '</b> 天（目标 ' + ws.target + '）</span></div>' : '')
      + '</div>';
  }

  function trainHtml(st) {
    var det = DB.dayDetail(S.day);
    var h = dayCard(st, 'train');

    h += '<div class="section-head"><div class="section-title">力量训练</div>'
      + '<div class="section-count">' + (det.hasStrength ? '' : '还没有记录') + '</div><div class="spacer"></div>'
      + '<button class="add-btn" data-act="open-exercise">+</button></div>';

    var stg = st.strength.filter(function (x) { return x.day === S.day; });
    if (!stg.length) {
      h += '<div class="empty">还没有力量记录，点上面的 + 添加</div>';
    } else {
      stg.forEach(function (x) {
        h += rowRecord('strength', x.id, x.exerciseName,
          U.fmtNum(x.weightKg) + 'kg × ' + x.reps + ' 次 × ' + x.sets + ' 组'
          + (x.bodyweight ? ' · 自重' : '') + ' · ' + U.fmtNum(x.minutes) + ' 分钟',
          Math.round(x.netKcal), 'net', 'blue');
      });
    }

    h += '<div class="section-head" style="margin-top:4px"><div class="section-title">有氧</div>'
      + '<div class="section-count">' + (det.hasCardio ? '' : '还没有记录') + '</div><div class="spacer"></div>'
      + '<button class="add-btn" data-act="open-cardio">+</button></div>';

    var car = st.cardio.filter(function (x) { return x.day === S.day; });
    if (!car.length) {
      h += '<div class="empty">还没有有氧记录，点上面的 + 添加</div>';
    } else {
      car.forEach(function (x) {
        h += rowRecord('cardio', x.id, x.typeName, cardioDesc(x),
          Math.round(x.netKcal), 'net', 'amber');
      });
    }

    h += '<button class="ghost-btn" style="margin-top:6px" data-act="toggle-rest">'
      + (det.isRest ? '取消「休息日」标记' : '标记今天为休息日') + '</button>';

    return h;
  }

  function cardioDesc(x) {
    var t = SEED.cardioById(x.typeId);
    var parts = [];
    if (t) {
      t.params.forEach(function (p) {
        var v = x.params[p.key];
        if (v == null) return;
        if (p.key === 'min') return;
        parts.push(p.label + ' ' + U.fmtNum(v) + p.unit);
      });
    }
    if (x.enumValue) parts.push(x.enumValue);
    parts.push(U.fmtNum(x.minutes) + ' 分钟');
    return parts.join(' · ');
  }

  function rowRecord(kind, id, title, sub, kcal, unitLbl, color) {
    return '<div class="row">'
      + '<div class="row-main"><div class="row-title">' + U.esc(title) + '</div>'
      + '<div class="row-sub">' + U.esc(sub) + '</div></div>'
      + '<div class="row-num"><span class="v ' + color + '">' + kcal + '</span>'
      + '<span class="u">kcal</span><div class="lbl">净消耗</div></div>'
      + '<button class="del-dot" data-act="del" data-kind="' + kind + '" data-id="' + id + '">×</button>'
      + '</div>';
  }

  function dietHtml(st) {
    var det = DB.dayDetail(S.day);
    var p = DB.get().profile;
    var target = M.proteinTarget(p.weightKg, p.proteinAuto, p.proteinCustomG);
    var tdee = det.tdee;
    var over = det.deficit != null && det.deficit < 0;

    var h = '<div class="card">'
      + '<div class="card-head">'
      + '<div><div class="card-title">' + U.dayTitle(S.day) + '</div>'
      + '<div class="card-sub">已摄入 ' + Math.round(det.intakeKcal) + ' kcal</div></div>'
      + '<div style="text-align:right"><div class="card-sub">' + (over ? '热量超出' : '热量缺口') + '</div>'
      + '<div class="big-num ' + (det.deficit == null ? 'dim-text' : over ? 'red' : 'green') + '">'
      + (det.deficit == null ? '—' : Math.abs(det.deficit)) + '<span class="unit">kcal</span></div></div>'
      + '</div>'
      + '<div class="stat-grid">'
      + statItem('蛋白质', U.fmtNum(det.intakeProtein), 'g', det.intakeProtein >= target ? 'blue' : '')
      + statItem('目标蛋白', String(target), 'g', '')
      + statItem('已记录', String(det.mealCount), '项', '')
      + '</div></div>';

    ['BREAKFAST', 'LUNCH', 'DINNER', 'SNACK'].forEach(function (mt) {
      var items = st.meals.filter(function (m) { return m.day === S.day && m.meal === mt; });
      var sum = 0;
      items.forEach(function (m) { sum += m.kcal; });

      h += '<div class="meal-block">'
        + '<div class="meal-head"><div class="meal-name">' + DB.mealLabel(mt) + '</div>'
        + (items.length ? '<div class="meal-kcal">' + Math.round(sum) + ' kcal</div>' : '')
        + '<div class="spacer"></div>'
        + '<button class="add-btn small" data-act="open-meal" data-meal="' + mt + '">+</button></div>';

      if (!items.length) {
        h += '<div class="meal-empty">还没有记录</div>';
      } else {
        h += '<div class="meal-items">';
        items.forEach(function (m) {
          var f = SEED.foodById(m.foodId);
          h += '<div class="food-row">'
            + '<div class="food-emoji">' + (f && f.emoji ? f.emoji : '🍽') + '</div>'
            + '<div class="food-name">' + U.esc(m.foodName) + '</div>'
            + '<div class="food-grams">' + U.fmtNum(m.grams) + 'g</div>'
            + '<div class="food-kcal">' + Math.round(m.kcal) + '<span class="u">kcal</span></div>'
            + '<button class="del-dot" style="width:22px;height:22px;font-size:11px" data-act="del" data-kind="meal" data-id="' + m.id + '">×</button>'
            + '</div>';
        });
        h += '</div>';
      }
      h += '</div>';
    });

    return h;
  }

  function statItem(label, value, unit, cls) {
    return '<div class="stat"><div class="stat-label">' + label + '</div>'
      + '<div class="stat-value ' + cls + '">' + value
      + (unit ? '<span class="unit">' + unit + '</span>' : '') + '</div></div>';
  }

  function meHtml(st) {
    var p = st.profile;
    var tdee = DB.effectiveTdee();
    var target = M.proteinTarget(p.weightKg, p.proteinAuto, p.proteinCustomG);
    var rate = M.targetDeficit(p.weeklyRateKg);

    function row(act, title, sub, right) {
      return '<div class="list-item" data-act="' + act + '">'
        + '<div class="t">' + title + (sub ? '<div class="s">' + sub + '</div>' : '') + '</div>'
        + (right ? '<div class="s">' + right + '</div>' : '')
        + '<div class="arrow">›</div></div>';
    }

    return '<div class="card">'
      + '<div class="card-title">' + U.esc(p.name || '你的资料') + '</div>'
      + '<div class="card-sub">' + (p.gender === 'MALE' ? '男' : '女') + ' · ' + p.age + ' 岁 · '
      + U.fmtNum(p.heightCm) + 'cm · ' + U.fmtNum(p.weightKg) + 'kg</div>'
      + '<div class="stat-grid">'
      + statItem('基础代谢', String(Math.round(tdee.bmr)), 'kcal', '')
      + statItem('日消耗', String(Math.round(tdee.value)), 'kcal', 'green')
      + statItem('目标缺口', String(rate), 'kcal', '')
      + '</div>'
      + '<div class="week-strip" style="border-top:none">'
      + '<span>' + (tdee.adaptive ? '已启用自适应 TDEE' : '按公式计算') + '</span>'
      + '<span class="dim-text">' + (tdee.note || '') + '</span></div>'
      + '</div>'

      + '<div class="section-head"><div class="section-title">设置</div></div>'
      + '<div class="card" style="padding:4px 14px">'
      + row('open-profile', '个人资料', '性别、身高、体重、目标', '')
      + row('open-theme', '外观', '', st.darkTheme ? '黑夜模式' : '白天模式')
      + row('toggle-heat', '热量缺口警示', '关闭后日历不再标绿/红', st.heatWarn ? '开' : '关')
      + row('open-export', '数据', '导出、导入、清空', '')
      + '</div>'

      + '<div class="section-head"><div class="section-title">关于</div></div>'
      + '<div class="card" style="padding:14px">'
      + '<div class="card-sub" style="line-height:1.9">'
      + 'Metri · 完全本地的减脂记录<br>'
      + '无账号、无服务器、无上传，数据只存在这台设备的浏览器里。<br>'
      + '蛋白质目标 ' + target + ' g/天 · 目标体重 ' + U.fmtNum(p.targetWeightKg) + ' kg'
      + '</div></div>';
  }

  function tabbarHtml() {
    function t(v, label) {
      return '<button class="tab' + (S.view === v ? ' on' : '') + '" data-act="tab" data-v="' + v + '">' + label + '</button>';
    }
    return '<nav class="tabbar">' + t('train', '训练') + t('diet', '饮食') + t('me', '我') + '</nav>';
  }

  // ---------------------------------------------------------------- 引导页

  function onboardHtml() {
    var st = DB.get();
    var p = st.profile;
    return '<div class="onboard">'
      + '<div class="brand">Metri</div>'
      + '<h1>先了解一下你</h1>'
      + '<div class="sub">这些只用来算你的基础代谢和目标，全部留在这台设备上'
      + '<br><span class="dim-text">开始后会给你的日历填上一周示例记录，随时可清空</span></div>'

      + '<div class="field"><label>怎么称呼你（可留空）</label>'
      + '<input id="ob-name" maxlength="12" placeholder="例如 ivan"></div>'

      + '<div class="field"><label>性别</label><div class="seg">'
      + '<button id="ob-male" class="on" data-act="ob-gender" data-v="MALE">男</button>'
      + '<button id="ob-female" data-act="ob-gender" data-v="FEMALE">女</button></div></div>'

      + '<div class="field-row">'
      + '<div class="field"><label>年龄</label><input id="ob-age" type="number" inputmode="numeric" value="28"></div>'
      + '<div class="field"><label>身高 cm</label><input id="ob-height" type="number" inputmode="decimal" value="178"></div>'
      + '<div class="field"><label>体重 kg</label><input id="ob-weight" type="number" inputmode="decimal" value="78"></div>'
      + '</div>'

      + '<div class="bmr-hint" id="ob-hint"></div>'
      + '<button class="primary-btn" data-act="ob-done">开始记录</button>'
      + '<button class="ghost-btn" data-act="ob-demo">不想填，先看示例数据</button>'
      + '</div>';
  }

  function dbg() {
    var d = document.getElementById('dbg');
    if (!d) return;
    var ob = document.querySelector('.onboard');
    var fr = document.querySelector('.field-row');
    var kids = document.querySelectorAll('.field-row .field');
    var kw = [];
    for (var i = 0; i < kids.length; i++) kw.push(Math.round(kids[i].getBoundingClientRect().width));
    var last = kids.length ? Math.round(kids[kids.length - 1].getBoundingClientRect().right) : 0;
    d.textContent = 'win=' + window.innerWidth + ' app=' + app.offsetWidth
      + ' onboard=' + (ob ? ob.offsetWidth : 0)
      + ' row=' + (fr ? fr.offsetWidth : 0) + '/' + (fr ? fr.scrollWidth : 0)
      + ' kids=' + kw.join(',') + ' lastRight=' + last;
  }

  function onboardHint() {
    var g = (document.querySelector('#ob-male').classList.contains('on')) ? 'MALE' : 'FEMALE';
    var age = Number(document.querySelector('#ob-age').value) || 28;
    var h = Number(document.querySelector('#ob-height').value) || 178;
    var w = Number(document.querySelector('#ob-weight').value) || 78;
    var bmr = M.bmr(g, w, h, age);
    var tdee = M.tdeeBase(bmr, 1.2);
    var box = document.getElementById('ob-hint');
    if (box) {
      box.innerHTML = '基础代谢 <b>' + Math.round(bmr) + '</b> kcal/天<br>'
        + '按久坐估算的日消耗 <b>' + Math.round(tdee) + '</b> kcal<br>'
        + '<span class="dim-text">训练消耗会单独加上，不重复计入</span>';
    }
  }

  // ---------------------------------------------------------------- 弹层开关

  function openSheet(type, data) {
    S.overlay = Object.assign({ type: type }, data || {});
    S.draft = null;
    S.query = '';
    S.exQuery = '';
    paintOverlays();
  }

  function closeSheet() {
    S.overlay = null;
    S.draft = null;
    paintOverlays();
  }

  // ---------------------------------------------------------------- 事件

  function onClick(e) {
    var el = e.target.closest('[data-act]');
    if (!el) return;
    var act = el.getAttribute('data-act');

    if (act === 'close-sheet') {
      if (e.target.closest('.sheet') && !el.classList.contains('close-btn')) return;
      closeSheet();
      return;
    }

    switch (act) {
      case 'tab': S.view = el.getAttribute('data-v'); render(); break;
      case 'prev-month': shiftMonth(-1); break;
      case 'next-month': shiftMonth(1); break;
      case 'toggle-collapse': S.collapsed = !S.collapsed; render(); break;
      case 'select-day': selectDay(Number(el.getAttribute('data-d'))); break;

      case 'open-exercise': openSheet('exercise'); break;
      case 'open-cardio': openSheet('cardio'); break;
      case 'open-meal': openSheet('food', { meal: el.getAttribute('data-meal') }); break;
      case 'open-theme': openSheet('theme'); break;
      case 'open-profile': openSheet('profile'); break;
      case 'open-export': openSheet('export'); break;

      case 'pick-exercise': startStrength(el.getAttribute('data-id')); break;
      case 'pick-cardio': startCardio(el.getAttribute('data-id')); break;
      case 'pick-food': startPortion(el.getAttribute('data-id')); break;

      case 'quick-add': quickAdd(el.getAttribute('data-id')); break;
      case 'estimate': runEstimate(); break;
      case 'confirm-estimate': confirmEstimate(); break;
      case 'set-mult': S.draft.mult = Number(el.getAttribute('data-v')); paintOverlays(); break;
      case 'set-serv': S.draft.grams = Math.round((SEED.foodById(S.draft.foodId).servingGrams || 100) * Number(el.getAttribute('data-v'))); paintOverlays(); break;

      case 'step': stepDraft(el.getAttribute('data-k'), Number(el.getAttribute('data-d'))); break;
      case 'cstep': stepCardio(el.getAttribute('data-k'), Number(el.getAttribute('data-d'))); break;
      case 'cpick': S.draft.enumValue = el.getAttribute('data-v'); paintOverlays(); break;
      case 'toggle-bw': S.draft.bw = !S.draft.bw; paintOverlays(); break;

      case 'save-strength': saveStrength(); break;
      case 'save-cardio': saveCardio(); break;
      case 'save-portion': savePortion(); break;

      case 'del': delRecord(el.getAttribute('data-kind'), Number(el.getAttribute('data-id'))); break;
      case 'toggle-rest': DB.toggleRest(S.day); render(); break;
      case 'toggle-heat': DB.setProfile({}); toggleHeat(); break;

      case 'set-theme': setTheme(el.getAttribute('data-dark') === 'true'); break;
      case 'set-gender': DB.setProfile({ gender: el.getAttribute('data-v') }); paintOverlays(); break;
      case 'set-rate': DB.setProfile({ weeklyRateKg: Number(el.getAttribute('data-v')) }); paintOverlays(); break;
      case 'set-activity': DB.setProfile({ activityFactor: Number(el.getAttribute('data-v')) }); paintOverlays(); break;

      case 'export-json': exportJson(); break;
      case 'export-csv': exportCsv(); break;
      case 'import-json': importJson(); break;
      case 'reset-all': resetAll(); break;

      case 'ob-gender':
        document.querySelector('#ob-male').classList.toggle('on', el.getAttribute('data-v') === 'MALE');
        document.querySelector('#ob-female').classList.toggle('on', el.getAttribute('data-v') === 'FEMALE');
        onboardHint();
        break;
      case 'ob-done': finishOnboard(); break;
      case 'ob-demo': enterDemo(); break;
    }
  }

  function onInput(e) {
    var el = e.target;
    var role = el.getAttribute('data-role');
    if (role === 'food-query') {
      S.query = el.value;
      var box = document.getElementById('food-results');
      if (box) box.innerHTML = SHEET.foodResults(S);
    } else if (role === 'ex-query') {
      S.exQuery = el.value;
      var box2 = document.getElementById('ex-results');
      if (box2) box2.innerHTML = SHEET.exResults(S);
    } else if (el.id === 'ob-age' || el.id === 'ob-height' || el.id === 'ob-weight') {
      onboardHint();
    } else if (role === 'pf-name') {
      DB.setProfile({ name: el.value.slice(0, 12) });
    }
  }

  // ---------------------------------------------------------------- 动作

  function shiftMonth(delta) {
    var y = S.month.y, m = S.month.m + delta;
    if (m < 1) { m = 12; y--; }
    if (m > 12) { m = 1; y++; }
    S.month = { y: y, m: m };
    // 选中日期跟着迁到新月同一天号，否则翻月后日历没有高亮、吸顶行也找不到
    var o = DB.fromEpochDay(S.day);
    var dim = new Date(Date.UTC(y, m, 0)).getUTCDate();
    S.day = Math.round(Date.UTC(y, m - 1, Math.min(o.d, dim)) / 86400000);
    render();
  }

  function selectDay(day) {
    S.day = day;
    var o = DB.fromEpochDay(day);
    S.month = { y: o.y, m: o.m };
    render();
  }

  function toggleHeat() {
    DB.setHeatWarn(!DB.get().heatWarn);
    render();
  }

  function setTheme(dark) {
    var st = DB.get();
    st.darkTheme = dark;
    DB.save();
    applyTheme(dark);
    render();
  }

  function startStrength(exId) {
    var ex = SEED.exerciseById(exId);
    var last = null;
    var st = DB.get();
    for (var i = st.strength.length - 1; i >= 0; i--) {
      if (st.strength[i].exerciseId === exId) { last = st.strength[i]; break; }
    }
    S.draft = {
      exId: exId,
      weight: last ? last.weightKg : 20,
      reps: last ? last.reps : 12,
      sets: last ? last.sets : 4,
      rest: 90,
      bw: ex.bodyweightDefault
    };
    S.overlay = { type: 'strength' };
    paintOverlays();
  }

  function startCardio(typeId) {
    var t = SEED.cardioById(typeId);
    var params = {};
    t.params.forEach(function (p) { params[p.key] = p.def; });
    S.draft = {
      typeId: typeId,
      params: params,
      enumValue: t.enumSpec ? t.enumSpec.def : null
    };
    S.overlay = { type: 'cardioParam' };
    paintOverlays();
  }

  function startPortion(foodId) {
    var f = SEED.foodById(foodId);
    S.draft = { foodId: foodId, grams: f.servingGrams || 100 };
    S.overlay = { type: 'portion', meal: S.overlay ? S.overlay.meal : 'LUNCH' };
    paintOverlays();
  }

  function clamp(v, min, max) { return Math.min(max, Math.max(min, v)); }

  function stepDraft(key, dir) {
    var type = S.overlay ? S.overlay.type : '';

    // 资料弹窗和力量参数都用 stepper，但键名含义完全不同，必须按弹窗类型分开走
    if (type === 'profile') {
      var p = DB.get().profile;
      if (key === 'age') DB.setProfile({ age: clamp(p.age + dir, 10, 99) });
      else if (key === 'height') DB.setProfile({ heightCm: clamp(p.heightCm + dir, 120, 230) });
      else if (key === 'pweight') {
        var w = clamp(p.weightKg + dir * 0.5, 30, 300);
        DB.setProfile({ weightKg: w });
        DB.setWeight(DB.today(), w);
      } else if (key === 'target') DB.setProfile({ targetWeightKg: clamp(p.targetWeightKg + dir * 0.5, 30, 300) });
      paintOverlays();
      return;
    }

    var d = S.draft;
    if (!d) return;
    if (key === 'weight') d.weight = Math.max(0, Math.round((d.weight + dir * 2.5) * 10) / 10);
    else if (key === 'reps') d.reps = clamp(d.reps + dir, 1, 50);
    else if (key === 'sets') d.sets = clamp(d.sets + dir, 1, 12);
    else if (key === 'grams') d.grams = clamp(d.grams + dir * 10, 10, 2000);
    paintOverlays();
  }

  function stepCardio(key, dir) {
    var d = S.draft;
    var t = SEED.cardioById(d.typeId);
    t.params.forEach(function (p) {
      if (p.key !== key) return;
      var v = d.params[key] + dir * p.step;
      d.params[key] = Math.min(p.max, Math.max(p.min, Math.round(v * 100) / 100));
    });
    paintOverlays();
  }

  function saveStrength() {
    var d = S.draft;
    var ex = SEED.exerciseById(d.exId);
    var st = DB.get();
    var tdee = DB.effectiveTdee();
    var weight = d.bw ? st.profile.weightKg : d.weight;
    var calc = M.strengthCalc(ex.met, st.profile.weightKg, d.weight, d.reps, d.sets, d.bw, tdee.bmr, d.rest);
    var minutes = calc.minutes;
    var net = calc.net;

    DB.addStrength({
      day: S.day,
      exerciseId: ex.id,
      exerciseName: ex.name,
      weightKg: weight,
      reps: d.reps,
      sets: d.sets,
      bodyweight: d.bw,
      minutes: Math.round(minutes * 10) / 10,
      netKcal: Math.round(net)
    });
    closeSheet();
    render();
    toast('已记录 ' + ex.name + ' · 净消耗 ' + Math.round(net) + ' kcal');
  }

  function saveCardio() {
    var d = S.draft;
    var t = SEED.cardioById(d.typeId);
    var st = DB.get();
    var tdee = DB.effectiveTdee();
    var met = Math.min(20, Math.max(1, t.met(d.params, d.enumValue)));
    var minutes = d.params.min || 30;
    var gross = M.grossKcal(met, st.profile.weightKg, minutes);
    var net = M.netKcal(gross, tdee.bmr, minutes);

    DB.addCardio({
      day: S.day,
      typeId: t.id,
      typeName: t.name,
      params: d.params,
      enumValue: d.enumValue,
      minutes: minutes,
      met: Math.round(met * 100) / 100,
      netKcal: Math.round(net)
    });
    closeSheet();
    render();
    toast('已记录 ' + t.name + ' · 净消耗 ' + Math.round(net) + ' kcal');
  }

  function savePortion() {
    var d = S.draft;
    var f = SEED.foodById(d.foodId);
    var meal = S.overlay.meal;
    var grams = d.grams;
    var kcal = Math.round(f.kcalPer100g * grams / 100);
    var protein = f.proteinPer100g * grams / 100;
    var unitG = f.servingGrams || 100;

    DB.addMeal({
      day: S.day,
      meal: meal,
      foodId: f.id,
      foodName: f.name,
      grams: grams,
      servings: Math.round(grams / unitG * 10) / 10,
      servingUnit: f.servingUnit,
      kcal: kcal,
      protein: protein
    });
    closeSheet();
    render();
    toast('已添加 ' + f.name + ' ' + Math.round(grams) + 'g · ' + kcal + ' kcal');
  }

  function quickAdd(foodId) {
    var f = SEED.foodById(foodId);
    var meal = S.overlay.meal;
    var grams = f.servingGrams || 100;
    var kcal = Math.round(f.kcalPer100g * grams / 100);
    DB.addMeal({
      day: S.day,
      meal: meal,
      foodId: f.id,
      foodName: f.name,
      grams: grams,
      servings: 1,
      servingUnit: f.servingUnit,
      kcal: kcal,
      protein: f.proteinPer100g * grams / 100
    });
    closeSheet();
    render();
    toast('已添加 ' + f.name + ' · ' + kcal + ' kcal');
  }

  function runEstimate() {
    var q = (S.query || '').trim();
    if (!q) return;
    S.busy = true;
    paintOverlays();
    // 本地规则是同步的，但给一帧让遮罩先渲染出来，否则像卡住了
    setTimeout(function () {
      S.busy = false;
      var est = EST.estimate(q);
      if (!est) { paintOverlays(); return; }
      S.overlay = { type: 'aiResult', est: est, query: q, meal: S.overlay.meal };
      S.draft = { mult: 1 };
      paintOverlays();
    }, 260);
  }

  function confirmEstimate() {
    var est = S.overlay.est;
    var mult = S.draft.mult || 1;
    var meal = S.overlay.meal;
    var kcal = Math.round(est.totalKcal * mult);

    DB.addMeal({
      day: S.day,
      meal: meal,
      foodId: 'est_' + Date.now(),
      foodName: S.overlay.query + (mult !== 1 ? '（' + mult + ' 份）' : ''),
      grams: Math.round(est.totalGrams * mult),
      servings: mult,
      servingUnit: est.servingUnit,
      kcal: kcal,
      protein: est.totalProtein * mult
    });
    closeSheet();
    render();
    toast('已添加 ' + kcal + ' kcal');
  }

  function delRecord(kind, id) {
    DB.removeRecord(kind, id);
    render();
    toast('已删除');
  }

  function exportJson() {
    U.download('metri-backup-' + DB.ymd(DB.today()) + '.json', DB.exportJSON(), 'application/json');
    toast('已导出 JSON 备份');
  }

  function exportCsv() {
    var files = DB.exportCSVs();
    var names = Object.keys(files);
    names.forEach(function (n, i) {
      setTimeout(function () {
        U.download(DB.ymd(DB.today()) + '-' + n, files[n], 'text/csv');
      }, i * 300);
    });
    toast('正在导出 ' + names.length + ' 个 CSV');
  }

  function importJson() {
    var input = document.createElement('input');
    input.type = 'file';
    input.accept = '.json,application/json';
    input.onchange = function () {
      var file = input.files && input.files[0];
      if (!file) return;
      var reader = new FileReader();
      reader.onload = function () {
        try {
          DB.importJSON(String(reader.result));
          closeSheet();
          render();
          toast('已导入备份');
        } catch (err) {
          toast('这个文件不是 Metri 的备份');
        }
      };
      reader.readAsText(file);
    };
    input.click();
  }

  function resetAll() {
    if (!confirm('清空后所有记录都无法恢复。确定要先导出备份再清空吗？')) return;
    DB.reset();
    S.day = DB.today();
    var t = DB.fromEpochDay(S.day);
    S.month = { y: t.y, m: t.m };
    closeSheet();
    render();
    toast('已清空');
  }

  /** 引导页的「先看示例」：灌入示例数据并直接进主界面，评委/新用户两步就能看到完整效果。 */
  function enterDemo() {
    seedDemo(true);
    DB.reload();
    var st = DB.get();
    if (!st.onboarded) { st.onboarded = true; DB.save(); }
    S.day = DB.today();
    var t = DB.fromEpochDay(S.day);
    S.month = { y: t.y, m: t.m };
    render();
    toast('已载入示例数据，可在「我 → 数据」里清空');
  }

  function finishOnboard() {
    var name = (document.getElementById('ob-name').value || '').trim().slice(0, 12);
    var g = document.querySelector('#ob-male').classList.contains('on') ? 'MALE' : 'FEMALE';
    var age = Math.min(99, Math.max(10, Number(document.getElementById('ob-age').value) || 28));
    var h = Math.min(230, Math.max(120, Number(document.getElementById('ob-height').value) || 178));
    var w = Math.max(30, Number(document.getElementById('ob-weight').value) || 78);

    DB.setProfile({ name: name, gender: g, age: age, heightCm: h, weightKg: w });
    DB.setWeight(DB.today(), w);

    var st = DB.get();
    st.onboarded = true;
    DB.save();

    S.day = DB.today();
    var t = DB.fromEpochDay(S.day);
    S.month = { y: t.y, m: t.m };
    render();
  }

  // ---------------------------------------------------------------- 演示数据

  /*
   * 给定资料，编一份「过去一周」的完整记录。设计原则是合理性：
   * - 训练按 胸 / 背+有氧 / 腿 / 肩臂+有氧 / 胸背+有氧 分化，周中插 2 个休息日；
   * - 力量重量按体重折算（卧推 0.55 倍体重这个量级），新手到中级都像；
   * - 主食克数按 TDEE 缩放（k 系数），女生长 TDEE 低，不会吃到 78kg 男生的饭量；
   * - 三餐结构固定：早餐碳水+蛋奶、午餐肉+主食+菜、晚餐偏轻，训练日加一勺蛋白粉；
   * - 周三炸鸡配可乐、休息日来份红烧肉——有点烟火气，但整周缺口都收敛在目标 550 附近，
   *   周汇总的「达标」不是 0/5 而是接近满格；
   * 所有数字都走 M.* 现算，与自己手动录入的口径完全一致。
   */
  function buildFirstWeek(p, idBase) {
    var t = DB.today();
    var bmr = M.bmr(p.gender, p.weightKg, p.heightCm, p.age);
    var tdee = M.tdeeBase(bmr, p.activityFactor || 1.2);
    var k = Math.min(1.35, Math.max(0.75, tdee / 2100));   // 食量随消耗缩放，钳制在 ±35%
    function g(v) { return Math.max(10, Math.round(v * k / 5) * 5); }
    function sw(mult, step) {
      var s = step || 2.5;
      return Math.max(s, Math.round(p.weightKg * mult / s) * s);
    }

    var seq = 0;
    function id() { return (idBase || 1) + seq++; }

    function stRec(day, exId, w, reps, sets) {
      var ex = SEED.exerciseById(exId);
      var c = M.strengthCalc(ex.met, p.weightKg, w, reps, sets, !w, bmr, 90);
      return {
        id: id(), day: day, exerciseId: exId, exerciseName: ex.name,
        weightKg: w || p.weightKg, reps: reps, sets: sets, bodyweight: !w,
        minutes: Math.round(c.minutes * 10) / 10, netKcal: Math.round(c.net),
        createdAt: Date.now()
      };
    }
    function cd(day, typeId, params) {
      var ty = SEED.cardioById(typeId);
      var min = params.min;
      var met = ty.met(params, null);
      var gross = M.grossKcal(met, p.weightKg, min);
      return {
        id: id(), day: day, typeId: typeId, typeName: ty.name, params: params,
        enumValue: null, minutes: min, met: Math.round(met * 100) / 100,
        netKcal: Math.round(M.netKcal(gross, bmr, min)), createdAt: Date.now()
      };
    }
    function meal(day, mt, foodId, grams) {
      var f = SEED.foodById(foodId);
      return {
        id: id(), day: day, meal: mt, foodId: foodId, foodName: f.name, grams: grams,
        servings: Math.round(grams / (f.servingGrams || 100) * 10) / 10,
        servingUnit: f.servingUnit, kcal: Math.round(f.kcalPer100g * grams / 100),
        protein: Math.round(f.proteinPer100g * grams / 100 * 10) / 10, createdAt: Date.now()
      };
    }

    var strength = [
      // 6 天前 · 胸 + 三头
      stRec(t - 6, 'bench_press', sw(0.55), 10, 4),
      stRec(t - 6, 'cable_crossover', 15, 12, 3),
      stRec(t - 6, 'tricep_pushdown', 25, 12, 3),
      // 4 天前 · 背 + 二头 + 爬坡走
      stRec(t - 4, 'lat_pulldown', sw(0.65), 12, 4),
      stRec(t - 4, 'seated_row', sw(0.55), 12, 3),
      stRec(t - 4, 'barbell_curl', sw(0.3), 12, 3),
      // 3 天前 · 腿
      stRec(t - 3, 'squat', sw(0.75), 8, 4),
      stRec(t - 3, 'leg_press', sw(1.8, 5), 10, 4),
      stRec(t - 3, 'leg_curl', 35, 12, 3),
      // 昨天 · 肩 + 手臂 + 慢跑
      stRec(t - 1, 'shoulder_press', sw(0.22, 2), 10, 3),
      stRec(t - 1, 'lateral_raise', 8, 15, 3),
      stRec(t - 1, 'hammer_curl', 12, 12, 3),
      // 今天 · 胸 + 背
      stRec(t, 'bench_press', sw(0.55), 12, 4),
      stRec(t, 'tbar_row', sw(0.45), 12, 3),
      stRec(t, 'reverse_fly', 8, 15, 3)
    ];

    var cardio = [
      cd(t - 4, 'incline_walk', { incline: 10, speed: 4.5, min: 30 }),
      cd(t - 1, 'treadmill', { speed: 7, incline: 0, min: 25 }),
      cd(t, 'incline_walk', { incline: 10, speed: 4.5, min: 25 })
    ];

    var meals = [];
    function eat(day, mt, fid, grams) { meals.push(meal(day, mt, fid, grams)); }

    // 6 天前
    eat(t - 6, 'BREAKFAST', 'oats', g(55)); eat(t - 6, 'BREAKFAST', 'milk', 250); eat(t - 6, 'BREAKFAST', 'egg', 100);
    eat(t - 6, 'LUNCH', 'rice', g(200)); eat(t - 6, 'LUNCH', 'chicken', 150); eat(t - 6, 'LUNCH', 'broccoli', 200);
    eat(t - 6, 'DINNER', 'sweetpotato', g(180)); eat(t - 6, 'DINNER', 'tofu', 200); eat(t - 6, 'DINNER', 'salad', 150);
    eat(t - 6, 'SNACK', 'whey', 30); eat(t - 6, 'SNACK', 'banana', 120);
    // 5 天前 · 休息日，吃得轻但晚上有红烧肉
    eat(t - 5, 'BREAKFAST', 'bread', g(70)); eat(t - 5, 'BREAKFAST', 'egg', 100); eat(t - 5, 'BREAKFAST', 'milk', 250);
    eat(t - 5, 'LUNCH', 'noodle', g(250)); eat(t - 5, 'LUNCH', 'tomato_egg', 200);
    eat(t - 5, 'DINNER', 'rice', g(150)); eat(t - 5, 'DINNER', 'pork', 100); eat(t - 5, 'DINNER', 'broccoli', 150);
    // 4 天前 · 背日 + 爬坡走，消耗大、吃得也多
    eat(t - 4, 'BREAKFAST', 'baozi', g(160)); eat(t - 4, 'BREAKFAST', 'milk', 250); eat(t - 4, 'BREAKFAST', 'egg', 50);
    eat(t - 4, 'LUNCH', 'rice', g(250)); eat(t - 4, 'LUNCH', 'beef', 150); eat(t - 4, 'LUNCH', 'salad', 200);
    eat(t - 4, 'DINNER', 'corn', g(200)); eat(t - 4, 'DINNER', 'chicken', 120); eat(t - 4, 'DINNER', 'broccoli', 150);
    eat(t - 4, 'SNACK', 'whey', 30); eat(t - 4, 'SNACK', 'yogurt', 150); eat(t - 4, 'SNACK', 'banana', 120);
    // 3 天前 · 腿日，碳水给足
    eat(t - 3, 'BREAKFAST', 'oats', g(55)); eat(t - 3, 'BREAKFAST', 'yogurt', 150); eat(t - 3, 'BREAKFAST', 'banana', 120);
    eat(t - 3, 'LUNCH', 'rice', g(250)); eat(t - 3, 'LUNCH', 'chicken', 150); eat(t - 3, 'LUNCH', 'broccoli', 200);
    eat(t - 3, 'DINNER', 'rice', g(250)); eat(t - 3, 'DINNER', 'beef', 150); eat(t - 3, 'DINNER', 'broccoli', 150);
    eat(t - 3, 'SNACK', 'whey', 30);
    // 2 天前 · 休息日，中午一顿炸鸡、下午可乐，真实的人就是这样
    eat(t - 2, 'BREAKFAST', 'bread', g(70)); eat(t - 2, 'BREAKFAST', 'egg', 100); eat(t - 2, 'BREAKFAST', 'milk', 250);
    eat(t - 2, 'LUNCH', 'rice', g(200)); eat(t - 2, 'LUNCH', 'fried_chicken', 100); eat(t - 2, 'LUNCH', 'salad', 150);
    eat(t - 2, 'DINNER', 'noodle', g(200)); eat(t - 2, 'DINNER', 'egg', 50);
    eat(t - 2, 'SNACK', 'cola', 330);
    // 昨天 · 肩臂 + 慢跑
    eat(t - 1, 'BREAKFAST', 'oats', g(55)); eat(t - 1, 'BREAKFAST', 'milk', 250); eat(t - 1, 'BREAKFAST', 'egg', 50);
    eat(t - 1, 'LUNCH', 'rice', g(230)); eat(t - 1, 'LUNCH', 'salmon', 130); eat(t - 1, 'LUNCH', 'broccoli', 150);
    eat(t - 1, 'DINNER', 'rice', g(230)); eat(t - 1, 'DINNER', 'chicken', 120); eat(t - 1, 'DINNER', 'tomato_egg', 200);
    eat(t - 1, 'SNACK', 'whey', 30); eat(t - 1, 'SNACK', 'yogurt', 150);
    // 今天：练了胸背和爬坡走，三餐记满，缺口正好落在目标附近
    eat(t, 'BREAKFAST', 'bread', g(70)); eat(t, 'BREAKFAST', 'egg', 100); eat(t, 'BREAKFAST', 'milk', 250);
    eat(t, 'LUNCH', 'rice', g(250)); eat(t, 'LUNCH', 'chicken', 150); eat(t, 'LUNCH', 'broccoli', 200);
    eat(t, 'DINNER', 'rice', g(200)); eat(t, 'DINNER', 'salmon', 130); eat(t, 'DINNER', 'broccoli', 150);
    eat(t, 'DINNER', 'tofu', 100); eat(t, 'DINNER', 'corn', g(150));
    eat(t, 'SNACK', 'whey', 30);

    // 体重缓慢下降的曲线，最后一点落在今天、等于当前体重
    var weightLog = {};
    weightLog[t - 6] = Math.round((p.weightKg + 0.5) * 10) / 10;
    weightLog[t - 4] = Math.round((p.weightKg + 0.35) * 10) / 10;
    weightLog[t - 2] = Math.round((p.weightKg + 0.2) * 10) / 10;
    weightLog[t] = p.weightKg;

    return {
      strength: strength, cardio: cardio, meals: meals,
      restDays: [t - 5, t - 2],
      weightLog: weightLog, count: seq
    };
  }

  /*
   * 带 ?demo=1 打开、且本地还没有数据时，写入一份示例记录。
   * 只在空库时注入，所以不会覆盖真实用户的数据。
   */
  function seedDemo(force) {
    if (!force && location.search.indexOf('demo=1') < 0) return;
    try { if (localStorage.getItem(DB.KEY)) return; } catch (e) { return; }

    var p = {
      name: 'ivan', gender: 'MALE', age: 27, heightCm: 181, weightKg: 76.5,
      activityFactor: 1.2, targetWeightKg: 72, weeklyRateKg: 0.5,
      proteinAuto: false, proteinCustomG: 120
    };
    var w = buildFirstWeek(p, 1);
    var state = {
      version: 1, onboarded: true, darkTheme: true, heatWarn: true,
      profile: p,
      weightLog: w.weightLog,
      strength: w.strength, cardio: w.cardio, meals: w.meals,
      restDays: w.restDays, seq: w.count + 1
    };
    try { localStorage.setItem(DB.KEY, JSON.stringify(state)); } catch (e) {}
  }

  // ---------------------------------------------------------------- 启动

  function init() {
    seedDemo();
    var st = DB.get();

    // 便于直接分享某个页面：?view=diet&sheet=food&theme=light
    if (/[?&]theme=light/.test(location.search)) { st.darkTheme = false; DB.save(); }
    var vm = /[?&]view=(train|diet|me)/.exec(location.search);
    if (vm) S.view = vm[1];
    applyTheme(st.darkTheme);
    S.day = DB.today();
    var t = DB.fromEpochDay(S.day);
    S.month = { y: t.y, m: t.m };

    app.addEventListener('click', onClick);
    app.addEventListener('input', onInput);

    render();
    if (!st.onboarded) onboardHint();

    // 截图 / 演示用：直接打开某个弹层
    var sm = /[?&]sheet=(\w+)/.exec(location.search);
    if (sm) {
      if (sm[1] === 'food') { S.overlay = { type: 'food', meal: 'LUNCH' }; }
      else if (sm[1] === 'estimate') {
        var est = EST.estimate('牛肉减脂便当');
        S.overlay = { type: 'aiResult', est: est, query: '牛肉减脂便当', meal: 'LUNCH' };
        S.draft = { mult: 1 };
      } else if (sm[1] === 'profile') { S.overlay = { type: 'profile' }; }
      else if (sm[1] === 'theme') { S.overlay = { type: 'theme' }; }
      paintOverlays();
    }
  }

  init();
})();
