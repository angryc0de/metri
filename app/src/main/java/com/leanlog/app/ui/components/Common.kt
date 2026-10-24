package com.leanlog.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leanlog.app.ui.theme.LL

/** 通用卡片。整个界面的层级都靠它，避免每个页面各写一套背景。 */
@Composable
fun LCard(
    modifier: Modifier = Modifier,
    padding: Dp = 14.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(LL.Surface, RoundedCornerShape(16.dp))
            .padding(padding),
        content = content,
    )
}

@Composable
fun SectionTitle(text: String, trailing: @Composable (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, color = LL.Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
fun Pill(text: String, color: Color, bg: Color = color.copy(alpha = 0.14f)) {
    Box(
        Modifier
            .background(bg, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun Dot(color: Color, size: Dp = 8.dp) {
    Box(Modifier.size(size).background(color, CircleShape))
}

/** 开关。不用 Material 的 Switch —— 尺寸太大，和这套紧凑排版不搭。 */
@Composable
fun ToggleSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    Box(
        Modifier
            .size(width = 42.dp, height = 24.dp)
            .background(if (checked) LL.Green else LL.Surface3, RoundedCornerShape(12.dp))
            .clickable { onChange(!checked) },
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .padding(horizontal = 3.dp)
                .size(18.dp)
                .background(if (checked) LL.OnAccent else LL.Muted, CircleShape)
        )
    }
}

@Composable
fun PrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = LL.Green,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .height(48.dp)
            .background(if (enabled) color else LL.Surface3, RoundedCornerShape(13.dp))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (enabled) LL.OnAccent else LL.Dim,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun GhostButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(48.dp)
            .background(LL.Surface3, RoundedCornerShape(13.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = LL.Text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

/**
 * 滚轮选择器。
 *
 * 实现要点：给 LazyColumn 上下各留半屏 padding，让第一项天然居中；
 * 配合 snap 手势，停下来的位置就是选中项。这样不需要自己算偏移，
 * 也不会出现「手指松开了但数字停在两行之间」的情况。
 */
@Composable
fun WheelPicker(
    items: List<String>,
    index: Int,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 40.dp,
    onIndexChange: (Int) -> Unit,
) {
    if (items.isEmpty()) return
    val visible = 3
    val state = rememberLazyListState(initialFirstVisibleItemIndex = index.coerceIn(0, items.lastIndex))
    val fling = rememberSnapFlingBehavior(lazyListState = state)

    // 吃掉四成初速度。默认摩擦下一划能飞出去七八格，压低惯性之后
    // 同样的手势只走三四格，停在想要的数字上不用来回微调。
    // 只影响松手后的惯性（onPreFling），拖拽时的手感不受影响。
    val slowDown = remember {
        object : NestedScrollConnection {
            override suspend fun onPreFling(available: Velocity): Velocity =
                available.copy(y = available.y * 0.4f)
        }
    }
    val pad = itemHeight * (visible / 2)

    LaunchedEffect(index) {
        if (state.firstVisibleItemIndex != index) {
            state.animateScrollToItem(index.coerceIn(0, items.lastIndex))
        }
    }
    LaunchedEffect(state) {
        snapshotFlow { state.isScrollInProgress to state.firstVisibleItemIndex }
            .collect { (scrolling, idx) ->
                if (!scrolling) onIndexChange(idx.coerceIn(0, items.lastIndex))
            }
    }

    Box(modifier.height(itemHeight * visible).nestedScroll(slowDown)) {
        // 固定居中的选中窗口：它不动、文字在里面滑过。
        // 有这条底之后"停在哪个数"一眼可见，滚动过程中的顿挫感也弱得多。
        Box(
            Modifier.align(Alignment.Center).fillMaxWidth().height(itemHeight)
                .background(LL.Surface3.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
        )
        LazyColumn(
            state = state,
            flingBehavior = fling,
            contentPadding = PaddingValues(vertical = pad),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            itemsIndexed(items) { i, label ->
                val selected = state.firstVisibleItemIndex == i
                Box(
                    Modifier.height(itemHeight).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        color = if (selected) LL.Text else LL.Dim,
                        fontSize = if (selected) 17.sp else 13.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        // 中间选中带的上下分隔，制造"窗口"感
        Column(Modifier.align(Alignment.Center).fillMaxWidth().height(itemHeight)) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(LL.Line))
            Spacer(Modifier.weight(1f))
            Box(Modifier.fillMaxWidth().height(1.dp).background(LL.Line))
        }
    }
}

/** 页面顶栏：返回、标题、右侧操作。 */
@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (onBack != null) {
            Box(
                Modifier.size(34.dp).background(LL.Surface2, CircleShape).clickable { onBack() },
                contentAlignment = Alignment.Center,
            ) {
                Chevron(left = true)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = LL.Text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            if (subtitle != null) {
                Text(subtitle, color = LL.Muted, fontSize = 11.sp)
            }
        }
        trailing?.invoke()
    }
}

/** 细线箭头，避免引 material-icons 依赖。left/up/down 都不给时画右箭头。 */
@Composable
fun Chevron(
    left: Boolean = false,
    up: Boolean = false,
    down: Boolean = false,
    color: Color = LL.Text,
    size: Dp = 14.dp,
) {
    androidx.compose.foundation.Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(
            width = w * 0.14f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
            join = androidx.compose.ui.graphics.StrokeJoin.Round,
        )
        val path = androidx.compose.ui.graphics.Path()
        when {
            left -> {
                path.moveTo(w * 0.66f, h * 0.16f)
                path.lineTo(w * 0.33f, h * 0.5f)
                path.lineTo(w * 0.66f, h * 0.84f)
            }
            up -> {
                path.moveTo(w * 0.16f, h * 0.66f)
                path.lineTo(w * 0.5f, h * 0.33f)
                path.lineTo(w * 0.84f, h * 0.66f)
            }
            down -> {
                path.moveTo(w * 0.16f, h * 0.34f)
                path.lineTo(w * 0.5f, h * 0.67f)
                path.lineTo(w * 0.84f, h * 0.34f)
            }
            else -> {
                path.moveTo(w * 0.34f, h * 0.16f)
                path.lineTo(w * 0.67f, h * 0.5f)
                path.lineTo(w * 0.34f, h * 0.84f)
            }
        }
        drawPath(path, color, style = stroke)
    }
}

@Composable
fun EmptyHint(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxWidth().background(LL.Surface, RoundedCornerShape(14.dp)).padding(18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = LL.Dim, fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun VDivider(color: Color = LL.Line) {
    Box(Modifier.width(1.dp).height(14.dp).background(color))
}
