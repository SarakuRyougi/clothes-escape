package com.sfyz.clothesescape.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.sfyz.clothesescape.data.SLOT_MAX
import com.sfyz.clothesescape.data.SLOT_MIN
import com.sfyz.clothesescape.data.Settings
import com.sfyz.clothesescape.notify.ReminderScheduler
import com.sfyz.clothesescape.ui.components.NeumButton
import com.sfyz.clothesescape.ui.components.NeumChip
import com.sfyz.clothesescape.ui.components.NeumSurface
import com.sfyz.clothesescape.ui.components.SectionTitle
import com.sfyz.clothesescape.ui.components.Stepper
import com.sfyz.clothesescape.ui.theme.LocalNeum
import com.sfyz.clothesescape.ui.theme.ThemeMode

@Composable
fun SettingsScreen(
    settings: Settings,
    onTheme: (ThemeMode) -> Unit,
    onSlots: (Int) -> Unit,
    onDays: (Int) -> Unit,
    onHour: (Int) -> Unit,
    onMotion: (Boolean) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val neum = LocalNeum.current
    val context = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) tick++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationsOk = remember(tick) {
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }
    val exactOk = remember(tick) { ReminderScheduler.canScheduleExact(context) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text("我的", style = MaterialTheme.typography.headlineMedium, color = neum.text)
                Spacer(Modifier.height(3.dp))
                Text(
                    "数据只存在这台手机里，不上传",
                    style = MaterialTheme.typography.bodyMedium,
                    color = neum.textSoft
                )
            }
        }

        item {
            NeumSurface(corner = 24.dp, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    SectionTitle("外观")
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NeumChip(
                            text = "跟随系统",
                            selected = settings.themeMode == ThemeMode.SYSTEM,
                            onClick = { onTheme(ThemeMode.SYSTEM) }
                        )
                        NeumChip(
                            text = "浅色",
                            selected = settings.themeMode == ThemeMode.LIGHT,
                            onClick = { onTheme(ThemeMode.LIGHT) }
                        )
                        NeumChip(
                            text = "深色",
                            selected = settings.themeMode == ThemeMode.DARK,
                            onClick = { onTheme(ThemeMode.DARK) }
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("减少动效", style = MaterialTheme.typography.titleMedium, color = neum.text)
                            Text(
                                "动画更短，低端机更稳",
                                style = MaterialTheme.typography.labelMedium,
                                color = neum.textSoft
                            )
                        }
                        Switch(
                            checked = settings.reduceMotion,
                            onCheckedChange = onMotion,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = neum.onAccent,
                                checkedTrackColor = neum.accent,
                                uncheckedThumbColor = neum.surface,
                                uncheckedTrackColor = neum.surfaceAlt,
                                uncheckedBorderColor = neum.outline
                            )
                        )
                    }
                }
            }
        }

        item {
            NeumSurface(corner = 24.dp, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    SectionTitle("晾衣架")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "每架有几个可点的位置",
                        style = MaterialTheme.typography.labelMedium,
                        color = neum.textSoft
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Stepper(
                            value = settings.slotCount,
                            onChange = onSlots,
                            min = SLOT_MIN,
                            max = SLOT_MAX,
                            suffix = " 位"
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "共 ${settings.slotCount * 15} 格",
                            style = MaterialTheme.typography.labelMedium,
                            color = neum.textSoft
                        )
                    }
                }
            }
        }

        item {
            NeumSurface(corner = 24.dp, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    SectionTitle("默认提醒")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "新建记录时预填的收衣时间",
                        style = MaterialTheme.typography.labelMedium,
                        color = neum.textSoft
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Stepper(settings.defaultDays, onDays, 1, 14, " 天后")
                        Spacer(Modifier.width(10.dp))
                        Stepper(settings.defaultHour, onHour, 0, 23, " 点")
                    }
                }
            }
        }

        item {
            NeumSurface(corner = 24.dp, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    SectionTitle("提醒能不能准时到")
                    Spacer(Modifier.height(10.dp))
                    PermissionRow(
                        title = "通知权限",
                        ok = notificationsOk,
                        okText = "已允许",
                        badText = "去开启",
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                val intent = Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .putExtra(AndroidSettings.EXTRA_APP_PACKAGE, context.packageName)
                                runCatching { context.startActivity(intent) }
                            }
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    PermissionRow(
                        title = "精确闹钟",
                        ok = exactOk,
                        okText = "已允许",
                        badText = "去开启",
                        onClick = {
                            val intent = Intent(AndroidSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                .setData(Uri.parse("package:${context.packageName}"))
                            runCatching { context.startActivity(intent) }
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "国产系统可能还会限制后台，记得在系统设置里允许自启动与后台运行。",
                        style = MaterialTheme.typography.labelMedium,
                        color = neum.textSoft
                    )
                }
            }
        }

        item {
            NeumSurface(corner = 24.dp, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    SectionTitle("备份")
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NeumButton("导出", onExport, primary = false, modifier = Modifier.weight(1f))
                        NeumButton("导入", onImport, primary = false, modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "导出一个 JSON 文件，换手机时导入即可恢复",
                        style = MaterialTheme.typography.labelMedium,
                        color = neum.textSoft
                    )
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "衣服要逃跑了 · v1.0",
                    style = MaterialTheme.typography.labelMedium,
                    color = neum.textSoft
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "水房 1–15 号架，收好每一件",
                    style = MaterialTheme.typography.labelMedium,
                    color = neum.textSoft.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    ok: Boolean,
    okText: String,
    badText: String,
    onClick: () -> Unit
) {
    val neum = LocalNeum.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !ok, onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = neum.text, modifier = Modifier.weight(1f))
        Box(
            Modifier
                .background(
                    (if (ok) neum.success else neum.danger).copy(alpha = 0.15f),
                    RoundedCornerShape(10.dp)
                )
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(
                if (ok) okText else badText,
                style = MaterialTheme.typography.labelLarge,
                color = if (ok) neum.success else neum.danger,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
