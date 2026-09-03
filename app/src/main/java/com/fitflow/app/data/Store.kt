package com.fitflow.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** 本地持久化（SharedPreferences JSON）：计划 / 设置 / 动作库 */
object Store {
    private const val PKEY = "fitflow.plans.v1"
    private const val SKEY = "fitflow.settings.v1"
    private const val CKEY = "fitflow.custom.v1"   // 自定义动作列表
    private const val VKEY = "fitflow.video.v1"    // 内置动作视频覆盖表 {内置id: uri}

    /* ---------- 计划 ---------- */
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

    /* ---------- 设置 ---------- */
    fun loadSettings(ctx: Context): SettingsData {
        val sp = ctx.getSharedPreferences(SKEY, Context.MODE_PRIVATE)
        return SettingsData(
            voice = sp.getBoolean("voice", true),
            beat = sp.getBoolean("beat", false),
            countdown = sp.getBoolean("countdown", false),
            volume = sp.getFloat("volume", 0.8f).toDouble()
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

    /* ---------- 动作库：内置 + 自定义 ---------- */

    /** 读取覆盖表：内置动作 id -> 视频 uri */
    private fun videoOverrides(ctx: Context): Map<String, String> {
        val raw = ctx.getSharedPreferences(VKEY, Context.MODE_PRIVATE).getString("data", null)
            ?: return emptyMap()
        return try {
            val o = JSONObject(raw)
            val m = mutableMapOf<String, String>()
            o.keys().forEach { k -> m[k] = o.optString(k) }
            m
        } catch (_: Exception) { emptyMap() }
    }

    private fun saveVideoOverrides(ctx: Context, map: Map<String, String>) {
        val o = JSONObject()
        map.forEach { (k, v) -> o.put(k, v) }
        ctx.getSharedPreferences(VKEY, Context.MODE_PRIVATE).edit().putString("data", o.toString()).apply()
    }

    /** 读取自定义动作（持久化部分） */
    fun loadCustom(ctx: Context): List<Exercise> {
        val raw = ctx.getSharedPreferences(CKEY, Context.MODE_PRIVATE).getString("data", null)
            ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { Exercise.fromJson(arr.getJSONObject(it)) }
        } catch (_: Exception) { emptyList() }
    }

    private fun saveCustom(ctx: Context, list: List<Exercise>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        ctx.getSharedPreferences(CKEY, Context.MODE_PRIVATE).edit().putString("data", arr.toString()).apply()
    }

    /** 完整动作库 = 内置（含视频覆盖）+ 自定义 */
    fun loadLibrary(ctx: Context): List<Exercise> {
        val ov = videoOverrides(ctx)
        val builtin = MOVE_LIBRARY.map { e ->
            if (ov.containsKey(e.id)) e.copy(videoUri = ov[e.id]) else e
        }
        return builtin + loadCustom(ctx)
    }

    /** 绑定/换绑/移除某个动作的视频（内置与自定义通用）。uri 传 null 表示移除 */
    fun bindVideo(ctx: Context, id: String, uri: String?) {
        val customs = loadCustom(ctx)
        val idx = customs.indexOfFirst { it.id == id }
        if (idx >= 0) {
            val updated = customs.mapIndexed { i, e -> if (i == idx) e.copy(videoUri = uri) else e }
            saveCustom(ctx, updated)
        } else {
            val ov = videoOverrides(ctx).toMutableMap()
            if (uri == null) ov.remove(id) else ov[id] = uri
            saveVideoOverrides(ctx, ov)
        }
    }

    /** 新增自定义动作 */
    fun addCustom(ctx: Context, ex: Exercise) {
        saveCustom(ctx, loadCustom(ctx) + ex)
    }

    /** 删除自定义动作 */
    fun removeCustom(ctx: Context, id: String) {
        saveCustom(ctx, loadCustom(ctx).filterNot { it.id == id })
    }

    /** 修改自定义动作的名称与提示 */
    fun updateCustom(ctx: Context, id: String, name: String, tip: String) {
        saveCustom(ctx, loadCustom(ctx).map { if (it.id == id) it.copy(name = name, tip = tip) else it })
    }

    /** 预置计划的行描述 */
    private data class PRow(val key: String, val value: Int?, val sets: Int?, val rest: Double?)

    private fun presetPlans(): List<Plan> {
        fun mk(name: String, prep: Int, moveRest: Int, rows: List<PRow>): Plan {
            val moves = rows.map { r ->
                val l = libMove(r.key)
                Move(randomId(), l.name, l.mode,
                    r.value ?: l.value, r.sets ?: l.sets, (r.rest ?: l.rest.toDouble()).toInt(),
                    l.tempo, l.figure, l.tip, null)
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
