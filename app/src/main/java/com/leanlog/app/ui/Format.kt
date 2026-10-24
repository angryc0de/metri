package com.leanlog.app.ui

import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val weekNames = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

fun epochToDate(day: Int): LocalDate = LocalDate.ofEpochDay(day.toLong())

/** "10月22日 周四" */
fun formatDayTitle(day: Int): String {
    val d = epochToDate(day)
    return "${d.monthValue} 月 ${d.dayOfMonth} 日 ${weekNames[d.dayOfWeek.value - 1]}"
}

/** "今天 / 昨天 / 10月20日" */
fun formatDayShort(day: Int): String {
    val today = LocalDate.now().toEpochDay().toInt()
    return when (day) {
        today -> "今天"
        today - 1 -> "昨天"
        else -> epochToDate(day).format(DateTimeFormatter.ofPattern("M 月 d 日"))
    }
}

fun fmtNum(v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString() else String.format("%.1f", v)

fun fmtSigned(v: Int): String = when {
    v > 0 -> "-$v"
    v < 0 -> "+${-v}"
    else -> "0"
}
