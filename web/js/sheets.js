/*
 * 所有弹层。渲染纯函数：输入 S.overlay / S.draft，输出 HTML 字符串，
 * 交互全部走 data-act 交给 app.js 的事件委托。
 */
(function (g) {
  'use strict';

  function head(title, sub) {
    return '<div class="sheet-head">'
      + '<div><div class="t">' + title + '</div>'
      + (sub ? '<div class="s">' + sub + '</div>' : '')
      + '</div><div class="spacer"></div>'
      + '<button class="close-btn" data-act="close-sheet">×</button></div>';
  }

  function wrap(inner, noGrab) {
    return '<div class="sheet-mask" data-act="close-sheet">'
      + '<div class="sheet" data-stop="1">'
      + (noGrab ? '' : '<div class="sheet-grab"></div>')
      + inner
      + '</div></div>';
  }

  // ------------------------------------------------------------ 选器械

  function exResults(S) {
    var st = DB.get();
    var q = (S.exQuery || '').trim();
    var body = '';

    var recentIds = [];
    for (var i = st.strength.length - 1; i >= 0 && recentIds.length < 5; i--) {
      var id = st.strength[i].exerciseId;
      if (recentIds.indexOf(id) < 0) recentIds.push(id);
    }

    if (!q && recentIds.length) {
      body += '<div class="grp-title">最近练过</div><div class="chip-row" style="margin-top:0">';
      for (var r = 0; r < recentIds.length; r++) {
        var re = SEED.exerciseById(recentIds[r]);
        if (re) body += '<button class="chip" data-act="pick-exercise" data-id="' + re.id + '">' + U.esc(re.name) + '</button>';
      }
      body += '</div>';
    }

    SEED.muscleGroups.forEach(function (grp) {
      var list = SEED.exercises.filter(function (e) {
        if (e.groups.indexOf(grp.id) < 0) return false;
        return !q || e.name.indexOf(q) >= 0;
      });
      if (!list.length) return;
      body += '<div class="grp-title">' + grp.name + '</div>';
      list.forEach(function (e) {
        body += '<div class="list-item" data-act="pick-exercise" data-id="' + e.id + '">'
          + '<div class="t">' + U.esc(e.name) + '</div>'
          + '<div class="s">MET ' + e.met.toFixed(1) + (e.bodyweightDefault ? ' · 自重' : '') + '</div>'
          + '<div class="arrow">›</div></div>';
      });
    });

    return body || '<div class="empty">没找到这个器械</div>';
  }

  function exercisePicker(S) {
    return wrap(head('选择器械', '47 个动作 · 按部位分组')
      + '<div class="sheet-body">'
      + '<input class="search-box" id="ex-search" placeholder="搜索器械名" '
      + 'value="' + U.esc(S.exQuery || '') + '" data-role="ex-query" autocomplete="off">'
      + '<div id="ex-results">' + exResults(S) + '</div>'
      + '</div>');
  }

  // ------------------------------------------------------------ 力量参数

  function strengthParam(S) {
    var d = S.draft;
    var ex = SEED.exerciseById(d.exId);
    if (!ex) return '';
    var st = DB.get();
    var tdee = DB.effectiveTdee();
    var calc = M.strengthCalc(ex.met, st.profile.weightKg, d.weight, d.reps, d.sets, d.bw, tdee.bmr, d.rest);
    var minutes = calc.minutes;
    var net = Math.round(calc.net);

    return wrap(head(U.esc(ex.name), '本次训练的参数')
      + '<div class="sheet-body">'
      + (ex.bodyweightDefault ? bodyweightToggle(d) : '')
      + row('重量', d.bw ? '自重' : U.fmtNum(d.weight), 'kg', 'weight', d.bw ? 0 : 2.5)
      + row('次数', d.reps, '次', 'reps', 1)
      + row('组数', d.sets, '组', 'sets', 1)
      + '<div class="preview-box">'
      + '预计 <b>' + U.fmtNum(minutes) + ' 分钟</b>（每下 3 秒 + 组间休息 90 秒，末组不休息）<br>'
      + '有效 MET <b>' + calc.met.toFixed(2) + '</b>，毛消耗 <b>' + Math.round(calc.gross) + ' kcal</b><br>'
      + '扣掉这段时间的静息消耗后，净计入 <b>' + net + ' kcal</b>'
      + '</div>'
      + '<button class="primary-btn" data-act="save-strength">记录这一组</button>'
      + '</div>');
  }

  function bodyweightToggle(d) {
    return '<div class="param-row" style="border-bottom:1px solid var(--line)">'
      + '<div class="lbl">自重</div>'
      + '<div class="val" style="font-size:13px;text-align:left">' + (d.bw ? '按体重计算，不填器械重量' : '使用器械重量') + '</div>'
      + '<button class="chip' + (d.bw ? ' on' : '') + '" data-act="toggle-bw">' + (d.bw ? '已开' : '开启') + '</button>'
      + '</div>';
  }

  function row(label, value, unit, key, step) {
    return '<div class="param-row">'
      + '<div class="lbl">' + label + '</div>'
      + '<button class="stepper" data-act="step" data-k="' + key + '" data-d="-1">−</button>'
      + '<div class="val">' + value + '<span class="u">' + unit + '</span></div>'
      + '<button class="stepper" data-act="step" data-k="' + key + '" data-d="1">+</button>'
      + '</div>';
  }

  // ------------------------------------------------------------ 有氧

  function cardioPicker() {
    var body = '';
    SEED.cardioTypes.forEach(function (t) {
      body += '<div class="list-item" data-act="pick-cardio" data-id="' + t.id + '">'
        + '<div class="t">' + U.esc(t.name) + '</div>'
        + '<div class="s">' + t.params.length + ' 项参数</div>'
        + '<div class="arrow">›</div></div>';
    });
    return wrap(head('选择有氧', 'MET 随参数实时计算')
      + '<div class="sheet-body">' + body + '</div>');
  }

  function cardioParam(S) {
    var d = S.draft;
    var t = SEED.cardioById(d.typeId);
    if (!t) return '';
    var st = DB.get();
    var tdee = DB.effectiveTdee();
    var met = Math.min(20, Math.max(1, t.met(d.params, d.enumValue)));
    var minutes = d.params.min || 30;
    var gross = M.grossKcal(met, st.profile.weightKg, minutes);
    var net = Math.round(M.netKcal(gross, tdee.bmr, minutes));

    var body = '';
    t.params.forEach(function (p) {
      var v = d.params[p.key];
      var shown = p.step >= 1 ? String(Math.round(v)) : U.fmtNum(v);
      body += '<div class="param-row">'
        + '<div class="lbl">' + p.label + '</div>'
        + '<button class="stepper" data-act="cstep" data-k="' + p.key + '" data-d="-1">−</button>'
        + '<div class="val">' + shown + '<span class="u">' + p.unit + '</span></div>'
        + '<button class="stepper" data-act="cstep" data-k="' + p.key + '" data-d="1">+</button>'
        + '</div>';
    });

    if (t.enumSpec) {
      body += '<div class="grp-title">' + t.enumSpec.label + '</div><div class="chip-row" style="margin-top:0">';
      t.enumSpec.options.forEach(function (o) {
        body += '<button class="chip' + (d.enumValue === o ? ' on' : '') + '" data-act="cpick" data-v="' + o + '">' + o + '</button>';
      });
      body += '</div>';
    }

    return wrap(head(U.esc(t.name), 'MET ' + met.toFixed(2) + ' · ' + U.fmtNum(minutes) + ' 分钟')
      + '<div class="sheet-body">' + body
      + '<div class="preview-box">毛消耗 <b>' + Math.round(gross) + ' kcal</b>，'
      + '扣掉这段时间的静息消耗后净计入 <b>' + net + ' kcal</b>。</div>'
      + '<button class="primary-btn" data-act="save-cardio">记录这次有氧</button>'
      + '</div>');
  }

  // ------------------------------------------------------------ 食物搜索

  /*
   * 搜索结果是独立的一段：输入时只重绘这一段，不动输入框本身，
   * 否则整块 innerHTML 重建会让手机键盘收起。
   */
  function foodResults(S) {
    var st = DB.get();
    var q = (S.query || '').trim();
    var lower = q.toLowerCase();
    var body = '';

    if (!q) {
      var seen = {}, recent = [];
      for (var i = st.meals.length - 1; i >= 0 && recent.length < 8; i--) {
        var m = st.meals[i];
        if (seen[m.foodId]) continue;
        seen[m.foodId] = 1;
        var f = SEED.foodById(m.foodId);
        if (f) recent.push(f);
      }
      if (recent.length) {
        body += '<div class="grp-title">最近吃过 · 点一下直接记</div><div class="chip-row" style="margin-top:0">';
        recent.forEach(function (f) {
          body += '<button class="chip" data-act="quick-add" data-id="' + f.id + '">'
            + (f.emoji || '🍽') + ' ' + U.esc(f.name) + '</button>';
        });
        body += '</div>';
      }
    }

    if (q) {
      var results = SEED.foods.filter(function (f) {
        return f.name.indexOf(q) >= 0
          || (f.alias && f.alias.toLowerCase().indexOf(lower) >= 0);
      });
      if (results.length) {
        results.forEach(function (f) {
          body += '<div class="list-item" data-act="pick-food" data-id="' + f.id + '">'
            + '<div style="font-size:15px;width:22px">' + (f.emoji || '🍽') + '</div>'
            + '<div class="t">' + U.esc(f.name)
            + '<div class="s">' + Math.round(f.kcalPer100g) + ' kcal / 100g · 1' + (f.servingUnit || '份')
            + ' ≈ ' + U.fmtNum(f.servingGrams || 100) + 'g</div></div>'
            + '<div class="arrow">›</div></div>';
        });
      } else {
        body += '<div class="empty" style="margin-bottom:10px">本地食物库没有「' + U.esc(q) + '」'
          + '<div style="margin-top:4px;font-size:10px">点下面的按钮，按常见分量拆解这一餐</div></div>';
      }
      body = '<div class="chip-row">'
        + '<button class="chip purple-chip" data-act="estimate">✨ 拆解「' + U.esc(q) + '」</button>'
        + '</div>' + body;
    }

    return body;
  }

  function foodSearch(S) {
    var meal = S.overlay.meal;
    return wrap(head('添加到' + DB.mealLabel(meal), '输入食物名，或直接描述「牛肉减脂便当」')
      + '<div class="sheet-body">'
      + '<input class="search-box" id="food-search" placeholder="搜索食物 / 描述这一餐" '
      + 'value="' + U.esc(S.query || '') + '" data-role="food-query" autocomplete="off">'
      + '<div id="food-results">' + foodResults(S) + '</div>'
      + '</div>');
  }

  // ------------------------------------------------------------ 份量

  function portion(S) {
    var d = S.draft;
    var f = SEED.foodById(d.foodId);
    if (!f) return '';
    var grams = d.grams;
    var kcal = Math.round(f.kcalPer100g * grams / 100);
    var protein = f.proteinPer100g * grams / 100;
    var unit = f.servingUnit || '份';
    var unitG = f.servingGrams || 100;

    return wrap(head(U.esc(f.name), Math.round(f.kcalPer100g) + ' kcal / 100g')
      + '<div class="sheet-body">'
      + '<div class="chip-row" style="margin-top:2px">'
      + [0.5, 1, 1.5, 2].map(function (n) {
        return '<button class="chip' + (Math.abs(grams - unitG * n) < 0.5 ? ' on' : '')
          + '" data-act="set-serv" data-v="' + n + '">' + n + ' ' + unit + '</button>';
      }).join('')
      + '</div>'
      + '<div class="param-row" style="margin-top:6px">'
      + '<div class="lbl">克数</div>'
      + '<button class="stepper" data-act="step" data-k="grams" data-d="-1">−</button>'
      + '<div class="val">' + Math.round(grams) + '<span class="u">g</span></div>'
      + '<button class="stepper" data-act="step" data-k="grams" data-d="1">+</button>'
      + '</div>'
      + '<div class="preview-box">'
      + '这一份约 <b>' + U.fmtNum(grams / unitG) + ' ' + unit + '</b>，'
      + '热量 <b>' + kcal + ' kcal</b>，蛋白 <b>' + U.fmtNum(protein) + ' g</b>'
      + '</div>'
      + '<button class="primary-btn" data-act="save-portion">添加 ' + kcal + ' kcal</button>'
      + '</div>');
  }

  // ------------------------------------------------------------ 拆解结果

  function aiResult(S) {
    var d = S.draft;
    var est = S.overlay.est;
    var mult = d.mult || 1;
    var total = Math.round(est.totalKcal * mult);

    var items = est.items.map(function (it) {
      return '<div class="row" style="background:transparent;padding:8px 0;border-bottom:1px solid var(--line);border-radius:0">'
        + '<div class="row-main"><div class="row-title">' + U.esc(it.name) + '</div>'
        + '<div class="row-sub">' + U.fmtNum(it.grams * mult) + ' g · 蛋白 ' + U.fmtNum(it.protein * mult) + ' g</div></div>'
        + '<div class="row-num"><span class="v">' + Math.round(it.kcal * mult) + '</span><span class="u">kcal</span></div>'
        + '</div>';
    }).join('');

    return wrap(head(U.esc(S.overlay.query), '拆解成 ' + est.items.length + ' 项 · 可核对，不必全信')
      + '<div class="sheet-body">'
      + '<div style="display:flex;gap:14px;margin-bottom:12px">'
      + stat('热量', total, 'kcal', '')
      + stat('蛋白', U.fmtNum(est.totalProtein * mult), 'g', 'blue')
      + stat('碳水', U.fmtNum(est.totalCarb * mult), 'g', 'amber')
      + stat('脂肪', U.fmtNum(est.totalFat * mult), 'g', 'purple')
      + '</div>'
      + '<div class="grp-title">份量倍数</div>'
      + '<div class="chip-row" style="margin-top:0">'
      + [0.5, 0.8, 1, 1.3, 1.5].map(function (n) {
        return '<button class="chip' + (mult === n ? ' on' : '') + '" data-act="set-mult" data-v="' + n + '">' + n + ' 份</button>';
      }).join('')
      + '</div>'
      + '<div class="grp-title">拆解（可核对）</div>'
      + items
      + (est.assumptions.length
        ? '<div class="preview-box">' + est.assumptions.map(function (a) { return '· ' + U.esc(a); }).join('<br>') + '</div>'
        : '')
      + '<button class="primary-btn" data-act="confirm-estimate">添加 ' + total + ' kcal</button>'
      + '</div>');
  }

  function stat(label, value, unit, cls) {
    return '<div class="stat"><div class="stat-label">' + label + '</div>'
      + '<div class="stat-value ' + cls + '">' + value + '<span class="unit">' + unit + '</span></div></div>';
  }

  // ------------------------------------------------------------ 皮肤

  function theme(S) {
    var st = DB.get();
    function card(dark, title) {
      var on = st.darkTheme === dark;
      var p = dark
        ? { bg: '#0b0d10', s: '#14171c', s2: '#1b1f26', t: '#e9ecf1', m: '#8a919e', g: '#3ddc84', b: '#5b8cff', a: '#ffc247' }
        : { bg: '#ffffff', s: '#f6f7f9', s2: '#edeff3', t: '#14171c', m: '#5b636f', g: '#0f9d58', b: '#3c6fe0', a: '#c07c05' };
      return '<div class="theme-card' + (on ? ' sel' : '') + '" data-act="set-theme" data-dark="' + dark + '" '
        + 'style="background:' + p.bg + ';border-color:' + (on ? p.g : 'transparent') + '">'
        + '<div class="bar" style="background:' + p.t + '"></div>'
        + '<div class="bar2" style="background:' + p.m + '"></div>'
        + '<div class="boxx" style="background:' + p.s + '"></div>'
        + '<div class="dots"><i class="dot" style="background:' + p.b + '"></i>'
        + '<i class="dot" style="background:' + p.a + '"></i>'
        + '<i class="dot" style="background:' + p.g + '"></i></div>'
        + '<div class="cap" style="color:' + p.m + '">' + (on ? '使用中' : '点按切换') + '</div>'
        + '</div>';
    }
    return wrap(head('外观', '两套配色，不跟随系统')
      + '<div class="sheet-body"><div class="theme-row">'
      + card(true, '黑夜') + card(false, '白天')
      + '</div></div>');
  }

  // ------------------------------------------------------------ 资料

  function profile(S) {
    var p = DB.get().profile;
    return wrap(head('个人资料', '改了会立即重算消耗与目标')
      + '<div class="sheet-body">'
      + '<div class="grp-title">称呼</div>'
      + '<input class="search-box" id="pf-name" maxlength="12" placeholder="怎么称呼你（可留空）" '
      + 'value="' + U.esc(p.name || '') + '" data-role="pf-name" autocomplete="off">'
      + '<div class="grp-title">性别</div><div class="seg" style="margin-top:0">'
      + '<button class="' + (p.gender === 'MALE' ? 'on' : '') + '" data-act="set-gender" data-v="MALE">男</button>'
      + '<button class="' + (p.gender === 'FEMALE' ? 'on' : '') + '" data-act="set-gender" data-v="FEMALE">女</button>'
      + '</div>'
      + '<div class="grp-title">身体数据</div>'
      + row('年龄', p.age, '岁', 'age', 1)
      + row('身高', U.fmtNum(p.heightCm), 'cm', 'height', 1)
      + row('体重', U.fmtNum(p.weightKg), 'kg', 'pweight', 0.5)
      + row('目标体重', U.fmtNum(p.targetWeightKg), 'kg', 'target', 0.5)
      + '<div class="grp-title">减重速度</div>'
      + '<div class="chip-row" style="margin-top:0">'
      + [0.25, 0.5, 0.75, 1].map(function (r) {
        return '<button class="chip' + (Math.abs(p.weeklyRateKg - r) < 0.01 ? ' on' : '') + '" data-act="set-rate" data-v="' + r + '">'
          + r + ' kg/周 · ' + M.targetDeficit(r) + ' kcal</button>';
      }).join('')
      + '</div>'
      + '<div class="grp-title">日常活动量（不含训练）</div>'
      + SEED.activityLevels.map(function (l) {
        var on = Math.abs(p.activityFactor - l.factor) < 0.001;
        return '<div class="list-item" data-act="set-activity" data-v="' + l.factor + '">'
          + '<div class="t">' + l.label + '<div class="s">' + l.desc + '</div></div>'
          + '<div class="s">×' + l.factor + '</div>'
          + '<div class="arrow" style="color:' + (on ? 'var(--green)' : 'var(--faint)') + '">' + (on ? '✓' : '') + '</div></div>';
      }).join('')
      + '</div>');
  }

  // ------------------------------------------------------------ 导出

  function exportSheet() {
    return wrap(head('数据', '只有你自己能拿到，没有云端')
      + '<div class="sheet-body">'
      + '<div class="list-item" data-act="export-json"><div class="t">导出全部数据（JSON）'
      + '<div class="s">完整备份，换设备时导入即可还原</div></div><div class="arrow">↓</div></div>'
      + '<div class="list-item" data-act="export-csv"><div class="t">导出 CSV'
      + '<div class="s">5 个文件，可直接用 Excel 打开</div></div><div class="arrow">↓</div></div>'
      + '<div class="list-item" data-act="import-json"><div class="t">从备份导入'
      + '<div class="s">会覆盖当前全部数据</div></div><div class="arrow">↑</div></div>'
      + '<div class="preview-box">数据只存在这台设备的浏览器里。清理浏览器数据会一起消失，'
      + '建议每周导出一次 JSON 存到网盘。</div>'
      + '<button class="ghost-btn" style="color:var(--red)" data-act="reset-all">清空所有数据</button>'
      + '</div>');
  }

  // ------------------------------------------------------------ 入口

  function html(S) {
    var o = S.overlay;
    if (!o) return '';
    switch (o.type) {
      case 'exercise': return exercisePicker(S);
      case 'strength': return strengthParam(S);
      case 'cardio': return cardioPicker();
      case 'cardioParam': return cardioParam(S);
      case 'food': return foodSearch(S);
      case 'portion': return portion(S);
      case 'aiResult': return aiResult(S);
      case 'theme': return theme(S);
      case 'profile': return profile(S);
      case 'export': return exportSheet();
      default: return '';
    }
  }

  g.SHEET = { html: html, foodResults: foodResults, exResults: exResults };
})(window);
