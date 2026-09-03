package com.fitflow.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.fitflow.app.ui.theme.Caccent
import com.fitflow.app.ui.theme.Caccent2
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin

/**
 * 简笔火柴人示范（示意级循环动画）。
 * 逻辑坐标系 0..140 x 0..170，等比适配画布。
 */
@Composable
fun StickFigure(figure: String, progress: Float = 0f, modifier: Modifier = Modifier) {
    val color = if (figure == "rest") Caccent2 else Caccent
    Canvas(modifier) {
        val scale = (size.minDimension / 170f).coerceAtMost(1.6f)
        drawStickFigure(figure, progress, color, scale,
            ox = size.width / 2f - 70f * scale, oy = (size.height - 170f * scale) / 2f)
    }
}

private data class Seg(val a: Offset, val b: Offset)
private fun seg(x1: Float, y1: Float, x2: Float, y2: Float) = Seg(Offset(x1, y1), Offset(x2, y2))

private fun poseLines(figure: String, p: Float): List<Seg> {
    val w = max(0f, sin(PI.toFloat() * p))          // 0→1→0
    val breathe = (sin(2f * PI.toFloat() * p) + 1f) * 0.5f
    return when (figure) {
        "rest" -> {
            val dy = breathe * 2f
            listOf(
                seg(66f, 50f + dy, 66f, 92f + dy),
                seg(63f, 56f + dy, 61f, 80f + dy), seg(61f, 80f + dy, 60f, 102f + dy),
                seg(69f, 56f + dy, 71f, 80f + dy), seg(71f, 80f + dy, 72f, 102f + dy),
                seg(66f, 92f + dy, 63f, 118f + dy), seg(63f, 118f + dy, 60f, 142f + dy),
                seg(66f, 92f + dy, 69f, 118f + dy), seg(69f, 118f + dy, 71f, 141f + dy),
                seg(60f, 142f + dy, 72f, 142f + dy), seg(71f, 141f + dy, 59f, 141f + dy)
            )
        }
        "squat", "lunge", "burpee" -> {
            val d = w
            val hipY = 92f + 26f * d
            listOf(
                seg(66f, 50f + 8f * d, 66f, hipY),
                seg(64f, 56f + 6f * d, 58f, 84f + 4f * d),
                seg(66f, 54f + 8f * d, 88f + 6f * d, 92f + 8f * d),
                seg(66f, hipY, 88f, hipY - 12f + 12f * d),
                seg(88f, hipY - 12f + 12f * d, 92f, hipY + 22f),
                seg(92f, hipY + 22f, 104f, hipY + 22f),
                seg(66f, hipY, 62f + 4f * d, 118f + 12f * d),
                seg(62f + 4f * d, 118f + 12f * d, 60f, 142f),
                seg(60f, 142f, 72f, 142f)
            )
        }
        "crunch" -> {
            val lift = w * 14f
            listOf(
                seg(104f, 138f, 62f, 138f),
                seg(58f, 138f - lift, 52f, 128f - lift),
                seg(62f, 138f, 52f, 126f),
                seg(52f, 126f, 60f, 112f),
                seg(100f, 138f, 100f, 130f),
                seg(70f, 138f, 74f, 148f)
            )
        }
        "plank", "pushup", "mountainClimber" -> {
            val down = if (figure == "plank") breathe * 1.5f else w * 6f
            val yOff = down
            listOf(
                seg(96f, 120f + yOff, 50f, 128f + yOff),
                seg(50f, 128f + yOff, 26f, 138f + yOff),
                seg(26f, 138f + yOff, 26f, 150f),
                seg(96f, 120f + yOff, 99f, 142f + yOff),
                seg(99f, 142f + yOff, 106f, 142f + yOff),
                seg(96f, 118f + yOff, 99f, 142f + yOff)
            )
        }
        "highKnees" -> {
            val lift = w
            listOf(
                seg(66f, 46f, 66f, 92f),
                seg(66f, 92f, 68f, 118f), seg(68f, 118f, 70f, 142f),
                seg(70f, 142f, 82f, 142f),
                seg(66f, 92f, 66f, 114f - 26f * lift),
                seg(66f, 114f - 26f * lift, 76f, 108f - 26f * lift),
                seg(58f, 56f, 50f, 70f), seg(50f, 70f, 46f, 92f),
                seg(72f, 56f, 84f, 64f + 14f * lift)
            )
        }
        else -> { // 开合跳姿态；同时兜底未列出的 figure（如自定义动作占位 generic / highKnees2）
            val open = w
            listOf(
                seg(66f, 50f - 3f * open, 66f, 92f),
                seg(64f, 56f, 52f, 92f - 26f * open),
                seg(72f, 56f, 82f, 92f - 26f * open),
                seg(66f, 92f, 63f, 118f), seg(63f, 118f, 60f, 142f),
                seg(66f, 92f, 69f, 118f), seg(69f, 118f, 71f, 141f)
            )
        }
    }
}

private fun headCenter(figure: String, p: Float): Pair<Offset, Float> {
    val w = max(0f, sin(PI.toFloat() * p))
    return when (figure) {
        "squat", "lunge", "burpee" -> Offset(72f + 2f, 46f + 6f * w) to 11f
        "crunch" -> Offset(50f, 124f - w * 12f) to 10f
        "plank", "pushup", "mountainClimber" -> Offset(104f, 116f) to 10f
        "highKnees" -> Offset(72f, 34f) to 11f
        "rest" -> Offset(70f, 40f + (sin(2f * PI.toFloat() * p) + 1f)) to 11f
        else -> Offset(70f, 38f) to 11f
    }
}

private fun DrawScope.drawStickFigure(figure: String, p: Float, color: Color, scale: Float, ox: Float, oy: Float) {
    fun X(v: Float) = ox + v * scale
    fun Y(v: Float) = oy + v * scale
    drawLine(Color.White.copy(alpha = 0.08f), Offset(X(12f), Y(150f)), Offset(X(128f), Y(150f)),
        strokeWidth = 2f * scale, cap = StrokeCap.Round)
    drawOval(Color.Black.copy(alpha = 0.35f), Offset(X(46f), Y(148f)), Size(52f * scale, 6f * scale))
    val stroke = 6.5f * scale
    poseLines(figure, p).forEach { s ->
        drawLine(color, Offset(X(s.a.x), Y(s.a.y)), Offset(X(s.b.x), Y(s.b.y)),
            strokeWidth = stroke, cap = StrokeCap.Round)
    }
    val (hc, r) = headCenter(figure, p)
    drawCircle(color, radius = r * scale, center = Offset(X(hc.x), Y(hc.y)), style = Stroke(stroke))
}
