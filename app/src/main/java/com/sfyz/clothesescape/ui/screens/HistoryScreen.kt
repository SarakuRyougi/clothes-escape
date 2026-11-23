package com.sfyz.clothesescape.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sfyz.clothesescape.data.RecordUi
import com.sfyz.clothesescape.data.TimeUtils
import com.sfyz.clothesescape.ui.UiState
import com.sfyz.clothesescape.ui.components.EmptyHint
import com.sfyz.clothesescape.ui.components.HistoryRow
import com.sfyz.clothesescape.ui.components.NeumSurface
import com.sfyz.clothesescape.ui.theme.LocalNeum

@Composable
fun HistoryScreen(
    state: UiState,
    onOpenRecord: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val neum = LocalNeum.current
    val records = state.records
    val stats = state.stats

    val grouped = records
        .groupBy { TimeUtils.dayKey(it.hungAt) }
        .toSortedMap(compareByDescending { it })

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column {
                Text("晾晒记录", style = MaterialTheme.typography.headlineMedium, color = neum.text)
                Spacer(Modifier.height(3.dp))
                Text(
                    "一共晾过 ${stats.total} 次，丢了 ${stats.lostItems} 件",
                    style = MaterialTheme.typography.bodyMedium,
                    color = neum.textSoft
                )
            }
        }

        item {
            NeumSurface(corner = 24.dp, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row {
                        Metric("本月", "${stats.monthCount}", "次", neum.accent, Modifier.weight(1f))
                        Metric("平均晾晒", "${stats.avgHours}", "小时", neum.text, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                    Row {
                        Metric("丢失记录", "${stats.lostRecords}", "条", neum.danger, Modifier.weight(1f))
                        Metric(
                            "最易丢",
                            if (stats.worstRack != null) "${stats.worstRack}" else "—",
                            if (stats.worstRack != null) "号架" else "",
                            neum.danger,
                            Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        if (records.isEmpty()) {
            item { EmptyHint("还没有记录") }
        }

        grouped.forEach { (day, dayRecords) ->
            item(key = "h_$day") {
                Text(
                    text = dayTitle(day, dayRecords.first().hungAt),
                    style = MaterialTheme.typography.labelLarge,
                    color = neum.textSoft,
                    modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                )
            }
            items(dayRecords, key = { it.id }) { record ->
                HistoryRow(
                    record = record,
                    now = state.now,
                    onClick = { onOpenRecord(record.id) }
                )
            }
        }
    }
}

private fun dayTitle(dayKey: String, sample: Long): String {
    val today = TimeUtils.dayKey(System.currentTimeMillis())
    val yesterday = TimeUtils.dayKey(System.currentTimeMillis() - 86_400_000L)
    return when (dayKey) {
        today -> "今天"
        yesterday -> "昨天"
        else -> TimeUtils.weekday(sample)
    }
}

@Composable
private fun Metric(
    label: String,
    value: String,
    unit: String,
    tint: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    val neum = LocalNeum.current
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = neum.textSoft)
        Spacer(Modifier.height(3.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium,
                color = tint,
                fontWeight = FontWeight.SemiBold
            )
            if (unit.isNotEmpty()) {
                Spacer(Modifier.padding(2.dp))
                Text(unit, style = MaterialTheme.typography.labelMedium, color = neum.textSoft)
            }
        }
    }
}
