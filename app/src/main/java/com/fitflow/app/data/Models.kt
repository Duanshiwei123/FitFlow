package com.fitflow.app.data

import org.json.JSONArray
import org.json.JSONObject

/** 计时模式：按个数 或 按时间 */
enum class Mode { REPS, TIME }

/** 一个训练动作（对应网页版 Move） */
data class Move(
    val id: String,
    val name: String,
    val mode: Mode,
    val value: Int,          // REPS=每组个数；TIME=每组秒数
    val sets: Int,
    val rest: Int,           // 组间休息秒
    val tempo: Double,       // 每个动作耗时秒（REPS 用于估算/打拍，TIME 用作节拍间隔）
    val figure: String,
    val tip: String = "",
    val videoUri: String? = null
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id).put("name", name)
        .put("mode", mode.name).put("value", value)
        .put("sets", sets).put("rest", rest).put("tempo", tempo)
        .put("figure", figure).put("tip", tip)
        .put("videoUri", videoUri ?: JSONObject.NULL)

    companion object {
        fun fromJson(o: JSONObject): Move = Move(
            id = o.optString("id", randomId()),
            name = o.optString("name", "动作"),
            mode = if (o.optString("mode") == "TIME") Mode.TIME else Mode.REPS,
            value = o.optInt("value", 15),
            sets = o.optInt("sets", 3),
            rest = o.optInt("rest", 30),
            tempo = o.optDouble("tempo", 2.0),
            figure = o.optString("figure", "squat"),
            tip = o.optString("tip", ""),
            videoUri = if (o.isNull("videoUri")) null else o.optString("videoUri", "")
        )
    }
}

data class Plan(
    val id: String,
    var name: String,
    var prep: Int,
    var moveRest: Int,
    var moves: List<Move>
) {
    fun toJson(): JSONObject {
        val arr = JSONArray()
        moves.forEach { arr.put(it.toJson()) }
        return JSONObject()
            .put("id", id).put("name", name)
            .put("prep", prep).put("moveRest", moveRest)
            .put("moves", arr)
    }

    fun copyDeep(): Plan = Plan(id, name, prep, moveRest, moves.map { it.copy() })

    companion object {
        fun fromJson(o: JSONObject): Plan {
            val arr = o.optJSONArray("moves") ?: JSONArray()
            val moves = (0 until arr.length()).map { Move.fromJson(arr.getJSONObject(it)) }
            return Plan(
                id = o.optString("id", randomId()),
                name = o.optString("name", "我的计划"),
                prep = o.optInt("prep", 10),
                moveRest = o.optInt("moveRest", 15),
                moves = moves
            )
        }
    }
}

data class SettingsData(
    var voice: Boolean = true,
    var beat: Boolean = true,
    var countdown: Boolean = true,
    var volume: Double = 0.7
)

fun randomId(): String {
    val a = (0..7).map { "abcdefghijklmnopqrstuvwxyz0123456789".random() }.joinToString("")
    val b = System.currentTimeMillis().toString(36)
    return a + b.takeLast(6)
}

/** 内置动作库模板 */
data class LibMove(
    val key: String, val name: String, val mode: Mode,
    val value: Int, val sets: Int, val rest: Int, val tempo: Double, val tip: String
)

val MOVE_LIBRARY = listOf(
    LibMove("highKnees", "高抬腿", Mode.TIME, 30, 3, 20, 0.5, "膝盖抬到髋部高度，核心收紧，落地要轻"),
    LibMove("jumpingJack", "开合跳", Mode.TIME, 40, 3, 20, 0.5, "手臂过头顶，落地屈膝缓冲"),
    LibMove("squat", "徒手深蹲", Mode.REPS, 20, 3, 30, 2.0, "臀部向后坐，膝盖不要内扣"),
    LibMove("pushup", "俯卧撑", Mode.REPS, 12, 3, 40, 3.0, "身体呈一条直线，肘部约 45°"),
    LibMove("lunge", "交替弓步蹲", Mode.REPS, 20, 3, 30, 2.0, "前膝不超过脚尖，后膝轻触地面"),
    LibMove("plank", "平板支撑", Mode.TIME, 45, 3, 30, 1.0, "臀部别塌也别翘，均匀呼吸"),
    LibMove("crunch", "卷腹", Mode.REPS, 20, 3, 25, 2.0, "用腹部发力，脖子不要用力"),
    LibMove("mountainClimber", "登山跑", Mode.TIME, 30, 3, 25, 0.4, "肩膀始终在手腕正上方"),
    LibMove("burpee", "波比跳", Mode.REPS, 10, 3, 40, 3.5, "累了可去掉跳跃，改站立收腿"),
    LibMove("rest", "休息 / 拉伸", Mode.TIME, 60, 1, 0, 1.0, "深呼吸，放松")
)

fun libMove(key: String): LibMove = MOVE_LIBRARY.firstOrNull { it.key == key } ?: MOVE_LIBRARY[2]

fun newMoveFromLib(key: String): Move {
    val l = libMove(key)
    return Move(randomId(), l.name, l.mode, l.value, l.sets, l.rest, l.tempo, l.key, l.tip)
}
