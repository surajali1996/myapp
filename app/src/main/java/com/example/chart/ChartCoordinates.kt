package com.example.chart

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.example.model.Candle
import com.example.model.ChartPoint
import com.example.model.Drawing
import com.example.model.DrawingType
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Coordinate space transforms between:
 * - Screen space (X: 0..width, Y: 0..height)
 * - Chart data space (X: candleIndex, Y: price)
 */
class ChartTransform(
    val chartWidth: Float,
    val chartHeight: Float,
    val minPrice: Double,
    val maxPrice: Double,
    val leftmostCandleIndex: Double,
    val visibleCandleCount: Double
) {
    val priceRange: Double = if (maxPrice > minPrice) maxPrice - minPrice else 1.0

    fun candleIndexToX(candleIndex: Double): Float {
        if (visibleCandleCount <= 0.0) return 0f
        val fraction = (candleIndex - leftmostCandleIndex) / visibleCandleCount
        return (fraction * chartWidth).toFloat()
    }

    fun priceToY(price: Double): Float {
        val fraction = (price - minPrice) / priceRange
        return (chartHeight - (fraction * chartHeight)).toFloat()
    }

    fun chartPointToScreen(point: ChartPoint): Offset {
        return Offset(candleIndexToX(point.candleIndex), priceToY(point.price))
    }

    fun xToCandleIndex(x: Float): Double {
        if (chartWidth <= 0f) return leftmostCandleIndex
        val fraction = (x / chartWidth).toDouble()
        return leftmostCandleIndex + (fraction * visibleCandleCount)
    }

    fun yToPrice(y: Float): Double {
        if (chartHeight <= 0f) return minPrice
        val fraction = ((chartHeight - y) / chartHeight).toDouble()
        return minPrice + (fraction * priceRange)
    }

    fun screenToChartPoint(offset: Offset, timestamp: Long = 0L): ChartPoint {
        return ChartPoint(
            price = yToPrice(offset.y),
            candleIndex = xToCandleIndex(offset.x),
            timestamp = timestamp
        )
    }

    val candleStepPx: Float
        get() = (chartWidth / visibleCandleCount).toFloat()

    val candleBodyWidthPx: Float
        get() = max(2.5f, candleStepPx * 0.72f)
}

object ChartMathUtils {

    /**
     * Magnet Snap: snaps target point to closest OHLC of nearest candle
     */
    fun snapToCandleOHLC(
        point: ChartPoint,
        candles: List<Candle>,
        formingCandle: Candle?
    ): ChartPoint {
        val all = if (formingCandle != null) candles + formingCandle else candles
        if (all.isEmpty()) return point

        // Find candle nearest to point.candleIndex
        val nearest = all.minByOrNull { abs(it.index - point.candleIndex) } ?: return point
        val ohlc = listOf(nearest.open, nearest.high, nearest.low, nearest.close)
        val closestPrice = ohlc.minByOrNull { abs(it - point.price) } ?: point.price

        return ChartPoint(
            price = closestPrice,
            candleIndex = nearest.index.toDouble(),
            timestamp = nearest.timestamp
        )
    }

    /**
     * Hit tests a drawing in screen pixel space with a touch tolerance (e.g. 28px).
     * Returns true if touchOffset hits the drawing or its control handles.
     */
    fun hitTestDrawing(
        drawing: Drawing,
        touchOffset: Offset,
        transform: ChartTransform,
        tolerancePx: Float = 36f
    ): Boolean {
        val screenPoints = drawing.points.map { transform.chartPointToScreen(it) }
        if (screenPoints.isEmpty()) return false

        // Check if hitting any anchor point directly
        for (pt in screenPoints) {
            if ((pt - touchOffset).getDistance() <= tolerancePx) {
                return true
            }
        }

        when (drawing.type) {
            DrawingType.TRENDLINE, DrawingType.ARROW -> {
                if (screenPoints.size >= 2) {
                    return distanceToSegment(touchOffset, screenPoints[0], screenPoints[1]) <= tolerancePx
                }
            }
            DrawingType.HORIZONTAL_LINE -> {
                val y = screenPoints[0].y
                return abs(touchOffset.y - y) <= tolerancePx
            }
            DrawingType.RAY -> {
                if (screenPoints.size >= 2) {
                    return distanceToRay(touchOffset, screenPoints[0], screenPoints[1]) <= tolerancePx
                }
            }
            DrawingType.FIBONACCI -> {
                if (screenPoints.size >= 2) {
                    val y1 = screenPoints[0].y
                    val y2 = screenPoints[1].y
                    val minY = min(y1, y2) - tolerancePx
                    val maxY = max(y1, y2) + tolerancePx
                    val minX = min(screenPoints[0].x, screenPoints[1].x) - tolerancePx
                    val maxX = max(screenPoints[0].x, screenPoints[1].x) + tolerancePx
                    return touchOffset.y in minY..maxY && touchOffset.x in minX..maxX
                }
            }
            DrawingType.RECTANGLE -> {
                if (screenPoints.size >= 2) {
                    val rect = Rect(
                        min(screenPoints[0].x, screenPoints[1].x),
                        min(screenPoints[0].y, screenPoints[1].y),
                        max(screenPoints[0].x, screenPoints[1].x),
                        max(screenPoints[0].y, screenPoints[1].y)
                    )
                    // Hit border or inside
                    return rect.inflate(tolerancePx).contains(touchOffset)
                }
            }
            DrawingType.BRUSH -> {
                for (i in 0 until screenPoints.size - 1) {
                    if (distanceToSegment(touchOffset, screenPoints[i], screenPoints[i + 1]) <= tolerancePx) {
                        return true
                    }
                }
            }
            DrawingType.TEXT_NOTE -> {
                val pt = screenPoints[0]
                return (pt - touchOffset).getDistance() <= tolerancePx * 1.5f
            }
        }
        return false
    }

    /**
     * Returns index of control handle hit (0..N-1), or -1 if no handle was hit.
     */
    fun hitTestHandle(
        drawing: Drawing,
        touchOffset: Offset,
        transform: ChartTransform,
        handleRadiusPx: Float = 36f
    ): Int {
        for (i in drawing.points.indices) {
            val pt = transform.chartPointToScreen(drawing.points[i])
            if ((pt - touchOffset).getDistance() <= handleRadiusPx) {
                return i
            }
        }
        return -1
    }

    private fun distanceToSegment(p: Offset, v: Offset, w: Offset): Float {
        val l2 = (v - w).getDistanceSquared()
        if (l2 == 0f) return (p - v).getDistance()
        val t = max(0f, min(1f, ((p.x - v.x) * (w.x - v.x) + (p.y - v.y) * (w.y - v.y)) / l2))
        val projection = Offset(v.x + t * (w.x - v.x), v.y + t * (w.y - v.y))
        return (p - projection).getDistance()
    }

    private fun distanceToRay(p: Offset, origin: Offset, directionPt: Offset): Float {
        val dx = directionPt.x - origin.x
        val dy = directionPt.y - origin.y
        val len = hypot(dx, dy)
        if (len == 0f) return (p - origin).getDistance()
        val u = Offset(dx / len, dy / len)
        val v = p - origin
        val t = max(0f, v.x * u.x + v.y * u.y)
        val proj = Offset(origin.x + t * u.x, origin.y + t * u.y)
        return (p - proj).getDistance()
    }
}
