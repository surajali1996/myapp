package com.example.engine

import com.example.model.Candle
import com.example.model.CryptoAsset
import com.example.model.Timeframe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

data class LiveMarketState(
    val candles: List<Candle>,
    val formingCandle: Candle,
    val lastTickPrice: Double,
    val lastTickChange: Double,
    val candleTimeRemainingSec: Int,
    val isRunning: Boolean = true
)

class MarketDataEngine(
    private val scope: CoroutineScope
) {
    private var currentAsset: CryptoAsset = CryptoAsset.ALL.first()
    private var currentTimeframe: Timeframe = Timeframe.SEC_5
    private var speedMultiplier: Float = 1.0f

    private val _marketState = MutableStateFlow<LiveMarketState>(createInitialState())
    val marketState: StateFlow<LiveMarketState> = _marketState.asStateFlow()

    private var tickerJob: Job? = null
    private var nextCandleIndex = 0L

    init {
        startTicker()
    }

    fun setAsset(asset: CryptoAsset) {
        currentAsset = asset
        resetMarket()
    }

    fun setTimeframe(timeframe: Timeframe) {
        currentTimeframe = timeframe
        resetMarket()
    }

    fun setSpeed(multiplier: Float) {
        speedMultiplier = multiplier
        restartTicker()
    }

    fun togglePausePlay() {
        val currentState = _marketState.value
        val newRunning = !currentState.isRunning
        _marketState.value = currentState.copy(isRunning = newRunning)
        if (newRunning) {
            startTicker()
        } else {
            tickerJob?.cancel()
            tickerJob = null
        }
    }

    private fun resetMarket() {
        tickerJob?.cancel()
        _marketState.value = createInitialState()
        startTicker()
    }

    private fun createInitialState(): LiveMarketState {
        val initialCandles = mutableListOf<Candle>()
        var price = currentAsset.basePrice
        var timestamp = System.currentTimeMillis() - (120 * currentTimeframe.candleDurationSeconds * 1000L)
        val initialCount = 100

        var currentIndex = 0L
        for (i in 0 until initialCount) {
            val volatility = currentAsset.volatility * currentAsset.basePrice
            val trend = (Random.nextDouble() - 0.495) * volatility * 1.5
            val open = price
            val close = open + trend
            val high = maxOf(open, close) + Random.nextDouble() * volatility * 0.8
            val low = minOf(open, close) - Random.nextDouble() * volatility * 0.8
            val volume = Random.nextDouble(5.0, 50.0)

            initialCandles.add(
                Candle(
                    index = currentIndex++,
                    timestamp = timestamp,
                    open = roundPrice(open),
                    high = roundPrice(high),
                    low = roundPrice(low),
                    close = roundPrice(close),
                    volume = volume
                )
            )
            price = close
            timestamp += currentTimeframe.candleDurationSeconds * 1000L
        }

        nextCandleIndex = currentIndex

        // Live forming candle
        val formingOpen = price
        val formingCandle = Candle(
            index = nextCandleIndex++,
            timestamp = timestamp,
            open = roundPrice(formingOpen),
            high = roundPrice(formingOpen),
            low = roundPrice(formingOpen),
            close = roundPrice(formingOpen),
            volume = 0.5
        )

        return LiveMarketState(
            candles = initialCandles,
            formingCandle = formingCandle,
            lastTickPrice = formingOpen,
            lastTickChange = 0.0,
            candleTimeRemainingSec = currentTimeframe.candleDurationSeconds.toInt(),
            isRunning = true
        )
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            var tickAccumulator = 0
            val tickDelayMs: Long = (350f / speedMultiplier).toLong().coerceIn(100L, 1000L)

            while (isActive) {
                delay(tickDelayMs)
                if (!_marketState.value.isRunning) continue

                tickAccumulator++
                val ticksPerSecond = (1000L / tickDelayMs).toInt().coerceAtLeast(1)

                val currentState = _marketState.value
                val currentForming = currentState.formingCandle

                // Price micro-tick simulation
                val volatility = currentAsset.volatility * currentAsset.basePrice * 0.45
                // Slight drift toward round base
                val drift = (Random.nextDouble() - 0.49) * volatility
                val newClose = roundPrice((currentForming.close + drift).coerceAtLeast(0.0001))
                val newHigh = maxOf(currentForming.high, newClose)
                val newLow = minOf(currentForming.low, newClose)
                val addedVol = Random.nextDouble(0.2, 1.8)
                val priceChange = newClose - currentState.lastTickPrice

                val updatedForming = currentForming.copy(
                    high = newHigh,
                    low = newLow,
                    close = newClose,
                    volume = currentForming.volume + addedVol
                )

                // Every 1 second approx:
                var newRemaining = currentState.candleTimeRemainingSec
                var newCandles = currentState.candles
                var activeForming = updatedForming

                if (tickAccumulator >= ticksPerSecond) {
                    tickAccumulator = 0
                    newRemaining -= 1

                    if (newRemaining <= 0) {
                        // Current candle is sealed into history!
                        newCandles = (currentState.candles + updatedForming).takeLast(250)
                        val nextTimestamp = updatedForming.timestamp + (currentTimeframe.candleDurationSeconds * 1000L)

                        // Brand new forming candle is created live!
                        activeForming = Candle(
                            index = nextCandleIndex++,
                            timestamp = nextTimestamp,
                            open = newClose,
                            high = newClose,
                            low = newClose,
                            close = newClose,
                            volume = 0.2
                        )
                        newRemaining = currentTimeframe.candleDurationSeconds.toInt()
                    }
                }

                _marketState.value = currentState.copy(
                    candles = newCandles,
                    formingCandle = activeForming,
                    lastTickPrice = newClose,
                    lastTickChange = priceChange,
                    candleTimeRemainingSec = newRemaining
                )
            }
        }
    }

    private fun restartTicker() {
        startTicker()
    }

    private fun roundPrice(p: Double): Double {
        val factor = Math.pow(10.0, currentAsset.digits.toDouble())
        return kotlin.math.round(p * factor) / factor
    }
}
