package com.example.model

import java.util.UUID

/**
 * Representation of a single candlestick bar.
 * The [index] is a strictly monotonic sequential identifier (e.g. 0, 1, 2 ...).
 * Real-time drawings anchor to [index] and [price], guaranteeing rock-solid sync
 * as live candles stream in!
 */
data class Candle(
    val index: Long,
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double
) {
    val isBullish: Boolean get() = close >= open
    val priceChange: Double get() = close - open
    val percentChange: Double get() = if (open != 0.0) ((close - open) / open) * 100.0 else 0.0
}

/**
 * Coordinate in chart space (Price Y-axis, CandleIndex X-axis).
 * By using continuous candle index (Double), drawings can sit between candles
 * or project forward into the future, and stay strictly anchored regardless of zoom/pan!
 */
data class ChartPoint(
    val price: Double,
    val candleIndex: Double,
    val timestamp: Long = 0L
)

enum class DrawingType(val title: String) {
    TRENDLINE("Trendline"),
    HORIZONTAL_LINE("Support / Resistance"),
    RAY("Ray Line"),
    FIBONACCI("Fibonacci Retracement"),
    RECTANGLE("Supply / Demand Zone"),
    ARROW("Trend Arrow"),
    BRUSH("Freehand Brush"),
    TEXT_NOTE("Text Annotation")
}

data class Drawing(
    val id: String = UUID.randomUUID().toString(),
    val type: DrawingType,
    val points: List<ChartPoint>,
    val colorHex: Long = 0xFF00E5FF, // Default CyanAccent
    val strokeWidth: Float = 2.5f,
    val isDashed: Boolean = false,
    val isLocked: Boolean = false,
    val text: String = "",
    val extraData: String = ""
)

enum class DrawingToolMode {
    POINTER,       // Select, drag to move, lock, delete
    TRENDLINE,     // 2-point line
    HORIZONTAL,    // 1-point horizontal level
    RAY,           // 2-point extended ray
    FIBONACCI,     // 2-point fib levels
    RECTANGLE,     // 2-point zone box
    ARROW,         // 2-point directed arrow
    BRUSH,         // Multi-point freehand drawing
    TEXT_NOTE,     // 1-point note
    CROSSHAIR      // Inspector
}

enum class ChartStyle(val label: String) {
    CANDLESTICK("Candles"),
    HOLLOW("Hollow"),
    HEIKIN_ASHI("Heikin Ashi"),
    LINE("Line Chart")
}

enum class IndicatorType(val label: String, val colorHex: Long) {
    EMA20("EMA 20", 0xFFFFD600),
    EMA50("EMA 50", 0xFF00E5FF),
    BOLLINGER_BANDS("Bollinger Bands (20,2)", 0xFFD500F9),
    VOLUME("Volume", 0xFF089981)
}

data class CryptoAsset(
    val symbol: String,
    val name: String,
    val basePrice: Double,
    val digits: Int = 2,
    val volatility: Double = 0.0018
) {
    companion object {
        val ALL = listOf(
            CryptoAsset("BTC/USDT", "Bitcoin", 67450.0, 2, 0.0016),
            CryptoAsset("ETH/USDT", "Ethereum", 3480.0, 2, 0.0022),
            CryptoAsset("SOL/USDT", "Solana", 185.50, 2, 0.0035),
            CryptoAsset("EUR/USD", "Euro / US Dollar", 1.0845, 4, 0.0004),
            CryptoAsset("XAU/USD", "Gold / US Dollar", 2385.20, 2, 0.0012)
        )
    }
}

enum class Timeframe(val label: String, val candleDurationSeconds: Long) {
    SEC_1("1s", 1),
    SEC_5("5s", 5),
    SEC_15("15s", 15),
    MIN_1("1m", 60),
    MIN_5("5m", 300),
    MIN_15("15m", 900)
}
