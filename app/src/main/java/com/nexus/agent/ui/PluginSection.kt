package com.nexus.agent.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nexus.agent.plugin.PluginField
import com.nexus.agent.plugin.PluginFieldType
import com.nexus.agent.plugin.PluginManager
import com.nexus.agent.plugin.PluginManifest
import com.nexus.agent.ui.components.LunaButton
import com.nexus.agent.ui.components.LunaOutlinedButton
import com.nexus.agent.ui.components.LunaTextButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * 插件区（挂在「插件」Tab 顶部）。
 *
 * 三块内容：
 * 1. 快捷入口：日常收集 / 触摸采集（原来散在游戏 Tab 中部，现在提到最上面）
 * 2. 插件列表：读 `filesDir/plugins/`，点「运行」交给副驾驶执行
 * 3. 导入：选本地 zip → 解压校验 → 装进插件目录
 *
 * @param onScrollToDaily 跳到「日常/触摸采集」区块
 * @param onScrollToTouch 跳到「训练数据采集」区块
 */
@Composable
fun PluginSection(
    onScrollToDaily: () -> Unit,
    onScrollToTouch: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var plugins by remember { mutableStateOf<List<PluginManifest>>(emptyList()) }
    var detail by remember { mutableStateOf<PluginManifest?>(null) }
    var installing by remember { mutableStateOf(false) }
    var runLog by remember { mutableStateOf("") }
    var running by remember { mutableStateOf(false) }

    fun reload() {
        plugins = PluginManager.list(context)
    }

    LaunchedEffect(Unit) { reload() }

    // 选 zip 导入
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        if (installing) return@rememberLauncherForActivityResult
        installing = true
        runLog = "جارٍ فك ضغط الإضافة والتحقق منها…"
        scope.launch {
            val r = PluginManager.installZip(context, uri)
            installing = false
            when (r) {
                is PluginManager.InstallResult.Ok -> {
                    reload()
                    runLog = "تم تثبيت الإضافة «${r.manifest.name}»" + if (r.overwritten) " (استبدال الإصدار السابق)" else ""
                    Toast.makeText(context, runLog, Toast.LENGTH_SHORT).show()
                }
                is PluginManager.InstallResult.Fail -> {
                    runLog = r.reason
                    Toast.makeText(context, r.reason, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Column {
        // ---------- 标题 + 导入 ----------
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("الإضافات", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(
                "${plugins.size} إضافات",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            LunaOutlinedButton(
                onClick = {
                    if (installing) {
                        Toast.makeText(context, "جارٍ التثبيت…", Toast.LENGTH_SHORT).show()
                    } else {
                        importLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                    }
                }
            ) { Text(if (installing) "جارٍ التثبيت…" else "استيراد إضافة") }
        }
        Text(
            "الإضافة عبارة عن تعريف plugin.json ونموذج إعدادات دون كود تنفيذي؛ يمكن تعديل JSON لإعادة تخصيصها.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // ---------- 快捷入口（上移的采集入口） ----------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LunaOutlinedButton(onClick = { onScrollToDaily() }, modifier = Modifier.weight(1f)) {
                Text("جمع البيانات اليومي")
            }
            LunaOutlinedButton(onClick = { onScrollToTouch() }, modifier = Modifier.weight(1f)) {
                Text("جمع اللمسات")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ---------- 插件列表 ----------
        if (plugins.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("لا توجد إضافات", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "اضغط «استيراد إضافة» واختر ملف ZIP، أو ضع plugin.json في " +
                            "Android/data/${context.packageName}/files/plugins/",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            plugins.forEach { p ->
                PluginCard(
                    manifest = p,
                    modelLabel = PluginManifest.modelLabel(PluginManager.model(context, p)),
                    running = running,
                    onRun = {
                        if (running) {
                            Toast.makeText(context, "إضافة أخرى قيد التشغيل", Toast.LENGTH_SHORT).show()
                        } else {
                            running = true
                            runLog = "جارٍ تشغيل «${p.name}»…"
                            PluginManager.run(
                                context = context,
                                manifest = p,
                                onReply = { msg -> runLog = msg },
                                onDone = { running = false }
                            )
                        }
                    },
                    onDetail = { detail = p },
                    onDelete = {
                        if (PluginManager.delete(context, p.id)) {
                            reload()
                            runLog = "تم حذف «${p.name}»"
                        } else {
                            runLog = "فشل الحذف: لم يُعثر على مجلد الإضافة"
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        if (runLog.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                runLog,
                style = MaterialTheme.typography.bodySmall,
                color = if (running) Color(0xFF8AB4F8) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // ---------- 详情 / 配置弹窗 ----------
    detail?.let { m ->
        PluginDetailDialog(
            manifest = m,
            onDismiss = { detail = null },
            onSave = { model, values ->
                PluginManager.setModel(context, m, model)
                PluginManager.saveSettings(context, m, values)
                detail = null
                runLog = "تم حفظ إعدادات «${m.name}»"
            },
            onSaveAndRun = { model, values ->
                PluginManager.setModel(context, m, model)
                PluginManager.saveSettings(context, m, values)
                detail = null
                if (!running) {
                    running = true
                    runLog = "جارٍ تشغيل «${m.name}»…"
                    PluginManager.run(
                        context = context,
                        manifest = m,
                        onReply = { msg -> runLog = msg },
                        onDone = { running = false }
                    )
                }
            }
        )
    }
}

/** 单个插件卡片 */
@Composable
private fun PluginCard(
    manifest: PluginManifest,
    modelLabel: String,
    running: Boolean,
    onRun: () -> Unit,
    onDetail: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    manifest.name,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "v${manifest.version}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (manifest.description.isNotBlank()) {
                Text(
                    manifest.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "النموذج: $modelLabel",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF8AB4F8)
                )
                if (manifest.builtin) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "مدمجة",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (manifest.settingsSchema.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "${manifest.settingsSchema.size} إعدادات",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // 0.8.12：白名单可见，用户才知道这个插件最多能干什么
                manifest.allowedTools?.let { tools ->
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (tools.isEmpty()) "تعطيل جميع الإجراءات" else "قائمة السماح: ${tools.size} إجراءات",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LunaButton(onClick = onRun) { Text(if (running) "جارٍ التشغيل…" else "تشغيل") }
                Spacer(modifier = Modifier.width(8.dp))
                LunaOutlinedButton(onClick = onDetail) { Text("إعداد") }
                Spacer(modifier = Modifier.weight(1f))
                LunaTextButton(onClick = onDelete) {
                    Text("حذف", color = Color(0xFFFF8A80))
                }
            }
        }
    }
}

/**
 * 插件详情：模型下拉 + schema 动态表单 + 工具白名单（只读）。
 *
 * 表单完全由 `settingsSchema` 驱动，支持 text / number / bool / select / slider 五种控件。
 */
@Composable
private fun PluginDetailDialog(
    manifest: PluginManifest,
    onDismiss: () -> Unit,
    onSave: (model: String, values: Map<String, String>) -> Unit,
    onSaveAndRun: (model: String, values: Map<String, String>) -> Unit
) {
    val context = LocalContext.current
    var model by remember(manifest.id) { mutableStateOf(PluginManager.model(context, manifest)) }
    var values by remember(manifest.id) {
        mutableStateOf<Map<String, String>>(PluginManager.settings(context, manifest))
    }
    var modelMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(manifest.name) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (manifest.description.isNotBlank()) {
                    Text(manifest.description, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // ---------- 模型选择 ----------
                Text("استخدام النموذج", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Box {
                    LunaOutlinedButton(onClick = { modelMenu = true }) {
                        Text(PluginManifest.modelLabel(model))
                    }
                    DropdownMenu(expanded = modelMenu, onDismissRequest = { modelMenu = false }) {
                        listOf(
                            PluginManifest.MODEL_DEFAULT,
                            PluginManifest.MODEL_CLOUD,
                            PluginManifest.MODEL_LOCAL
                        ).forEach { m ->
                            DropdownMenuItem(
                                text = { Text(PluginManifest.modelLabel(m)) },
                                onClick = {
                                    model = m
                                    modelMenu = false
                                }
                            )
                        }
                    }
                }
                Text(
                    when (model) {
                        PluginManifest.MODEL_CLOUD -> "تجاوز النموذج المحلي واستخدام السحابة مباشرة (أدق، يتطلب الاتصال ومفتاحاً)"
                        PluginManifest.MODEL_LOCAL -> "تفضيل Qwen2-VL المحلي (سريع ودون اتصال، وقدرته على التخطيط محدودة)"
                        else -> "تلقائي: المحلي أولاً، ثم السحابي عند الحاجة"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // ---------- 工具白名单（0.8.12，只读）----------
                manifest.allowedTools?.let { tools ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("الأدوات المتاحة", style = MaterialTheme.typography.labelLarge)
                    Text(
                        if (tools.isEmpty()) {
                            "لم يُصرح بأي إجراء (المحادثة فقط دون التحكم في الهاتف)"
                        } else {
                            tools.joinToString("、")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (manifest.settingsSchema.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("المعلمات", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(4.dp))
                    manifest.settingsSchema.forEach { field ->
                        PluginFieldRow(
                            field = field,
                            value = values[field.key] ?: field.default,
                            onChange = { v -> values = values.toMutableMap().apply { put(field.key, v) } }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        },
        confirmButton = {
            LunaTextButton(onClick = { onSaveAndRun(model, values) }) { Text("حفظ وتشغيل") }
        },
        dismissButton = {
            Row {
                LunaTextButton(onClick = onDismiss) { Text("إلغاء") }
                LunaTextButton(onClick = { onSave(model, values) }) { Text("حفظ") }
            }
        }
    )
}

/** 单个配置项：按类型渲染不同控件 */
@Composable
private fun PluginFieldRow(
    field: PluginField,
    value: String,
    onChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        when (field.type) {
            PluginFieldType.BOOL -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = value.equals("true", ignoreCase = true),
                        onCheckedChange = { onChange(if (it) "true" else "false") }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(field.label, style = MaterialTheme.typography.bodySmall)
                }
            }

            PluginFieldType.SELECT -> {
                var expanded by remember { mutableStateOf(false) }
                Text(field.label, style = MaterialTheme.typography.labelSmall)
                Box {
                    LunaOutlinedButton(onClick = { expanded = true }) {
                        Text(value.ifBlank { field.options.firstOrNull() ?: "يرجى الاختيار" })
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        field.options.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt) },
                                onClick = {
                                    onChange(opt)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            PluginFieldType.SLIDER -> {
                // 0.8.12：滑动条 —— 「程度」类参数不该让人手打 0~100 的数字
                val range = field.sliderRange
                val current = value.toIntOrNull()
                    ?: field.default.toIntOrNull()
                    ?: range.first
                val clamped = current.coerceIn(range.first, range.last).toFloat()
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            field.label,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "${clamped.toInt()}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = clamped,
                        onValueChange = { onChange(it.roundToInt().toString()) },
                        valueRange = range.first.toFloat()..range.last.toFloat(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            PluginFieldType.NUMBER -> {
                OutlinedTextField(
                    value = value,
                    onValueChange = { v -> if (v.isEmpty() || v.matches(Regex("""^-?\d*\.?\d*$"""))) onChange(v) },
                    label = { Text(field.label) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            PluginFieldType.TEXT -> {
                OutlinedTextField(
                    value = value,
                    onValueChange = onChange,
                    label = { Text(field.label) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        if (field.hint.isNotBlank()) {
            Text(
                field.hint,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** 供 UI 层复用的加载封装（避免每次重组都读盘） */
suspend fun loadPluginsSafely(context: android.content.Context): List<PluginManifest> =
    withContext(Dispatchers.IO) { PluginManager.list(context) }
