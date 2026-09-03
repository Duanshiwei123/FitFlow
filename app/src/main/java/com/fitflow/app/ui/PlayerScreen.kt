package com.fitflow.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitflow.app.audio.AudioEngine
import com.fitflow.app.data.Mode
import com.fitflow.app.data.Move
import com.fitflow.app.data.Plan
import com.fitflow.app.data.Store
import com.fitflow.app.data.VideoFiles
import com.fitflow.app.engine.Engine
import com.fitflow.app.engine.Ev
import com.fitflow.app.engine.EvType
import com.fitflow.app.engine.Step
import com.fitflow.app.engine.StepKind
import com.fitflow.app.ui.theme.Caccent
import com.fitflow.app.ui.theme.Caccent2
import com.fitflow.app.ui.theme.Cbg
import com.fitflow.app.ui.theme.Cline
import com.fitflow.app.ui.theme.Cmuted
import com.fitflow.app.ui.theme.Csurface2
import com.fitflow.app.ui.theme.Ctext
import com.fitflow.app.ui.theme.Cwarn
import kotlinx.coroutines.delay
import kotlin.math.ceil
import kotlin.math.floor

@Composable
fun PlayerScreen(planId: String, onlyIndex: Int?, audio: AudioEngine, onExit: () -> Unit) {
    val ctx = LocalContext.current
    val plan = remember { Store.loadPlans(ctx).firstOrNull { it.id == planId } }
    if (plan == null) {
        LaunchedEffect(Unit) { onExit() }
        return
    }
    val settings = remember { Store.loadSettings(ctx) }
    audio.voiceOn = settings.voice

    val source = remember(planId, onlyIndex) {
        val mv = onlyIndex?.let { plan.moves.getOrNull(it) }
        if (mv != null) plan.copy(prep = plan.prep, moveRest = 0, moves = listOf(mv)) else plan
    }
    val steps = remember(source.id) { Engine.buildTimeline(source) }
    val totalDur = steps.sumOf { it.duration }

    var idx by remember { mutableIntStateOf(0) }
    var stepStart by remember { mutableFloatStateOf(0f) }
    var events by remember { mutableStateOf<List<Ev>>(emptyList()) }
    var evIdx by remember { mutableIntStateOf(0) }
    var paused by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }

    var remain by remember { mutableFloatStateOf(0f) }
    var progress by remember { mutableFloatStateOf(0f) }
    var overall by remember { mutableFloatStateOf(0f) }
    var repDone by remember { mutableIntStateOf(0) }
    var phase by remember { mutableFloatStateOf(0f) }
    var lastBeatT by remember { mutableFloatStateOf(-1f) }

    fun fakeMove(s: Step) = Move("", s.title, s.mode, s.target, 1, 0, s.tempo, s.figure)

    fun enterStep(i: Int) {
        idx = i.coerceIn(0, steps.size - 1)
        val st = steps[idx]
        audio.stopAll()
        events = Engine.buildEvents(st, settings)
        evIdx = 0
        stepStart = audio.now().toFloat()
    }

    /** 当前训练组是否正在放示范视频（有视频时跳过合成音/语音，只听视频原声） */
    fun demoActive(): Boolean {
        val s = steps[idx.coerceIn(0, steps.size - 1)]
        if (s.kind != StepKind.WORK) return false
        return source.moves.getOrNull(s.moveIndex)?.videoUri?.let { VideoFiles.usable(it) } == true
    }

    fun fire(e: Ev) {
        if (demoActive()) return
        // 节拍器已移除：不再播放 beat / beatAccent 提示音
        if (e.sound == "beat" || e.sound == "beatAccent") return
        if (e.type == EvType.SPEAK) audio.speak(e.text)
        else audio.cue(e.sound)
    }

    fun refreshView(el: Float) {
        val st = steps[idx.coerceIn(0, steps.size - 1)]
        remain = (st.duration - el).toFloat().coerceAtLeast(0f)
        progress = if (st.duration > 0) (el / st.duration).toFloat().coerceIn(0f, 1f) else 1f
        val passed = st.startAt + el.toDouble().coerceAtMost(st.duration)
        overall = (passed / totalDur).toFloat().coerceIn(0f, 1f)
        if (st.kind == StepKind.WORK && st.mode == Mode.REPS && st.tempo > 0)
            repDone = floor(el / st.tempo).toInt().coerceIn(0, st.target)
        val dur = Engine.animDuration(fakeMove(st))
        phase = if (dur > 0) ((el % dur) / dur).toFloat() else 0f
    }

    fun tick() {
        val st = steps[idx.coerceIn(0, steps.size - 1)]
        val el = audio.now() - stepStart
        while (evIdx < events.size && events[evIdx].t <= el) {
            fire(events[evIdx]); evIdx++
        }
        if (el >= st.duration) {
            if (idx >= steps.size - 1) { audio.cue("finish"); finished = true }
            else { enterStep(idx + 1); refreshView(0f) }
            return
        }
        refreshView(el.toFloat())
    }

    fun skip(delta: Int) {
        if (delta < 0 && (audio.now() - stepStart) > 2.5) { enterStep(idx); return }
        enterStep((idx + delta).coerceIn(0, steps.size - 1))
    }

    LaunchedEffect(Unit) {
        audio.reset()
        enterStep(0)
        refreshView(0f)
        while (true) {
            when {
                finished -> { delay(200) }
                paused -> { refreshView(0f); delay(50) }
                else -> { tick(); delay(50) }
            }
        }
    }

    DisposableEffect(Unit) { onDispose { audio.stopAll() } }

    val st = steps[idx.coerceIn(0, steps.size - 1)]
    val isRest = st.kind == StepKind.REST || st.kind == StepKind.PREP
    val isWork = st.kind == StepKind.WORK
    val warn = isWork && remain <= 3f
    val glow = if (isWork) Caccent else if (isRest) Caccent2 else Cwarn
    val numColor = when { warn -> Cwarn; isRest -> Caccent2; else -> Ctext }
    val showRep = isWork && st.mode == Mode.REPS
    val bigNum = if (showRep) repDone.toString() else ceil(remain).toInt().toString()
    val unit = if (showRep) "/ ${st.target} 个" else "秒"
    val beatGlow = lastBeatT >= 0 && (audio.now() - lastBeatT) < 0.16f

    // 当前训练组若有可用示范视频，则优先播放视频代替火柴人
    val curMove = source.moves.getOrNull(st.moveIndex)
    val demoVideo = if (isWork) curMove?.videoUri?.takeIf { VideoFiles.usable(it) } else null

    Column(Modifier.fillMaxSize().background(Cbg).navigationBarsPadding()) {
        Column(Modifier.fillMaxWidth().statusBarsPadding()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onExit) { Icon(Icons.Default.ArrowBack, "退出", tint = Cmuted) }
                Text(plan.name + (if (onlyIndex != null) " · 单动作" else ""),
                    style = MaterialTheme.typography.labelLarge, color = Cmuted,
                    textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                Text("${idx + 1}/${steps.size}", style = MaterialTheme.typography.labelLarge, color = Cmuted,
                    modifier = Modifier.padding(end = 14.dp))
            }
            Box(Modifier.fillMaxWidth().padding(horizontal = 14.dp).height(3.dp).background(Color(0x14FFFFFF))) {
                Box(Modifier.fillMaxWidth(overall).height(3.dp).background(Caccent))
            }
        }
        Box(Modifier.fillMaxWidth().aspectRatio(4.2f).background(
            Brush.verticalGradient(listOf(glow.copy(alpha = 0.14f), Color.Transparent))))

        if (finished) {
            FinishBody(
                modifier = Modifier.weight(1f),
                totalDur = totalDur,
                workCount = steps.count { it.kind == StepKind.WORK },
                moveCount = steps.filter { it.kind == StepKind.WORK }.map { it.title }.distinct().size,
                onRestart = {
                    finished = false; paused = false
                    audio.reset(); enterStep(0); refreshView(0f)
                },
                onClose = { onExit() }
            )
        } else {
            Column(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                if (demoVideo != null) {
                    // 训练组：播放用户绑定的示范视频（弹性占满剩余高度）
                    Box(Modifier.fillMaxWidth().weight(1f).padding(top = 4.dp, bottom = 2.dp)) {
                        LoopVideoPlayer(
                            uri = demoVideo,
                            playing = !paused && !finished,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Text(st.title, style = MaterialTheme.typography.titleLarge, color = Ctext)
                    Text(st.sub, style = MaterialTheme.typography.bodyMedium, color = Cmuted)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                        Box(Modifier.size(width = 96.dp, height = 96.dp), contentAlignment = Alignment.Center) {
                            Ring(progress = progress, color = Caccent)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(bigNum, color = numColor, fontWeight = FontWeight.ExtraBold,
                                    fontSize = 34.sp, lineHeight = 38.sp,
                                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                                Text(unit, color = Cmuted, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Spacer(Modifier.size(16.dp))
                        if (st.next.isNotEmpty())
                            Text("接下来\n${st.next}", color = Cmuted, style = MaterialTheme.typography.bodySmall,
                                maxLines = 2)
                    }
                } else {
                    Box(Modifier.fillMaxWidth().aspectRatio(1.25f).padding(top = 4.dp)
                        .background(Color(0x0AFFFFFF), RoundedCornerShape(22.dp))
                        .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(22.dp))) {
                        StickFigure(st.figure, phase, Modifier.fillMaxSize().padding(6.dp))
                    }
                    Spacer(Modifier.size(8.dp))
                    Text(st.title, style = MaterialTheme.typography.titleLarge, color = Ctext)
                    Text(st.sub, style = MaterialTheme.typography.bodyMedium, color = Cmuted)
                    Spacer(Modifier.size(4.dp))
                    Box(Modifier.size(210.dp), contentAlignment = Alignment.Center) {
                        Ring(progress = progress, color = if (isRest) Caccent2 else Caccent)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(bigNum, color = numColor, fontWeight = FontWeight.ExtraBold,
                                fontSize = 64.sp, lineHeight = 72.sp,
                                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                            Text(unit, color = Cmuted, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Spacer(Modifier.size(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).background(
                            if (beatGlow && settings.beat) Caccent else Color(0x40FFFFFF), CircleShape))
                        Spacer(Modifier.size(8.dp))
                        if (st.next.isNotEmpty())
                            Text("接下来 ${st.next}", color = Cmuted, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                CtrlRound(58, Csurface2, onClick = { skip(-1) }) { TriangleIcon(left = true) }
                Spacer(Modifier.size(18.dp))
                CtrlRound(76, Caccent, onClick = {
                    paused = !paused
                    if (paused) audio.pause() else audio.resume()
                }) { PlayPauseIcon(paused, Color(0xFF10130A)) }
                Spacer(Modifier.size(18.dp))
                CtrlRound(58, Csurface2, onClick = { skip(1) }) { TriangleIcon(left = false) }
            }
        }
    }
}

@Composable
private fun TriangleIcon(left: Boolean) {
    Box(Modifier.size(22.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val p = Path().apply {
                val bx = if (left) size.width * 0.74f else size.width * 0.26f
                val tx = if (left) size.width * 0.26f else size.width * 0.74f
                moveTo(bx, size.height * 0.2f)
                lineTo(tx, size.height * 0.5f)
                lineTo(bx, size.height * 0.8f)
                close()
            }
            drawPath(p, Ctext)
        }
    }
}

@Composable
private fun CtrlRound(size: Int, color: Color, onClick: () -> Unit,
                     content: @Composable () -> Unit = {}) {
    Box(Modifier.size(size.dp).background(color, CircleShape)
        .border(1.dp, if (color == Caccent) Color.Transparent else Cline, CircleShape)
        .clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        content()
    }
}

@Composable
private fun PlayPauseIcon(paused: Boolean, color: Color) {
    Canvas(Modifier.size(28.dp)) {
        if (paused) {
            val p = Path().apply {
                moveTo(size.width * 0.28f, size.height * 0.12f)
                lineTo(size.width * 0.9f, size.height * 0.5f)
                lineTo(size.width * 0.28f, size.height * 0.88f)
                close()
            }
            drawPath(p, color)
        } else {
            val rw = size.width * 0.15f
            drawRect(color, Offset(size.width * 0.30f, size.height * 0.14f),
                Size(rw, size.height * 0.72f))
            drawRect(color, Offset(size.width * 0.62f, size.height * 0.14f),
                Size(rw, size.height * 0.72f))
        }
    }
}

@Composable
private fun Ring(progress: Float, color: Color) {
    Canvas(Modifier.fillMaxSize()) {
        drawArc(Color(0x14FFFFFF), 0f, 360f, false, style = Stroke(width = 9f))
        drawArc(color, -90f, progress * 360f, false, style = Stroke(width = 9f, cap = StrokeCap.Round))
    }
}

@Composable
private fun FinishBody(modifier: Modifier = Modifier, totalDur: Double, workCount: Int, moveCount: Int,
                       onRestart: () -> Unit, onClose: () -> Unit) {
    Column(modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("🎉 训练完成", style = MaterialTheme.typography.headlineLarge, color = Ctext)
        Spacer(Modifier.size(6.dp))
        Text("今天是偷偷变强的一天", color = Cmuted)
        Spacer(Modifier.size(26.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(26.dp)) {
            FinishStat(humanTime(totalDur), "总时长")
            FinishStat("$workCount", "训练组数")
            FinishStat("$moveCount", "动作数")
        }
        Spacer(Modifier.size(30.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            FfButton("再来一次", onClick = onRestart)
            FfButton("完成", onClick = onClose, primary = true)
        }
    }
}

@Composable
private fun FinishStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Ctext, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
        Text(label, color = Cmuted, style = MaterialTheme.typography.bodySmall)
    }
}
