package com.fitflow.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fitflow.app.data.Move
import com.fitflow.app.data.Plan
import com.fitflow.app.data.Store
import com.fitflow.app.engine.Engine
import com.fitflow.app.ui.theme.Caccent
import com.fitflow.app.ui.theme.Cbg
import com.fitflow.app.ui.theme.Cline
import com.fitflow.app.ui.theme.Cmuted
import com.fitflow.app.ui.theme.Csurface
import com.fitflow.app.ui.theme.Csurface2
import com.fitflow.app.ui.theme.Ctext

@Composable
fun PlansScreen(onStartPlan: (String) -> Unit, onEditPlan: (String) -> Unit) {
    val ctx = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    val all = Store.loadPlans(ctx)
    var renaming by remember { mutableStateOf<Plan?>(null) }
    var deleting by remember { mutableStateOf<Plan?>(null) }

    Column(Modifier.fillMaxSize().background(Cbg).statusBarsPadding()) {
        TopBar(title = "FitFlow")
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text("把看到的训练计划", style = MaterialTheme.typography.headlineMedium, color = Ctext)
            Text("变成能直接跟练的", style = MaterialTheme.typography.headlineMedium, color = Ctext)
            Row { Text("节奏", style = MaterialTheme.typography.headlineMedium, color = Caccent) }
            Spacer(Modifier.size(6.dp))
            Text("输入动作、个数、组数与间歇 —— 自动生成带节拍、语音和示范的跟练流程。",
                style = MaterialTheme.typography.bodySmall, color = Cmuted)
        }
        Spacer(Modifier.size(12.dp))
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp)
        ) {
            items(all, key = { it.id }) { plan ->
                PlanCard(
                    plan = plan,
                    onStart = { onStartPlan(plan.id) },
                    onEdit = { onEditPlan(plan.id) },
                    onRename = { renaming = plan },
                    onDelete = { deleting = plan }
                )
            }
        }
    }

    renaming?.let { plan ->
        PromptDialog(title = "重命名计划", initial = plan.name, onDismiss = { renaming = null },
            onConfirm = { v ->
                renaming = null
                if (v.isEmpty()) return@PromptDialog
                Store.savePlans(ctx, Store.loadPlans(ctx).map {
                    if (it.id == plan.id) it.copy(name = v) else it
                })
                tick++
            })
    }
    deleting?.let { plan ->
        ConfirmDialog(title = "删除计划", message = "确定删除「${plan.name}」吗？此操作不可恢复。",
            danger = true, onDismiss = { deleting = null },
            onConfirm = {
                deleting = null
                Store.savePlans(ctx, Store.loadPlans(ctx).filterNot { it.id == plan.id })
                tick++
            })
    }
}

@Composable
private fun PlanCard(plan: Plan, onStart: () -> Unit, onEdit: () -> Unit,
                     onRename: () -> Unit, onDelete: () -> Unit) {
    val total = Engine.planDuration(plan)
    val groups = plan.moves.sumOf { it.sets.coerceAtLeast(1) }

    Column(Modifier.fillMaxWidth()
        .background(Csurface, RoundedCornerShape(18.dp))
        .border(1.dp, Cline, RoundedCornerShape(18.dp))
        .padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(plan.name, style = MaterialTheme.typography.titleMedium, color = Ctext,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            IconButton(onClick = onRename) { Icon(Icons.Default.Create, "重命名", tint = Cmuted) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("⏱ " + humanTime(total), accent = true)
            Chip("${plan.moves.size} 个动作")
            Chip("$groups 组")
        }
        Spacer(Modifier.size(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            plan.moves.take(6).forEach { m ->
                Column(Modifier.width(62.dp).background(Csurface2, RoundedCornerShape(12.dp))
                    .border(1.dp, Cline, RoundedCornerShape(12.dp)).padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(width = 46.dp, height = 42.dp)) { StickFigure(m.figure, 0.3f) }
                    Text(m.name, style = MaterialTheme.typography.labelSmall, color = Cmuted,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Spacer(Modifier.size(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { FfButton("开始跟练", onStart, primary = true, modifier = Modifier.fillMaxWidth()) }
            Box(Modifier.weight(1f)) { FfButton("编辑", onEdit, modifier = Modifier.fillMaxWidth()) }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "删除", tint = Cmuted) }
        }
    }
}

fun humanTime(sec: Double): String {
    val s = sec.toInt()
    return when {
        s < 60 -> "$s 秒"
        s % 60 == 0 -> "${s / 60} 分钟"
        else -> "${s / 60} 分 ${s % 60} 秒"
    }
}
