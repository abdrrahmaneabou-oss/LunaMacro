package com.nexus.agent.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.nexus.agent.accessibility.NexusAccessibilityService
import com.nexus.agent.input.ShizukuManager
import com.nexus.agent.system.RomProfile
import com.nexus.agent.system.StellarInstaller
import com.nexus.agent.ui.theme.NexusAgentTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import com.nexus.agent.ui.components.LunaOutlinedButton

/**
 * 首次启动部署向导。
 *
 * 面向「零基础」测试者：逐项检测环境缺失，一键跳转引导，实时打勾。
 * 全程只需测试者跟着点「安装 / 开启 / 允许」，无需任何技术背景。
 *
 * 步骤：欢迎 → 安装 Stellar → 激活服务(配对) → 授权 → 无障碍 → 悬浮窗 → 录音 → 完成。
 *
 * 唯一无法自动化的一步是「无线配对输 6 位配对码」（Shizuku 机制的硬限制，
 * 除非 root），向导会给出傻瓜式引导。
 */
class OnboardingActivity : ComponentActivity() {

    companion object {
        const val PREFS = "nexus_prefs"
        const val KEY_ONBOARDING = "onboarding_completed"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ShizukuManager.init(applicationContext)

        setContent {
            NexusAgentTheme {
                OnboardingScreen(
                    onFinish = {
                        getSharedPreferences(PREFS, MODE_PRIVATE)
                            .edit().putBoolean(KEY_ONBOARDING, true).apply()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { 8 })
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // ===== 实时状态检测 =====
    var stellarInstalled by remember { mutableStateOf(StellarInstaller.isInstalled(context)) }
    var shizukuRunning by remember { mutableStateOf(Shizuku.pingBinder()) }
    var shizukuGranted by remember { mutableStateOf(ShizukuManager.hasShizukuPermission()) }
    var accessibilityOk by remember { mutableStateOf(NexusAccessibilityService.isServiceRunning(context)) }
    var overlayOk by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var recordOk by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED
        )
    }

    LaunchedEffect(Unit) {
        while (true) {
            stellarInstalled = StellarInstaller.isInstalled(context)
            shizukuRunning = Shizuku.pingBinder()
            shizukuGranted = shizukuRunning && ShizukuManager.hasShizukuPermission()
            accessibilityOk = NexusAccessibilityService.isServiceRunning(context)
            overlayOk = Settings.canDrawOverlays(context)
            recordOk = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            delay(1000)
        }
    }

    // 录音权限请求
    val recordLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    // 每页「是否已就绪」
    fun stepOk(page: Int): Boolean = when (page) {
        0 -> true                       // 欢迎
        1 -> stellarInstalled           // Stellar 已装
        2 -> shizukuRunning             // 服务已激活
        3 -> shizukuGranted             // 已授权
        4 -> accessibilityOk            // 无障碍
        5 -> overlayOk                  // 悬浮窗
        6 -> recordOk                   // 录音
        7 -> true                       // 完成
        else -> true
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        // 0.9.0 批次三：MIMU / 和顺 定制系统的专属保活引导。
        // 这类系统会把无障碍和前台服务在息屏后掐掉，通用引导里没有这一步，
        // 用户装完第一晚就会被「无障碍未连接」打回原形。
        var showMimuTips by remember { mutableStateOf(RomProfile.isMimu()) }
        if (showMimuTips) {
            MimTipsDialog(
                onDismiss = { showMimuTips = false },
                onOpenAppInfo = {
                    showMimuTips = false
                    openAppInfo(context)
                }
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                when (page) {
                    0 -> WelcomePage()
                    1 -> StellarInstallPage(stellarInstalled) {
                        if (StellarInstaller.canInstall(context)) {
                            StellarInstaller.install(context)
                        } else {
                            StellarInstaller.openInstallPermissionSettings(context)
                        }
                    }
                    2 -> ActivatePage(
                        ok = shizukuRunning,
                        onOpenStellar = { openApp(context, StellarInstaller.STELLAR_PACKAGE) },
                        onOpenWirelessDebug = { openWirelessDebug(context) }
                    )
                    3 -> GrantPage(shizukuGranted) {
                        ShizukuManager.requestShizukuPermission()
                    }
                    4 -> AccessibilityPage(accessibilityOk) {
                        try { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) } catch (_: Throwable) {}
                    }
                    5 -> OverlayPage(overlayOk) {
                        try {
                            context.startActivity(
                                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                            )
                        } catch (_: Throwable) {}
                    }
                    6 -> RecordPage(recordOk) {
                        recordLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                    7 -> DonePage(
                        onFinish = onFinish,
                        onRegisterDeepSeek = { openUrl(context, "https://platform.deepseek.com/") },
                        onRegisterDashScope = { openUrl(context, "https://bailian.console.aliyun.com/") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 进度点
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(8) { i ->
                    Text(
                        text = if (i == pagerState.currentPage) "●" else if (stepOk(i)) "✓" else "○",
                        color = when {
                            i == pagerState.currentPage -> MaterialTheme.colorScheme.primary
                            stepOk(i) -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 底部按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (pagerState.currentPage > 0) {
                    LunaOutlinedButton(onClick = {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                    }) { Text("السابق") }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                if (pagerState.currentPage < 7) {
                    Button(
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        }
                    ) { Text("التالي") }
                } else {
                    Button(onClick = onFinish) { Text("بدء الاستخدام") }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

/** 打开某包名的主界面 */
private fun openApp(context: android.content.Context, pkg: String) {
    try {
        val intent = context.packageManager.getLaunchIntentForPackage(pkg)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    } catch (_: Throwable) {}
}

/** 打开 URL（浏览器，用于 API Key 注册跳转） */
private fun openUrl(context: android.content.Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Throwable) {
        android.widget.Toast.makeText(context, "تعذر فتح المتصفح، افتح الرابط يدوياً: $url", android.widget.Toast.LENGTH_LONG).show()
    }
}

/** 打开系统开发者选项（「无线调试」开关在里面） */
private fun openWirelessDebug(context: android.content.Context) {
    try {
        context.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
    } catch (_: Throwable) {}
}

/**
 * 打开本应用的「应用信息」页。
 *
 * MIMU / 和顺 这类系统的**自启动**与**后台耗电策略**都藏在应用信息里，
 * 单独做几个 intent 去跳是跳不到的（各家改了 Action 名），
 * 老实送到应用信息页，用户自己点两下最稳。
 */
private fun openAppInfo(context: android.content.Context) {
    try {
        context.startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (e: Throwable) {
        android.widget.Toast.makeText(
            context,
            "تعذر فتح معلومات التطبيق. انتقل يدوياً إلى الإعدادات ← التطبيقات ← LunaMacro",
            android.widget.Toast.LENGTH_LONG
        ).show()
    }
}

/**
 * 定制系统的专属保活提示（0.9.0 批次三）。
 *
 * 三步缺一不可，所以写成编号清单而不是一段话 —— 用户会照着一条条做。
 * 最后一句特意说明「会被自动降级成纯数字生命」：与其让它天天报错，
 * 不如提前告诉用户「这台机器上它只能陪聊」，预期管理比修 bug 更能救体验。
 */
@Composable
private fun MimTipsDialog(onDismiss: () -> Unit, onOpenAppInfo: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${RomProfile.label()} يتطلب ثلاث خطوات إضافية") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "قد يوقف هذا النظام خدمة إمكانية الوصول والخدمة الأمامية بعد إطفاء الشاشة. دون الخطوات التالية، " +
                        "قد تصبح قراءة الشاشة والتحكم في الهاتف غير مستقرين.",
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("① معلومات التطبيق ← تفعيل التشغيل التلقائي", style = MaterialTheme.typography.bodyMedium)
                Text("② التطبيقات الأخيرة ← قفل بطاقة التطبيق لمنع إغلاقه", style = MaterialTheme.typography.bodyMedium)
                Text("③ البطارية ← السماح بالاستهلاك في الخلفية دون قيود", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "إذا لم يعمل Shizuku بعد هذه الخطوات، ينتقل LunaMacro تلقائياً إلى وضع المحادثة فقط: " +
                        "لن يحاول التحكم في الهاتف أو يكرر إشعارات الفشل.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("حسناً") }
        },
        dismissButton = {
            TextButton(onClick = onOpenAppInfo) { Text("فتح معلومات التطبيق") }
        }
    )
}

// ===== 各步骤页面 =====

@Composable
private fun WelcomePage() {
    StepLayout(
        title = "مرحباً",
        icon = "👋",
        desc = "اتبع خطوات الإعداد البسيطة لبدء استخدام LunaMacro. لا تتطلب خبرة تقنية.",
        action = null,
        onAction = {}
    )
}

@Composable
private fun StellarInstallPage(ok: Boolean, onInstall: () -> Unit) {
    StepLayout(
        title = "الخطوة الأولى: تثبيت Stellar",
        icon = if (ok) "✅" else "📦",
        desc = if (ok) "تم تثبيت Stellar" else "Stellar مكوّن أساسي للحصول على صلاحيات النظام، وهو مضمّن في التطبيق.\nاضغط تثبيت Stellar ثم وافق على التثبيت في نافذة النظام.",
        action = if (!ok) "تثبيت Stellar" else null,
        onAction = onInstall
    )
}

@Composable
private fun ActivatePage(ok: Boolean, onOpenStellar: () -> Unit, onOpenWirelessDebug: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(if (ok) "✅" else "🔑", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text("الخطوة الثانية: تفعيل الخدمة", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            if (ok) "تم تفعيل الخدمة" else "يلزم إدخال رمز اقتران من 6 أرقام مرة واحدة (من آليات أمان Android).\n\n① فعّل التصحيح اللاسلكي\n② افتح Stellar\n③ اختر التصحيح اللاسلكي ← تشغيل\n④ افتح الإشعارات وأدخل الرمز المكوّن من 6 أرقام في إشعار Stellar",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (!ok) {
            Spacer(modifier = Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LunaOutlinedButton(onClick = onOpenWirelessDebug) { Text("تفعيل التصحيح اللاسلكي") }
                Button(onClick = onOpenStellar) { Text("فتح Stellar") }
            }
        }
    }
}

@Composable
private fun GrantPage(ok: Boolean, onGrant: () -> Unit) {
    StepLayout(
        title = "الخطوة الثالثة: منح الإذن",
        icon = if (ok) "✅" else "🔓",
        desc = if (ok) "تم منح الإذن لـ LunaMacro" else "امنح LunaMacro إذن التحكم.\nاضغط منح الإذن ثم اختر السماح.",
        action = if (!ok) "منح الإذن" else null,
        onAction = onGrant
    )
}

@Composable
private fun AccessibilityPage(ok: Boolean, onOpen: () -> Unit) {
    StepLayout(
        title = "الخطوة الرابعة: تفعيل إمكانية الوصول",
        icon = if (ok) "✅" else "👁",
        desc = if (ok) "خدمة إمكانية الوصول مفعلة" else "تتيح هذه الخدمة للمساعد قراءة الشاشة.\nاضغط تفعيل وابحث عن LunaMacro في القائمة ثم شغّل الخدمة.",
        action = if (!ok) "تفعيل" else null,
        onAction = onOpen
    )
}

@Composable
private fun OverlayPage(ok: Boolean, onOpen: () -> Unit) {
    StepLayout(
        title = "الخطوة الخامسة: تفعيل النافذة العائمة",
        icon = if (ok) "✅" else "🪟",
        desc = if (ok) "النافذة العائمة مفعلة" else "تتيح للمساعد عرض حالة التفكير والتشغيل.\nاضغط تفعيل واسمح بالظهور فوق التطبيقات الأخرى.",
        action = if (!ok) "تفعيل" else null,
        onAction = onOpen
    )
}

@Composable
private fun RecordPage(ok: Boolean, onRequest: () -> Unit) {
    StepLayout(
        title = "الخطوة السادسة: تفعيل الميكروفون",
        icon = if (ok) "✅" else "🎤",
        desc = if (ok) "إذن التسجيل مفعل" else "يتيح للمساعد سماع عبارة التنشيط «你好高总».\nاضغط تفعيل ثم السماح.",
        action = if (!ok) "تفعيل" else null,
        onAction = onRequest
    )
}

@Composable
private fun DonePage(onFinish: () -> Unit, onRegisterDeepSeek: () -> Unit, onRegisterDashScope: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text("🎉", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text("اكتمل الإعداد", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "اكتمل إعداد البيئة!\n\nيلزم مفتاح API لنموذج اللغة حتى يستطيع المساعد التفكير وتحليل الصور:\n· DeepSeek (موصى به)\n· Qwen DashScope\n\nإذا لم يكن لديك حساب، سجّل أدناه ثم أدخل المفتاح في إعدادات التطبيق.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LunaOutlinedButton(onClick = onRegisterDeepSeek) { Text("التسجيل في DeepSeek") }
            LunaOutlinedButton(onClick = onRegisterDashScope) { Text("التسجيل في Qwen") }
        }
    }
}

/** 通用步骤布局 */
@Composable
private fun StepLayout(
    title: String,
    icon: String,
    desc: String,
    action: String?,
    onAction: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(icon, fontSize = 48.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = desc,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (action != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onAction) { Text(action) }
        }
    }
}
