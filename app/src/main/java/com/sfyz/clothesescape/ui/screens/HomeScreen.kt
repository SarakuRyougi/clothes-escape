package com.sfyz.clothesescape.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.sfyz.clothesescape.ui.UiState
import com.sfyz.clothesescape.ui.board.Cell
import com.sfyz.clothesescape.ui.board.RackBoard
import com.sfyz.clothesescape.ui.board.Region
import com.sfyz.clothesescape.ui.components.EmptyHint
import com.sfyz.clothesescape.ui.components.LegendDot
import com.sfyz.clothesescape.ui.components.PopIn
import com.sfyz.clothesescape.ui.components.RecordCard
import com.sfyz.clothesescape.ui.components.ShirtBadge
import com.sfyz.clothesescape.ui.components.StatPill
import com.sfyz.clothesescape.ui.theme.LocalNeum
import com.sfyz.clothesescape.ui.theme.LocalReduceMotion

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    state: UiState,
    onTapCell: (Cell) -> Unit,
    onCreateRegion: (List<Cell>, Int) -> Unit,
    onOpenRecord: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val neum = LocalNeum.current
    val reduceMotion = LocalReduceMotion.current
    val active = state.activeRecords

    Column(modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 22.dp, end = 22.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = buildAnnotatedString {
                        append("衣服要逃走了")
                        // The bundled face has no emoji glyph; keep the splash
                        // character on the system family so it never shows tofu.
                        withStyle(SpanStyle(fontFamily = FontFamily.Default)) {
                            append("💦")
                        }
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    color = neum.text
                )
            }
            ShirtBadge(size = 40.dp)
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatPill(
                value = active.size.toString(),
                label = "处晾晒中",
                tint = neum.accent
            )
            if (state.overdueCount > 0) {
                StatPill(
                    value = state.overdueCount.toString(),
                    label = "件逾期",
                    tint = neum.danger
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        RackBoard(
            occupied = state.occupied,
            cols = state.settings.slotCount,
            onTapCell = onTapCell,
            onCreateRegion = onCreateRegion,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LegendDot(neum.accent, "晾晒中")
            LegendDot(neum.danger, "逾期")
        }

        if (active.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 22.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(active, key = { it.id }) { record ->
                    PopIn(
                        reduceMotion = reduceMotion,
                        modifier = Modifier.animateItemPlacement()
                    ) {
                        RecordCard(
                            record = record,
                            now = state.now,
                            onClick = { onOpenRecord(record.id) }
                        )
                    }
                }
            }
        } else {
            EmptyHint("还没有晾着的衣服")
        }

        Spacer(Modifier.height(10.dp))
    }
}
