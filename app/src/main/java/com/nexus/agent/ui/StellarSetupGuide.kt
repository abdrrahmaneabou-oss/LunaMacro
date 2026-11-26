package com.nexus.agent.ui

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.agent.input.ShizukuManager
import com.nexus.agent.system.StabilityHelper
import com.nexus.agent.ui.components.LunaOutlinedButton
import com.nexus.agent.ui.components.LunaTextButton

private const val STELLAR_PKG = "roro.stellar.manager"

/**
 * Stellar 配置引导页：分 5 步引导用户完成 vivo OriginOS 的白名单配置。
 */
@Composable
fun StellarSetupGuide(onClose: () -> Unit) {
    val context = LocalContext.current
    var step by remember { mutableStateOf(0) }
    var verifyResult by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("دليل إعداد Stellar", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            LunaTextButton(onClick = onClose) { Text("إغلاق") }
        }

        Text("Step ${step + 1}/5", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(progress = { (step + 1) / 5f }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))

        when (step) {
            0 -> GuideCard(
                title = "① استثناءات البطارية",
                desc = "على أجهزة vivo، اسمح باستهلاك الطاقة في الخلفية لـ Stellar لمنع النظام من إيقافه.",
                button = "فتح إعدادات البطارية"
            ) {
                try {
                    context.startActivity(
                        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                } catch (e: Throwable) {
                    Toast.makeText(context, "افتح إعدادات البطارية وإدارة استهلاك الخلفية يدوياً", Toast.LENGTH_LONG).show()
                }
            }
            1 -> GuideCard(
                title = "② السماح بالتشغيل التلقائي",
                desc = "اسمح بالتشغيل التلقائي لـ Stellar حتى يبدأ بعد إعادة تشغيل الجهاز أو إيقاف العملية.",
                button = "فتح إدارة التشغيل التلقائي"
            ) {
                // 自动探测厂商跳转（vivo/iQOO/小米/OPPO/华为/一加/三星等）
                val ok = StabilityHelper.openAutoStartByVendor(context)
                if (!ok) {
                    Toast.makeText(context, "افتح إعدادات التطبيقات والتشغيل التلقائي يدوياً", Toast.LENGTH_LONG).show()
                }
            }
            2 -> GuideCard(
                title = "③ استثناءات وضع السكون",
                desc = "أضف Stellar إلى استثناءات وضع السكون لمنع إيقافه عند إطفاء الشاشة.",
                button = "فتح إعدادات البطارية الإضافية"
            ) {
                try {
                    context.startActivity(
                        Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                } catch (e: Throwable) {
                    Toast.makeText(context, "افتح إعدادات البطارية الإضافية يدوياً", Toast.LENGTH_LONG).show()
                }
            }
            3 -> GuideCard(
                title = "④ مفاتيح Stellar",
                desc = "افتح Stellar وتأكد من تفعيل Watchdog وStart on Boot وFollow Startup.",
                button = "فتح Stellar"
            ) {
                val launch = context.packageManager.getLaunchIntentForPackage(STELLAR_PKG)
                if (launch != null) {
                    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launch)
                } else {
                    Toast.makeText(context, "لم يتم العثور على Stellar؛ ثبّته أولاً", Toast.LENGTH_SHORT).show()
                }
            }
            4 -> GuideCard(
                title = "⑤ التحقق",
                desc = "بعد اكتمال الإعداد اضغط التحقق لاختبار عمل Stellar.",
                button = "تحقق"
            ) {
                verifyResult = verifyStellar(context)
            }
        }

        // 第 5 步的验证结果显示
        verifyResult?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium, color = if (it.contains("نجاح")) Color(0xFF69F0AE) else Color(0xFFFF8A80))
            if (it.contains("成功")) {
                Spacer(Modifier.height(8.dp))
                Button(onClick = onClose) { Text("تم") }
            }
        }

        Spacer(Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (step > 0) {
                LunaOutlinedButton(onClick = { step-- }, modifier = Modifier.weight(1f)) { Text("السابق") }
            }
            if (step < 4) {
                Button(onClick = { step++ }, modifier = Modifier.weight(1f)) { Text("التالي") }
            }
        }
    }
}

/**
 * 单步引导卡片。
 */
@Composable
private fun GuideCard(title: String, desc: String, button: String, onAction: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(desc, style = MaterialTheme.typography.bodySmall, color = Color.Gray, lineHeight = 20.sp)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onAction) { Text(button) }
        }
    }
}

/**
 * 验证 Stellar 是否真正可用（执行 echo ok）。
 */
private fun verifyStellar(context: Context): String {
    return try {
        if (!com.nexus.agent.input.ShizukuManager.isShizukuAvailable()) {
            return "Stellar 未运行，请重启手机后再试"
        }
        val svc = ShizukuManager.getService()
        if (svc == null) {
            "仍未生效：服务未绑定，请重启手机后再试"
        } else {
            val out = svc.execCommand("echo ok")
            if (out.contains("ok", ignoreCase = true)) {
                "验证成功：Stellar 工作正常"
            } else {
                "仍未生效：命令执行异常，请重启手机后再试"
            }
        }
    } catch (e: Throwable) {
        "验证失败：${e.message}"
    }
}

/**
 * 检查 Stellar 是否已加入电池优化白名单。
 */
fun isStellarIgnoringBatteryOptimizations(context: Context): Boolean {
    return try {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        pm.isIgnoringBatteryOptimizations(STELLAR_PKG)
    } catch (e: Throwable) {
        false
    }
}
