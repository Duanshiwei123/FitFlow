package com.fitflow.app.data

import android.content.Context
import org.json.JSONArray

/** 本地持久化（SharedPreferences JSON） */
object Store {
    private const val PKEY = "fitflow.plans.v1"
    private const val SKEY = "fitflow.settings.v1"

    fun loadPlans(ctx: Context): List<Plan> {
        val raw = ctx.getSharedPreferences(PKEY, Context.MODE_PRIVATE).getString("data", null)
        if (raw != null) {
            try {
                val arr = JSONArray(raw)
                if (arr.length() > 0)
                    return (0 until arr.length()).map { Plan.fromJson(arr.getJSONObject(it)) }
            } catch (_: Exception) { /* 数据损坏则重置 */ }
        }
        val presets = presetPlans()
        savePlans(ctx, presets)
        return presets
    }

    fun savePlans(ctx: Context, plans: List<Plan>) {
        val arr = JSONArray()
        plans.forEach { arr.put(it.toJson()) }
        ctx.getSharedPreferences(PKEY, Context.MODE_PRIVATE).edit().putString("data", arr.toString()).apply()
    }

    fun loadSettings(ctx: Context): SettingsData {
        val sp = ctx.getSharedPreferences(SKEY, Context.MODE_PRIVATE)
        return SettingsData(
            voice = sp.getBoolean("voice", true),
            beat = sp.getBoolean("beat", true),
            countdown = sp.getBoolean("countdown", true),
            volume = sp.getFloat("volume", 0.7f).toDouble()
        )
    }

    fun saveSettings(ctx: Context, s: SettingsData) {
        ctx.getSharedPreferences(SKEY, Context.MODE_PRIVATE).edit()
            .putBoolean("voice", s.voice)
            .putBoolean("beat", s.beat)
            .putBoolean("countdown", s.countdown)
            .putFloat("volume", s.volume.toFloat())
            .apply()
    }

    fun newPlan(name: String): Plan = Plan(randomId(), name, 10, 15, listOf(newMoveFromLib("jumpingJack")))

    /** 预置计划的行描述 */
    private data class PRow(val key: String, val value: Int?, val sets: Int?, val rest: Double?)

    private fun presetPlans(): List<Plan> {
        fun mk(name: String, prep: Int, moveRest: Int, rows: List<PRow>): Plan {
            val moves = rows.map { r ->
                val l = libMove(r.key)
                Move(randomId(), l.name, l.mode,
                    r.value ?: l.value, r.sets ?: l.sets, (r.rest ?: l.rest.toDouble()).toInt(),
                    l.tempo, r.key, l.tip)
            }
            return Plan(randomId(), name, prep, moveRest, moves)
        }
        return listOf(
            mk("新手全身燃脂 · 12 分钟", 10, 15, listOf(
                PRow("jumpingJack", 40, 2, 20.0),
                PRow("squat", 15, 3, 30.0),
                PRow("pushup", 8, 3, 40.0),
                PRow("highKnees", 30, 3, 25.0),
                PRow("plank", 30, 2, 30.0),
                PRow("crunch", 15, 2, 20.0)
            )),
            mk("Tabata · 4 分钟暴汗", 8, 10, listOf(
                PRow("burpee", 8, 2, 40.0),
                PRow("mountainClimber", 20, 2, 25.0),
                PRow("highKnees", 20, 2, 20.0),
                PRow("squat", 20, 2, 30.0)
            )),
            mk("腹部核心 · 8 分钟", 10, 20, listOf(
                PRow("crunch", 20, 3, 25.0),
                PRow("plank", 40, 3, 30.0),
                PRow("mountainClimber", 30, 3, 25.0)
            ))
        )
    }
}
