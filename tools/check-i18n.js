/* 静态校验：所有 T('...') 的 key 必须在 i18n.js 的 EN 词典里，防止英文模式漏翻。 */
const fs = require('fs');
const path = require('path');

const dir = path.join(__dirname, '..', 'web', 'js');
const dictSrc = fs.readFileSync(path.join(dir, 'i18n.js'), 'utf8');

// 从 i18n.js 提取词典 key（形如 'xxx': 'yyy'）
const dictKeys = new Set();
const kvRe = /'((?:[^'\\]|\\.)*)'\s*:\s*'((?:[^'\\]|\\.)*)'/g;
let m2;
while ((m2 = kvRe.exec(dictSrc))) dictKeys.add(m2[1].replace(/\\'/g, "'"));

let missing = 0;
for (const f of ['app.js', 'sheets.js', 'store.js', 'util.js', 'estimator.js', 'seed.js', 'math.js']) {
  const src = fs.readFileSync(path.join(dir, f), 'utf8');
  const re = /\bT\('((?:[^'\\]|\\.)*)'\)/g;
  let m;
  while ((m = re.exec(src))) {
    const key = m[1].replace(/\\'/g, "'");
    if (!dictKeys.has(key)) {
      console.log(`MISSING [${f}] T('${key}')`);
      missing++;
    }
  }
}
console.log(missing === 0 ? 'OK：所有 T() key 都在词典里' : `共 ${missing} 个缺失`);
process.exit(missing ? 1 : 0);
