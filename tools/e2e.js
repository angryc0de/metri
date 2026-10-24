/*
 * 端到端验证 v2：从引导页开始走完整用户路径。
 * 覆盖：引导页 → 示例入口 → 主界面 → 切页 → 搜索拆解添加 → 资料 stepper → 休息日 → 导出 → 换肤 → 持久化。
 * 用 Edge 的 CDP 驱动，不依赖任何测试框架。
 */
const { spawn } = require('child_process');
const http = require('http');

const EDGE = 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe';
const PORT = 9334;
const APP = 'http://127.0.0.1:8123/';

function getJson(url) {
  return new Promise((res, rej) => {
    http.get(url, r => {
      let b = '';
      r.on('data', d => (b += d));
      r.on('end', () => { try { res(JSON.parse(b)); } catch (e) { rej(e); } });
    }).on('error', rej);
  });
}

async function waitTarget() {
  for (let i = 0; i < 50; i++) {
    try {
      const list = await getJson(`http://127.0.0.1:${PORT}/json/list`);
      const page = list.find(t => t.type === 'page' && t.webSocketDebuggerUrl);
      if (page) return page;
    } catch (e) { /* 还没起来 */ }
    await new Promise(r => setTimeout(r, 300));
  }
  throw new Error('CDP target 未就绪');
}

class CDP {
  constructor(ws) {
    this.ws = ws;
    this.id = 0;
    this.pending = new Map();
    ws.onmessage = ev => {
      const m = JSON.parse(ev.data);
      if (m.id && this.pending.has(m.id)) {
        this.pending.get(m.id)(m);
        this.pending.delete(m.id);
      }
    };
  }
  send(method, params) {
    const id = ++this.id;
    return new Promise(res => {
      this.pending.set(id, res);
      this.ws.send(JSON.stringify({ id, method, params }));
    });
  }
  async eval(expression) {
    const r = await this.send('Runtime.evaluate', {
      expression, returnByValue: true, awaitPromise: true, userGesture: true
    });
    if (r.result && r.result.exceptionDetails) {
      throw new Error(r.result.exceptionDetails.text + ' | ' + (r.result.exceptionDetails.exception || {}).description);
    }
    return r.result && r.result.result ? r.result.result.value : undefined;
  }
}

const sleep = ms => new Promise(r => setTimeout(r, ms));

let pass = 0, fail = 0;
function ok(name, cond, extra) {
  if (cond) { pass++; console.log('PASS ' + name); }
  else { fail++; console.log('FAIL ' + name + (extra ? '  → ' + extra : '')); }
}

(async () => {
  const tmp = process.env.TEMP + '\\edge-e2e-' + Date.now();
  const proc = spawn(EDGE, [
    '--headless=new', '--disable-gpu', '--hide-scrollbars',
    `--remote-debugging-port=${PORT}`, `--user-data-dir=${tmp}`,
    '--window-size=430,932', APP
  ], { stdio: 'ignore' });

  let cdp;
  try {
    const target = await waitTarget();
    const ws = new WebSocket(target.webSocketDebuggerUrl);
    await new Promise((res, rej) => { ws.onopen = res; ws.onerror = rej; });
    cdp = new CDP(ws);
    await cdp.send('Runtime.enable');
    await sleep(1200);

    // ---------- 引导页
    ok('首次访问显示引导页',
      (await cdp.eval("document.querySelector('.onboard h1').textContent")) === '先了解一下你');
    ok('BMR 提示已实时计算',
      (await cdp.eval("document.getElementById('ob-hint').innerText")).indexOf('1758') >= 0);
    ok('引导页有示例入口',
      (await cdp.eval("!!document.querySelector('[data-act=\"ob-demo\"]')")) === true);

    // ---------- 从示例进入主界面
    await cdp.eval("document.querySelector('[data-act=\"ob-demo\"]').click()");
    await sleep(600);
    ok('示例入口进入主界面', (await cdp.eval("!!document.querySelector('.tabbar')")) === true);
    ok('日历渲染 31 个本月格子',
      (await cdp.eval("document.querySelectorAll('.cell:not(.out)').length")) === 31);
    ok('力量记录渲染 2 条',
      (await cdp.eval("document.querySelectorAll('.row').length")) >= 2);

    // ---------- 切饮食 + 拆解链路
    await cdp.eval("document.querySelector('[data-v=\"diet\"]').click()");
    await sleep(200);
    ok('饮食页有 4 个餐次',
      (await cdp.eval("document.querySelectorAll('.meal-block').length")) === 4);
    const before = await cdp.eval("document.querySelectorAll('.food-row').length");

    await cdp.eval("document.querySelector('[data-meal=\"LUNCH\"]').click()");
    await sleep(300);
    ok('食物搜索弹层出现', (await cdp.eval("!!document.querySelector('.sheet')")) === true);
    await cdp.eval(`
      (function(){
        var el = document.getElementById('food-search');
        el.value = '牛肉减脂便当';
        el.dispatchEvent(new Event('input', { bubbles: true }));
      })()
    `);
    await sleep(200);
    await cdp.eval("document.querySelector('[data-act=\"estimate\"]').click()");
    await sleep(700);
    const sheetText = await cdp.eval("document.querySelector('.sheet').innerText");
    ok('拆解 5 项且含杂粮饭',
      sheetText.indexOf('杂粮饭') >= 0 &&
      (await cdp.eval("document.querySelectorAll('.sheet .row').length")) === 5);
    await cdp.eval("document.querySelector('[data-act=\"confirm-estimate\"]').click()");
    await sleep(400);
    ok('添加后记录 +1',
      (await cdp.eval("document.querySelectorAll('.food-row').length")) === before + 1);

    // ---------- 资料 stepper
    await cdp.eval("document.querySelector('[data-v=\"me\"]').click()");
    await sleep(200);
    await cdp.eval("document.querySelector('[data-act=\"open-profile\"]').click()");
    await sleep(300);
    const ageBefore = await cdp.eval("JSON.parse(localStorage.getItem('metri.v1')).profile.age");
    await cdp.eval("document.querySelector('[data-act=\"step\"][data-k=\"age\"][data-d=\"1\"]').click()");
    await sleep(200);
    const ageAfter = await cdp.eval("JSON.parse(localStorage.getItem('metri.v1')).profile.age");
    ok('资料年龄 stepper 生效', ageAfter === ageBefore + 1, ageBefore + ' → ' + ageAfter);

    // ---------- 导出内容
    const jsonLen = await cdp.eval("DB.exportJSON().length");
    ok('JSON 导出有完整内容', jsonLen > 500, jsonLen + ' chars');
    ok('JSON 里包含示例用户',
      (await cdp.eval("DB.exportJSON()")).indexOf('ivan') >= 0);
    ok('CSV 导出 5 个文件',
      (await cdp.eval("Object.keys(DB.exportCSVs()).length")) === 5);

    // ---------- 休息日标记
    await cdp.eval("document.querySelector('[data-act=\"close-sheet\"]').click()");
    await sleep(200);
    await cdp.eval("document.querySelector('[data-v=\"train\"]').click()");
    await sleep(300);
    const restBefore = await cdp.eval("document.querySelectorAll('.rest').length");
    await cdp.eval("document.querySelector('[data-act=\"toggle-rest\"]').click()");
    await sleep(300);
    const restAfter = await cdp.eval("document.querySelectorAll('.rest').length");
    ok('休息日标记生效', restAfter === restBefore + 1, restBefore + ' → ' + restAfter);

    // ---------- 换肤 + 持久化
    await cdp.eval("document.querySelector('[data-v=\"me\"]').click()");
    await sleep(200);
    await cdp.eval("document.querySelector('[data-act=\"open-theme\"]').click()");
    await sleep(300);
    await cdp.eval("document.querySelector('[data-dark=\"false\"]').click()");
    await sleep(300);
    ok('切到白天模式',
      (await cdp.eval("document.documentElement.getAttribute('data-theme')")) === 'light');
    await cdp.eval("location.reload()");
    await sleep(1500);
    ok('刷新后主题保持',
      (await cdp.eval("document.documentElement.getAttribute('data-theme')")) === 'light');
    await cdp.eval("document.querySelector('[data-v=\"diet\"]').click()");
    await sleep(300);
    ok('刷新后记录仍在',
      (await cdp.eval("document.body.innerText")).indexOf('牛肉减脂便当') >= 0);

    // ---------- 清空重来：填资料进门，应该是空白记录（无预制数据）
    await cdp.eval("localStorage.clear()");
    await cdp.eval("location.reload()");
    await sleep(1500);
    ok('清空后回到引导页', !!(await cdp.eval("document.querySelector('.onboard')")));
    await cdp.eval(`
      (function(){
        document.getElementById('ob-name').value = 'tester';
        document.getElementById('ob-age').value = '30';
        document.getElementById('ob-height').value = '180';
        document.getElementById('ob-weight').value = '80';
        document.querySelector('[data-act="ob-done"]').click();
      })()
    `);
    await sleep(800);
    ok('填完资料进入主界面', !!(await cdp.eval("document.querySelector('.tabbar')")));
    const fresh = JSON.parse(await cdp.eval("localStorage.getItem('metri.v1')"));
    ok('无预制数据：力量记录为空', fresh.strength.length === 0, '实际 ' + fresh.strength.length);
    ok('无预制数据：有氧记录为空', fresh.cardio.length === 0, '实际 ' + fresh.cardio.length);
    ok('无预制数据：饮食记录为空', fresh.meals.length === 0, '实际 ' + fresh.meals.length);
    ok('资料已保存（体重 80kg）', fresh.profile.weightKg === 80, '实际 ' + fresh.profile.weightKg);

    // 「先看示例」按钮：显式点击才灌入示例数据
    await cdp.eval("localStorage.clear()");
    await cdp.eval("location.reload()");
    await sleep(1500);
    await cdp.eval("document.querySelector('[data-act=\"ob-demo\"]').click()");
    await sleep(800);
    const demo = JSON.parse(await cdp.eval("localStorage.getItem('metri.v1')"));
    ok('示例数据：力量记录 15 条（5 个训练日）', demo.strength.length === 15, '实际 ' + demo.strength.length);
    ok('示例数据：有氧记录 3 条', demo.cardio.length === 3, '实际 ' + demo.cardio.length);
    const demoMealDays = new Set(demo.meals.map(m => m.day));
    ok('示例数据：饮食覆盖 7 天', demoMealDays.size === 7, '实际 ' + demoMealDays.size);
    ok('示例数据：休息日 2 天', demo.restDays.length === 2, '实际 ' + demo.restDays.length);

  } catch (e) {
    fail++;
    console.log('ERROR ' + e.message);
  } finally {
    try { if (cdp) await cdp.eval('1'); } catch (e) {}
    proc.kill();
    await sleep(500);
    try { require('fs').rmSync(tmp, { recursive: true, force: true }); } catch (e) {}
    console.log(`\n结果：${pass} 通过 / ${fail} 失败`);
    process.exit(fail ? 1 : 0);
  }
})();
