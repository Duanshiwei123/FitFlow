package com.fitflow.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fitflow.app.data.Mode
import com.fitflow.app.data.Move
import com.fitflow.app.data.MOVE_LIBRARY
import com.fitflow.app.data.Plan
import com.fitflow.app.data.Store
import com.fitflow.app.data.libMove
import com.fitflow.app.data.newMoveFromLib
import com.fitflow.app.engine.Engine
import com.fitflow.app.ui.theme.Caccent
import com.fitflow.app.ui.theme.Cbg
import com.fitflow.app.ui.theme.Cline
import com.fitflow.app.ui.theme.Cmuted
import com.fitflow.app.ui.theme.Csurface
import com.fitflow.app.ui.theme.Csurface2
import com.fitflow.app.ui.theme.Csurface3
import com.fitflow.app.ui.theme.Ctext

@Composable
fun EditorScreen(planId: String, onBack: () -> Unit, onStartAll: (String) -> Unit,
                 onStartMove: (String, Int) -> Unit) {
    val ctx = LocalContext.current
    var plans by remember { mutableStateOf(Store.loadPlans(ctx)) }
    val plan = plans.firstOrNull { it.id == planId }
    if (plan == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    fun commit(newPlan: Plan) {
        plans = plans.map { if (it.id == planId) newPlan else it }
        Store.savePlans(ctx, plans)
    }
    fun commitMoves(newMoves: List<Move>) = commit(plan.copy(moves = newMoves))

    var expanded by remember { mutableStateOf(setOf<String>()) }
    var libPickMoveId by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().background(Cbg).statusBarsPadding()) {
        TopBar(title = "编辑计划", onBack = { onBack() }, actions = {
            FfButton("开始", { onStartAll(plan.id) }, primary = true)
        })
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 40.dp)) {
            OutlinedTextField(value = plan.name, onValueChange = { commit(plan.copy(name = it)) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                label = { Text("计划名称") }, textStyle = MaterialTheme.typography.titleMedium, colors = tfColors())
            Spacer(Modifier.size(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f)) { NumberField("开始前准备", plan.prep, "秒", 0, 300, 5) { commit(plan.copy(prep = it)) } }
                Column(Modifier.weight(1f)) { NumberField("动作间休息", plan.moveRest, "秒", 0, 300, 5) { commit(plan.copy(moveRest = it)) } }
            }
            Spacer(Modifier.size(10.dp))
            Row { Chip("总时长 " + humanTime(Engine.planDuration(plan)), accent = true) }
            Spacer(Modifier.size(6.dp))
            Text("共 ${plan.moves.size} 个动作 · 训练/休息按下方顺序执行",
                style = MaterialTheme.typography.bodySmall, color = Cmuted)
            Spacer(Modifier.size(14.dp))

            plan.moves.forEachIndexed { index, move ->
                MoveEditorCard(
                    move = move, index = index, totalMoves = plan.moves.size,
                    open = move.id in expanded,
                    onToggle = { expanded = if (move.id in expanded) expanded - move.id else expanded + move.id },
                    onChange = { commitMoves(plan.moves.mapIndexed { i, m -> if (i == index) it else m }) },
                    onPickFigure = { libPickMoveId = move.id },
                    onDelete = {
                        if (plan.moves.size > 1)
                            commitMoves(plan.moves.filterIndexed { i, _ -> i != index })
                    },
                    onMoveUp = {
                        if (index > 0) {
                            val l = plan.moves.toMutableList()
                            val t = l[index]; l[index] = l[index - 1]; l[index - 1] = t
                            commitMoves(l)
                        }
                    },
                    onMoveDown = {
                        if (index < plan.moves.size - 1) {
                            val l = plan.moves.toMutableList()
                            val t = l[index]; l[index] = l[index + 1]; l[index + 1] = t
                            commitMoves(l)
                        }
                    },
                    onTryMove = { onStartMove(plan.id, index) }
                )
                Spacer(Modifier.size(10.dp))
            }

            Spacer(Modifier.size(6.dp))
            FfButton("+ 添加动作", { commitMoves(plan.moves + newMoveFromLib("jumpingJack")) },
                modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.size(8.dp))
            Text("点动作右侧铅笔展开编辑；点缩略图换动作类型；点「试练」单独练这一组。",
                style = MaterialTheme.typography.bodySmall, color = Cmuted)
        }
    }

    // 动作库选择
    libPickMoveId?.let { moveId ->
        val mv = plan.moves.firstOrNull { it.id == moveId } ?: return@let
        var selectKey by remember(moveId) { mutableStateOf(mv.figure) }
        AlertDialog(onDismissRequest = { libPickMoveId = null }, containerColor = Csurface,
            titleContentColor = Ctext, textContentColor = Ctext, title = { Text("选择动作") },
            text = {
                Column(Modifier.horizontalScroll(rememberScrollState())) {
                    MOVE_LIBRARY.forEach { l ->
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                            .background(if (selectKey == l.key) Csurface3 else Color.Transparent)
                            .clickable { selectKey = l.key }.padding(vertical = 8.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(30.dp, 34.dp)) { StickFigure(l.key, 0.25f) }
                            Spacer(Modifier.size(8.dp))
                            Text(l.name, style = MaterialTheme.typography.bodyLarge, color = Ctext)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    libPickMoveId = null
                    val l = libMove(selectKey)
                    commitMoves(plan.moves.map {
                        if (it.id == moveId) it.copy(name = l.name, figure = l.key, mode = l.mode,
                            value = l.value, sets = l.sets, rest = l.rest, tempo = l.tempo, tip = l.tip)
                        else it
                    })
                }) { Text("应用", color = Caccent, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { libPickMoveId = null }) { Text("取消", color = Cmuted) } })
    }
}

@Composable
private fun MoveEditorCard(move: Move, index: Int, totalMoves: Int, open: Boolean,
                           onToggle: () -> Unit, onChange: (Move) -> Unit,
                           onPickFigure: () -> Unit, onDelete: () -> Unit,
                           onMoveUp: () -> Unit, onMoveDown: () -> Unit, onTryMove: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(Csurface, RoundedCornerShape(16.dp))
        .border(1.dp, Cline, RoundedCornerShape(16.dp)).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(width = 40.dp, height = 44.dp).clickable(onClick = onPickFigure)) {
                StickFigure(move.figure, 0.25f)
            }
            Spacer(Modifier.size(8.dp))
            Column(Modifier.weight(1f).clickable(onClick = onToggle)) {
                Text(move.name, style = MaterialTheme.typography.titleMedium, color = Ctext,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(moveSummary(move), style = MaterialTheme.typography.bodySmall, color = Cmuted)
            }
            TextButton(onClick = onTryMove) { Text("试练", color = Caccent) }
            IconButton(onClick = onToggle) { Icon(Icons.Default.Create, "展开", tint = Cmuted) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TextButton(onClick = onMoveUp, enabled = index > 0) { Text("上移", color = Cmuted) }
            TextButton(onClick = onMoveDown, enabled = index < totalMoves - 1) { Text("下移", color = Cmuted) }
            TextButton(onClick = onDelete) { Text("删除", color = Cmuted) }
        }
        if (open) {
            Spacer(Modifier.size(4.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Cline))
            Spacer(Modifier.size(10.dp))
            MoveFields(move, onChange, onPickFigure)
        }
    }
}

@Composable
private fun MoveFields(move: Move, onChange: (Move) -> Unit, onPickFigure: () -> Unit) {
    SegmentRow(listOf("按个数", "按时间"), if (move.mode == Mode.REPS) 0 else 1) { i ->
        val mode = if (i == 0) Mode.REPS else Mode.TIME
        val v = if (mode == Mode.REPS && move.value > 200) 15
        else if (mode == Mode.TIME && move.value < 10) 30 else move.value
        onChange(move.copy(mode = mode, value = v))
    }
    Spacer(Modifier.size(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f)) {
            NumberField(if (move.mode == Mode.REPS) "每组个数" else "每组时长", move.value,
                if (move.mode == Mode.REPS) "个" else "秒",
                min = 1, max = if (move.mode == Mode.REPS) 300 else 900) { onChange(move.copy(value = it)) }
        }
        Column(Modifier.weight(1f)) {
            NumberFieldDouble(if (move.mode == Mode.REPS) "每个动作耗时" else "节拍间隔",
                move.tempo, 0.3, 20.0) { onChange(move.copy(tempo = it)) }
        }
    }
    Spacer(Modifier.size(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f)) { NumberField("组数", move.sets, "组", 1, 20, 1) { onChange(move.copy(sets = it)) } }
        Column(Modifier.weight(1f)) { NumberField("组间休息", move.rest, "秒", 0, 300, 5) { onChange(move.copy(rest = it)) } }
    }
    Spacer(Modifier.size(10.dp))
    OutlinedTextField(value = move.tip, onValueChange = { onChange(move.copy(tip = it)) },
        modifier = Modifier.fillMaxWidth(), label = { Text("动作要点（开练前播报）") },
        textStyle = MaterialTheme.typography.bodyMedium, colors = tfColors())
    Spacer(Modifier.size(8.dp))
    TextButton(onClick = onPickFigure) { Text("更换动作类型…", color = Caccent) }
}

@Composable
private fun NumberFieldDouble(label: String, value: Double, min: Double, max: Double,
                              onChange: (Double) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Cmuted)
        Spacer(Modifier.size(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StepButton("-") { onChange(((value - 0.5).coerceAtLeast(min))) }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (value == value.toLong().toDouble()) value.toInt().toString()
                else "%.1f".format(value), style = MaterialTheme.typography.titleLarge,
                    color = Ctext, fontWeight = FontWeight.ExtraBold)
                Text("秒", style = MaterialTheme.typography.labelMedium, color = Cmuted)
            }
            StepButton("+") { onChange(((value + 0.5).coerceAtMost(max))) }
        }
    }
}

private fun moveSummary(m: Move): String {
    val v = if (m.mode == Mode.REPS) "${m.value} 个 × " else "${m.value} 秒 × "
    return "$v${m.sets} 组 · 组休 ${m.rest} 秒"
}
