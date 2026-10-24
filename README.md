# Metri · 减脂记录

砍掉繁琐打卡、只留必需的减脂记录工具：记饮食、记训练、看**热量缺口**和**蛋白质摄入**。无账号、无服务器、无上传 —— 数据完全在本地。

同一个产品，两份实现：

| 端 | 技术栈 | 位置 | 适用 |
|---|---|---|---|
| **Android** | Kotlin + Jetpack Compose + Room | `app/` | 日常长期使用，支持 AI 食物描述拆解（接入 OpenAI 兼容接口，如智谱 GLM-4-Flash 免费模型） |
| **Web** | 纯静态 HTML + CSS + 原生 JS，零依赖零构建 | `web/` | 即开即用、分享给别人，所有现代浏览器可跑 |

## 核心功能

1. **记饮食** —— 内置常见食物按份/克记录；库外的直接描述（如「牛肉减脂便当」），自动拆解成食材清单逐项核对（Web 端为本地规则估算，Android 端可接大模型）。
2. **记训练** —— 力量器械 + 有氧全部按 MET 实时计算净消耗：负重影响 MET、坡度速度影响强度，并扣除训练时段的静息消耗避免与基础代谢重复计算。
3. **看缺口** —— 日历一眼看出哪天有缺口、哪天吃超了；周汇总给出平均缺口离目标的差距和达标天数。缺口落在目标 ±100 kcal 内算达标。
4. **数据自主** —— JSON 完整备份 + 分表 CSV 导出，两端数据模型同构（日期均为 epochDay），备份可互导。
5. **中英双语** —— 两端都跟随系统语言自动切换中 / 英（也可用 `?lang=en` 强制指定 Web 端语言），全部界面、器械、食物与提示语均有英文词典覆盖。

## 目录结构

```
├── app/                Android 工程（Kotlin + Compose + Room）
│   └── src/...         计算层 LeanLogMath.kt / 数据层 Room / UI 层 Compose
├── web/                Web 版（index.html + css/ + js/）
│   └── js/math.js      计算层，与 Kotlin 版 1:1 对齐
├── tools/
│   ├── check-web.js    计算层 Node 校验（与 Android JUnit 期望值对齐）
│   ├── e2e.js          Web 端到端测试（Edge CDP 驱动）
│   ├── MakeIcon.java   自适应图标生成（字形轮廓 → vector + 各密度 PNG）
│   └── crop.py         截图裁剪小工具
├── gradle/             Gradle wrapper
└── build.gradle.kts    根构建脚本
```

## 构建与运行

### Android

```bash
# 需要 JDK 17+，Android Studio 直接打开根目录即可
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Web

零构建，任何静态服务器都行：

```bash
cd web
python -m http.server 8000
# 打开 http://localhost:8000
# 可选参数：?demo=1 载入示例数据  ?theme=light 白天主题  ?view=diet 直达某页  ?lang=en 英文界面
```

### 测试

```bash
node tools/check-web.js   # 计算层对齐校验
node tools/check-i18n.js  # 双语词条覆盖校验
node tools/e2e.js         # Web 端到端（需要 Edge，含英文模式断言）
```

## 隐私

- 所有数据存在设备本地（Android: Room；Web: localStorage），不上传任何服务器
- Android 端 AI 拆解需要用户自行在设置里填入 API Key，请求直连所填服务，不经过任何中间方
- 签名密钥（`keystore/`）不随仓库分发

## License

MIT
