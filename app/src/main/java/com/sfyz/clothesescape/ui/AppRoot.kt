package com.sfyz.clothesescape.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sfyz.clothesescape.data.RecordUi
import com.sfyz.clothesescape.data.Slot
import com.sfyz.clothesescape.ui.board.Cell
import com.sfyz.clothesescape.ui.board.Region
import com.sfyz.clothesescape.ui.components.BottomTabs
import com.sfyz.clothesescape.ui.screens.CreateSheet
import com.sfyz.clothesescape.ui.screens.DetailScreen
import com.sfyz.clothesescape.ui.screens.HistoryScreen
import com.sfyz.clothesescape.ui.screens.HomeScreen
import com.sfyz.clothesescape.ui.screens.SettingsScreen
import com.sfyz.clothesescape.ui.theme.LocalNeum
import com.sfyz.clothesescape.ui.theme.softShadow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow

private data class CreateRequest(val cells: List<Cell>, val skipped: Int)

@Composable
fun AppRoot(vm: AppViewModel, openRecord: MutableStateFlow<Long?>) {
    val state by vm.state.collectAsStateWithLifecycle()
    val toast by vm.toast.collectAsStateWithLifecycle()
    val neum = LocalNeum.current
    val context = LocalContext.current

    var tab by rememberSaveable { mutableStateOf(0) }
    var detailId by rememberSaveable { mutableStateOf<Long?>(null) }
    var createRequest by remember { mutableStateOf<CreateRequest?>(null) }
    var lastShown by remember { mutableStateOf<RecordUi?>(null) }

    // Notification permission (Android 13+).
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Notification taps / deep links.
    val incoming by openRecord.collectAsStateWithLifecycle()
    LaunchedEffect(incoming) {
        val id = incoming
        if (id != null) {
            detailId = id
            openRecord.value = null
        }
    }

    LaunchedEffect(toast) {
        if (toast != null) {
            delay(2600)
            vm.consumeToast()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { vm.exportBackup(it) } }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { vm.importBackup(it) } }

    val record = state.records.firstOrNull { it.id == detailId }
    LaunchedEffect(record) {
        if (record != null) lastShown = record
    }

    BackHandler(enabled = detailId != null) { detailId = null }

    Box(
        Modifier
            .fillMaxSize()
            .background(neum.bg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                Crossfade(targetState = tab, label = "tabs") { current ->
                    when (current) {
                        0 -> HomeScreen(
                            state = state,
                            onTapCell = { cell ->
                                val slot = Slot(cell.row + 1, cell.col + 1)
                                val occupant = state.occupied[slot]
                                if (occupant != null) {
                                    detailId = occupant.recordId
                                } else {
                                    createRequest = CreateRequest(listOf(cell), 0)
                                }
                            },
                            onCreateRegion = { cells, skipped ->
                                createRequest = CreateRequest(cells, skipped)
                            },
                            onOpenRecord = { detailId = it }
                        )

                        1 -> HistoryScreen(state = state, onOpenRecord = { detailId = it })

                        else -> SettingsScreen(
                            settings = state.settings,
                            onTheme = { mode -> vm.updateSettings { it.copy(themeMode = mode) } },
                            onSlots = { value -> vm.updateSettings { it.copy(slotCount = value) } },
                            onDays = { value -> vm.updateSettings { it.copy(defaultDays = value) } },
                            onHour = { value -> vm.updateSettings { it.copy(defaultHour = value) } },
                            onMotion = { value -> vm.updateSettings { it.copy(reduceMotion = value) } },
                            onExport = {
                                exportLauncher.launch("clothes-escape-backup.json")
                            },
                            onImport = { importLauncher.launch(arrayOf("application/json", "text/*")) }
                        )
                    }
                }
            }

            BottomTabs(selected = tab, onSelect = { tab = it })
            Spacer(Modifier.height(10.dp))
        }

        AnimatedVisibility(
            visible = record != null,
            enter = fadeIn(tween(180)) +
                slideInHorizontally(tween(240)) { it / 8 } +
                scaleIn(tween(240), initialScale = 0.96f),
            exit = fadeOut(tween(150)) + slideOutHorizontally(tween(180)) { it / 14 }
        ) {
            val shown = record ?: lastShown
            if (shown != null) {
                DetailScreen(
                    record = shown,
                    now = state.now,
                    onBack = { detailId = null },
                    onCollectItem = { itemId -> vm.collectItem(shown.id, itemId) },
                    onLostItem = { itemId -> vm.lostItem(shown.id, itemId) },
                    onCollectAll = { vm.collectAll(shown.id) },
                    onLostAll = { vm.lostAll(shown.id) },
                    onExtend = { days -> vm.extend(shown.id, days) },
                    onCancel = { vm.cancelRecord(shown.id); detailId = null },
                    onDelete = { vm.deleteRecord(shown.id); detailId = null }
                )
            }
        }

        createRequest?.let { request ->
            CreateSheet(
                slots = request.cells.map { Slot(it.row + 1, it.col + 1) },
                skipped = request.skipped,
                recentNames = state.recentNames,
                defaultDays = state.settings.defaultDays,
                defaultHour = state.settings.defaultHour,
                onDismiss = { createRequest = null },
                onSave = { names, photo, days, hour ->
                    vm.createRecord(
                        region = request.cells.map { Slot(it.row + 1, it.col + 1) },
                        names = names,
                        photoPath = photo,
                        days = days,
                        hour = hour
                    )
                    createRequest = null
                }
            )
        }

        AnimatedVisibility(
            visible = toast != null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp),
            enter = fadeIn(tween(160)) + slideInVertically(tween(220)) { it / 2 },
            exit = fadeOut(tween(140)) + slideOutVertically(tween(180)) { it / 3 }
        ) {
            Box(
                Modifier
                    .softShadow(corner = 18.dp, offset = 4.dp)
                    .background(neum.text, RoundedCornerShape(18.dp))
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                Text(
                    text = toast ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = neum.bg
                )
            }
        }
    }
}

