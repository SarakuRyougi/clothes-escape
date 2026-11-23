package com.sfyz.clothesescape.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sfyz.clothesescape.data.ITEM_COLLECTED
import com.sfyz.clothesescape.data.ITEM_LOST
import com.sfyz.clothesescape.data.LaundryRepository
import com.sfyz.clothesescape.data.RecordUi
import com.sfyz.clothesescape.data.Settings
import com.sfyz.clothesescape.data.SettingsStore
import com.sfyz.clothesescape.data.Slot
import com.sfyz.clothesescape.data.Stats
import com.sfyz.clothesescape.data.StatsCalculator
import com.sfyz.clothesescape.data.TimeUtils
import com.sfyz.clothesescape.notify.ReminderScheduler
import com.sfyz.clothesescape.ui.board.BoardOccupant
import com.sfyz.clothesescape.util.BackupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.net.Uri

data class UiState(
    val loading: Boolean = true,
    val records: List<RecordUi> = emptyList(),
    val occupied: Map<Slot, BoardOccupant> = emptyMap(),
    val recentNames: List<String> = emptyList(),
    val stats: Stats = Stats(),
    val now: Long = System.currentTimeMillis(),
    val settings: Settings = Settings()
) {
    val activeRecords: List<RecordUi> get() = records.filter { it.active }
    val history: List<RecordUi> get() = records.filter { !it.active }
    val overdueCount: Int get() = records.count { it.isOverdue(now) }
}

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = LaundryRepository(app)
    private val settingsStore = SettingsStore(app)
    private val backup = BackupManager(app)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    val settings: StateFlow<Settings> = settingsStore.state

    init {
        refresh()
        viewModelScope.launch {
            settingsStore.state.collect { s ->
                _state.update { it.copy(settings = s) }
            }
        }
        viewModelScope.launch {
            while (isActive) {
                delay(20_000L)
                _state.update { it.copy(now = System.currentTimeMillis()) }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) { repo.loadAll() }
            val recent = withContext(Dispatchers.IO) { repo.recentNames() }
            val now = System.currentTimeMillis()
            _state.update {
                it.copy(
                    loading = false,
                    records = loaded,
                    occupied = buildOccupied(loaded, now),
                    recentNames = recent,
                    stats = StatsCalculator.compute(loaded, now),
                    now = now,
                    settings = settingsStore.state.value
                )
            }
        }
    }

    private fun buildOccupied(records: List<RecordUi>, now: Long): Map<Slot, BoardOccupant> {
        val map = HashMap<Slot, BoardOccupant>()
        records.filter { it.active }.forEach { record ->
            val occupant = BoardOccupant(
                recordId = record.id,
                accentIndex = record.accentIndex,
                count = record.items.size.coerceAtLeast(1),
                overdue = record.isOverdue(now)
            )
            record.slots.forEach { map[it] = occupant }
        }
        return map
    }

    // ------------------------------------------------------------ commands

    fun createRecord(
        region: List<Slot>,
        names: List<String>,
        photoPath: String?,
        days: Int,
        hour: Int
    ) {
        if (region.isEmpty()) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val due = TimeUtils.dueAt(now, days, hour)
            withContext(Dispatchers.IO) {
                val id = repo.createRecord(
                    region = region,
                    garmentNames = names,
                    photoPath = photoPath,
                    note = "",
                    hungAt = now,
                    dueAt = due,
                    reminderAt = due
                )
                ReminderScheduler.schedule(getApplication(), id, due)
            }
            _toast.value = "已挂上 ${region.size} 个位置，${TimeUtils.full(due)} 提醒你"
            refresh()
        }
    }

    fun collectAll(recordId: Long) = mutate {
        repo.collectAll(recordId)
        ReminderScheduler.cancel(getApplication(), recordId)
        _toast.value = "收好了，衣服回家啦"
    }

    fun lostAll(recordId: Long) = mutate {
        repo.lostAll(recordId)
        ReminderScheduler.cancel(getApplication(), recordId)
        _toast.value = "已记为丢失，看看统计里哪一格最容易丢"
    }

    fun collectItem(recordId: Long, itemId: Long) = mutate {
        repo.setItemState(recordId, itemId, ITEM_COLLECTED)
    }

    fun lostItem(recordId: Long, itemId: Long) = mutate {
        repo.setItemState(recordId, itemId, ITEM_LOST)
    }

    fun extend(recordId: Long, days: Int) = mutate {
        repo.extend(recordId, days)
        ReminderScheduler.resync(getApplication())
        _toast.value = "提醒已推迟 $days 天"
    }

    fun cancelRecord(recordId: Long) = mutate {
        repo.cancel(recordId)
        ReminderScheduler.cancel(getApplication(), recordId)
        _toast.value = "这条记录已取消"
    }

    fun deleteRecord(recordId: Long) = mutate {
        repo.deleteRecord(recordId)
        ReminderScheduler.cancel(getApplication(), recordId)
        _toast.value = "记录已删除"
    }

    fun updateSettings(transform: (Settings) -> Settings) {
        settingsStore.update(transform)
    }

    fun requestResync() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { ReminderScheduler.resync(getApplication()) }
            _toast.value = "提醒已重新排好"
        }
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            val count = withContext(Dispatchers.IO) {
                runCatching { backup.exportTo(getApplication(), uri) }.getOrDefault(-1)
            }
            _toast.value = if (count >= 0) "已导出 $count 条记录" else "导出失败"
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            val count = withContext(Dispatchers.IO) {
                runCatching { backup.importFrom(getApplication(), uri) }.getOrDefault(-1)
            }
            if (count >= 0) {
                withContext(Dispatchers.IO) { ReminderScheduler.resync(getApplication()) }
                _toast.value = "已恢复 $count 条记录"
                refresh()
            } else {
                _toast.value = "导入失败，文件可能不完整"
            }
        }
    }

    fun consumeToast() {
        _toast.value = null
    }

    private fun mutate(block: suspend () -> Unit) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { block() }
            refresh()
        }
    }
}
