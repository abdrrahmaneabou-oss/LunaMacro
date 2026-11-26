package com.nexus.agent.ui

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.agent.accessibility.NexusAccessibilityService
import com.nexus.agent.bridge.MplLog
import com.nexus.agent.bridge.MplProtocol
import com.nexus.agent.bridge.MplServer
import com.nexus.agent.system.StabilityHelper
import com.nexus.agent.userservice.NexusForegroundService

/**
 * 设置页「MPL 桥」区块（0.9.31，0.9.32 补可靠性）。
 *
 * 独立文件，不动 [SettingsTab] 结构 —— 那一坨已经 2700 行了，
 * 再往里塞新功能等于给未来的自己挖坑。
 *
 * 这里只做这些事：开关、端口、粘贴配对码、看最近调用、看最近一次连接、电池提醒。
 * **不显示完整 token**：设置页可能被截屏、被旁人看到，
 * 而 token 一旦泄露，等于把"手"交给了别人。
 */
@Composable
internal fun MplSection(context: Context) {
    var enabled by remember { mutableStateOf(MplServer.isEnabled(context)) }
    var running by remember { mutableStateOf(MplServer.isRunning()) }
    var portText by remember { mutableStateOf(MplServer.port(context).toString()) }
    var tokenInput by remember { mutableStateOf("") }
    var hint by remember { mutableStateOf("") }
    var recent by remember { mutableStateOf(MplLog.recent(context)) }
    var lastConn by remember { mutableStateOf(MplLog.lastConn(context)) }
    var batteryOk by remember { mutableStateOf(StabilityHelper.isIgnoringBatteryOptimizations(context)) }
    var bound by remember { mutableStateOf(MplServer.boundAddresses()) }
    var accessibilityOk by remember { mutableStateOf(NexusAccessibilityService.isBound()) }
    var residentOk by remember { mutableStateOf(NexusForegroundService.isRunning()) }

    // 页面开着时每 2 秒刷一次：用户多半刚在 MPL 里点完"试一下"，回来看有没有到
    LaunchedEffect(enabled) {
        while (true) {
            running = MplServer.isRunning()
            bound = MplServer.boundAddresses()
            recent = MplLog.recent(context)
            lastConn = MplLog.lastConn(context)
            batteryOk = StabilityHelper.isIgnoringBatteryOptimizations(context)
            accessibilityOk = NexusAccessibilityService.isBound()
            residentOk = NexusForegroundService.isRunning()
            kotlinx.coroutines.delay(2000)
        }
    }

    Text("جسر MPL", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(4.dp))
    Text(
        "يسمح لمكوّن MPL على الهاتف نفسه بتفويض قراءة الشاشة وفتح التطبيقات إلى LunaMacro. " +
            "يستمع فقط على 127.0.0.1، ولا يسمح باتصالات الشبكة المحلية. يُقبل رمز الاقتران في ترويسة الطلب فقط. " +
            "يدعم هذا الإصدار قراءة الشاشة وفتح التطبيقات فقط، ولا يسمح بتنفيذ اللمسات.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        lineHeight = 17.sp
    )
    Spacer(Modifier.height(8.dp))

    // 0.9.33：把「能不能动手」的两个前提摆在最上面。
    // 联调时最耗时的一步就是猜「端口通了、为什么 injection=none」——
    // 无障碍掉了 / 前台服务没起，这两行能一眼看出来，不用回首页看弹窗。
    Text(
        buildString {
            append(if (accessibilityOk) "✅" else "❌").append(" إمكانية الوصول")
            append(if (accessibilityOk) " (يمكن قراءة الشاشة)" else " (منقطع: أعد تفعيل الخدمة من الرئيسية)")
            append("\n")
            append(if (residentOk) "✅" else "❌").append(" الخدمة الأمامية")
            append(if (residentOk) " (تعمل في الخلفية)" else " (متوقفة: قد تُجمّد الخدمة ولا تستجيب)")
        },
        style = MaterialTheme.typography.bodySmall,
        color = if (accessibilityOk && residentOk) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.error,
        lineHeight = 17.sp
    )
    Spacer(Modifier.height(10.dp))

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("تفعيل جسر MPL", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(3.dp))
            Text(
                if (enabled) {
                    if (running) "جارٍ الاستماع: ${bound.joinToString(", ")} · المنفذ ${MplServer.currentPort()}"
                    else "مفعل لكن تعذر الاستماع (قد يكون المنفذ مشغولاً)"
                } else {
                    "معطل: لا خدمة على المنفذ 18080"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = enabled,
            onCheckedChange = { on ->
                val ok = MplServer.setEnabled(context, on)
                enabled = MplServer.isEnabled(context)
                running = MplServer.isRunning()
                bound = MplServer.boundAddresses()
                batteryOk = StabilityHelper.isIgnoringBatteryOptimizations(context)
                hint = if (on && !ok) "فشل التشغيل: المنفذ $portText مشغول" else ""
            }
        )
    }

    // 0.9.32 W3：打开了桥但不是「电池不限制」时，后台会被系统冻结 ——
    // 端口还在、但一个包都不回。这正是上次联调里最难查的那个“超时”。
    if (enabled && !batteryOk) {
        Spacer(Modifier.height(8.dp))
        Text(
            "⚠️ يسمح النظام بتحسين بطارية التطبيق، ما قد يجمّد LunaMacro في الخلفية رغم بقاء المنفذ مفتوحاً. " +
                "سينتهي طلب MPL دون استجابة. اختر السماح بالتشغيل في الخلفية دون قيود.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            lineHeight = 17.sp
        )
        Spacer(Modifier.height(6.dp))
        Button(
            onClick = {
                runCatching { StabilityHelper.requestIgnoreBatteryOptimizations(context) }
                    .onFailure { hint = "تعذر فتح إعدادات النظام، اسمح بالتشغيل في الخلفية يدوياً" }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        ) { Text("فتح إعدادات البطارية غير المقيدة") }
    }

    Spacer(Modifier.height(14.dp))

    Text("منفذ الاستماع", style = MaterialTheme.typography.titleSmall)
    Spacer(Modifier.height(6.dp))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = portText,
            onValueChange = { s -> portText = s.filter { it.isDigit() }.take(5) },
            label = { Text("المنفذ (الافتراضي ${MplProtocol.PORT})") },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Button(
            onClick = {
                val p = portText.toIntOrNull()
                hint = when {
                    p == null || p !in 1024..65535 -> "يجب أن يكون المنفذ بين 1024 و65535"
                    MplServer.setPort(context, p) -> {
                        running = MplServer.isRunning()
                        bound = MplServer.boundAddresses()
                        "تم التبديل إلى $p"
                    }
                    else -> "فشل التبديل: $p مشغول"
                }
            },
            colors = ButtonDefaults.buttonColors()
        ) { Text("تطبيق") }
    }

    Spacer(Modifier.height(14.dp))

    Text("رمز الاقتران", style = MaterialTheme.typography.titleSmall)
    Spacer(Modifier.height(3.dp))
    Text(
        "يولّد MPL رمزاً سداسي عشرياً من 64 خانة، انسخه هنا. الحالي: ${MplServer.maskedToken(context)}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        lineHeight = 17.sp
    )
    Spacer(Modifier.height(6.dp))
    OutlinedTextField(
        value = tokenInput,
        onValueChange = { tokenInput = it },
        label = { Text("لصق رمز الاقتران") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(8.dp))
    Row {
        Button(
            onClick = {
                hint = if (MplServer.setToken(context, tokenInput)) {
                    tokenInput = ""
                    "تم حفظ رمز الاقتران (يبقى بعد إعادة التشغيل)"
                } else {
                    "صيغة غير صالحة: يلزم 32–128 خانة سداسية عشرية"
                }
            }
        ) { Text("حفظ") }
        Spacer(Modifier.width(8.dp))
        Button(
            onClick = {
                MplServer.clearToken(context)
                tokenInput = ""
                hint = "تم مسح رمز الاقتران؛ يلزم إعادة الاقتران في MPL"
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) { Text("مسح") }
    }

    if (hint.isNotBlank()) {
        Spacer(Modifier.height(6.dp))
        Text(
            hint,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            lineHeight = 18.sp
        )
    }

    Spacer(Modifier.height(14.dp))

    Text("آخر اتصال", style = MaterialTheme.typography.titleSmall)
    Spacer(Modifier.height(3.dp))
    Text(
        "يُحتسب كل طلب بما فيه ping وhealth ورموز الاقتران الخاطئة. " +
            "أما آخر الاستدعاءات فيسجل الإجراءات فقط.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        lineHeight = 17.sp
    )
    Spacer(Modifier.height(4.dp))
    val conn = lastConn
    if (conn == null) {
        Text(
            "لا توجد اتصالات بعد. إذا ضغطت زر MPL ولم يظهر شيء هنا، لم يُرسل الطلب.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        Text(
            conn.line(),
            style = MaterialTheme.typography.bodySmall,
            color = when {
                conn.status == 401 -> MaterialTheme.colorScheme.error
                conn.status == 0 -> MaterialTheme.colorScheme.onSurfaceVariant
                conn.status == 200 -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }

    Spacer(Modifier.height(14.dp))

    Text("آخر استدعاء", style = MaterialTheme.typography.titleSmall)
    Spacer(Modifier.height(4.dp))
    if (recent.isEmpty()) {
        Text(
            "لا توجد استدعاءات. بعد اختبار MPL سيظهر هنا أول 8 أحرف من المعرّف والإجراء والمدة.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        recent.take(8).forEach { e ->
            Text(
                e.line(),
                style = MaterialTheme.typography.bodySmall,
                color = if (e.ok) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(vertical = 1.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "共 ${recent.size} 条（最多保留 ${MplLog.MAX} 条）。id 前 8 位能和 MPL 的动作历史对上。",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
