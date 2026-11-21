package com.example.engine

import com.example.model.CurvePoint
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Generates a smooth 256-entry Look-Up Table (0..255 -> 0..255) from control points
 * using monotone cubic Hermite interpolation (Fritsch-Carlson), matching Adobe Lightroom's
 * tone curve behavior without ringing or overshoot.
 */
object SplineCurveLut {

    fun buildLut(rawPoints: List<CurvePoint>): IntArray {
        val lut = IntArray(256)
        if (rawPoints.isEmpty()) {
            for (i in 0..255) lut[i] = i
            return lut
        }

        val sorted = rawPoints
            .map { CurvePoint(it.x.coerceIn(0f, 1f), it.y.coerceIn(0f, 1f)) }
            .sortedBy { it.x }

        // Deduplicate very close x values
        val points = mutableListOf<CurvePoint>()
        for (pt in sorted) {
            if (points.isEmpty() || abs(pt.x - points.last().x) > 0.005f) {
                points.add(pt)
            } else {
                points[points.lastIndex] = pt
            }
        }

        if (points.size == 1) {
            val v = (points[0].y * 255f).roundToInt().coerceIn(0, 255)
            lut.fill(v)
            return lut
        }

        val n = points.size
        val xs = FloatArray(n) { points[it].x }
        val ys = FloatArray(n) { points[it].y }

        // Slopes of secant lines
        val d = FloatArray(n - 1)
        for (i in 0 until n - 1) {
            val dx = (xs[i + 1] - xs[i]).coerceAtLeast(1e-5f)
            d[i] = (ys[i + 1] - ys[i]) / dx
        }

        // Tangents at control points
        val m = FloatArray(n)
        m[0] = d[0]
        m[n - 1] = d[n - 2]
        for (i in 1 until n - 1) {
            if (d[i - 1] * d[i] <= 0f) {
                m[i] = 0f
            } else {
                m[i] = (d[i - 1] + d[i]) * 0.5f
            }
        }

        // Enforce monotonicity
        for (i in 0 until n - 1) {
            if (abs(d[i]) < 1e-6f) {
                m[i] = 0f
                m[i + 1] = 0f
            } else {
                val a = m[i] / d[i]
                val b = m[i + 1] / d[i]
                val s = a * a + b * b
                if (s > 9f) {
                    val tau = 3f / kotlin.math.sqrt(s)
                    m[i] = tau * a * d[i]
                    m[i + 1] = tau * b * d[i]
                }
            }
        }

        for (i in 0..255) {
            val x = i / 255f
            val y = when {
                x <= xs[0] -> ys[0]
                x >= xs[n - 1] -> ys[n - 1]
                else -> {
                    var seg = 0
                    while (seg < n - 2 && x > xs[seg + 1]) {
                        seg++
                    }
                    val h = (xs[seg + 1] - xs[seg]).coerceAtLeast(1e-5f)
                    val t = ((x - xs[seg]) / h).coerceIn(0f, 1f)
                    val t2 = t * t
                    val t3 = t2 * t
                    val h00 = 2f * t3 - 3f * t2 + 1f
                    val h10 = t3 - 2f * t2 + t
                    val h01 = -2f * t3 + 3f * t2
                    val h11 = t3 - t2
                    h00 * ys[seg] + h10 * h * m[seg] + h01 * ys[seg + 1] + h11 * h * m[seg + 1]
                }
            }
            lut[i] = (y * 255f).roundToInt().coerceIn(0, 255)
        }
        return lut
    }
}
