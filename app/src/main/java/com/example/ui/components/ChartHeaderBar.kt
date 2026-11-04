package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChartStyle
import com.example.model.CryptoAsset
import com.example.model.Timeframe
import com.example.ui.theme.CandleBearRedBright
import com.example.ui.theme.CandleBullGreenBright
import com.example.ui.theme.ChartSurface
import com.example.ui.theme.ChartSurfaceElevated
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun ChartHeaderBar(
    currentAsset: CryptoAsset,
    onSelectAsset: (CryptoAsset) -> Unit,
    currentTimeframe: Timeframe,
    onSelectTimeframe: (Timeframe) -> Unit,
    currentPrice: Double,
    priceChange: Double,
    isLiveRunning: Boolean,
    onToggleLive: () -> Unit,
    candleTimeRemainingSec: Int,
    speedMultiplier: Float,
    onChangeSpeed: (Float) -> Unit,
    currentStyle: ChartStyle,
    onSelectStyle: (ChartStyle) -> Unit,
    onOpenIndicators: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAssetMenu by remember { mutableStateOf(false) }
    var showStyleMenu by remember { mutableStateOf(false) }
    var showSpeedMenu by remember { mutableStateOf(false) }

    // Pulsing animation for live ticker
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Surface(
        color = ChartSurface,
        tonalElevation = 4.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, ChartSurfaceElevated),
        modifier = modifier.fillMaxWidth().testTag("chart_header_bar")
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
            // Row 1: Asset selector, Live price, Live badge, Play/Pause
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Asset dropdown button
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ChartSurfaceElevated)
                            .clickable { showAssetMenu = true }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .testTag("asset_selector_button")
                    ) {
                        Text(
                            text = currentAsset.symbol,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Asset",
                            tint = CyanAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showAssetMenu,
                        onDismissRequest = { showAssetMenu = false },
                        modifier = Modifier.background(ChartSurfaceElevated)
                    ) {
                        CryptoAsset.ALL.forEach { asset ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(asset.symbol, color = TextPrimary, fontWeight = FontWeight.Bold)
                                        Text(asset.name, color = TextSecondary, fontSize = 11.sp)
                                    }
                                },
                                onClick = {
                                    onSelectAsset(asset)
                                    showAssetMenu = false
                                }
                            )
                        }
                    }
                }

                // Price display
                val isUp = priceChange >= 0
                val formattedPrice = String.format(Locale.US, "%.${currentAsset.digits}f", currentPrice)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formattedPrice,
                        color = if (isUp) CandleBullGreenBright else CandleBearRedBright,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.testTag("header_current_price")
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isUp) CandleBullGreenBright.copy(alpha = 0.18f) else CandleBearRedBright.copy(alpha = 0.18f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        val sign = if (isUp) "+" else ""
                        val changePct = if (currentAsset.basePrice > 0) (priceChange / currentAsset.basePrice) * 100 else 0.0
                        Text(
                            text = "$sign${String.format(Locale.US, "%.2f", changePct)}%",
                            color = if (isUp) CandleBullGreenBright else CandleBearRedBright,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Live pulse badge & Play/Pause
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Pulsing Live Dot
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(ChartSurfaceElevated)
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .scale(if (isLiveRunning) pulseScale else 1f)
                                    .clip(CircleShape)
                                    .background(if (isLiveRunning) CandleBullGreenBright else GoldAccent)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isLiveRunning) "LIVE" else "PAUSED",
                                color = if (isLiveRunning) CandleBullGreenBright else GoldAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${candleTimeRemainingSec}s",
                                color = TextSecondary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Play/Pause button
                    IconButton(
                        onClick = onToggleLive,
                        modifier = Modifier.size(32.dp).testTag("toggle_live_feed")
                    ) {
                        Icon(
                            imageVector = if (isLiveRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isLiveRunning) "Pause Feed" else "Resume Feed",
                            tint = if (isLiveRunning) CyanAccent else GoldAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Row 2: Timeframe selector pills, Chart Style, Indicators, Speed
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Timeframe pills
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Timeframe.values().forEach { tf ->
                        val isSelected = currentTimeframe == tf
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) CyanAccent.copy(alpha = 0.2f) else ChartSurfaceElevated)
                                .border(
                                    width = if (isSelected) 1.dp else 0.dp,
                                    color = if (isSelected) CyanAccent else Color.Transparent,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable { onSelectTimeframe(tf) }
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                                .testTag("timeframe_${tf.label}")
                        ) {
                            Text(
                                text = tf.label,
                                color = if (isSelected) CyanAccent else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                // Controls: Chart style, Speed, Indicators
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Chart Style Selector
                    Box {
                        IconButton(
                            onClick = { showStyleMenu = true },
                            modifier = Modifier.size(32.dp).testTag("select_chart_style")
                        ) {
                            Icon(
                                imageVector = when (currentStyle) {
                                    ChartStyle.CANDLESTICK, ChartStyle.HOLLOW -> Icons.Default.CandlestickChart
                                    ChartStyle.HEIKIN_ASHI -> Icons.Default.BarChart
                                    ChartStyle.LINE -> Icons.Default.ShowChart
                                },
                                contentDescription = "Chart Style",
                                tint = CyanAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showStyleMenu,
                            onDismissRequest = { showStyleMenu = false },
                            modifier = Modifier.background(ChartSurfaceElevated)
                        ) {
                            ChartStyle.values().forEach { style ->
                                DropdownMenuItem(
                                    text = { Text(style.label, color = TextPrimary) },
                                    onClick = {
                                        onSelectStyle(style)
                                        showStyleMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Speed multiplier
                    Box {
                        IconButton(
                            onClick = { showSpeedMenu = true },
                            modifier = Modifier.size(32.dp).testTag("select_speed")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "Speed",
                                tint = if (speedMultiplier > 1f) GoldAccent else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showSpeedMenu,
                            onDismissRequest = { showSpeedMenu = false },
                            modifier = Modifier.background(ChartSurfaceElevated)
                        ) {
                            listOf(0.5f, 1.0f, 2.0f, 5.0f).forEach { speed ->
                                DropdownMenuItem(
                                    text = { Text("${speed}x Speed", color = TextPrimary) },
                                    onClick = {
                                        onChangeSpeed(speed)
                                        showSpeedMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Indicators Dialog Trigger
                    IconButton(
                        onClick = onOpenIndicators,
                        modifier = Modifier.size(32.dp).testTag("open_indicators_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoGraph,
                            contentDescription = "Technical Indicators",
                            tint = TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
