package com.fitflow.app.ui

import androidx.compose.animation.animateDpAsState
import androidx.compose.animation.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.hapticfeedback.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.fitflow.app.data.Exercise
import com.fitflow.app.data.Mode
import com.fitflow.app.data.Move
import com.fitflow.app.data.Plan
import com.fitflow.app.data.Store
import com.fitflow.app.data.VideoFiles
import com.fitflow.app.engine.Engine
import com.fitflow.app.ui.theme.Caccent
import com.fitflow.app.ui.theme.Cbg
import com.fitflow.app.ui.theme.Cline
import com.fitflow.app.ui.theme.Cmuted
import com.fitflow.app.ui.theme.Csurface
import com.fitflow.app.ui.theme.Csurface2
import com.fitflow.app.ui.theme.Csurface3
import com.fitflow.app.ui.theme.Ctext
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.longPressDraggableHandle
import sh.calvin.reorderable.rememberReorderableLazyListState

/** 编辑页 LazyColumn 里「头部配置区」占用的 item 数量，拖动下标换算时要减掉它 */
private const val HEADER_ITEMS = 1

/** 计划编辑页 */
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
    // 选动作弹窗模式：""=追加新动作；moveId=替换该动作
    var pickMode by remember { mutableStateOf<String?>("") }
    var pickTrigger by remember { mutableStateOf(0) }

    val haptic = LocalHapticFeedback.current
    // 库内部会缓存 onMove 回调，必须用 rememberUpdatedState 才能读到最新的动作列表
    val currentMoves by rememberUpdatedState(plan.moves)

    // 丝滑重排：由 Reorderable 库负责让位动画 / 跟手 / 边缘自动滚动
    val lazyListState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val moves = currentMoves
        if (moves.size >= 2) {
            // LazyColumn 的全局下标含头部配置区，换算成 plan.moves 的下标
            val fromIdx = (from.index - HEADER_ITEMS).coerceIn(0, moves.lastIndex)
            val toIdx = (to.index - HEADER_ITEMS).coerceIn(0, moves.lastIndex)
            if (fromIdx != toIdx) {
                commitMoves(moves.toMutableList().apply { add(toIdx, removeAt(fromIdx)) })
                haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
            }
        }
    }

    Column(Modifier.fillMaxSize().background(Cbg).statusBarsPadding()) {
        TopBar(title = "编辑计划", onBack = { onBack() }, actions = {
            FfButton("开始", { onStartAll(plan.id) }, primary = true)
        })
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            state = lazyListState,
            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
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
                Text("共 ${plan.moves.size} 个动作 · 长按任意动作拖动可调整顺序",
                    style = MaterialTheme.typography.bodySmall, color = Cmuted)
                Spacer(Modifier.size(14.dp))
            }
            itemsIndexed(plan.moves, key = { _, m -> m.id }) { index, move ->
                ReorderableItem(reorderState, key = move.id) { isDragging ->
                    val interactionSource = remember { MutableInteractionSource() }
                    MoveEditorCard(
                        move = move, index = index, totalMoves = plan.moves.size,
                        open = move.id in expanded,
                        onToggle = { expanded = if (move.id in expanded) expanded - move.id else expanded + move.id },
                        onChange = { commitMoves(plan.moves.mapIndexed { i, m -> if (i == index) it else m }) },
                        onPickFigure = { pickMode = move.id; pickTrigger++ },
                        onDelete = {
                            if (plan.moves.size > 1)
                                commitMoves(plan.moves.filterIndexed { i, _ -> i != index })
                        },
                        onTryMove = { onStartMove(plan.id, index) },
                        isDragging = isDragging,
                        // 整张卡片都能长按拖动，和原来的交互一致
                        dragHandle = Modifier.longPressDraggableHandle(
                            onDragStarted = {
                                haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                            },
                            onDragStopped = {
                                haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                            },
                            interactionSource = interactionSource
                        )
                    )
                }
            }
            item {
                Spacer(Modifier.size(6.dp))
                FfButton("+ 添加动作", { pickMode = ""; pickTrigger++ },
                    modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.size(8.dp))
                Text("点「+ 添加动作」从动作库添加（含你自定义的动作）；点动作缩略图可替换动作；点「试练」单独练一组。",
                    style = MaterialTheme.typography.bodySmall, color = Cmuted)
            }
        }
    }

    // 动作库选择弹窗
    if (pickTrigger > 0 && pickMode != null) {
        val library = remember(pickTrigger) { Store.loadLibrary(ctx) }
        var target by remember { mutableStateOf(pickMode) }
        AlertDialog(onDismissRequest = { pickMode = null },
            containerColor = Csurface, titleContentColor = Ctext, textContentColor = Ctext,
            title = { Text(if (target == "") "从动作库添加动作" else "更换动作") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    library.forEach { ex ->
                        LibraryPickRow(ex = ex, onClick = {
                            val mv = ex.toMove()
                            if (target == "") {
                                commitMoves(plan.moves + mv)
                            } else {
                                commitMoves(plan.moves.map {
                                    if (it.id == target) it.copy(name = mv.name, figure = mv.figure,
                                        mode = mv.mode, value = mv.value, sets = mv.sets, rest = mv.rest,
                                        tempo = mv.tempo, tip = mv.tip, videoUri = mv.videoUri)
                                    else it
                                })
                            }
                            pickMode = null
                        })
                    }
                    Text("（自定义动作可先在「动作库」页绑定跟练视频）",
                        style = MaterialTheme.typography.bodySmall, color = Cmuted)
                }
            },
            confirmButton = {
                TextButton(onClick = { pickMode = null }) { Text("取消", color = Cmuted) }
            })
    }
}

@Composable
private fun LibraryPickRow(ex: Exercise, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth()
        .background(Color.Transparent)
        .clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(32.dp, 36.dp)) { StickFigure(ex.figure, 0.25f) }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(ex.name, style = MaterialTheme.typography.bodyLarge, color = Ctext,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(if (VideoFiles.usable(ex.videoUri)) "含跟练视频" else "暂无视频",
                style = MaterialTheme.typography.bodySmall,
                color = if (VideoFiles.usable(ex.videoUri)) Caccent else Cmuted)
        }
        if (ex.videoUri != null) Chip("▶", accent = true)
    }
}

@Composable
private fun MoveEditorCard(move: Move, index: Int, totalMoves: Int, open: Boolean,
                           onToggle: () -> Unit, onChange: (Move) -> Unit,
                           onPickFigure: () -> Unit, onDelete: () -> Unit,
                           onTryMove: () -> Unit,
                           isDragging: Boolean, dragHandle: Modifier) {
    // 拎起来时轻微放大 + 浮起阴影，用 spring 收尾，松手有轻微回弹
    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.03f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "dragScale"
    )
    val elevation by animateDpAsState(
        targetValue = if (isDragging) 10.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "dragElevation"
    )
    val cardShape = RoundedCornerShape(16.dp)
    val cardModifier = Modifier
        .fillMaxWidth()
        .then(dragHandle)
        .zIndex(if (isDragging) 1f else 0f)
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            shadowElevation = elevation.toPx()
            shape = cardShape
        }
        .background(if (isDragging) Csurface3 else Csurface, cardShape)
        .border(1.dp, if (isDragging) Caccent.copy(alpha = 0.6f) else Cline, cardShape)
        .padding(12.dp)
    Column(cardModifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(width = 40.dp, height = 44.dp).clickable(onClick = onPickFigure)) {
                StickFigure(move.figure, 0.25f)
            }
            Spacer(Modifier.size(8.dp))
            Column(Modifier.weight(1f).clickable(onClick = onToggle)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(move.name, style = MaterialTheme.typography.titleMedium, color = Ctext,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    if (VideoFiles.usable(move.videoUri)) {
                        Spacer(Modifier.width(6.dp))
                        Chip("视频", accent = true)
                    }
                }
                Text(moveSummary(move), style = MaterialTheme.typography.bodySmall, color = Cmuted)
            }
            TextButton(onClick = onTryMove) { Text("试练", color = Caccent) }
            IconButton(onClick = onToggle) { Icon(Icons.Default.Create, "展开", tint = Cmuted) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TextButton(onClick = onDelete) { Text("删除", color = Cmuted) }
            Text(if (isDragging) "拖动中…松手确认新位置" else "长按拖动可调整顺序",
                style = MaterialTheme.typography.labelMedium,
                color = if (isDragging) Caccent else Cmuted)
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
    TextButton(onClick = onPickFigure) { Text("更换动作…", color = Caccent) }
    if (VideoFiles.usable(move.videoUri)) {
        Text("🎬 已绑定示范视频（跟练时会播放）", style = MaterialTheme.typography.bodySmall, color = Caccent)
    }
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
