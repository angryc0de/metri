package com.leanlog.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leanlog.app.core.CalendarStatus
import com.leanlog.app.t
import com.leanlog.app.ui.DayCell
import com.leanlog.app.ui.theme.LL
import java.time.LocalDate
import java.time.YearMonth

/**
 * 月历。信息密度是这块最难的地方 —— 一个小格子里要同时表达：
 * 日期、热量状态、力量/有氧标记、休息、缺口数值。
 *
 * 取舍（和原型一致）：
 * - 状态用**日期数字颜色**表达，不用整格填色，避免一片红绿看着有压力；
 * - 训练类型用**实心圆点**（蓝=力量、黄=有氧），一眼扫过去就能看出这几天练了什么；
 * - 休息日写紫色斜体「休」；没记录的日子只把数字变灰 —— 「休」和「没记」是两回事。
 *
 * 布局：整块日历不再是页面顶部的固定区块，而是直接铺进页面那个 LazyColumn。
 * 这样上滑时日历会跟着走，只有**本周那一行**吸顶 —— 既省出记录的空间，
 * 又保证滚到哪里都能知道今天是这周的哪天。
 */
@OptIn(ExperimentalFoundationApi::class)
fun LazyListScope.calendarSection(
    month: YearMonth,
    cells: List<DayCell>,
    selectedDay: Int,
    heatWarn: Boolean,
    collapsed: Boolean,
    listState: LazyListState,
    onSelect: (Int) -> Unit,
    onShiftMonth: (Long) -> Unit,
    onToggleCollapse: () -> Unit,
) {
    val today = LocalDate.now().toEpochDay().toInt()
    val weeks = cells.chunked(7).filter { it.isNotEmpty() }
    // 日历上方固定两个 item（月份条、星期表头），周行从下标 2 开始
    val weekStartIndex = 2

    item(key = "cal-header") {
        CalendarHeader(month, collapsed, onShiftMonth, onToggleCollapse)
    }
    item(key = "cal-weekday") { WeekdayHeader() }

    if (collapsed) {
        // 折叠态只有一行，吸顶反而会一直占着顶部，所以普通 item 即可
        val row = weeks.firstOrNull { run -> run.any { it.day == selectedDay } } ?: weeks.firstOrNull()
        if (row != null) {
            item(key = "cal-week") {
                WeekRow(row, selectedDay, today, heatWarn, pinned = false, itemIndex = -1, listState = listState, onSelect = onSelect)
            }
        }
    } else {
        val pinned = pinnedWeekIndex(weeks, selectedDay)
        weeks.forEachIndexed { index, row ->
            if (index == pinned) {
                stickyHeader(key = "cal-week-$index") {
                    WeekRow(
                        row, selectedDay, today, heatWarn,
                        pinned = true,
                        itemIndex = weekStartIndex + index,
                        listState = listState,
                        onSelect = onSelect,
                    )
                }
            } else {
                item(key = "cal-week-$index") {
                    WeekRow(row, selectedDay, today, heatWarn, pinned = false, itemIndex = -1, listState = listState, onSelect = onSelect)
                }
            }
        }
    }

    item(key = "cal-legend") { CalendarLegend(heatWarn, collapsed) }
}

/**
 * 吸顶的是**选中日期所在的那一周**，不是「今天所在的那一周」。
 * 用户点哪天就是在看哪天，滚动时留住那一行才不会丢掉上下文。
 * 选中日期理论上一定落在当前月份里（翻月时 ViewModel 会把它挪进来），
 * 万一没有就退化成第一周，保证始终有一行能吸住。
 */
private fun pinnedWeekIndex(weeks: List<List<DayCell>>, selectedDay: Int): Int =
    weeks.indexOfFirst { row -> row.any { it.day == selectedDay } }.coerceAtLeast(0)

@Composable
private fun CalendarHeader(
    month: YearMonth,
    collapsed: Boolean,
    onShiftMonth: (Long) -> Unit,
    onToggleCollapse: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 翻月按钮贴着年月左右站 —— 读者一眼就知道这两个箭头操作的是中间这个月份，
        // 而不是整条日历。原先挤在右侧，还要靠"今天"按钮隔开，很费解。
        CalendarIconButton(onClick = { onShiftMonth(-1) }) {
            Chevron(left = true, color = LL.Muted, size = 12.dp)
        }
        Spacer(Modifier.size(8.dp))
        // 给月份留个最小宽度：9 月和 12 月的字数不一样，不兜住的话
        // 右边那个「›」每次翻月都会左右跳一下，手感很毛躁。
        Text(
            "${month.year} ${t("年")} ${month.monthValue} ${t("月")}",
            modifier = Modifier.widthIn(min = 102.dp),
            color = LL.Text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.size(8.dp))
        CalendarIconButton(onClick = { onShiftMonth(1) }) {
            Chevron(left = false, color = LL.Muted, size = 12.dp)
        }
        Spacer(Modifier.weight(1f))
        // 折叠按钮画的是"下一步会发生什么"：展开时向上收，收起时向下放。
        CalendarIconButton(onClick = { onToggleCollapse() }) {
            Chevron(up = !collapsed, down = collapsed, color = LL.Muted, size = 12.dp)
        }
    }
}

@Composable
private fun CalendarIconButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier.size(26.dp).background(LL.Surface2, RoundedCornerShape(8.dp)).clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun WeekdayHeader() {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 0.dp).padding(bottom = 4.dp)) {
        listOf(t("一"), t("二"), t("三"), t("四"), t("五"), t("六"), t("日")).forEach { label ->
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(label, color = LL.Dim, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun WeekRow(
    row: List<DayCell>,
    selectedDay: Int,
    today: Int,
    heatWarn: Boolean,
    pinned: Boolean,
    itemIndex: Int,
    listState: LazyListState,
    onSelect: (Int) -> Unit,
) {
    // 只有这一行真的被顶到列表顶部才需要背景和分隔线，
    // 停在自然位置时什么都不画 —— 否则日历中间会凭空多出一条横线。
    // 判据：该 item 在可视信息里的 offset 归零，就是被 pin 在顶边了。
    val stuck = pinned && itemIndex >= 0 &&
        listState.layoutInfo.visibleItemsInfo
            .firstOrNull { it.index == itemIndex }
            ?.let { it.offset <= 0 } == true

    // 吸顶时用页面底色把下面滚过的内容盖住 —— 不用比页面浅一档的 Surface2。
    // 那一档底色会把这一行画成一条"灰条"，看着像被选中或被禁用；
    // 保持原底色，吸顶的效果只体现在"它没跟着走"这件事本身。
    Column(
        Modifier.fillMaxWidth()
            .background(if (stuck) LL.Bg else Color.Transparent)
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            row.forEach { cell ->
                DayCellView(
                    cell = cell,
                    selected = cell.day == selectedDay,
                    isToday = cell.day == today,
                    heatWarn = heatWarn,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelect(cell.day) },
                )
            }
        }
        // 吸顶时下方是滚过去的记录，要留一点呼吸空间：
        // 紧贴的话这一行看起来像是被内容压住了，而不是"浮在上面"。
        if (stuck) {
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun CalendarLegend(heatWarn: Boolean, collapsed: Boolean) {
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp, bottom = if (collapsed) 2.dp else 0.dp, start = 16.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        LegendItem(LL.StrengthDot, t("力量"))
        LegendItem(LL.CardioDot, t("有氧"))
        Text(
            t("休"), color = LL.Purple, fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold, fontStyle = FontStyle.Italic,
        )
        Spacer(Modifier.weight(1f))
        if (heatWarn) {
            Text(t("数字颜色 = 热量缺口"), color = LL.Dim, fontSize = 9.sp)
        } else {
            Text(t("缺口警示已关闭"), color = LL.Dim, fontSize = 9.sp)
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Dot(color, 6.dp)
        Spacer(Modifier.size(4.dp))
        Text(label, color = LL.Dim, fontSize = 9.sp)
    }
}

@Composable
private fun DayCellView(
    cell: DayCell,
    selected: Boolean,
    isToday: Boolean,
    heatWarn: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val future = cell.status == CalendarStatus.FUTURE
    val dayNumColor = when (cell.status) {
        CalendarStatus.DEFICIT -> LL.Green
        CalendarStatus.SURPLUS -> LL.Red
        CalendarStatus.EVEN -> LL.Muted
        CalendarStatus.PLAIN -> LL.Text
        CalendarStatus.EMPTY, CalendarStatus.TRAINED_ONLY -> LL.Faint
        CalendarStatus.FUTURE -> LL.Future
    }
    val borderColor = when {
        // 上月/下月补位的格子只占位，不画框 —— 否则 5 行的月份底下那排空框会像没渲染完
        !cell.inMonth -> Color.Transparent
        selected -> LL.Text
        isToday -> LL.Text.copy(alpha = 0.5f)
        cell.status == CalendarStatus.DEFICIT -> LL.Green.copy(alpha = 0.30f)
        cell.status == CalendarStatus.SURPLUS -> LL.Red.copy(alpha = 0.30f)
        future -> LL.Hair.copy(alpha = 0.03f)
        else -> LL.Hair.copy(alpha = 0.06f)
    }
    val bg = when {
        !cell.inMonth -> Color.Transparent
        selected -> LL.Surface3
        future -> Color.Transparent
        else -> LL.Hair.copy(alpha = 0.018f)
    }
    val showRest = cell.status != CalendarStatus.FUTURE &&
        cell.status != CalendarStatus.EMPTY &&
        !cell.hasStrength && !cell.hasCardio

    Column(
        modifier
            .padding(2.dp)
            .height(52.dp)
            .background(bg, RoundedCornerShape(11.dp))
            .border(1.dp, borderColor, RoundedCornerShape(11.dp))
            .clickable(enabled = !future && cell.inMonth) { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(2.dp))
        // 日期数字、标记位、缺口数字三段都装在一个 52dp 的格子里。
        // 之前用裸 Text 排列，行高（约字号的 1.3 倍）会把最后一段挤出格子底部 ——
        // 「休」和缺口数字同时出现时，数字只露出顶端几个像素。改成固定高度的
        // Box + 收紧 lineHeight，把三段的占位钉死，flexible 空间全留给中间那条缝。
        Box(Modifier.height(18.dp), contentAlignment = Alignment.Center) {
            Text(
                if (cell.inMonth) LocalDate.ofEpochDay(cell.day.toLong()).dayOfMonth.toString() else "",
                color = if (cell.inMonth) dayNumColor else dayNumColor.copy(alpha = 0.3f),
                fontSize = 13.5.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Box(Modifier.height(12.dp), contentAlignment = Alignment.Center) {
            if (showRest) {
                Text(
                    t("休"),
                    color = LL.Purple,
                    fontSize = 11.sp,
                    lineHeight = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontStyle = FontStyle.Italic,
                )
            } else if (cell.hasStrength || cell.hasCardio) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (cell.hasStrength) Dot(LL.StrengthDot, 8.dp)
                    if (cell.hasCardio) Dot(LL.CardioDot, 8.dp)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        val showSub = heatWarn && (
            cell.status == CalendarStatus.DEFICIT ||
                cell.status == CalendarStatus.SURPLUS ||
                cell.status == CalendarStatus.EVEN
            )
        if (showSub) {
            val subColor = when (cell.status) {
                CalendarStatus.DEFICIT -> LL.Green
                CalendarStatus.SURPLUS -> LL.Red
                else -> LL.Muted
            }
            // 缺口本身已经隐含负号，不再画蛇添足加 "-"；超出的 "+" 保留，
            // 因为红绿在色弱眼里分不出来，需要一个字面上的正负来兜底。
            Text(
                when {
                    cell.deficit > 0 -> "${cell.deficit}"
                    cell.deficit < 0 -> "+${-cell.deficit}"
                    else -> "0"
                },
                color = subColor.copy(alpha = 0.9f),
                fontSize = 9.sp,
                lineHeight = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 3.dp),
            )
        } else {
            Spacer(Modifier.height(3.dp))
        }
    }
}
