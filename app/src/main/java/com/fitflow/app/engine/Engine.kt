package com.fitflow.app.engine

import com.fitflow.app.data.Mode
import com.fitflow.app.data.Move
import com.fitflow.app.data.Plan
import com.fitflow.app.data.SettingsData
import kotlin.math.max

/* 时间线引擎：计划 -> Step 列表 + 音频事件表（思路同网页版） */

enum class StepKind { PREP, WORK, REST, DONE }

data class Step(
    val kind: StepKind,
    val title: String,
    val sub: String,
    val duration: Double,
    val startAt: Double = 0.0,
    val moveIndex: Int = -1,
    val setIndex: Int = 0,
    val setTotal: Int = 1,
    val mode: Mode = Mode.REPS,
    val target: Int = 0,
    val tempo: Double = 2.0,
    val figure: String = "rest",
    val tip: String = "",
    val next: String = ""
)

enum class EvType { BEEP, SPEAK }

data class Ev(val t: Double, val type: EvType, val sound: String = "", val text: String = "", val rep: Int = 0)

object Engine {

    fun moveDuration(m: Move): Double =
        if (m.mode == Mode.TIME) max(1.0, m.value.toDouble())
        else max(1.0, m.value * max(0.3, m.tempo))

    fun planDuration(plan: Plan): Double {
        var total = plan.prep.toDouble()
        plan.moves.forEachIndexed { i, m ->
            val sets = max(1, m.sets)
            total += sets * moveDuration(m) + (sets - 1) * m.rest
            if (i < plan.moves.size - 1) total += plan.moveRest
        }
        return total
    }

    fun buildTimeline(plan: Plan): List<Step> {
        val steps = mutableListOf<Step>()
        if (plan.prep > 0) steps += Step(StepKind.PREP, "准备", "马上开始", plan.prep.toDouble(), figure = "rest")
        plan.moves.forEachIndexed { mi, m ->
            val sets = max(1, m.sets)
            for (s in 0 until sets) {
                steps += Step(
                    StepKind.WORK, m.name,
                    if (sets > 1) "第 ${s + 1} / $sets 组" else "单组",
                    moveDuration(m),
                    moveIndex = mi, setIndex = s, setTotal = sets,
                    mode = m.mode, target = m.value, tempo = max(0.3, m.tempo),
                    figure = m.figure, tip = m.tip
                )
                if (s < sets - 1 && m.rest > 0)
                    steps += Step(StepKind.REST, "组间休息", "下一组：${m.name}", m.rest.toDouble(), figure = "rest", next = m.name)
            }
            if (mi < plan.moves.size - 1 && plan.moveRest > 0) {
                val nx = plan.moves[mi + 1]
                steps += Step(StepKind.REST, "换动作", "下一个：${nx.name}", plan.moveRest.toDouble(), figure = "rest", next = nx.name, tip = nx.tip)
            }
        }
        steps += Step(StepKind.DONE, "训练完成", "今天也很棒", 0.0, figure = "rest")
        var acc = 0.0
        return steps.map { st -> st.copy(startAt = acc).also { acc += it.duration } }
    }

    fun buildEvents(step: Step, s: SettingsData): List<Ev> {
        val ev = mutableListOf<Ev>()
        val d = step.duration
        when (step.kind) {
            StepKind.PREP -> {
                ev += Ev(0.0, EvType.SPEAK, text = "准备，${d.toInt()} 秒后开始")
                ev += Ev(0.0, EvType.BEEP, sound = "start")
                pushCountdown(ev, d, s)
                return ev
            }
            StepKind.DONE -> {
                ev += Ev(0.0, EvType.SPEAK, text = "训练完成，太棒了")
                ev += Ev(0.0, EvType.BEEP, sound = "finish")
                return ev
            }
            StepKind.REST -> {
                ev += Ev(0.0, EvType.SPEAK, text = "休息 ${d.toInt()} 秒")
                ev += Ev(0.0, EvType.BEEP, sound = "rest")
                pushCountdown(ev, d, s)
                if (s.voice && step.next.isNotEmpty()) {
                    val lead = minOf(3.0, maxOf(1.2, d * 0.25))
                    ev += Ev(max(0.0, d - lead), EvType.SPEAK, text = "准备，下一个 ${step.next}")
                }
                return ev
            }
            StepKind.WORK -> {}
        }
        val label = buildString {
            append(step.title).append("，")
            if (step.setTotal > 1) append("第 ${step.setIndex + 1} 组，")
            append(if (step.mode == Mode.REPS) "${step.target} 个" else "${d.toInt()} 秒")
        }
        ev += Ev(0.0, EvType.SPEAK, text = label)
        ev += Ev(0.0, EvType.BEEP, sound = "start")
        if (s.beat) {
            if (step.mode == Mode.REPS) {
                for (i in 0 until step.target) {
                    val t = i * step.tempo
                    if (t >= d) break
                    ev += Ev(t, EvType.BEEP, sound = if (i % 5 == 4) "beatAccent" else "beat", rep = i + 1)
                }
                if (s.voice && step.target >= 8)
                    ev += Ev((step.target / 2) * step.tempo, EvType.SPEAK, text = "${step.target / 2} 个，一半了")
            } else {
                val stepT = maxOf(0.5, step.tempo)
                var k = 0
                while (k * stepT < d - 0.15) {
                    ev += Ev(k * stepT, EvType.BEEP, sound = if (k % 8 == 7) "beatAccent" else "beat", rep = k + 1)
                    k++
                }
            }
        }
        pushCountdown(ev, d, s)
        ev += Ev(d, EvType.BEEP, sound = "end")
        return ev
    }

    private fun pushCountdown(ev: MutableList<Ev>, d: Double, s: SettingsData) {
        if (!s.countdown || d < 4) return
        for (n in 3 downTo 1) ev += Ev(d - (4 - n), EvType.BEEP, sound = "tick")
    }

    /** 火柴人动画周期 */
    fun animDuration(m: Move): Double {
        val cycle = when (m.figure) {
            "highKnees", "mountainClimber", "lunge", "jumpingJack" -> 2
            "squat", "pushup", "crunch", "burpee" -> 1
            else -> 0
        }
        return if (cycle == 0) (if (m.figure == "plank") 3.6 else 3.2)
        else maxOf(0.5, minOf(8.0, m.tempo * cycle))
    }
}
