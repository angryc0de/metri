package com.leanlog.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leanlog.app.ui.theme.LL

/**
 * 自绘底部弹窗。
 * 不用 Material 的 ModalBottomSheet：它的拖拽/回弹行为和这套密集排版不搭，
 * 而且这里所有弹窗都是固定内容，不需要手势折叠。
 */
@Composable
fun SheetShell(
    title: String,
    subtitle: String? = null,
    onDismiss: () -> Unit,
    heightFraction: Float = 0.82f,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        Modifier.fillMaxSize()
            .background(LL.Scrim.copy(alpha = 0.6f))
            .clickable(indication = null, interactionSource = MutableInteractionSource()) { onDismiss() }
    ) {
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(heightFraction)
                .background(LL.Surface, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .clickable(indication = null, interactionSource = MutableInteractionSource()) { }
        ) {
            Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(width = 34.dp, height = 4.dp).background(LL.Line2, RoundedCornerShape(2.dp)))
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(title, color = LL.Text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    if (subtitle != null) {
                        Text(subtitle, color = LL.Muted, fontSize = 10.5.sp)
                    }
                }
                trailing?.invoke()
                Spacer(Modifier.size(8.dp))
                Box(
                    Modifier.size(28.dp).background(LL.Surface2, RoundedCornerShape(9.dp))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("×", color = LL.Muted, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(LL.Line))
            Column(Modifier.fillMaxWidth().weight(1f)) { content() }
        }
    }
}

/** 全屏二级页（参数选择、AI 结果等）。返回键行为干净，不会出现弹窗叠弹窗。 */
@Composable
fun FullScreenPage(
    title: String,
    subtitle: String? = null,
    onBack: () -> Unit,
    trailing: @Composable (() -> Unit)? = null,
    footer: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    // 全屏页是覆盖在 App 最外层的，不继承上面那层 Column 的 insets，
    // 标题会直接顶到状态栏时间和信号图标上，所以这里必须自己补。
    Column(Modifier.fillMaxSize().background(LL.Bg).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier.size(34.dp).background(LL.Surface2, RoundedCornerShape(11.dp))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center,
            ) { Chevron(left = true, color = LL.Text, size = 13.dp) }
            Column(Modifier.weight(1f)) {
                Text(title, color = LL.Text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                if (subtitle != null) Text(subtitle, color = LL.Muted, fontSize = 10.5.sp)
            }
            trailing?.invoke()
        }
        Column(Modifier.fillMaxWidth().weight(1f)) { content() }
        if (footer != null) {
            Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(14.dp)) { footer() }
        }
    }
}

/** 带搜索框的输入条 */
@Composable
fun SearchField(
    value: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit,
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .background(LL.Surface2, RoundedCornerShape(11.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
                color = LL.Text, fontSize = 13.sp,
            ),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(LL.Green),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(placeholder, color = LL.Dim, fontSize = 12.5.sp)
                }
                inner()
            },
        )
        if (value.isNotEmpty()) {
            Spacer(Modifier.width(8.dp))
            Text("清除", color = LL.Dim, fontSize = 10.sp, modifier = Modifier.clickable { onValueChange("") })
        }
    }
}

/** 通用数字弹窗体（年龄、身高、体重等都复用） */
@Composable
fun NumberEditor(
    value: String,
    suffix: String,
    hint: String,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Box(
                Modifier.width(130.dp).height(52.dp).background(LL.Surface2, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.foundation.text.BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = LL.Text, fontSize = 26.sp, fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    ),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(LL.Green),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                    ),
                )
            }
            Spacer(Modifier.size(8.dp))
            Text(suffix, color = LL.Muted, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(hint, color = LL.Dim, fontSize = 10.sp)
        Spacer(Modifier.height(22.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GhostButton("取消", Modifier.weight(1f)) { onCancel() }
            PrimaryButton("保存", Modifier.weight(1f)) { onConfirm() }
        }
    }
}
