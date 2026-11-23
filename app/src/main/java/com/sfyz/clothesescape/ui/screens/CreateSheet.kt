package com.sfyz.clothesescape.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.sfyz.clothesescape.data.Slot
import com.sfyz.clothesescape.data.TimeUtils
import com.sfyz.clothesescape.data.regionLabel
import com.sfyz.clothesescape.data.regionSpanText
import com.sfyz.clothesescape.ui.components.NeumButton
import com.sfyz.clothesescape.ui.components.NeumChip
import com.sfyz.clothesescape.ui.components.NeumSurface
import com.sfyz.clothesescape.ui.components.SectionTitle
import com.sfyz.clothesescape.ui.components.SheetGrabber
import com.sfyz.clothesescape.ui.components.Stepper
import com.sfyz.clothesescape.ui.theme.LocalNeum
import com.sfyz.clothesescape.ui.theme.softShadow
import com.sfyz.clothesescape.util.ImageCompressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSheet(
    slots: List<Slot>,
    skipped: Int,
    recentNames: List<String>,
    defaultDays: Int,
    defaultHour: Int,
    onDismiss: () -> Unit,
    onSave: (names: List<String>, photoPath: String?, days: Int, hour: Int) -> Unit
) {
    val neum = LocalNeum.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var photoPath by remember { mutableStateOf<String?>(null) }
    var pendingFile by remember { mutableStateOf<File?>(null) }
    var input by remember { mutableStateOf("") }
    val names = remember { mutableStateListOf<String>() }
    var days by remember { mutableIntStateOf(defaultDays.coerceAtLeast(1)) }
    var hour by remember { mutableIntStateOf(defaultHour) }
    val openedAt = remember { System.currentTimeMillis() }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val tmp = pendingFile
        if (ok && tmp != null && tmp.exists()) {
            scope.launch {
                val saved = withContext(Dispatchers.IO) {
                    ImageCompressor.compress(context, Uri.fromFile(tmp), ImageCompressor.newPhotoFile(context))
                }
                if (saved != null) photoPath = saved
                tmp.delete()
            }
        }
    }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                val saved = withContext(Dispatchers.IO) {
                    ImageCompressor.compress(context, uri, ImageCompressor.newPhotoFile(context))
                }
                if (saved != null) photoPath = saved
            }
        }
    }

    fun openCamera() {
        runCatching {
            val file = File(context.cacheDir, "cam_${System.currentTimeMillis()}.jpg")
            pendingFile = file
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            takePicture.launch(uri)
        }
    }

    fun addName(raw: String) {
        val t = raw.trim()
        if (t.isNotEmpty() && names.none { it == t }) names.add(t)
        input = ""
    }

    /** Names to persist: chips plus whatever is still sitting in the text field. */
    fun effectiveNames(): List<String> {
        val pending = input.trim()
        return if (pending.isNotEmpty() && names.none { it == pending }) names.toList() + pending else names.toList()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = neum.surface,
        scrimColor = neum.shadow.copy(alpha = 0.4f),
        dragHandle = { SheetGrabber() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 26.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("新晾晒记录", style = MaterialTheme.typography.headlineMedium, color = neum.text)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "${regionLabel(slots)} · ${regionSpanText(slots)} 格",
                        style = MaterialTheme.typography.bodyMedium,
                        color = neum.textSoft
                    )
                }
                if (skipped > 0) {
                    Box(
                        Modifier
                            .background(neum.danger.copy(alpha = 0.14f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                    ) {
                        Text(
                            "跳过 $skipped 格",
                            style = MaterialTheme.typography.labelMedium,
                            color = neum.danger
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionTitle("现场照片")
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                NeumChip(
                    text = if (photoPath == null) "拍一张" else "重拍",
                    selected = false,
                    onClick = { openCamera() }
                )
                Spacer(Modifier.width(8.dp))
                NeumChip(text = "从相册选", selected = false, onClick = { pickImage.launch("image/*") })
                Spacer(Modifier.width(10.dp))
                val path = photoPath
                if (path != null) {
                    AsyncImage(
                        model = File(path),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(Modifier.width(8.dp))
                    NeumChip(text = "移除", selected = false, onClick = { photoPath = null })
                }
            }

            Spacer(Modifier.height(18.dp))
            SectionTitle("这次晾了什么")
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .softShadow(corner = 16.dp, offset = 3.dp)
                        .background(neum.surfaceAlt, RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    BasicTextField(
                        value = input,
                        onValueChange = { input = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = neum.text),
                        cursorBrush = SolidColor(neum.accent),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { addName(input) }),
                        decorationBox = { inner ->
                            if (input.isEmpty()) {
                                Text(
                                    "例如：黑卫衣、白 T",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = neum.textSoft
                                )
                            }
                            inner()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.width(8.dp))
                NeumButton(text = "添加", onClick = { addName(input) }, primary = true)
            }

            if (names.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                ChipWrap(items = names.toList(), selected = true) { names.remove(it) }
            }

            val recent = recentNames.filter { it.isNotBlank() && names.none { n -> n == it } }.take(9)
            if (recent.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "常晾的衣服",
                    style = MaterialTheme.typography.labelMedium,
                    color = neum.textSoft
                )
                Spacer(Modifier.height(6.dp))
                ChipWrap(items = recent, selected = false) { addName(it) }
            }

            Spacer(Modifier.height(18.dp))
            SectionTitle("几天后收衣服")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1 to "明天", 2 to "2 天后", 3 to "3 天后", 5 to "5 天后").forEach { (d, label) ->
                    NeumChip(text = label, selected = days == d, onClick = { days = d })
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("天数", style = MaterialTheme.typography.labelLarge, color = neum.textSoft)
                Spacer(Modifier.width(8.dp))
                Stepper(value = days, onChange = { days = it }, min = 1, max = 14, suffix = " 天")
                Spacer(Modifier.width(14.dp))
                Text("时刻", style = MaterialTheme.typography.labelLarge, color = neum.textSoft)
                Spacer(Modifier.width(8.dp))
                Stepper(value = hour, onChange = { hour = it }, min = 0, max = 23, suffix = " 点")
            }

            Spacer(Modifier.height(12.dp))
            NeumSurface(corner = 16.dp, modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "将于 ${TimeUtils.full(TimeUtils.dueAt(openedAt, days, hour))} 提醒你",
                        style = MaterialTheme.typography.bodyMedium,
                        color = neum.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                NeumButton(
                    text = "先不建了",
                    onClick = onDismiss,
                    primary = false,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(12.dp))
                NeumButton(
                    text = "挂上，开始计时",
                    onClick = {
                        onSave(effectiveNames(), photoPath, days, hour)
                    },
                    primary = true,
                    modifier = Modifier.weight(1.4f)
                )
            }
        }
    }
}

@Composable
private fun ChipWrap(
    items: List<String>,
    selected: Boolean,
    onClick: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { item ->
                    NeumChip(
                        text = if (selected) "$item ×" else item,
                        selected = selected,
                        onClick = { onClick(item) },
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            }
        }
    }
}
