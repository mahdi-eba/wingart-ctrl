package studio.wingart.ctrl

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/** Small vector icon set drawn directly on a Canvas (no icon library = small, fast APK, identical look everywhere). */
enum class Ic { PREV, PLAY, NEXT, VDOWN, VUP, MUTE, REW, FWD, FULL, CC, SLOW, FAST, UP, DOWN, LEFT, RIGHT, BKSP, ENTER, COPY, PASTE, CLOSE, REFRESH }

fun DrawScope.drawIc(t: Ic, c: Color) {
    val s = size.minDimension
    val sw = s * 0.09f
    val st = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
    fun o(x: Float, y: Float) = Offset(x * s, y * s)
    fun ln(x1: Float, y1: Float, x2: Float, y2: Float) = drawLine(c, o(x1, y1), o(x2, y2), strokeWidth = sw, cap = StrokeCap.Round)
    fun poly(fill: Boolean, vararg v: Float) {
        val p = Path()
        p.moveTo(v[0] * s, v[1] * s)
        var i = 2
        while (i < v.size) { p.lineTo(v[i] * s, v[i + 1] * s); i += 2 }
        p.close()
        if (fill) drawPath(p, c) else drawPath(p, c, style = st)
    }
    fun rect(x: Float, y: Float, w: Float, h: Float) = drawRect(c, o(x, y), Size(w * s, h * s))
    fun rrect(x: Float, y: Float, w: Float, h: Float, r: Float) = drawRoundRect(c, o(x, y), Size(w * s, h * s), CornerRadius(r * s), style = st)
    fun spk() = poly(true, 0.08f, 0.38f, 0.26f, 0.38f, 0.48f, 0.2f, 0.48f, 0.8f, 0.26f, 0.62f, 0.08f, 0.62f)
    when (t) {
        Ic.PREV -> { rect(0.14f, 0.2f, 0.09f, 0.6f); poly(true, 0.86f, 0.2f, 0.86f, 0.8f, 0.3f, 0.5f) }
        Ic.PLAY -> { poly(true, 0.1f, 0.2f, 0.1f, 0.8f, 0.52f, 0.5f); rect(0.62f, 0.2f, 0.1f, 0.6f); rect(0.8f, 0.2f, 0.1f, 0.6f) }
        Ic.NEXT -> { poly(true, 0.14f, 0.2f, 0.14f, 0.8f, 0.7f, 0.5f); rect(0.77f, 0.2f, 0.09f, 0.6f) }
        Ic.VDOWN -> { spk(); ln(0.66f, 0.5f, 0.92f, 0.5f) }
        Ic.VUP -> { spk(); ln(0.66f, 0.5f, 0.92f, 0.5f); ln(0.79f, 0.37f, 0.79f, 0.63f) }
        Ic.MUTE -> { spk(); ln(0.66f, 0.38f, 0.9f, 0.62f); ln(0.9f, 0.38f, 0.66f, 0.62f) }
        Ic.REW -> { poly(true, 0.5f, 0.25f, 0.5f, 0.75f, 0.08f, 0.5f); poly(true, 0.92f, 0.25f, 0.92f, 0.75f, 0.5f, 0.5f) }
        Ic.FWD -> { poly(true, 0.08f, 0.25f, 0.08f, 0.75f, 0.5f, 0.5f); poly(true, 0.5f, 0.25f, 0.5f, 0.75f, 0.92f, 0.5f) }
        Ic.FULL -> {
            ln(0.14f, 0.38f, 0.14f, 0.14f); ln(0.14f, 0.14f, 0.38f, 0.14f)
            ln(0.62f, 0.14f, 0.86f, 0.14f); ln(0.86f, 0.14f, 0.86f, 0.38f)
            ln(0.86f, 0.62f, 0.86f, 0.86f); ln(0.86f, 0.86f, 0.62f, 0.86f)
            ln(0.38f, 0.86f, 0.14f, 0.86f); ln(0.14f, 0.86f, 0.14f, 0.62f)
        }
        Ic.CC -> {
            rrect(0.1f, 0.22f, 0.8f, 0.56f, 0.1f)
            ln(0.26f, 0.45f, 0.42f, 0.45f); ln(0.54f, 0.45f, 0.74f, 0.45f)
            ln(0.26f, 0.62f, 0.5f, 0.62f); ln(0.6f, 0.62f, 0.74f, 0.62f)
        }
        Ic.SLOW -> { ln(0.5f, 0.25f, 0.25f, 0.5f); ln(0.25f, 0.5f, 0.5f, 0.75f); ln(0.82f, 0.25f, 0.57f, 0.5f); ln(0.57f, 0.5f, 0.82f, 0.75f) }
        Ic.FAST -> { ln(0.5f, 0.25f, 0.75f, 0.5f); ln(0.75f, 0.5f, 0.5f, 0.75f); ln(0.18f, 0.25f, 0.43f, 0.5f); ln(0.43f, 0.5f, 0.18f, 0.75f) }
        Ic.UP -> poly(true, 0.5f, 0.2f, 0.2f, 0.76f, 0.8f, 0.76f)
        Ic.DOWN -> poly(true, 0.5f, 0.8f, 0.2f, 0.24f, 0.8f, 0.24f)
        Ic.LEFT -> poly(true, 0.2f, 0.5f, 0.76f, 0.2f, 0.76f, 0.8f)
        Ic.RIGHT -> poly(true, 0.8f, 0.5f, 0.24f, 0.2f, 0.24f, 0.8f)
        Ic.BKSP -> { poly(false, 0.08f, 0.5f, 0.3f, 0.22f, 0.92f, 0.22f, 0.92f, 0.78f, 0.3f, 0.78f); ln(0.48f, 0.4f, 0.72f, 0.6f); ln(0.72f, 0.4f, 0.48f, 0.6f) }
        Ic.ENTER -> { ln(0.82f, 0.2f, 0.82f, 0.6f); ln(0.82f, 0.6f, 0.22f, 0.6f); ln(0.22f, 0.6f, 0.4f, 0.42f); ln(0.22f, 0.6f, 0.4f, 0.78f) }
        Ic.COPY -> { rrect(0.32f, 0.1f, 0.56f, 0.62f, 0.08f); rrect(0.12f, 0.28f, 0.56f, 0.62f, 0.08f) }
        Ic.PASTE -> { rrect(0.2f, 0.2f, 0.6f, 0.72f, 0.08f); rect(0.38f, 0.08f, 0.24f, 0.2f) }
        Ic.CLOSE -> { ln(0.2f, 0.2f, 0.8f, 0.8f); ln(0.8f, 0.2f, 0.2f, 0.8f) }
        Ic.REFRESH -> {
            drawArc(c, 40f, 280f, false, o(0.15f, 0.15f), Size(0.7f * s, 0.7f * s), style = st)
            poly(true, 0.693f, 0.339f, 0.847f, 0.211f, 0.854f, 0.375f)
        }
    }
}
