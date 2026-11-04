package com.example

import com.example.chart.ChartTransform
import com.example.engine.TechnicalIndicators
import com.example.model.Candle
import com.example.model.ChartPoint
import com.example.model.Drawing
import com.example.model.DrawingType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testCoordinateTransformInvertibility() {
        val transform = ChartTransform(
            chartWidth = 1000f,
            chartHeight = 600f,
            minPrice = 50000.0,
            maxPrice = 70000.0,
            leftmostCandleIndex = 10.0,
            visibleCandleCount = 40.0
        )

        val testIndex = 25.5
        val screenX = transform.candleIndexToX(testIndex)
        val invertedIndex = transform.xToCandleIndex(screenX)
        assertEquals(testIndex, invertedIndex, 0.001)

        val testPrice = 64250.0
        val screenY = transform.priceToY(testPrice)
        val invertedPrice = transform.yToPrice(screenY)
        assertEquals(testPrice, invertedPrice, 0.01)
    }

    @Test
    fun testDrawingLockAndMove() {
        val p1 = ChartPoint(price = 60000.0, candleIndex = 20.0)
        val p2 = ChartPoint(price = 65000.0, candleIndex = 30.0)
        val drawing = Drawing(
            type = DrawingType.TRENDLINE,
            points = listOf(p1, p2),
            isLocked = false
        )

        assertFalse(drawing.isLocked)

        // Lock drawing
        val locked = drawing.copy(isLocked = true)
        assertTrue(locked.isLocked)

        // Move unlocked points
        val deltaPrice = 500.0
        val deltaIndex = 2.0
        val moved = drawing.copy(
            points = drawing.points.map {
                it.copy(price = it.price + deltaPrice, candleIndex = it.candleIndex + deltaIndex)
            }
        )

        assertEquals(60500.0, moved.points[0].price, 0.001)
        assertEquals(22.0, moved.points[0].candleIndex, 0.001)
    }

    @Test
    fun testEMACalculation() {
        val candles = listOf(
            Candle(0, 1000, 10.0, 12.0, 9.0, 11.0, 100.0),
            Candle(1, 2000, 11.0, 13.0, 10.0, 12.0, 150.0),
            Candle(2, 3000, 12.0, 14.0, 11.0, 13.0, 120.0)
        )
        val ema = TechnicalIndicators.calculateEMA(candles, 2)
        assertEquals(3, ema.size)
        assertTrue(ema[2L]!! > 11.0)
    }
}
