package com.leanlog.app.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leanlog.app.core.SeedData
import com.leanlog.app.data.SettingsStore
import com.leanlog.app.ui.AppViewModel
import com.leanlog.app.ui.ProfileField
import com.leanlog.app.ui.components.Dot
import com.leanlog.app.ui.components.GhostButton
import com.leanlog.app.ui.components.PrimaryButton
import com.leanlog.app.ui.components.SheetShell
import com.leanlog.app.ui.components.WheelPicker
import com.leanlog.app.ui.fmtNum
import com.leanlog.app.ui.theme.LL
import com.leanlog.app.ui.theme.Palette

// ------------------------------------------------------------------ 蛋白目标

@Composable
fun ProteinSheet(vm: AppViewModel, onDismiss: () -> Unit) {
    val profile = vm.state.value.profile
    val weight = profile.weightKg
    var grams by remember {
        mutableStateOf(
            if (profile.proteinAuto) Math.round(weight * 1.8).toInt() else profile.proteinCustomG
        )
    }
    val options = remember { (8..52).map { it * 5 } } // 40–260g，步长 5
    val idx = options.indexOfFirst { it >= grams }.let { if (it < 0) options.lastIndex else it }
    val autoValue = Math.round(weight * 1.8).toInt()

    SheetShell(
        title = "每日蛋白目标",
        subtitle = "只影响蛋白进度条，不参与热量缺口计算",
        onDismiss = onDismiss,
        heightFraction = 0.68f,
    ) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("$grams", color = LL.Text, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text(" g / 天", color = LL.Dim, fontSize = 12.sp)
                Spacer(Modifier.weight(1f))
                Text(
                    if (profile.proteinAuto) "当前：自动" else "当前：自定义",
                    color = if (profile.proteinAuto) LL.Muted else LL.Amber,
                    fontSize = 10.5.sp,
                )
            }

            Spacer(Modifier.height(8.dp))
            Text("按体重折算", color = LL.Dim, fontSize = 9.5.sp)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1.2 to "维持", 1.6 to "减脂", 2.0 to "高强度").forEach { (factor, label) ->
                    val target = Math.round(weight * factor).toInt()
                    val on = grams == target
                    Box(
                        Modifier.background(
                            if (on) LL.Blue.copy(alpha = 0.18f) else LL.Surface2,
                            RoundedCornerShape(9.dp),
                        ).clickable { grams = target }.padding(horizontal = 11.dp, vertical = 7.dp),
                    ) {
                        Column {
                            Text(
                                "${fmtNum(factor)} g/kg",
                                color = if (on) LL.Blue else LL.Muted,
                                fontSize = 11.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                            )
                            Text("$label · $target g", color = LL.Dim, fontSize = 8.5.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            WheelPicker(
                items = options.map { "$it g" },
                index = idx,
                modifier = Modifier.fillMaxWidth(),
                itemHeight = 34.dp,
            ) { i -> grams = options[i] }

            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GhostButton("取消", Modifier.weight(1f)) { onDismiss() }
                PrimaryButton("保存 $grams g", Modifier.weight(1.4f)) {
                    vm.setProteinTarget(grams, auto = false)
                }
            }
            if (!profile.proteinAuto) {
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier.fillMaxWidth().clickable { vm.setProteinTarget(autoValue, auto = true) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "恢复按体重自动计算（$autoValue g）",
                        color = LL.Muted, fontSize = 11.sp,
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

// ------------------------------------------------------------------ 个人信息

@Composable
fun ProfileEditSheet(vm: AppViewModel, field: ProfileField, onDismiss: () -> Unit) {
    val p = vm.state.value.profile
    val (title, initial, suffix, hint, isNumber) = when (field) {
        ProfileField.NAME -> Quint("姓名", p.name, "", "用于页面问候，可留空", false)
        ProfileField.AGE -> Quint("年龄", p.age.toString(), "岁", "BMR 公式需要年龄，10–100", true)
        ProfileField.HEIGHT -> Quint("身高", fmtNum(p.heightCm), "cm", "BMR 公式需要身高，120–230", true)
        ProfileField.WEIGHT -> Quint("今天体重", fmtNum(p.weightKg), "kg", "记录后会写入体重曲线", true)
        ProfileField.TARGET_WEIGHT -> Quint("目标体重", fmtNum(p.targetWeightKg), "kg", "减脂目标，仅用于展示", true)
        else -> Quint("", "", "", "", true)
    }
    var text by remember { mutableStateOf(initial) }

    SheetShell(title = title, onDismiss = onDismiss, heightFraction = 0.5f) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth().background(LL.Surface2, RoundedCornerShape(13.dp)).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = { v ->
                        text = if (isNumber) v.filter { it.isDigit() || it == '.' } else v
                    },
                    singleLine = true,
                    textStyle = TextStyle(color = LL.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold),
                    cursorBrush = SolidColor(LL.Green),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (isNumber) KeyboardType.Decimal else KeyboardType.Text
                    ),
                    modifier = Modifier.weight(1f),
                )
                if (suffix.isNotEmpty()) Text(suffix, color = LL.Muted, fontSize = 12.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text(hint, color = LL.Dim, fontSize = 10.sp)
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GhostButton("取消", Modifier.weight(1f)) { onDismiss() }
                PrimaryButton("保存", Modifier.weight(1f)) { vm.saveProfile(field, text) }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

private data class Quint(
    val title: String,
    val initial: String,
    val suffix: String,
    val hint: String,
    val isNumber: Boolean,
)

@Composable
fun GenderSheet(vm: AppViewModel, onDismiss: () -> Unit) {
    val current = vm.state.value.profile.gender
    SheetShell(title = "性别", subtitle = "BMR 公式的常数项不同", onDismiss = onDismiss, heightFraction = 0.36f) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("MALE" to "男", "FEMALE" to "女").forEach { (value, label) ->
                val on = current == value
                Row(
                    Modifier.fillMaxWidth()
                        .background(if (on) LL.Green.copy(alpha = 0.14f) else LL.Surface2, RoundedCornerShape(12.dp))
                        .clickable { vm.saveProfile(ProfileField.GENDER, value) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(label, color = if (on) LL.Green else LL.Text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.weight(1f))
                    if (on) Text("✓", color = LL.Green, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ActivitySheet(vm: AppViewModel, onDismiss: () -> Unit) {
    val current = vm.state.value.profile.activityFactor
    SheetShell(
        title = "日常活动量",
        subtitle = "只描述训练之外的日常，训练消耗单独计算",
        onDismiss = onDismiss,
        heightFraction = 0.6f,
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            SeedData.activityLevels.forEach { level ->
                val on = kotlin.math.abs(current - level.factor) < 0.01
                Column(
                    Modifier.fillMaxWidth()
                        .background(if (on) LL.Green.copy(alpha = 0.14f) else LL.Surface2, RoundedCornerShape(12.dp))
                        .clickable { vm.saveProfile(ProfileField.ACTIVITY, level.factor.toString()) }
                        .padding(13.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${level.label} · ${fmtNum(level.factor)}",
                            color = if (on) LL.Green else LL.Text,
                            fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.weight(1f))
                        if (on) Text("✓", color = LL.Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(level.desc, color = LL.Dim, fontSize = 9.5.sp)
                }
            }
        }
    }
}

@Composable
fun RateSheet(vm: AppViewModel, onDismiss: () -> Unit) {
    val current = vm.state.value.profile.weeklyRateKg
    SheetShell(
        title = "每周减重",
        subtitle = "目标缺口 = 每周减重 × 7700 / 7",
        onDismiss = onDismiss,
        heightFraction = 0.45f,
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            listOf(0.25, 0.5, 0.75, 1.0).forEach { rate ->
                val on = kotlin.math.abs(current - rate) < 0.01
                Row(
                    Modifier.fillMaxWidth()
                        .background(if (on) LL.Green.copy(alpha = 0.14f) else LL.Surface2, RoundedCornerShape(12.dp))
                        .clickable { vm.saveProfile(ProfileField.RATE, rate.toString()) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            "${fmtNum(rate)} kg / 周",
                            color = if (on) LL.Green else LL.Text,
                            fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "每天目标缺口 ${Math.round(rate * 7700 / 7)} kcal",
                            color = LL.Dim, fontSize = 9.5.sp,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    if (on) Text("✓", color = LL.Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text(
                "每周超过 1kg 的减重速度很难只掉脂肪，通常会带走肌肉。",
                color = LL.Dim, fontSize = 9.5.sp,
            )
        }
    }
}

// ------------------------------------------------------------------ AI 配置

@Composable
fun AiConfigSheet(vm: AppViewModel, onDismiss: () -> Unit) {
    val snapshot = remember { vm.aiSettingsSnapshot() }
    var baseUrl by remember { mutableStateOf(snapshot[0]) }
    var apiKey by remember { mutableStateOf(snapshot[1]) }
    var model by remember { mutableStateOf(snapshot[2]) }
    var visionModel by remember { mutableStateOf(snapshot[3]) }
    var showKey by remember { mutableStateOf(false) }

    SheetShell(
        title = "AI 模型",
        subtitle = "OpenAI 兼容接口，Key 只存在本机",
        onDismiss = onDismiss,
        heightFraction = 0.86f,
    ) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            FieldLabel("Base URL")
            Field(baseUrl, "https://open.bigmodel.cn/api/paas/v4") { baseUrl = it }
            Spacer(Modifier.height(12.dp))

            FieldLabel("API Key")
            Row(
                Modifier.fillMaxWidth().background(LL.Surface2, RoundedCornerShape(11.dp)).padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it.trim() },
                    singleLine = true,
                    visualTransformation = if (showKey) androidx.compose.ui.text.input.VisualTransformation.None
                    else PasswordVisualTransformation(),
                    textStyle = TextStyle(color = LL.Text, fontSize = 12.5.sp),
                    cursorBrush = SolidColor(LL.Green),
                    modifier = Modifier.weight(1f),
                )
                Text(
                    if (showKey) "隐藏" else "显示",
                    color = LL.Dim, fontSize = 10.sp,
                    modifier = Modifier.clickable { showKey = !showKey },
                )
            }
            Spacer(Modifier.height(12.dp))

            FieldLabel("文本模型")
            Field(model, "glm-4-flash") { model = it }
            Spacer(Modifier.height(12.dp))

            FieldLabel("图片识别模型（可留空）")
            Field(visionModel, "glm-4v-flash") { visionModel = it }

            Spacer(Modifier.height(14.dp))
            Column(Modifier.fillMaxWidth().background(LL.Surface2, RoundedCornerShape(11.dp)).padding(12.dp)) {
                Text("为什么必须自己填 Key", color = LL.Muted, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(5.dp))
                Text(
                    "Key 打进 APK 会被反编译提取，所以这个 App 不内置任何 Key。" +
                        "填自己的 Key 后，热量估算和拍照识别才会真正联网；" +
                        "不填也能用 —— 会走本地规则估算，只是精度低一些。",
                    color = LL.Dim, fontSize = 9.5.sp, lineHeight = 15.sp,
                )
            }

            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GhostButton("取消", Modifier.weight(1f)) { onDismiss() }
                PrimaryButton("保存", Modifier.weight(1.4f)) {
                    vm.saveAiConfig(baseUrl, apiKey, model, visionModel)
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, color = LL.Muted, fontSize = 9.5.sp, modifier = Modifier.padding(bottom = 5.dp))
}

@Composable
private fun Field(value: String, placeholder: String, onChange: (String) -> Unit) {
    Box(
        Modifier.fillMaxWidth().background(LL.Surface2, RoundedCornerShape(11.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(color = LL.Text, fontSize = 12.5.sp),
            cursorBrush = SolidColor(LL.Green),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                if (value.isEmpty()) Text(placeholder, color = LL.Dim, fontSize = 12.sp)
                inner()
            },
        )
    }
}

// ------------------------------------------------------------------ 导出

@Composable
fun ExportSheet(vm: AppViewModel, onDismiss: () -> Unit) {
    val state = vm.state.value
    SheetShell(
        title = "数据导出",
        subtitle = "备份能恢复，CSV 能拿去分析",
        onDismiss = onDismiss,
        heightFraction = 0.62f,
    ) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(8.dp))
            ExportRow("JSON 全量备份", "个人资料、体重、饮食、力量、有氧，可用它恢复")
            ExportRow("meals.csv", "每次进食一条，含克数与营养")
            ExportRow("strength.csv", "每组力量训练与净消耗")
            ExportRow("cardio.csv", "有氧参数、MET 与净消耗")
            ExportRow("weight.csv", "体重记录")
            ExportRow("daily.csv", "按天汇总的摄入、消耗与缺口，可直接做透视表")

            Spacer(Modifier.height(14.dp))
            Column(Modifier.fillMaxWidth().background(LL.Surface2, RoundedCornerShape(11.dp)).padding(12.dp)) {
                Text(
                    "文件会同时写入系统「下载 / LeanLog」目录，并保留一份在应用私有目录里。",
                    color = LL.Dim, fontSize = 9.5.sp, lineHeight = 15.sp,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "目前共 ${state.detail.meals.size} 条饮食（当天）· 体重 ${state.weights.size} 条记录",
                    color = LL.Dim, fontSize = 9.5.sp,
                )
            }

            Spacer(Modifier.height(16.dp))
            PrimaryButton("导出全部格式", Modifier.fillMaxWidth()) { vm.exportData() }
            Spacer(Modifier.height(20.dp))
        }
    }
}

// ------------------------------------------------------------------ 外观

@Composable
fun ThemeSheet(vm: AppViewModel, onDismiss: () -> Unit) {
    val dark = vm.state.value.darkTheme
    SheetShell(
        title = "外观",
        subtitle = "换肤立即生效，默认黑夜模式",
        onDismiss = onDismiss,
        heightFraction = 0.46f,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ThemeCard("黑夜", dark, LL.Dark, Modifier.weight(1f)) { vm.setTheme(true) }
            ThemeCard("白天", !dark, LL.Light, Modifier.weight(1f)) { vm.setTheme(false) }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "两套皮肤只交换底色和文字，绿=缺口、红=超出、蓝=力量、黄=有氧的语义不变，换皮不改变任何判断。",
            color = LL.Dim, fontSize = 9.5.sp, lineHeight = 15.sp,
            modifier = Modifier.padding(horizontal = 18.dp),
        )
    }
}

@Composable
private fun ThemeCard(label: String, selected: Boolean, palette: Palette, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .height(158.dp)
            .background(palette.bg, RoundedCornerShape(16.dp))
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) LL.Green else palette.line,
                shape = RoundedCornerShape(16.dp),
            )
            .clickable { onClick() }
            .padding(12.dp),
    ) {
        // 迷你预览：标题条 + 副标题 + 一张卡片 + 三个语义点
        Box(Modifier.fillMaxWidth(0.72f).height(7.dp).background(palette.text, RoundedCornerShape(4.dp)))
        Spacer(Modifier.height(5.dp))
        Box(Modifier.fillMaxWidth(0.44f).height(6.dp).background(palette.dim, RoundedCornerShape(3.dp)))
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().height(30.dp).background(palette.surface, RoundedCornerShape(9.dp)))
        Spacer(Modifier.height(9.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Dot(palette.blue, 7.dp)
            Spacer(Modifier.width(5.dp))
            Dot(palette.amber, 7.dp)
            Spacer(Modifier.weight(1f))
            Dot(palette.green, 7.dp)
        }
        Spacer(Modifier.weight(1f))
        Text(label, color = palette.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(
            if (selected) "使用中" else "点按切换",
            color = if (selected) palette.green else palette.dim,
            fontSize = 9.5.sp,
        )
    }
}

@Composable
private fun ExportRow(title: String, desc: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(6.dp).background(LL.Green, CircleShape))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, color = LL.Text, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
            Text(desc, color = LL.Dim, fontSize = 9.5.sp)
        }
    }
}
