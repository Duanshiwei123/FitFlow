package com.fitflow.app

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.fitflow.app.audio.AudioEngine
import com.fitflow.app.ui.EditorScreen
import com.fitflow.app.ui.PlansScreen
import com.fitflow.app.ui.PlayerScreen
import com.fitflow.app.ui.theme.FitFlowTheme

sealed interface Screen {
    object Home : Screen
    data class Edit(val planId: String) : Screen
    data class Play(val planId: String, val onlyIndex: Int?) : Screen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FitFlowTheme { FitFlowApp() } }
    }
}

@Composable
private fun FitFlowApp() {
    val ctx = LocalContext.current
    val audio = remember { AudioEngine(ctx) }
    DisposableEffect(Unit) { onDispose { audio.shutdown() } }

    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    var prev by remember { mutableStateOf<Screen>(Screen.Home) }
    fun nav(s: Screen) { prev = screen; screen = s }

    BackHandler {
        when (val cur = screen) {
            is Screen.Play -> { audio.stopAll(); screen = prev }
            is Screen.Edit -> screen = Screen.Home
            Screen.Home -> (ctx as? Activity)?.finish()
        }
    }

    when (val cur = screen) {
        Screen.Home -> PlansScreen(
            onStartPlan = { nav(Screen.Play(it, null)) },
            onEditPlan = { nav(Screen.Edit(it)) }
        )
        is Screen.Edit -> EditorScreen(
            planId = cur.planId,
            onBack = { screen = Screen.Home },
            onStartAll = { nav(Screen.Play(it, null)) },
            onStartMove = { id, mi -> nav(Screen.Play(id, mi)) }
        )
        is Screen.Play -> PlayerScreen(
            planId = cur.planId,
            onlyIndex = cur.onlyIndex,
            audio = audio,
            onExit = { audio.stopAll(); screen = prev }
        )
    }
}
