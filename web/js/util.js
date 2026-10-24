/* 格式化与小组件，安卓版 Format.kt 的对应物。 */
(function (g) {
  'use strict';

  var WEEK_CN = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
  var WEEK_EN = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
  var MON_EN = ['', 'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

  function isEn() { return g.LANG === 'en'; }

  function fmtNum(v) {
    if (v == null) return '—';
    var n = Number(v);
    if (Math.abs(n - Math.round(n)) < 0.05) return String(Math.round(n));
    return n.toFixed(1);
  }

  /** 中文 "10 月 23 日 周五" / 英文 "Fri, Oct 23" */
  function dayTitle(day) {
    var o = DB.fromEpochDay(day);
    if (isEn()) return WEEK_EN[o.dow] + ', ' + MON_EN[o.m] + ' ' + o.d;
    return o.m + ' 月 ' + o.d + ' 日 ' + WEEK_CN[o.dow];
  }

  /** "今天 / 昨天 / 10 月 20 日" */
  function dayShort(day) {
    var t = DB.today();
    if (day === t) return isEn() ? 'Today' : '今天';
    if (day === t - 1) return isEn() ? 'Yesterday' : '昨天';
    var o = DB.fromEpochDay(day);
    if (isEn()) return MON_EN[o.m] + ' ' + o.d;
    return o.m + ' 月 ' + o.d + ' 日';
  }

  /** "2026 年 10 月" / "October 2026" */
  function monthLabel(y, m) {
    if (isEn()) return MON_EN[m] + ' ' + y;
    return y + ' 年 ' + m + ' 月';
  }

  function esc(s) {
    return String(s == null ? '' : s)
      .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }

  function chev(dir) {
    var d = {
      left: 'M7.5 2L3.5 6l4 4',
      right: 'M4.5 2L8.5 6l-4 4',
      up: 'M2 7.6L6 3.6l4 4',
      down: 'M2 4.4L6 8.4l4-4'
    }[dir];
    return '<svg class="chev" viewBox="0 0 12 12"><path d="' + d + '"/></svg>';
  }

  function dot(kind) {
    return '<i class="dot ' + kind + '"></i>';
  }

  /** 触发一次下载，不需要服务器。 */
  function download(name, text, mime) {
    var blob = new Blob(['\ufeff' + text], { type: (mime || 'text/plain') + ';charset=utf-8' });
    var url = URL.createObjectURL(blob);
    var a = document.createElement('a');
    a.href = url;
    a.download = name;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    setTimeout(function () { URL.revokeObjectURL(url); }, 2000);
  }

  g.U = {
    WEEK_CN: WEEK_CN,
    fmtNum: fmtNum,
    dayTitle: dayTitle,
    dayShort: dayShort,
    monthLabel: monthLabel,
    esc: esc,
    chev: chev,
    dot: dot,
    download: download
  };
})(window);
