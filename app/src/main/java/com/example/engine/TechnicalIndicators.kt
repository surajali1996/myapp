package com.example.engine

import com.example.model.Candle
import kotlin.math.sqrt

data class BollingerBandPoint(
    val middle: Double,
    val upper: Double,
    val lower: Double
)

object TechnicalIndicators {

    /**
     * Calculates Exponential Moving Average (EMA) for given period.
     * Returns a map of candle index to EMA value.
     */
    fun calculateEMA(candles: List<Candle>, period: Int): Map<Long, Double> {
        if (candles.isEmpty() || period <= 0) return emptyMap()
        val result = mutableMapOf<Long, Double>()
        val multiplier = 2.0 / (period + 1)

        var ema = candles.first().close
        result[candles.first().index] = ema

        for (i in 1 until candles.size) {
            val candle = candles[i]
            ema = ((candle.close - ema) * multiplier) + ema
            result[candle.index] = ema
        }
        return result
    }

    /**
     * Calculates Bollinger Bands (period 20, multiplier 2.0).
     */
    fun calculateBollingerBands(
        candles: List<Candle>,
        period: Int = 20,
        kMultiplier: Double = 2.0
    ): Map<Long, BollingerBandPoint> {
        if (candles.size < 2) return emptyMap()
        val result = mutableMapOf<Long, BollingerBandPoint>()

        for (i in candles.indices) {
            val startIdx = maxOf(0, i - period + 1)
            val subList = candles.subList(startIdx, i + 1)
            val mean = subList.map { it.close }.average()

            val variance = subList.map { (it.close - mean) * (it.close - mean) }.average()
            val stdDev = sqrt(variance)

            val upper = mean + (kMultiplier * stdDev)
            val lower = mean - (kMultiplier * stdDev)

            result[candles[i].index] = BollingerBandPoint(
                middle = mean,
                upper = upper,
                lower = lower
            )
        }
        return result
    }

    /**
     * Converts standard candles to Heikin Ashi candles.
     */
    fun toHeikinAshi(candles: List<Candle>): List<Candle> {
        if (candles.isEmpty()) return emptyList()
        val haCandles = ArrayList<Candle>(candles.size)

        var prevHaOpen = candles.first().open
        var prevHaClose = candles.first().close

        for (candle in candles) {
            val haClose = (candle.open + candle.high + candle.low + candle.close) / 4.0
            val haOpen = (prevHaOpen + prevHaClose) / 2.0
            val haHigh = maxOf(candle.high, haOpen, haClose)
            val haLow = minOf(candle.low, haOpen, haClose)

            haCandles.add(
                candle.copy(
                    open = haOpen,
                    high = haHigh,
                    low = haLow,
                    close = haClose
                )
            )

            prevHaOpen = haOpen
            prevHaClose = haClose
        }
        return haCandles
    }
}
