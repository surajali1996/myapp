package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.DrawingToolMode
import com.example.ui.components.ChartHeaderBar
import com.example.ui.components.ChartIndicatorsDialog
import com.example.ui.components.DrawingManagerDialog
import com.example.ui.components.DrawingQuickToolbar
import com.example.ui.components.DrawingToolRail
import com.example.ui.theme.CandleBearRedBright
import com.example.ui.theme.CandleBullGreenBright
import com.example.ui.theme.ChartBackground
import com.example.ui.theme.ChartSurface
import com.example.ui.theme.ChartSurfaceElevated
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.ChartViewModel
import java.util.Locale

@Composable
fun ChartScreen(
    viewModel: ChartViewModel,
    modifier: Modifier = Modifier
) {
    val marketState by viewModel.marketState.collectAsStateWithLifecycle()
    val asset by viewModel.selectedAsset.collectAsStateWithLifecycle()
    val timeframe by viewModel.timeframe.collectAsStateWithLifecycle()
    val chartStyle by viewModel.chartStyle.collectAsStateWithLifecycle()
    val activeIndicators by viewModel.activeIndicators.collectAsStateWithLifecycle()
    val speedMultiplier by viewModel.speedMultiplier.collectAsStateWithLifecycle()

    val visibleCandleCount by viewModel.visibleCandleCount.collectAsStateWithLifecycle()
    val rightCandleIndexOffset by viewModel.rightCandleIndexOffset.collectAsStateWithLifecycle()
    val autoScroll by viewModel.autoScroll.collectAsStateWithLifecycle()

    val activeToolMode by viewModel.activeToolMode.collectAsStateWithLifecycle()
    val drawings by viewModel.drawings.collectAsStateWithLifecycle()
    val selectedDrawingId by viewModel.selectedDrawingId.collectAsStateWithLifecycle()
    val previewDrawing by viewModel.currentDrawingPreview.collectAsStateWithLifecycle()
    val isMagnetEnabled by viewModel.isMagnetEnabled.collectAsStateWithLifecycle()
    val isDrawingsHidden by viewModel.isDrawingsHidden.collectAsStateWithLifecycle()

    val ema20Values by viewModel.ema20Values.collectAsStateWithLifecycle()
    val ema50Values by viewModel.ema50Values.collectAsStateWithLifecycle()
    val bollingerBands by viewModel.bollingerBands.collectAsStateWithLifecycle()

    var showManagerDialog by remember { mutableStateOf(false) }
    var showIndicatorsDialog by remember { mutableStateOf(false) }

    val selectedDrawing = remember(drawings, selectedDrawingId) {
        drawings.find { it.id == selectedDrawingId }
    }

    if (showManagerDialog) {
        DrawingManagerDialog(
            drawings = drawings,
            selectedId = selectedDrawingId,
            onSelectDrawing = { id ->
                viewModel.selectDrawing(id)
                showManagerDialog = false
            },
            onToggleLock = { viewModel.toggleLock(it) },
            onDeleteDrawing = { viewModel.deleteDrawing(it) },
            onLockAll = { viewModel.lockAllDrawings() },
            onUnlockAll = { viewModel.unlockAllDrawings() },
            onClearAll = { viewModel.clearAllDrawings() },
            onDismiss = { showManagerDialog = false }
        )
    }

    if (showIndicatorsDialog) {
        ChartIndicatorsDialog(
            activeIndicators = activeIndicators,
            onToggleIndicator = { viewModel.toggleIndicator(it) },
            onDismiss = { showIndicatorsDialog = false }
        )
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(ChartBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("chart_screen_scaffold"),
        topBar = {
            ChartHeaderBar(
                currentAsset = asset,
                onSelectAsset = { viewModel.selectAsset(it) },
                currentTimeframe = timeframe,
                onSelectTimeframe = { viewModel.selectTimeframe(it) },
                currentPrice = marketState.formingCandle.close,
                priceChange = marketState.lastTickChange,
                isLiveRunning = marketState.isRunning,
                onToggleLive = { viewModel.toggleLivePause() },
                candleTimeRemainingSec = marketState.candleTimeRemainingSec,
                speedMultiplier = speedMultiplier,
                onChangeSpeed = { viewModel.setSpeedMultiplier(it) },
                currentStyle = chartStyle,
                onSelectStyle = { viewModel.setChartStyle(it) },
                onOpenIndicators = { showIndicatorsDialog = true }
            )
        },
        bottomBar = {
            // Bottom Status Info Bar
            Surface(
                color = ChartSurface,
                tonalElevation = 2.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, ChartSurfaceElevated),
                modifier = Modifier.fillMaxWidth().testTag("bottom_status_bar")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    val forming = marketState.formingCandle
                    val isBull = forming.close >= forming.open
                    val priceColor = if (isBull) CandleBullGreenBright else CandleBearRedBright

                    // OHLC values
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "O: ${String.format(Locale.US, "%.${asset.digits}f", forming.open)}",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "H: ${String.format(Locale.US, "%.${asset.digits}f", forming.high)}",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "L: ${String.format(Locale.US, "%.${asset.digits}f", forming.low)}",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "C: ${String.format(Locale.US, "%.${asset.digits}f", forming.close)}",
                            color = priceColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Mode & drawings status
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isMagnetEnabled) {
                            Text(
                                text = "🧲 SNAP",
                                color = CyanAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        Text(
                            text = if (activeToolMode == DrawingToolMode.POINTER) "Select/Move" else activeToolMode.name,
                            color = if (activeToolMode == DrawingToolMode.POINTER) TextSecondary else GoldAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main Chart Canvas
            ChartCanvas(
                candles = marketState.candles,
                formingCandle = marketState.formingCandle,
                asset = asset,
                chartStyle = chartStyle,
                activeIndicators = activeIndicators,
                ema20Values = ema20Values,
                ema50Values = ema50Values,
                bollingerBands = bollingerBands,
                drawings = drawings,
                selectedDrawingId = selectedDrawingId,
                previewDrawing = previewDrawing,
                isDrawingsHidden = isDrawingsHidden,
                activeMode = activeToolMode,
                isMagnetEnabled = isMagnetEnabled,
                visibleCandleCount = visibleCandleCount,
                rightCandleIndexOffset = rightCandleIndexOffset,
                onPan = { viewModel.onPan(it) },
                onZoom = { viewModel.onZoom(it) },
                onSelectDrawing = { viewModel.selectDrawing(it) },
                onMoveSelectedBy = { dPrice, dIdx -> viewModel.moveSelectedDrawingBy(dPrice, dIdx) },
                onMoveHandleBy = { handleIdx, pt -> viewModel.moveHandleBy(handleIdx, pt) },
                onDrawingTouchStart = { viewModel.onDrawingTouchStart(it) },
                onDrawingTouchMove = { viewModel.onDrawingTouchMove(it) },
                onDrawingTouchEnd = { viewModel.onDrawingTouchEnd() }
            )

            // Left Drawing Tool Rail
            DrawingToolRail(
                activeMode = activeToolMode,
                onSelectMode = { viewModel.setToolMode(it) },
                isMagnetEnabled = isMagnetEnabled,
                onToggleMagnet = { viewModel.toggleMagnet() },
                isDrawingsHidden = isDrawingsHidden,
                onToggleVisibility = { viewModel.toggleDrawingsVisibility() },
                drawingsCount = drawings.size,
                onOpenManager = { showManagerDialog = true },
                canUndo = viewModel.canUndo(),
                onUndo = { viewModel.undo() },
                canRedo = viewModel.canRedo(),
                onRedo = { viewModel.redo() },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 8.dp, top = 8.dp)
            )

            // Floating Quick Toolbar (when a drawing is selected)
            DrawingQuickToolbar(
                selectedDrawing = selectedDrawing,
                onToggleLock = { viewModel.toggleLock(it) },
                onDelete = { viewModel.deleteDrawing(it) },
                onChangeColor = { id, color -> viewModel.updateDrawingColor(id, color) },
                onChangeStroke = { id, stroke -> viewModel.updateDrawingStroke(id, stroke) },
                onChangeText = { id, text -> viewModel.updateDrawingText(id, text) },
                onDismiss = { viewModel.selectDrawing(null) },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            )

            // "Jump to Latest [↓]" button (appears when user has scrolled back in history)
            AnimatedVisibility(
                visible = rightCandleIndexOffset > 4.0,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp)
            ) {
                FloatingActionButton(
                    onClick = { viewModel.scrollToLatest() },
                    containerColor = ChartSurfaceElevated,
                    contentColor = CyanAccent,
                    elevation = FloatingActionButtonDefaults.elevation(6.dp),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("jump_to_latest_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Jump to Latest Candle",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Latest", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
