package com.sfyz.clothesescape.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sfyz.clothesescape.data.ITEM_COLLECTED
import com.sfyz.clothesescape.data.ITEM_DRYING
import com.sfyz.clothesescape.data.ITEM_LOST
import com.sfyz.clothesescape.data.ItemUi
import com.sfyz.clothesescape.data.RecordUi
import com.sfyz.clothesescape.data.Slot
import com.sfyz.clothesescape.data.TimeUtils
import com.sfyz.clothesescape.data.regionLabel
import com.sfyz.clothesescape.ui.components.NeumButton
import com.sfyz.clothesescape.ui.components.NeumSurface
import com.sfyz.clothesescape.ui.components.SectionTitle
import com.sfyz.clothesescape.ui.components.statusColor
import com.sfyz.clothesescape.ui.components.statusLabel
import com.sfyz.clothesescape.ui.theme.LocalNeum
import com.sfyz.clothesescape.ui.theme.softShadow
import java.io.File

@Composable
fun DetailScreen(
    record: RecordUi,
    now: Long,
    onBack: () -> Unit,
    onCollectItem: (Long) -> Unit,
    onLostItem: (Long) -> Unit,
    onCollectAll: () -> Unit,
    onLostAll: () -> Unit,
    onExtend: (Int) -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val neum = LocalNeum.current
    val accent = statusColor(record, now)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(neum.bg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, top = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBubble(Icons.Filled.ArrowBack, onBack)
            Spacer(Modifier.width(12.dp))
            Text(
                "晾晒详情",
                style = MaterialTheme.typography.titleLarge,
                color = neum.text,
                modifier = Modifier.weight(1f)
            )
            IconBubble(Icons.Filled.Delete, onDelete, tint = neum.danger)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            val photo = record.photoPath
            if (photo != null && File(photo).exists()) {
                AsyncImage(
                    model = File(photo),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .softShadow(corner = 24.dp, offset = 5.dp)
                        .clip(RoundedCornerShape(24.dp))
                )
                Spacer(Modifier.height(14.dp))
            }

            NeumSurface(corner = 22.dp, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .background(accent.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                statusLabel(record, now),
                                style = MaterialTheme.typography.labelLarge,
                                color = accent,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = regionLabel(record.slots.sortedWith(compareBy({ it.rack }, { it.slot }))),
                            style = MaterialTheme.typography.titleMedium,
                            color = neum.text
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = if (record.active) {
                            TimeUtils.remainingText(now, record.dueAt) + " · 提醒于 " + TimeUtils.full(record.reminderAt)
                        } else {
                            "挂上于 " + TimeUtils.full(record.hungAt)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = neum.textSoft
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "位置：" + rackListText(record.slots),
                        style = MaterialTheme.typography.labelMedium,
                        color = neum.textSoft,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionTitle("这次晾的衣服")
            Spacer(Modifier.height(8.dp))

            NeumSurface(corner = 22.dp, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    if (record.items.isEmpty()) {
                        Text(
                            "没有记录具体衣物",
                            style = MaterialTheme.typography.bodyMedium,
                            color = neum.textSoft,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                    record.items.forEachIndexed { index, item ->
                        ItemRow(
                            item = item,
                            editable = record.active,
                            onCollect = { onCollectItem(item.id) },
                            onLost = { onLostItem(item.id) }
                        )
                        if (index != record.items.lastIndex) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(neum.outline.copy(alpha = 0.55f))
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            if (record.active) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NeumButton(
                        text = "全部收回",
                        onClick = onCollectAll,
                        primary = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(10.dp))
                    NeumButton(
                        text = "推迟 1 天",
                        onClick = { onExtend(1) },
                        primary = false,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NeumButton(
                        text = "标记丢失",
                        onClick = onLostAll,
                        primary = false,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(10.dp))
                    NeumButton(
                        text = "取消这条",
                        onClick = onCancel,
                        primary = false,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                NeumButton(
                    text = "删除这条记录",
                    onClick = onDelete,
                    primary = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(26.dp))
        }
    }
}

private fun rackListText(slots: List<Slot>): String {
    if (slots.isEmpty()) return "未指定"
    return slots.groupBy { it.rack }
        .toSortedMap()
        .map { (rack, list) ->
            val nums = list.map { it.slot }.sorted()
            "$rack 号架 第 ${nums.joinToString("、")} 位"
        }
        .joinToString("；")
}

@Composable
private fun ItemRow(
    item: ItemUi,
    editable: Boolean,
    onCollect: () -> Unit,
    onLost: () -> Unit
) {
    val neum = LocalNeum.current
    val (label, tint) = when (item.state) {
        ITEM_COLLECTED -> "已收" to neum.success
        ITEM_LOST -> "丢了" to neum.danger
        else -> "晾着" to neum.accent
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                item.name,
                style = MaterialTheme.typography.titleMedium,
                color = if (item.state == ITEM_LOST) neum.textSoft else neum.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = tint)
        }
        if (editable) {
            if (item.state != ITEM_COLLECTED) {
                MiniAction("收", neum.success, onCollect)
                Spacer(Modifier.width(8.dp))
            }
            if (item.state != ITEM_LOST) {
                MiniAction("丢", neum.danger, onLost)
            }
        }
    }
}

@Composable
private fun MiniAction(label: String, tint: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .softShadow(corner = 12.dp, offset = 2.dp)
            .background(tint.copy(alpha = 0.16f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = tint, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun IconBubble(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    tint: androidx.compose.ui.graphics.Color? = null
) {
    val neum = LocalNeum.current
    Box(
        modifier = Modifier
            .size(38.dp)
            .softShadow(corner = 14.dp, offset = 3.dp)
            .background(neum.surface, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint ?: neum.text,
            modifier = Modifier.size(18.dp)
        )
    }
}
