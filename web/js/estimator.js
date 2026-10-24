/*
 * 本地规则估算，从 LocalEstimator.kt 1:1 移植。
 *
 * 网页版没有大模型可调（国内主流 API 都不支持浏览器直连），所以这条路径不是兜底，
 * 而是主路径。设计原则照旧：绝不返回空，并且必须把「拆解」做出来 ——
 * 用户看到「杂粮饭 140g / 卤牛肉 110g / 烹调油 8g」才知道该改哪一项，
 * 而不是面对一个无法验证的总数。
 */
(function (g) {
  'use strict';

  function item(name, grams, kcal, p, c, f) {
    return { name: name, grams: grams, kcal: kcal, protein: p, carb: c, fat: f, confidence: 0.55 };
  }

  function has(q, keys) {
    for (var i = 0; i < keys.length; i++) if (q.indexOf(keys[i]) >= 0) return true;
    return false;
  }

  function round1(v) { return Math.round(v * 10) / 10; }

  function hasMealKeyword(q) {
    return has(q, ['便当', '盒饭', '套餐', '外卖', '沙拉', '面', '火锅', '炒', '饭']);
  }

  function parseCount(q) {
    var cn = { '一': 1, '两': 2, '二': 2, '三': 3, '四': 4, '五': 5 };
    for (var k in cn) {
      if (q.indexOf(k + '个') >= 0 || q.indexOf(k + '杯') >= 0 || q.indexOf(k + '碗') >= 0 || q.indexOf(k + '份') >= 0) {
        return cn[k];
      }
    }
    var m = q.match(/(\d+(?:\.\d+)?)\s*(个|杯|碗|份|根|片|勺|块)/);
    if (m) {
      var v = parseFloat(m[1]);
      if (!isNaN(v)) return Math.min(20, Math.max(0.5, v));
    }
    return 1;
  }

  // ------------------------------------------------------------ 规则

  function bento(q) {
    var light = q.indexOf('减脂') >= 0 || q.indexOf('低脂') >= 0 || q.indexOf('健身') >= 0;
    var protein;
    if (has(q, ['牛', '肥牛', '牛腩'])) protein = item('卤牛肉', 110, 148, 28, 2, 3.6);
    else if (has(q, ['鸡', '鸡胸'])) protein = item('鸡胸肉', 110, 146, 27, 1, 3.3);
    else if (has(q, ['猪', '排骨', '肉'])) protein = item('猪肉片', 110, 220, 20, 2, 15);
    else if (has(q, ['鱼', '三文鱼', '鳕鱼'])) protein = item('鱼类主菜', 110, 150, 21, 1, 7);
    else protein = item('主菜', 110, 180, 18, 4, 9);

    var staple = light ? item('杂粮饭', 140, 122, 4.2, 26, 1.1) : item('白米饭', 180, 209, 4.7, 46, 0.5);
    var oil = light ? item('烹调油', 8, 71, 0, 0, 8) : item('烹调油·酱汁', 12, 106, 0, 1, 12);

    var items = [staple, protein, item('西兰花·胡萝卜', 120, 40, 3.4, 7, 0.4), oil];
    if (light) items.splice(1, 0, item('水煮蛋', 50, 72, 6.7, 0.5, 5));

    var notes = ['按常见外卖便当分量拆解'];
    if (light) notes.push('「减脂」描述已下调主食与用油');
    notes.push('若实际饭量更多，可把份量调到 1.2–1.3 份');
    return { items: items, notes: notes };
  }

  function salad() {
    return {
      items: [
        item('生菜·番茄·黄瓜', 150, 45, 2, 7, 0.5),
        item('鸡胸肉', 100, 133, 24.6, 1, 3),
        item('甜玉米粒', 60, 67, 2.4, 13, 0.7),
        item('沙拉酱·油醋汁', 15, 60, 0.3, 1.5, 6)
      ],
      notes: ['沙拉酱是这类餐最容易漏掉的热量来源']
    };
  }

  function noodleBowl() {
    return {
      items: [
        item('面条', 250, 343, 11.3, 68, 2.5),
        item('牛肉·浇头', 80, 108, 15, 1, 4.5),
        item('青菜·萝卜', 60, 12, 0.9, 2, 0.1),
        item('汤底浮油', 10, 90, 0, 0, 10)
      ],
      notes: ['汤面按整碗估算，汤没喝完也要计入大部分热量', '汤底含盐偏高，减脂期注意水肿']
    };
  }

  function hotpot() {
    return {
      items: [
        item('肉类·丸子', 180, 260, 22, 6, 16),
        item('蔬菜', 180, 54, 4, 8, 0.6),
        item('红油锅底·蘸料', 25, 220, 1, 3, 23)
      ],
      notes: ['蘸料和锅底占这餐热量近一半', '清汤锅底可减掉约 150 kcal']
    };
  }

  function stirFry(q) {
    var veggie = has(q, ['青菜', '菜', '茄子', '土豆丝', '豆角']) && !has(q, ['肉', '鸡', '牛', '猪', '虾']);
    if (veggie) {
      return {
        items: [item('蔬菜', 200, 60, 3, 9, 0.5), item('烹调油', 12, 106, 0, 0, 12)],
        notes: ['家常炒菜用油量通常比预期多']
      };
    }
    return {
      items: [
        item('主料（肉/蛋）', 150, 235, 22, 5, 13),
        item('配菜', 100, 30, 2, 5, 0.3),
        item('烹调油·糖', 12, 106, 0, 2, 11)
      ],
      notes: ['红烧/糖醋类含糖，热量按家常做法估']
    };
  }

  function generic(q) {
    var name = q.length > 12 ? q.slice(0, 12) : q;
    return {
      items: [
        item(name + ' · 主料', 150, 235, 16, 18, 9),
        item('烹调油', 10, 89, 0, 0, 10)
      ],
      notes: ['没有匹配到明确品类，按「家常一荤一素」的均值估']
    };
  }

  // ------------------------------------------------------------ 入口

  function estimate(query) {
    var q = String(query || '').trim();
    if (!q) return null;

    // 本地食物库能直接命中的单品，优先用真实数据
    var hit = null;
    for (var i = 0; i < SEED.foods.length; i++) {
      var f = SEED.foods[i];
      if (q.indexOf(f.name) >= 0 && !hasMealKeyword(q)) { hit = f; break; }
    }
    if (hit) {
      var count = parseCount(q);
      var grams = (hit.servingGrams || 100) * count;
      var one = item(
        hit.name + ' ' + count + (hit.servingUnit || '份'),
        grams,
        round1(hit.kcalPer100g * grams / 100),
        round1(hit.proteinPer100g * grams / 100),
        0, 0
      );
      return pack([one], count, hit.servingUnit || '份', grams,
        ['来自本地食物库「' + hit.name + '」，按 100g 营养值折算', '未经 AI 校验']);
    }

    var r;
    if (has(q, ['便当', '盒饭', '套餐', '外卖', '减脂餐', '轻食碗'])) r = bento(q);
    else if (has(q, ['沙拉', '沙律'])) r = salad();
    else if (has(q, ['面', '米线', '粉', '拉面', '馄饨', '饺子'])) r = noodleBowl();
    else if (has(q, ['火锅', '麻辣烫', '香锅', '冒菜', '干锅'])) r = hotpot();
    else if (has(q, ['炒', '红烧', '煎', '炸', '焖', '炖', '烤'])) r = stirFry(q);
    else r = generic(q);

    var totalG = 0;
    for (var j = 0; j < r.items.length; j++) totalG += r.items[j].grams;
    return pack(r.items, 1, '份', totalG, r.notes.concat(['本地规则估算，误差约 ±25%']));
  }

  function pack(items, qty, unit, grams, notes) {
    var kcal = 0, protein = 0, carb = 0, fat = 0, gsum = 0;
    for (var i = 0; i < items.length; i++) {
      kcal += items[i].kcal; protein += items[i].protein;
      carb += items[i].carb; fat += items[i].fat; gsum += items[i].grams;
    }
    return {
      items: items,
      servingQty: qty,
      servingUnit: unit,
      servingGrams: grams || gsum,
      assumptions: notes,
      fromAi: false,
      totalKcal: Math.round(kcal),
      totalProtein: round1(protein),
      totalCarb: round1(carb),
      totalFat: round1(fat),
      totalGrams: round1(gsum)
    };
  }

  g.EST = { estimate: estimate };
})(window);
