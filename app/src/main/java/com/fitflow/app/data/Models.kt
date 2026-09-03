package com.fitflow.app.data

import org.json.JSONArray
import org.json.JSONObject

/** 计时模式：按个数 或 按时间 */
enum class Mode { REPS, TIME }

/** 一个训练动作（对应网页版 Move；videoUri 为本地绝对路径或 http(s) 链接） */
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
            figure = o.optString("figure", "generic"),
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

/** 音频偏好：voice=语音播报 beat=节拍器 countdown=倒计时滴答 */
data class SettingsData(
    var voice: Boolean = true,
    var beat: Boolean = false,
    var countdown: Boolean = false,
    var volume: Double = 0.8
)

fun randomId(): String {
    val a = (0..7).map { "abcdefghijklmnopqrstuvwxyz0123456789".random() }.joinToString("")
    val b = System.currentTimeMillis().toString(36)
    return a + b.takeLast(6)
}

/**
 * 动作库条目：内置动作 + 用户自定义动作的统一形态。
 * @param id    唯一标识（内置动作用 figure 名，如 pushup；自定义用 randomId）
 * @param figure 火柴人姿势；自定义动作为 "generic"（无视频时的占位示范）
 * @param videoUri 绑定的跟练视频：本地绝对路径 或 http(s):// 链接
 * @param builtin 是否内置（内置不可删除）
 */
data class Exercise(
    val id: String,
    val name: String,
    val figure: String,
    val mode: Mode,
    val value: Int,
    val sets: Int,
    val rest: Int,
    val tempo: Double,
    val tip: String = "",
    val videoUri: String? = null,
    val builtin: Boolean = false
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id).put("name", name).put("figure", figure)
        .put("mode", mode.name).put("value", value).put("sets", sets)
        .put("rest", rest).put("tempo", tempo).put("tip", tip)
        .put("videoUri", videoUri ?: JSONObject.NULL)

    fun toMove(): Move = Move(randomId(), name, mode, value, sets, rest, tempo, figure, tip, videoUri)

    companion object {
        fun fromJson(o: JSONObject): Exercise {
            val m = o.optString("mode", "REPS")
            return Exercise(
                id = o.optString("id", randomId()),
                name = o.optString("name", "动作"),
                figure = o.optString("figure", "generic"),
                mode = if (m == "TIME") Mode.TIME else Mode.REPS,
                value = o.optInt("value", 15),
                sets = o.optInt("sets", 3),
                rest = o.optInt("rest", 30),
                tempo = o.optDouble("tempo", 2.0),
                tip = o.optString("tip", ""),
                videoUri = if (o.isNull("videoUri")) null else o.optString("videoUri", "")
            )
        }
    }
}

/** 内置动作库模板 */
val MOVE_LIBRARY = listOf(
    Exercise("highKnees", "高抬腿", "highKnees", Mode.TIME, 30, 3, 20, 0.5,
        "膝盖抬到髋部高度，核心收紧，落地要轻", builtin = true),
    Exercise("jumpingJack", "开合跳", "jumpingJack", Mode.TIME, 40, 3, 20, 0.5,
        "手臂过头顶，落地屈膝缓冲", builtin = true),
    Exercise("squat", "徒手深蹲", "squat", Mode.REPS, 20, 3, 30, 2.0,
        "臀部向后坐，膝盖不要内扣", builtin = true),
    Exercise("pushup", "俯卧撑", "pushup", Mode.REPS, 12, 3, 40, 3.0,
        "身体呈一条直线，肘部约 45°", builtin = true),
    Exercise("lunge", "交替弓步蹲", "lunge", Mode.REPS, 20, 3, 30, 2.0,
        "前膝不超过脚尖，后膝轻触地面", builtin = true),
    Exercise("plank", "平板支撑", "plank", Mode.TIME, 45, 3, 30, 1.0,
        "臀部别塌也别翘，均匀呼吸", builtin = true),
    Exercise("crunch", "卷腹", "crunch", Mode.REPS, 20, 3, 25, 2.0,
        "用腹部发力，脖子不要用力", builtin = true),
    Exercise("mountainClimber", "登山跑", "mountainClimber", Mode.TIME, 30, 3, 25, 0.4,
        "肩膀始终在手腕正上方", builtin = true),
    Exercise("burpee", "波比跳", "burpee", Mode.REPS, 10, 3, 40, 3.5,
        "累了可去掉跳跃，改站立收腿", builtin = true),
    Exercise("rest", "休息 / 拉伸", "rest", Mode.TIME, 60, 1, 0, 1.0,
        "深呼吸，放松", builtin = true)
)

fun libMove(id: String): Exercise = MOVE_LIBRARY.firstOrNull { it.id == id } ?: MOVE_LIBRARY[2]

fun newMoveFromLib(id: String): Move = libMove(id).toMove()
