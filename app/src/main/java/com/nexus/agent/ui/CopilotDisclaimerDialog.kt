package com.nexus.agent.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nexus.agent.ui.components.LunaButton
import com.nexus.agent.ui.theme.LunaBackground
import com.nexus.agent.ui.theme.LunaErrBg
import com.nexus.agent.ui.theme.LunaErrFg
import com.nexus.agent.ui.theme.LunaOnBackground
import com.nexus.agent.ui.theme.LunaOnSurfaceVariant
import com.nexus.agent.ui.theme.LunaOutline
import com.nexus.agent.ui.theme.LunaPrimary
import com.nexus.agent.ui.theme.LunaSurface
import com.nexus.agent.ui.theme.LunaWarnBg
import com.nexus.agent.ui.theme.LunaWarnFg
import kotlinx.coroutines.delay

/** 免责声明强制阅读时长（秒） */
private const val DISCLAIMER_COUNTDOWN = 35

/**
 * 副驾驶免责声明（全屏、不可取消）。
 *
 * 首次进入「副驾驶」Tab 时强制展示：必须在倒计时结束后才能点「同意并继续」，
 * 返回键与点击外部都不关闭，避免用户「一秒划过」把风险条款点掉。
 *
 * @param onAccept 用户点击同意后回调（调用方负责写 SharedPreferences 并关闭）
 */
@Composable
fun CopilotDisclaimerDialog(onAccept: () -> Unit) {
    var remaining by remember { mutableIntStateOf(DISCLAIMER_COUNTDOWN) }

    LaunchedEffect(Unit) {
        while (remaining > 0) {
            delay(1000)
            remaining -= 1
        }
    }

    Dialog(
        onDismissRequest = { /* 不可取消：只能点「同意并继续」 */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = LunaBackground
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 顶部标题区
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 40.dp, bottom = 16.dp)
                ) {
                    Text(
                        "المساعد العائم · اقرأ قبل الاستخدام",
                        style = MaterialTheme.typography.headlineMedium,
                        color = LunaOnBackground
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(30.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(LunaPrimary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .width(14.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(LunaOutline)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "تقرأ هذه الوظيفة شاشتك وقد تنفذ اللمسات نيابة عنك. اقرأ البنود التالية خلال 35 ثانية.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LunaOnSurfaceVariant
                    )
                }

                // 正文（可滚动）
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DisclaimerItem(
                        index = "01",
                        title = "قراءة الشاشة الفورية ومخاطر الخصوصية",
                        body = "عند تفعيل المرافقة التفاعلية يلتقط التطبيق الشاشة دورياً للتحليل المحلي.\n" +
                            "• تُعالج اللقطات في ذاكرة الجهاز ولا تُرفع إلى السحابة (استدلال محلي).\n" +
                            "• يمكن التقاط أي محتوى ظاهر؛ لا تعرض بطاقات الهوية أو الحسابات البنكية أو كلمات المرور أو رموز التحقق أثناء التشغيل.\n" +
                            "• يتوقف التقاط الشاشة تلقائياً عند اكتشاف تطبيقات المصارف أو الدفع أو كلمات المرور.",
                        bg = LunaWarnBg,
                        fg = LunaWarnFg
                    )
                    DisclaimerItem(
                        index = "02",
                        title = "إخلاء المسؤولية عن محتوى الذكاء الاصطناعي",
                        body = "تُنشأ التعليقات والاقتراحات محلياً بواسطة Qwen2-VL-2B وقد تتضمن أخطاء في التعرف أو الصياغة أو الحقائق. " +
                            "المحتوى للترفيه فقط وليس توصية بتنفيذ إجراءات؛ يتحمل المستخدم مخاطر الاعتماد عليه.",
                        bg = LunaSurface,
                        fg = LunaOnBackground
                    )
                    DisclaimerItem(
                        index = "03",
                        title = "يحظر الغش في الألعاب وعمليات الدفع",
                        body = "يمكن للمساعد المحادثة والتحكم فعلياً في الهاتف حسب التعليمات (فتح التطبيقات واللمس والسحب والمهام متعددة الخطوات).\n" +
                            "• يُحظر استخدام الأتمتة أو الماكرو أو تشغيل حسابات متعددة للغش والإخلال بعدالة الألعاب.\n" +
                            "• يُحظر استخدامه للدفع والتحويل والشراء والتعرف على رموز التحقق أو أي إجراءات مالية أو مرتبطة بأمان الحساب (يرفض التنفيذ عند اكتشاف واجهات المصارف أو الدفع).\n" +
                            "• قد يخطئ الذكاء الاصطناعي في قراءة الشاشة أو اختيار الأزرار. أكد العمليات المهمة بنفسك ولا تسلم التحكم بالكامل.\n" +
                            "• التزم بشروط خدمة الألعاب والتطبيقات والقوانين المحلية.\n" +
                            "يتحمل المستخدم جميع عواقب الاستخدام المخالف.",
                        bg = LunaErrBg,
                        fg = LunaErrFg
                    )
                    Text(
                        "بالضغط على الزر أدناه تؤكد قراءة جميع البنود والموافقة عليها. يمكنك تعطيل النافذة العائمة والمرافقة التفاعلية من إعدادات المساعد.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LunaOnSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // 底部按钮：倒计时结束前不可点
                val unlocked = remaining <= 0
                LunaButton(
                    onClick = { if (unlocked) onAccept() },
                    enabled = unlocked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        if (unlocked) "أوافق وأتابع" else "اقرأ بعناية قبل الموافقة (${remaining}s)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (unlocked) MaterialTheme.colorScheme.onPrimary else LunaOnSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun DisclaimerItem(
    index: String,
    title: String,
    body: String,
    bg: androidx.compose.ui.graphics.Color,
    fg: androidx.compose.ui.graphics.Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = bg
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    index,
                    style = MaterialTheme.typography.titleMedium,
                    color = fg.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = fg
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = fg.copy(alpha = 0.85f),
                lineHeight = 21.sp
            )
        }
    }
}
