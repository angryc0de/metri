/* 提取 Kotlin 里所有 t("...") 的 key，与 Lang.kt 的 EN 词典比对，输出缺失清单。 */
const fs = require('fs');
const path = require('path');

const ROOT = path.join(__dirname, '..', 'app', 'src', 'main', 'java');
const langSrc = fs.readFileSync(path.join(ROOT, 'com', 'leanlog', 'app', 'Lang.kt'), 'utf8');

// Lang.kt 词典条目："中文" to "English",
const dictKeys = new Set();
const kvRe = /"((?:[^"\\]|\\.)*)"\s+to\s+"((?:[^"\\]|\\.)*)"/g;
let m2;
while ((m2 = kvRe.exec(langSrc))) dictKeys.add(m2[1].replace(/\\"/g, '"'));

function walk(dir) {
  return fs.readdirSync(dir, { withFileTypes: true }).flatMap(e => {
    const p = path.join(dir, e.name);
    return e.isDirectory() ? walk(p) : (p.endsWith('.kt') ? [p] : []);
  });
}

let missing = 0;
for (const f of walk(ROOT)) {
  const src = fs.readFileSync(f, 'utf8');
  const re = /(?<![A-Za-z0-9_.$])t\("((?:[^"\\]|\\.)*)"\)/g;
  let m;
  while ((m = re.exec(src))) {
    const key = m[1].replace(/\\"/g, '"').replace(/\\n/g, '\n');
    if (!dictKeys.has(key)) {
      console.log(`MISSING [${path.relative(ROOT, f)}] t("${key}")`);
      missing++;
    }
  }
}
console.log(`\n词典现有 ${dictKeys.size} 条，缺失 ${missing} 条`);
process.exit(missing ? 1 : 0);
