package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chart.ChartMathUtils
import com.example.engine.LiveMarketState
import com.example.engine.MarketDataEngine
import com.example.engine.TechnicalIndicators
import com.example.model.Candle
import com.example.model.ChartPoint
import com.example.model.ChartStyle
import com.example.model.CryptoAsset
import com.example.model.Drawing
import com.example.model.DrawingToolMode
import com.example.model.DrawingType
import com.example.model.IndicatorType
import com.example.model.Timeframe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class ChartViewModel : ViewModel() {

    private val engine = MarketDataEngine(viewModelScope)
    val marketState: StateFlow<LiveMarketState> = engine.marketState

    // Asset & Timeframe
    private val _selectedAsset = MutableStateFlow(CryptoAsset.ALL.first())
    val selectedAsset: StateFlow<CryptoAsset> = _selectedAsset.asStateFlow()

    private val _timeframe = MutableStateFlow(Timeframe.SEC_5)
    val timeframe: StateFlow<Timeframe> = _timeframe.asStateFlow()

    private val _chartStyle = MutableStateFlow(ChartStyle.CANDLESTICK)
    val chartStyle: StateFlow<ChartStyle> = _chartStyle.asStateFlow()

    private val _activeIndicators = MutableStateFlow(setOf(IndicatorType.EMA20, IndicatorType.VOLUME))
    val activeIndicators: StateFlow<Set<IndicatorType>> = _activeIndicators.asStateFlow()

    private val _speedMultiplier = MutableStateFlow(1.0f)
    val speedMultiplier: StateFlow<Float> = _speedMultiplier.asStateFlow()

    // Viewport state
    private val _visibleCandleCount = MutableStateFlow(36.0)
    val visibleCandleCount: StateFlow<Double> = _visibleCandleCount.asStateFlow()

    // 0.0 means anchored to latest candle. > 0 means scrolled back in time.
    private val _rightCandleIndexOffset = MutableStateFlow(0.0)
    val rightCandleIndexOffset: StateFlow<Double> = _rightCandleIndexOffset.asStateFlow()

    private val _autoScroll = MutableStateFlow(true)
    val autoScroll: StateFlow<Boolean> = _autoScroll.asStateFlow()

    // Drawing tools
    private val _activeToolMode = MutableStateFlow(DrawingToolMode.POINTER)
    val activeToolMode: StateFlow<DrawingToolMode> = _activeToolMode.asStateFlow()

    private val _drawings = MutableStateFlow<List<Drawing>>(emptyList())
    val drawings: StateFlow<List<Drawing>> = _drawings.asStateFlow()

    private val _selectedDrawingId = MutableStateFlow<String?>(null)
    val selectedDrawingId: StateFlow<String?> = _selectedDrawingId.asStateFlow()

    private val _isMagnetEnabled = MutableStateFlow(false)
    val isMagnetEnabled: StateFlow<Boolean> = _isMagnetEnabled.asStateFlow()

    private val _currentDrawingPreview = MutableStateFlow<Drawing?>(null)
    val currentDrawingPreview: StateFlow<Drawing?> = _currentDrawingPreview.asStateFlow()

    private val _isDrawingsHidden = MutableStateFlow(false)
    val isDrawingsHidden: StateFlow<Boolean> = _isDrawingsHidden.asStateFlow()

    // Undo / Redo history
    private val undoStack = mutableListOf<List<Drawing>>()
    private val redoStack = mutableListOf<List<Drawing>>()

    // Crosshair inspection
    private val _crosshairPoint = MutableStateFlow<ChartPoint?>(null)
    val crosshairPoint: StateFlow<ChartPoint?> = _crosshairPoint.asStateFlow()

    val inspectedCandle: StateFlow<Candle?> = combine(marketState, _crosshairPoint) { state, point ->
        if (point == null) return@combine null
        val allCandles = state.candles + state.formingCandle
        allCandles.minByOrNull { kotlin.math.abs(it.index - point.candleIndex) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Precomputed technical indicators
    val ema20Values: StateFlow<Map<Long, Double>> = combine(marketState, activeIndicators) { state, indicators ->
        if (IndicatorType.EMA20 in indicators) {
            val allCandles = state.candles + state.formingCandle
            TechnicalIndicators.calculateEMA(allCandles, 20)
        } else emptyMap()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val ema50Values: StateFlow<Map<Long, Double>> = combine(marketState, activeIndicators) { state, indicators ->
        if (IndicatorType.EMA50 in indicators) {
            val allCandles = state.candles + state.formingCandle
            TechnicalIndicators.calculateEMA(allCandles, 50)
        } else emptyMap()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val bollingerBands: StateFlow<Map<Long, com.example.engine.BollingerBandPoint>> = combine(marketState, activeIndicators) { state, indicators ->
        if (IndicatorType.BOLLINGER_BANDS in indicators) {
            val allCandles = state.candles + state.formingCandle
            TechnicalIndicators.calculateBollingerBands(allCandles, 20)
        } else emptyMap()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // --- Market actions ---
    fun selectAsset(asset: CryptoAsset) {
        _selectedAsset.value = asset
        engine.setAsset(asset)
        scrollToLatest()
    }

    fun selectTimeframe(tf: Timeframe) {
        _timeframe.value = tf
        engine.setTimeframe(tf)
        scrollToLatest()
    }

    fun toggleLivePause() {
        engine.togglePausePlay()
    }

    fun setSpeedMultiplier(mult: Float) {
        _speedMultiplier.value = mult
        engine.setSpeed(mult)
    }

    fun setChartStyle(style: ChartStyle) {
        _chartStyle.value = style
    }

    fun toggleIndicator(indicator: IndicatorType) {
        val current = _activeIndicators.value
        _activeIndicators.value = if (indicator in current) current - indicator else current + indicator
    }

    // --- Viewport actions ---
    fun onPan(deltaCandles: Double) {
        val newOffset = (_rightCandleIndexOffset.value + deltaCandles).coerceAtLeast(-6.0)
        _rightCandleIndexOffset.value = newOffset
        _autoScroll.value = (newOffset <= 1.0)
    }

    fun onZoom(zoomFactor: Float) {
        val newCount = (_visibleCandleCount.value / zoomFactor).coerceIn(12.0, 110.0)
        _visibleCandleCount.value = newCount
    }

    fun scrollToLatest() {
        _rightCandleIndexOffset.value = 0.0
        _autoScroll.value = true
    }

    // --- Drawing Tool selection & Magnet ---
    fun setToolMode(mode: DrawingToolMode) {
        _activeToolMode.value = mode
        if (mode != DrawingToolMode.POINTER) {
            _selectedDrawingId.value = null
        }
    }

    fun toggleMagnet() {
        _isMagnetEnabled.value = !_isMagnetEnabled.value
    }

    fun toggleDrawingsVisibility() {
        _isDrawingsHidden.value = !_isDrawingsHidden.value
    }

    // --- Drawing creation ---
    fun onDrawingTouchStart(rawPoint: ChartPoint) {
        val point = if (_isMagnetEnabled.value) {
            val s = marketState.value
            ChartMathUtils.snapToCandleOHLC(rawPoint, s.candles, s.formingCandle)
        } else rawPoint

        val mode = _activeToolMode.value
        val type = when (mode) {
            DrawingToolMode.TRENDLINE -> DrawingType.TRENDLINE
            DrawingToolMode.HORIZONTAL -> DrawingType.HORIZONTAL_LINE
            DrawingToolMode.RAY -> DrawingType.RAY
            DrawingToolMode.FIBONACCI -> DrawingType.FIBONACCI
            DrawingToolMode.RECTANGLE -> DrawingType.RECTANGLE
            DrawingToolMode.ARROW -> DrawingType.ARROW
            DrawingToolMode.BRUSH -> DrawingType.BRUSH
            DrawingToolMode.TEXT_NOTE -> DrawingType.TEXT_NOTE
            else -> return
        }

        if (type == DrawingType.HORIZONTAL_LINE) {
            // Horizontal line completes on single tap
            val d = Drawing(
                type = DrawingType.HORIZONTAL_LINE,
                points = listOf(point),
                colorHex = 0xFFFFD600
            )
            saveDrawingToHistory(d)
            _selectedDrawingId.value = d.id
            _activeToolMode.value = DrawingToolMode.POINTER
            return
        }

        if (type == DrawingType.TEXT_NOTE) {
            val d = Drawing(
                type = DrawingType.TEXT_NOTE,
                points = listOf(point),
                colorHex = 0xFF00E5FF,
                text = "Key Level"
            )
            saveDrawingToHistory(d)
            _selectedDrawingId.value = d.id
            _activeToolMode.value = DrawingToolMode.POINTER
            return
        }

        // 2-point or brush drawing starts preview
        _currentDrawingPreview.value = Drawing(
            type = type,
            points = listOf(point, point)
        )
    }

    fun onDrawingTouchMove(rawPoint: ChartPoint) {
        val preview = _currentDrawingPreview.value ?: return
        val point = if (_isMagnetEnabled.value) {
            val s = marketState.value
            ChartMathUtils.snapToCandleOHLC(rawPoint, s.candles, s.formingCandle)
        } else rawPoint

        if (preview.type == DrawingType.BRUSH) {
            _currentDrawingPreview.value = preview.copy(
                points = preview.points + point
            )
        } else {
            _currentDrawingPreview.value = preview.copy(
                points = listOf(preview.points[0], point)
            )
        }
    }

    fun onDrawingTouchEnd() {
        val preview = _currentDrawingPreview.value ?: return
        _currentDrawingPreview.value = null

        // Only save if meaningful
        if (preview.points.size >= 2) {
            val p0 = preview.points[0]
            val p1 = preview.points[1]
            val dist = kotlin.math.abs(p0.candleIndex - p1.candleIndex) + kotlin.math.abs(p0.price - p1.price)
            if (dist > 0.0001 || preview.type == DrawingType.BRUSH) {
                saveDrawingToHistory(preview)
                _selectedDrawingId.value = preview.id
            }
        }
        // Revert to pointer so user can immediately move, lock, or delete their new drawing!
        _activeToolMode.value = DrawingToolMode.POINTER
    }

    // --- Drawing Selection, Move, Lock, Delete ---
    fun selectDrawing(id: String?) {
        _selectedDrawingId.value = id
    }

    fun moveSelectedDrawingBy(deltaPrice: Double, deltaCandleIndex: Double) {
        val id = _selectedDrawingId.value ?: return
        val currentList = _drawings.value
        val drawing = currentList.find { it.id == id } ?: return
        if (drawing.isLocked) return // Cannot move locked drawing!

        val updatedPoints = drawing.points.map { pt ->
            pt.copy(
                price = pt.price + deltaPrice,
                candleIndex = pt.candleIndex + deltaCandleIndex
            )
        }
        val updatedDrawing = drawing.copy(points = updatedPoints)
        _drawings.value = currentList.map { if (it.id == id) updatedDrawing else it }
    }

    fun moveHandleBy(handleIndex: Int, targetPoint: ChartPoint) {
        val id = _selectedDrawingId.value ?: return
        val currentList = _drawings.value
        val drawing = currentList.find { it.id == id } ?: return
        if (drawing.isLocked) return

        if (handleIndex in drawing.points.indices) {
            val updatedPoints = drawing.points.toMutableList()
            updatedPoints[handleIndex] = targetPoint
            val updated = drawing.copy(points = updatedPoints)
            _drawings.value = currentList.map { if (it.id == id) updated else it }
        }
    }

    fun toggleLock(id: String) {
        recordUndoState()
        val currentList = _drawings.value
        _drawings.value = currentList.map {
            if (it.id == id) it.copy(isLocked = !it.isLocked) else it
        }
    }

    fun deleteDrawing(id: String) {
        recordUndoState()
        _drawings.value = _drawings.value.filter { it.id != id }
        if (_selectedDrawingId.value == id) {
            _selectedDrawingId.value = null
        }
    }

    fun deleteSelected() {
        val id = _selectedDrawingId.value ?: return
        deleteDrawing(id)
    }

    fun updateDrawingColor(id: String, colorHex: Long) {
        recordUndoState()
        _drawings.value = _drawings.value.map {
            if (it.id == id) it.copy(colorHex = colorHex) else it
        }
    }

    fun updateDrawingStroke(id: String, width: Float) {
        recordUndoState()
        _drawings.value = _drawings.value.map {
            if (it.id == id) it.copy(strokeWidth = width) else it
        }
    }

    fun updateDrawingDashed(id: String, isDashed: Boolean) {
        recordUndoState()
        _drawings.value = _drawings.value.map {
            if (it.id == id) it.copy(isDashed = isDashed) else it
        }
    }

    fun updateDrawingText(id: String, text: String) {
        recordUndoState()
        _drawings.value = _drawings.value.map {
            if (it.id == id) it.copy(text = text) else it
        }
    }

    fun lockAllDrawings() {
        recordUndoState()
        _drawings.value = _drawings.value.map { it.copy(isLocked = true) }
    }

    fun unlockAllDrawings() {
        recordUndoState()
        _drawings.value = _drawings.value.map { it.copy(isLocked = false) }
    }

    fun clearAllDrawings() {
        if (_drawings.value.isNotEmpty()) {
            recordUndoState()
            _drawings.value = emptyList()
            _selectedDrawingId.value = null
        }
    }

    // --- Undo / Redo ---
    private fun recordUndoState() {
        undoStack.add(_drawings.value)
        if (undoStack.size > 30) undoStack.removeAt(0)
        redoStack.clear()
    }

    private fun saveDrawingToHistory(drawing: Drawing) {
        recordUndoState()
        _drawings.value = _drawings.value + drawing
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val previous = undoStack.removeAt(undoStack.lastIndex)
            redoStack.add(_drawings.value)
            _drawings.value = previous
            if (_selectedDrawingId.value !in previous.map { it.id }) {
                _selectedDrawingId.value = null
            }
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.lastIndex)
            undoStack.add(_drawings.value)
            _drawings.value = next
        }
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    // --- Crosshair ---
    fun setCrosshair(point: ChartPoint?) {
        _crosshairPoint.value = point
    }
}
