package com.fitflow.app

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.fitflow.app.audio.AudioEngine
import com.fitflow.app.ui.EditorScreen
import com.fitflow.app.ui.LibraryScreen
import com.fitflow.app.ui.PlansScreen
import com.fitflow.app.ui.PlayerScreen
import com.fitflow.app.ui.theme.Caccent
import com.fitflow.app.ui.theme.Cbg
import com.fitflow.app.ui.theme.Cline
import com.fitflow.app.ui.theme.Cmuted
import com.fitflow.app.ui.theme.Csurface
import com.fitflow.app.ui.theme.Ctext
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

    // 底部主区页签：0=我的计划 1=动作库
    var tab by remember { mutableIntStateOf(0) }

    BackHandler {
        when (val cur = screen) {
            is Screen.Play -> { audio.stopAll(); screen = prev }
            is Screen.Edit -> screen = Screen.Home
            Screen.Home -> {
                if (tab != 0) tab = 0
                else (ctx as? Activity)?.finish()
            }
        }
    }

    when (val cur = screen) {
        Screen.Home -> Column(Modifier.fillMaxSize().background(Cbg)) {
            Box(Modifier.weight(1f)) {
                when (tab) {
                    0 -> PlansScreen(
                        onStartPlan = { nav(Screen.Play(it, null)) },
                        onEditPlan = { nav(Screen.Edit(it)) },
                        onGoLibrary = { tab = 1 }
                    )
                    else -> LibraryScreen()
                }
            }
            HomeNavBar(tab) { tab = it }
        }
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

@Composable
private fun HomeNavBar(tab: Int, onChange: (Int) -> Unit) {
    NavigationBar(containerColor = Csurface) {
        listOf("我的计划" to 0, "动作库" to 1).forEach { (label, idx) ->
            val selected = tab == idx
            NavigationBarItem(
                selected = selected,
                onClick = { onChange(idx) },
                icon = {
                    if (idx == 0) Icon(Icons.Default.Home, null, tint = if (selected) Caccent else Cmuted)
                    else Icon(Icons.Default.List, null, tint = if (selected) Caccent else Cmuted)
                },
                label = { Text(label, color = if (selected) Ctext else Cmuted) },
                colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                    indicatorColor = Caccent.copy(alpha = 0.18f),
                    selectedTextColor = Ctext,
                    selectedIconColor = Caccent
                )
            )
        }
    }
}
