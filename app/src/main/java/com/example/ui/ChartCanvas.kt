package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.testTag
import com.example.chart.ChartMathUtils
import com.example.chart.ChartTransform
import com.example.engine.BollingerBandPoint
import com.example.model.Candle
import com.example.model.ChartPoint
import com.example.model.ChartStyle
import com.example.model.CryptoAsset
import com.example.model.Drawing
import com.example.model.DrawingToolMode
import com.example.model.DrawingType
import com.example.model.IndicatorType
import com.example.ui.theme.BearishFill
import com.example.ui.theme.BullishFill
import com.example.ui.theme.CandleBearRed
import com.example.ui.theme.CandleBearRedBright
import com.example.ui.theme.CandleBullGreen
import com.example.ui.theme.CandleBullGreenBright
import com.example.ui.theme.ChartBackground
import com.example.ui.theme.ChartBorder
import com.example.ui.theme.ChartGridLine
import com.example.ui.theme.ChartSurfaceElevated
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private const val Y_AXIS_WIDTH_PX = 160f
private const val X_AXIS_HEIGHT_PX = 60f

@Composable
fun ChartCanvas(
    candles: List<Candle>,
    formingCandle: Candle,
    asset: CryptoAsset,
    chartStyle: ChartStyle,
    activeIndicators: Set<IndicatorType>,
    ema20Values: Map<Long, Double>,
    ema50Values: Map<Long, Double>,
    bollingerBands: Map<Long, BollingerBandPoint>,
    drawings: List<Drawing>,
    selectedDrawingId: String?,
    previewDrawing: Drawing?,
    isDrawingsHidden: Boolean,
    activeMode: DrawingToolMode,
    isMagnetEnabled: Boolean,
    visibleCandleCount: Double,
    rightCandleIndexOffset: Double,
    onPan: (Double) -> Unit,
    onZoom: (Float) -> Unit,
    onSelectDrawing: (String?) -> Unit,
    onMoveSelectedBy: (Double, Double) -> Unit,
    onMoveHandleBy: (Int, ChartPoint) -> Unit,
    onDrawingTouchStart: (ChartPoint) -> Unit,
    onDrawingTouchMove: (ChartPoint) -> Unit,
    onDrawingTouchEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Combine all candles
    val allCandles = remember(candles, formingCandle) { candles + formingCandle }
    val latestCandleIndex = formingCandle.index.toDouble()

    // Time formatter for X-axis
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }

    Box(modifier = modifier.fillMaxSize().background(ChartBackground).testTag("chart_canvas_container")) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(activeMode, selectedDrawingId, drawings, visibleCandleCount, rightCandleIndexOffset) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val chartW = size.width - Y_AXIS_WIDTH_PX
                        val chartH = size.height - X_AXIS_HEIGHT_PX

                        // Calculate current bounds for transform
                        val rightmostIndex = latestCandleIndex + rightCandleIndexOffset + 3.0
                        val leftmostIndex = rightmostIndex - visibleCandleCount
                        val visibleList = allCandles.filter { it.index >= leftmostIndex - 2 && it.index <= rightmostIndex + 2 }

                        val minPrice = if (visibleList.isNotEmpty()) {
                            val minP = visibleList.minOf { it.low }
                            val maxP = visibleList.maxOf { it.high }
                            val pad = (maxP - minP) * 0.12
                            max(0.00001, minP - pad)
                        } else asset.basePrice * 0.98

                        val maxPrice = if (visibleList.isNotEmpty()) {
                            val minP = visibleList.minOf { it.low }
                            val maxP = visibleList.maxOf { it.high }
                            val pad = (maxP - minP) * 0.12
                            maxP + pad
                        } else asset.basePrice * 1.02

                        val transform = ChartTransform(
                            chartWidth = chartW,
                            chartHeight = chartH,
                            minPrice = minPrice,
                            maxPrice = maxPrice,
                            leftmostCandleIndex = leftmostIndex,
                            visibleCandleCount = visibleCandleCount
                        )

                        val startPos = down.position
                        var isDraggingHandle = -1
                        var isDraggingBody = false
                        var targetDrawing: Drawing? = null

                        if (activeMode != DrawingToolMode.POINTER) {
                            // Creation mode
                            val chartPt = transform.screenToChartPoint(startPos, System.currentTimeMillis())
                            onDrawingTouchStart(chartPt)

                            var pointerDown = true
                            while (pointerDown) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull()
                                if (change == null || !change.pressed) {
                                    pointerDown = false
                                    onDrawingTouchEnd()
                                } else {
                                    val currentPt = transform.screenToChartPoint(change.position, System.currentTimeMillis())
                                    onDrawingTouchMove(currentPt)
                                    change.consume()
                                }
                            }
                        } else {
                            // Pointer mode: Select, Move, Pan, Zoom
                            // Check handle hit on currently selected drawing
                            val currentSelected = drawings.find { it.id == selectedDrawingId }
                            if (currentSelected != null && !currentSelected.isLocked) {
                                val handleIdx = ChartMathUtils.hitTestHandle(currentSelected, startPos, transform)
                                if (handleIdx != -1) {
                                    isDraggingHandle = handleIdx
                                    targetDrawing = currentSelected
                                }
                            }

                            // If not dragging handle, check if touching drawing body
                            if (isDraggingHandle == -1) {
                                val hit = drawings.findLast { ChartMathUtils.hitTestDrawing(it, startPos, transform) }
                                if (hit != null) {
                                    targetDrawing = hit
                                    onSelectDrawing(hit.id)
                                    if (!hit.isLocked) {
                                        isDraggingBody = true
                                    }
                                } else {
                                    // Tapped empty space
                                    onSelectDrawing(null)
                                }
                            }

                            // Event loop for drag/pan/zoom
                            var prevPos = startPos
                            var pointerDown = true

                            while (pointerDown) {
                                val event = awaitPointerEvent()
                                val pressedChanges = event.changes.filter { it.pressed }

                                if (pressedChanges.isEmpty()) {
                                    pointerDown = false
                                } else if (pressedChanges.size >= 2) {
                                    // Pinch zoom
                                    val zoom = event.calculateZoom()
                                    if (zoom != 1f) {
                                        onZoom(zoom)
                                    }
                                    val pan = event.calculatePan()
                                    val deltaCandles = -(pan.x / transform.candleStepPx).toDouble()
                                    if (abs(deltaCandles) > 0.01) {
                                        onPan(deltaCandles)
                                    }
                                    pressedChanges.forEach { it.consume() }
                                } else {
                                    // Single pointer move
                                    val change = pressedChanges.first()
                                    val currPos = change.position
                                    val deltaPx = currPos - prevPos

                                    if (isDraggingHandle != -1 && targetDrawing != null) {
                                        // Dragging anchor handle
                                        val newChartPoint = transform.screenToChartPoint(currPos, System.currentTimeMillis())
                                        val finalPoint = if (isMagnetEnabled) {
                                            ChartMathUtils.snapToCandleOHLC(newChartPoint, candles, formingCandle)
                                        } else newChartPoint
                                        onMoveHandleBy(isDraggingHandle, finalPoint)
                                        change.consume()
                                    } else if (isDraggingBody && targetDrawing != null) {
                                        // Dragging entire drawing
                                        val deltaCandleIndex = (deltaPx.x / transform.candleStepPx).toDouble()
                                        val deltaPrice = -(deltaPx.y / chartH) * transform.priceRange
                                        onMoveSelectedBy(deltaPrice, deltaCandleIndex)
                                        change.consume()
                                    } else {
                                        // Pan chart horizontally
                                        val deltaCandles = -(deltaPx.x / transform.candleStepPx).toDouble()
                                        if (abs(deltaCandles) > 0.01) {
                                            onPan(deltaCandles)
                                        }
                                        change.consume()
                                    }
                                    prevPos = currPos
                                }
                            }
                        }
                    }
                }
        ) {
            val chartW = size.width - Y_AXIS_WIDTH_PX
            val chartH = size.height - X_AXIS_HEIGHT_PX

            if (chartW <= 0f || chartH <= 0f) return@Canvas

            // Compute visible range
            val rightmostIndex = latestCandleIndex + rightCandleIndexOffset + 3.0
            val leftmostIndex = rightmostIndex - visibleCandleCount
            val visibleCandles = allCandles.filter { it.index >= leftmostIndex - 2 && it.index <= rightmostIndex + 2 }

            // Dynamic Price Bounds with breathing margin
            val minPrice = if (visibleCandles.isNotEmpty()) {
                val minP = visibleCandles.minOf { it.low }
                val maxP = visibleCandles.maxOf { it.high }
                val pad = max((maxP - minP) * 0.12, asset.basePrice * 0.0005)
                max(0.00001, minP - pad)
            } else asset.basePrice * 0.98

            val maxPrice = if (visibleCandles.isNotEmpty()) {
                val minP = visibleCandles.minOf { it.low }
                val maxP = visibleCandles.maxOf { it.high }
                val pad = max((maxP - minP) * 0.12, asset.basePrice * 0.0005)
                maxP + pad
            } else asset.basePrice * 1.02

            val transform = ChartTransform(
                chartWidth = chartW,
                chartHeight = chartH,
                minPrice = minPrice,
                maxPrice = maxPrice,
                leftmostCandleIndex = leftmostIndex,
                visibleCandleCount = visibleCandleCount
            )

            // 1. Draw Grid Lines & Price Labels
            drawGridAndAxes(
                transform = transform,
                asset = asset,
                timeFormatter = timeFormatter,
                visibleCandles = visibleCandles
            )

            // 2. Draw Volume Bars (if enabled)
            if (IndicatorType.VOLUME in activeIndicators) {
                drawVolumeBars(transform, visibleCandles)
            }

            // 3. Draw Indicators (Bollinger Bands, EMAs)
            if (IndicatorType.BOLLINGER_BANDS in activeIndicators) {
                drawBollingerBands(transform, visibleCandles, bollingerBands)
            }
            if (IndicatorType.EMA50 in activeIndicators) {
                drawIndicatorLine(transform, visibleCandles, ema50Values, Color(IndicatorType.EMA50.colorHex), 1.5f)
            }
            if (IndicatorType.EMA20 in activeIndicators) {
                drawIndicatorLine(transform, visibleCandles, ema20Values, Color(IndicatorType.EMA20.colorHex), 2.0f)
            }

            // 4. Draw Candlesticks
            drawCandlesticks(
                transform = transform,
                visibleCandles = visibleCandles,
                chartStyle = chartStyle,
                formingCandleId = formingCandle.index
            )

            // 5. Draw Live Price Line & Glow Pulsing Tag
            drawLivePriceLine(
                transform = transform,
                livePrice = formingCandle.close,
                asset = asset,
                isBullish = formingCandle.close >= formingCandle.open
            )

            // 6. Draw User Drawings (Trendlines, Zones, Fib, Notes, etc.)
            if (!isDrawingsHidden) {
                drawings.forEach { drawing ->
                    val isSelected = drawing.id == selectedDrawingId
                    drawSingleDrawing(
                        drawing = drawing,
                        transform = transform,
                        isSelected = isSelected,
                        asset = asset
                    )
                }

                // In-progress preview
                if (previewDrawing != null) {
                    drawSingleDrawing(
                        drawing = previewDrawing,
                        transform = transform,
                        isSelected = false,
                        asset = asset
                    )
                }
            }
        }
    }
}

// --- Drawing Rendering Sub-functions ---

private fun DrawScope.drawGridAndAxes(
    transform: ChartTransform,
    asset: CryptoAsset,
    timeFormatter: SimpleDateFormat,
    visibleCandles: List<Candle>
) {
    val chartW = transform.chartWidth
    val chartH = transform.chartHeight

    // Horizontal Price Grid Lines (6 evenly spaced levels)
    val priceStepCount = 6
    val priceStep = transform.priceRange / priceStepCount
    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#787B86")
        textSize = 28f
        isAntiAlias = true
    }

    for (i in 0..priceStepCount) {
        val price = transform.minPrice + (i * priceStep)
        val y = transform.priceToY(price)

        // Subtle horizontal grid line
        drawLine(
            color = ChartGridLine,
            start = Offset(0f, y),
            end = Offset(chartW, y),
            strokeWidth = 1f
        )

        // Price label on right Y-axis
        val formattedPrice = String.format(Locale.US, "%.${asset.digits}f", price)
        drawContext.canvas.nativeCanvas.drawText(
            formattedPrice,
            chartW + 16f,
            y + 10f,
            paint
        )
    }

    // Vertical Time Grid Lines
    val candleStep = transform.candleStepPx
    val skipCandles = max(1, (110f / candleStep).toInt())

    for (candle in visibleCandles) {
        if (candle.index % skipCandles == 0L) {
            val x = transform.candleIndexToX(candle.index.toDouble())
            if (x in 0f..chartW) {
                drawLine(
                    color = ChartGridLine,
                    start = Offset(x, 0f),
                    end = Offset(x, chartH),
                    strokeWidth = 1f
                )

                // Timestamp label on bottom X-axis
                val timeStr = timeFormatter.format(Date(candle.timestamp))
                drawContext.canvas.nativeCanvas.drawText(
                    timeStr,
                    x - 45f,
                    chartH + 34f,
                    paint
                )
            }
        }
    }

    // Axis dividers
    drawLine(
        color = ChartBorder,
        start = Offset(chartW, 0f),
        end = Offset(chartW, chartH),
        strokeWidth = 1.5f
    )
    drawLine(
        color = ChartBorder,
        start = Offset(0f, chartH),
        end = Offset(chartW + Y_AXIS_WIDTH_PX, chartH),
        strokeWidth = 1.5f
    )
}

private fun DrawScope.drawCandlesticks(
    transform: ChartTransform,
    visibleCandles: List<Candle>,
    chartStyle: ChartStyle,
    formingCandleId: Long
) {
    val bodyWidth = transform.candleBodyWidthPx

    for (candle in visibleCandles) {
        val x = transform.candleIndexToX(candle.index.toDouble())
        val isBull = candle.close >= candle.open
        val color = if (isBull) CandleBullGreen else CandleBearRed

        val openY = transform.priceToY(candle.open)
        val closeY = transform.priceToY(candle.close)
        val highY = transform.priceToY(candle.high)
        val lowY = transform.priceToY(candle.low)

        val topY = min(openY, closeY)
        val bottomY = max(openY, closeY)
        val height = max(2f, bottomY - topY)

        // Wick
        drawLine(
            color = color,
            start = Offset(x, highY),
            end = Offset(x, lowY),
            strokeWidth = 1.5f
        )

        // Body
        val rect = Rect(x - bodyWidth / 2f, topY, x + bodyWidth / 2f, bottomY)
        when (chartStyle) {
            ChartStyle.HOLLOW -> {
                if (isBull) {
                    drawRect(color = color, topLeft = rect.topLeft, size = rect.size, style = Stroke(width = 1.8f))
                } else {
                    drawRect(color = color, topLeft = rect.topLeft, size = rect.size)
                }
            }
            ChartStyle.LINE -> {
                // Line chart connects closes
            }
            else -> {
                drawRect(color = color, topLeft = rect.topLeft, size = rect.size)
            }
        }

        // Live candle glow pulse
        if (candle.index == formingCandleId) {
            drawCircle(
                color = if (isBull) CandleBullGreenBright.copy(alpha = 0.35f) else CandleBearRedBright.copy(alpha = 0.35f),
                radius = bodyWidth * 0.9f,
                center = Offset(x, closeY)
            )
        }
    }

    if (chartStyle == ChartStyle.LINE && visibleCandles.size >= 2) {
        val linePath = Path()
        linePath.moveTo(transform.candleIndexToX(visibleCandles[0].index.toDouble()), transform.priceToY(visibleCandles[0].close))
        for (i in 1 until visibleCandles.size) {
            val c = visibleCandles[i]
            linePath.lineTo(transform.candleIndexToX(c.index.toDouble()), transform.priceToY(c.close))
        }
        drawPath(path = linePath, color = CyanAccent, style = Stroke(width = 2.5f))
    }
}

private fun DrawScope.drawVolumeBars(
    transform: ChartTransform,
    visibleCandles: List<Candle>
) {
    val chartH = transform.chartHeight
    val maxVol = visibleCandles.maxOfOrNull { it.volume } ?: 1.0
    val maxBarHeight = chartH * 0.22f // Max 22% of chart height
    val bodyWidth = transform.candleBodyWidthPx

    for (candle in visibleCandles) {
        val x = transform.candleIndexToX(candle.index.toDouble())
        val barH = max(2f, (candle.volume / maxVol).toFloat() * maxBarHeight)
        val color = if (candle.close >= candle.open) CandleBullGreen.copy(alpha = 0.38f) else CandleBearRed.copy(alpha = 0.38f)

        drawRect(
            color = color,
            topLeft = Offset(x - bodyWidth / 2f, chartH - barH),
            size = Size(bodyWidth, barH)
        )
    }
}

private fun DrawScope.drawLivePriceLine(
    transform: ChartTransform,
    livePrice: Double,
    asset: CryptoAsset,
    isBullish: Boolean
) {
    val chartW = transform.chartWidth
    val y = transform.priceToY(livePrice)
    val color = if (isBullish) CandleBullGreenBright else CandleBearRedBright

    // Dashed horizontal line across chart
    drawLine(
        color = color.copy(alpha = 0.75f),
        start = Offset(0f, y),
        end = Offset(chartW, y),
        strokeWidth = 1.5f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
    )

    // Pulsing price tag badge on right axis
    val tagHeight = 44f
    val tagWidth = Y_AXIS_WIDTH_PX - 8f
    val tagRect = Rect(chartW + 4f, y - tagHeight / 2f, chartW + 4f + tagWidth, y + tagHeight / 2f)

    drawRoundRect(
        color = color,
        topLeft = tagRect.topLeft,
        size = tagRect.size,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
    )

    val paint = android.graphics.Paint().apply {
        this.color = android.graphics.Color.WHITE
        textSize = 28f
        isFakeBoldText = true
        isAntiAlias = true
    }

    val priceText = String.format(Locale.US, "%.${asset.digits}f", livePrice)
    drawContext.canvas.nativeCanvas.drawText(
        priceText,
        chartW + 16f,
        y + 10f,
        paint
    )
}

private fun DrawScope.drawIndicatorLine(
    transform: ChartTransform,
    visibleCandles: List<Candle>,
    values: Map<Long, Double>,
    color: Color,
    strokeWidth: Float
) {
    if (visibleCandles.isEmpty() || values.isEmpty()) return
    val path = Path()
    var started = false

    for (candle in visibleCandles) {
        val v = values[candle.index] ?: continue
        val x = transform.candleIndexToX(candle.index.toDouble())
        val y = transform.priceToY(v)
        if (!started) {
            path.moveTo(x, y)
            started = true
        } else {
            path.lineTo(x, y)
        }
    }

    if (started) {
        drawPath(path = path, color = color, style = Stroke(width = strokeWidth))
    }
}

private fun DrawScope.drawBollingerBands(
    transform: ChartTransform,
    visibleCandles: List<Candle>,
    bands: Map<Long, BollingerBandPoint>
) {
    if (visibleCandles.isEmpty() || bands.isEmpty()) return

    val upperPath = Path()
    val lowerPath = Path()
    val middlePath = Path()
    val bandFillPath = Path()

    var started = false
    val validCandles = visibleCandles.filter { it.index in bands }

    for (candle in validCandles) {
        val b = bands[candle.index] ?: continue
        val x = transform.candleIndexToX(candle.index.toDouble())
        val uy = transform.priceToY(b.upper)
        val ly = transform.priceToY(b.lower)
        val my = transform.priceToY(b.middle)

        if (!started) {
            upperPath.moveTo(x, uy)
            lowerPath.moveTo(x, ly)
            middlePath.moveTo(x, my)
            bandFillPath.moveTo(x, uy)
            started = true
        } else {
            upperPath.lineTo(x, uy)
            lowerPath.lineTo(x, ly)
            middlePath.lineTo(x, my)
            bandFillPath.lineTo(x, uy)
        }
    }

    if (started) {
        // Close fill path
        for (i in validCandles.indices.reversed()) {
            val candle = validCandles[i]
            val b = bands[candle.index] ?: continue
            bandFillPath.lineTo(transform.candleIndexToX(candle.index.toDouble()), transform.priceToY(b.lower))
        }
        bandFillPath.close()

        // Translucent fill
        drawPath(path = bandFillPath, color = PurpleAccent.copy(alpha = 0.08f), style = Fill)
        drawPath(path = upperPath, color = PurpleAccent.copy(alpha = 0.6f), style = Stroke(width = 1.2f))
        drawPath(path = lowerPath, color = PurpleAccent.copy(alpha = 0.6f), style = Stroke(width = 1.2f))
        drawPath(path = middlePath, color = PurpleAccent.copy(alpha = 0.4f), style = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)))
    }
}

private fun DrawScope.drawSingleDrawing(
    drawing: Drawing,
    transform: ChartTransform,
    isSelected: Boolean,
    asset: CryptoAsset
) {
    val screenPts = drawing.points.map { transform.chartPointToScreen(it) }
    if (screenPts.isEmpty()) return

    val baseColor = Color(drawing.colorHex)
    val strokeWidth = drawing.strokeWidth
    val pathEffect = if (drawing.isDashed) PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f) else null

    when (drawing.type) {
        DrawingType.TRENDLINE -> {
            if (screenPts.size >= 2) {
                drawLine(
                    color = baseColor,
                    start = screenPts[0],
                    end = screenPts[1],
                    strokeWidth = strokeWidth,
                    pathEffect = pathEffect
                )
            }
        }
        DrawingType.HORIZONTAL_LINE -> {
            val y = screenPts[0].y
            drawLine(
                color = baseColor,
                start = Offset(0f, y),
                end = Offset(transform.chartWidth, y),
                strokeWidth = strokeWidth,
                pathEffect = pathEffect ?: PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
            )

            // Price badge on axis
            val priceStr = String.format(Locale.US, "%.${asset.digits}f", drawing.points[0].price)
            val badgeRect = Rect(transform.chartWidth + 6f, y - 18f, transform.chartWidth + 140f, y + 18f)
            drawRoundRect(
                color = baseColor.copy(alpha = 0.85f),
                topLeft = badgeRect.topLeft,
                size = badgeRect.size,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
            )
            val p = android.graphics.Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = 24f
                isFakeBoldText = true
                isAntiAlias = true
            }
            drawContext.canvas.nativeCanvas.drawText(priceStr, badgeRect.left + 10f, y + 8f, p)
        }
        DrawingType.RAY -> {
            if (screenPts.size >= 2) {
                val p0 = screenPts[0]
                val p1 = screenPts[1]
                val dx = p1.x - p0.x
                val dy = p1.y - p0.y
                val angle = atan2(dy, dx)
                val farX = p0.x + cos(angle) * 3000f
                val farY = p0.y + sin(angle) * 3000f

                drawLine(
                    color = baseColor,
                    start = p0,
                    end = Offset(farX, farY),
                    strokeWidth = strokeWidth,
                    pathEffect = pathEffect
                )
            }
        }
        DrawingType.FIBONACCI -> {
            if (screenPts.size >= 2) {
                val y0 = screenPts[0].y
                val y1 = screenPts[1].y
                val minX = min(screenPts[0].x, screenPts[1].x)
                val maxX = max(transform.chartWidth, max(screenPts[0].x, screenPts[1].x))

                val fibLevels = listOf(
                    0.000 to "0.0% (High)",
                    0.236 to "23.6%",
                    0.382 to "38.2%",
                    0.500 to "50.0%",
                    0.618 to "61.8% (Golden)",
                    0.786 to "78.6%",
                    1.000 to "100.0% (Low)"
                )

                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#B0BEC5")
                    textSize = 22f
                    isAntiAlias = true
                }

                for (idx in fibLevels.indices) {
                    val (ratio, label) = fibLevels[idx]
                    val yLevel = y0 + (y1 - y0) * ratio.toFloat()
                    val priceLevel = drawing.points[0].price + (drawing.points[1].price - drawing.points[0].price) * ratio

                    // Level line
                    drawLine(
                        color = baseColor.copy(alpha = if (ratio == 0.618 || ratio == 0.5) 0.9f else 0.5f),
                        start = Offset(minX, yLevel),
                        end = Offset(maxX, yLevel),
                        strokeWidth = if (ratio == 0.618) 2f else 1.2f
                    )

                    // Shaded band between consecutive levels
                    if (idx < fibLevels.size - 1) {
                        val nextRatio = fibLevels[idx + 1].first
                        val nextY = y0 + (y1 - y0) * nextRatio.toFloat()
                        drawRect(
                            color = baseColor.copy(alpha = if (ratio >= 0.382 && ratio <= 0.618) 0.12f else 0.05f),
                            topLeft = Offset(minX, min(yLevel, nextY)),
                            size = Size(maxX - minX, abs(nextY - yLevel))
                        )
                    }

                    // Ratio & price text
                    val formattedP = String.format(Locale.US, "%.${asset.digits}f", priceLevel)
                    drawContext.canvas.nativeCanvas.drawText("$label  $formattedP", minX + 12f, yLevel - 6f, paint)
                }
            }
        }
        DrawingType.RECTANGLE -> {
            if (screenPts.size >= 2) {
                val left = min(screenPts[0].x, screenPts[1].x)
                val top = min(screenPts[0].y, screenPts[1].y)
                val right = max(screenPts[0].x, screenPts[1].x)
                val bottom = max(screenPts[0].y, screenPts[1].y)
                val rect = Rect(left, top, right, bottom)

                // Fill zone
                drawRect(color = baseColor.copy(alpha = 0.22f), topLeft = rect.topLeft, size = rect.size)
                // Border
                drawRect(color = baseColor, topLeft = rect.topLeft, size = rect.size, style = Stroke(width = strokeWidth, pathEffect = pathEffect))
            }
        }
        DrawingType.ARROW -> {
            if (screenPts.size >= 2) {
                val p0 = screenPts[0]
                val p1 = screenPts[1]
                drawLine(color = baseColor, start = p0, end = p1, strokeWidth = strokeWidth)

                // Arrow head
                val angle = atan2(p1.y - p0.y, p1.x - p0.x)
                val headLen = 28f
                val headAngle = Math.PI / 6.0 // 30 deg
                val a1 = Offset(p1.x - headLen * cos(angle - headAngle).toFloat(), p1.y - headLen * sin(angle - headAngle).toFloat())
                val a2 = Offset(p1.x - headLen * cos(angle + headAngle).toFloat(), p1.y - headLen * sin(angle + headAngle).toFloat())

                val arrowHead = Path().apply {
                    moveTo(p1.x, p1.y)
                    lineTo(a1.x, a1.y)
                    lineTo(a2.x, a2.y)
                    close()
                }
                drawPath(path = arrowHead, color = baseColor)
            }
        }
        DrawingType.BRUSH -> {
            if (screenPts.size >= 2) {
                val path = Path()
                path.moveTo(screenPts[0].x, screenPts[0].y)
                for (i in 1 until screenPts.size) {
                    path.lineTo(screenPts[i].x, screenPts[i].y)
                }
                drawPath(path = path, color = baseColor, style = Stroke(width = strokeWidth))
            }
        }
        DrawingType.TEXT_NOTE -> {
            val pt = screenPts[0]
            val textStr = if (drawing.text.isNotEmpty()) drawing.text else "Note"

            val p = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 28f
                isFakeBoldText = true
                isAntiAlias = true
            }
            val textW = p.measureText(textStr)
            val noteRect = Rect(pt.x - 10f, pt.y - 36f, pt.x + textW + 20f, pt.y + 12f)

            // Pin marker
            drawCircle(color = baseColor, radius = 6f, center = pt)
            drawRoundRect(
                color = ChartSurfaceElevated.copy(alpha = 0.95f),
                topLeft = noteRect.topLeft,
                size = noteRect.size,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
            )
            drawRoundRect(
                color = baseColor,
                topLeft = noteRect.topLeft,
                size = noteRect.size,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f),
                style = Stroke(width = 1.5f)
            )
            drawContext.canvas.nativeCanvas.drawText(textStr, pt.x, pt.y - 4f, p)
        }
    }

    // Draw control handles and selection border if selected
    if (isSelected) {
        for (pt in screenPts) {
            // White circular handle with colored border
            drawCircle(
                color = Color.White,
                radius = 9f,
                center = pt
            )
            drawCircle(
                color = if (drawing.isLocked) GoldAccent else CyanAccent,
                radius = 9f,
                center = pt,
                style = Stroke(width = 3.5f)
            )
        }
    }
}
