package com.sfyz.clothesescape

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sfyz.clothesescape.ui.AppRoot
import com.sfyz.clothesescape.ui.AppViewModel
import com.sfyz.clothesescape.ui.theme.AppFonts
import com.sfyz.clothesescape.ui.theme.ClothesEscapeTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    private val openRecord = MutableStateFlow<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        openRecord.value = intent?.getLongExtra(EXTRA_RECORD_ID, -1L)?.takeIf { it > 0L }

        setContent {
            val vm: AppViewModel = viewModel()
            val settings by vm.settings.collectAsStateWithLifecycle()
            val context = LocalContext.current
            val family = remember(context) { AppFonts.composeFamily(context) }
            val typeface = remember(context) { AppFonts.typeface(context) }
            ClothesEscapeTheme(
                mode = settings.themeMode,
                reduceMotion = settings.reduceMotion,
                fontFamily = family,
                typeface = typeface
            ) {
                AppRoot(vm = vm, openRecord = openRecord)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getLongExtra(EXTRA_RECORD_ID, -1L).takeIf { it > 0L }?.let { openRecord.value = it }
    }

    companion object {
        const val EXTRA_RECORD_ID = "record_id"
    }
}
