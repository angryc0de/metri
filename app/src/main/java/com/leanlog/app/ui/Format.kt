package com.leanlog.app.ui

import com.leanlog.app.isEnglish
import com.leanlog.app.t
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val weekNames = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
private val weekShortEn = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
private val monthShortEn = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
)

fun epochToDate(day: Int): LocalDate = LocalDate.ofEpochDay(day.toLong())

/** 中文 "10月22日 周四" / 英文 "Thu, Oct 22" */
fun formatDayTitle(day: Int): String {
    val d = epochToDate(day)
    return if (isEnglish()) {
        "${weekShortEn[d.dayOfWeek.value - 1]}, ${monthShortEn[d.monthValue - 1]} ${d.dayOfMonth}"
    } else {
        "${d.monthValue} 月 ${d.dayOfMonth} 日 ${weekNames[d.dayOfWeek.value - 1]}"
    }
}

/** 中文 "今天 / 昨天 / 10月20日" / 英文 "Today / Yesterday / Oct 20" */
fun formatDayShort(day: Int): String {
    val today = LocalDate.now().toEpochDay().toInt()
    return when (day) {
        today -> t("今天")
        today - 1 -> t("昨天")
        else -> {
            val d = epochToDate(day)
            if (isEnglish()) "${monthShortEn[d.monthValue - 1]} ${d.dayOfMonth}"
            else "${d.monthValue} 月 ${d.dayOfMonth} 日"
        }
    }
}

fun fmtNum(v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString() else String.format("%.1f", v)

fun fmtSigned(v: Int): String = when {
    v > 0 -> "-$v"
    v < 0 -> "+${-v}"
    else -> "0"
}
