package com.nexus.agent.ui

import com.nexus.agent.schedule.ScheduledTask

// Display adapters only. Never pass the translated labels back into persisted game data.
internal fun arabicGameName(name: String): String = when (name) {
    "原神" -> "Genshin Impact"
    "崩坏：星穹铁道", "崩坏:星穹铁道", "星穹铁道" -> "Honkai: Star Rail"
    else -> name
}

internal fun arabicScreenType(type: String): String = when (type) {
    "主界面" -> "الرئيسية"
    "委托界面" -> "المهام"
    "派遣界面" -> "البعثات"
    "加载中" -> "التحميل"
    "战斗界面" -> "القتال"
    "其他" -> "أخرى"
    else -> type
}

internal val ARABIC_WEEK = listOf("الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت", "الأحد")

internal fun arabicRepeatText(task: ScheduledTask): String = when (task.repeat) {
    ScheduledTask.Repeat.DAILY -> "يومياً ${task.hourText}"
    ScheduledTask.Repeat.WEEKLY -> "أسبوعياً ${task.daysOfWeek.sorted().joinToString("/") { ARABIC_WEEK[it - 1] }} ${task.hourText}"
    ScheduledTask.Repeat.ONCE -> "مرة واحدة"
}

internal fun arabicStellarResult(raw: String): String = when (raw) {
    "Stellar 未运行，请重启手机后再试" -> "Stellar لا يعمل؛ أعد تشغيل الهاتف وحاول مجدداً"
    "仍未生效：服务未绑定，请重启手机后再试" -> "لم تُربط الخدمة؛ أعد تشغيل الهاتف وحاول مجدداً"
    "验证成功：Stellar 工作正常" -> "نجح التحقق: Stellar يعمل بصورة طبيعية"
    "仍未生效：命令执行异常，请重启手机后再试" -> "خطأ في تنفيذ الأمر؛ أعد تشغيل الهاتف وحاول مجدداً"
    else -> if (raw.startsWith("验证失败：")) "فشل التحقق: " + raw.removePrefix("验证失败：") else raw
}
