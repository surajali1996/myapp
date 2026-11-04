package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CropLandscape
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LineWeight
import androidx.compose.material.icons.filled.MenuOpen
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.ShortText
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DrawingToolMode
import com.example.ui.theme.ChartSurface
import com.example.ui.theme.ChartSurfaceElevated
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

data class ToolItem(
    val mode: DrawingToolMode,
    val icon: ImageVector,
    val label: String
)

private val TOOLS = listOf(
    ToolItem(DrawingToolMode.POINTER, Icons.Default.NearMe, "Pointer / Select & Move"),
    ToolItem(DrawingToolMode.TRENDLINE, Icons.Default.Timeline, "Trendline"),
    ToolItem(DrawingToolMode.HORIZONTAL, Icons.Default.LineWeight, "Horizontal Support/Resistance"),
    ToolItem(DrawingToolMode.RAY, Icons.AutoMirrored.Filled.TrendingFlat, "Ray Line"),
    ToolItem(DrawingToolMode.FIBONACCI, Icons.Default.FormatListNumbered, "Fibonacci Retracement"),
    ToolItem(DrawingToolMode.RECTANGLE, Icons.Default.CropLandscape, "Demand/Supply Zone"),
    ToolItem(DrawingToolMode.ARROW, Icons.Default.NorthEast, "Trend Arrow"),
    ToolItem(DrawingToolMode.BRUSH, Icons.Default.Brush, "Freehand Brush"),
    ToolItem(DrawingToolMode.TEXT_NOTE, Icons.Default.ShortText, "Price / Text Note")
)

@Composable
fun DrawingToolRail(
    activeMode: DrawingToolMode,
    onSelectMode: (DrawingToolMode) -> Unit,
    isMagnetEnabled: Boolean,
    onToggleMagnet: () -> Unit,
    isDrawingsHidden: Boolean,
    onToggleVisibility: () -> Unit,
    drawingsCount: Int,
    onOpenManager: () -> Unit,
    canUndo: Boolean,
    onUndo: () -> Unit,
    canRedo: Boolean,
    onRedo: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = ChartSurface.copy(alpha = 0.95f),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, ChartSurfaceElevated),
        modifier = modifier.testTag("drawing_tool_rail")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp)
        ) {
            // Expand / Collapse Drawer toggle
            IconButton(
                onClick = { isExpanded = !isExpanded },
                modifier = Modifier.size(38.dp).testTag("toggle_tools_drawer")
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.MenuOpen else Icons.Default.Tune,
                    contentDescription = "Toggle Drawing Tools",
                    tint = if (isExpanded) CyanAccent else TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Always visible primary mode: Pointer
            ToolIconButton(
                icon = Icons.Default.NearMe,
                contentDescription = "Pointer (Select & Move)",
                isSelected = activeMode == DrawingToolMode.POINTER,
                onClick = { onSelectMode(DrawingToolMode.POINTER) },
                testTag = "tool_pointer"
            )

            // When collapsed, show active tool if not pointer
            if (!isExpanded && activeMode != DrawingToolMode.POINTER) {
                val currentTool = TOOLS.find { it.mode == activeMode }
                if (currentTool != null) {
                    ToolIconButton(
                        icon = currentTool.icon,
                        contentDescription = currentTool.label,
                        isSelected = true,
                        onClick = { /* already selected */ },
                        testTag = "tool_active_selected"
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(top = 2.dp)
                ) {
                    // List all specific drawing tools
                    TOOLS.filter { it.mode != DrawingToolMode.POINTER }.forEach { tool ->
                        ToolIconButton(
                            icon = tool.icon,
                            contentDescription = tool.label,
                            isSelected = activeMode == tool.mode,
                            onClick = {
                                onSelectMode(tool.mode)
                                isExpanded = false
                            },
                            testTag = "tool_${tool.mode.name.lowercase()}"
                        )
                    }

                    Spacer(
                        modifier = Modifier
                            .height(1.dp)
                            .width(24.dp)
                            .background(ChartSurfaceElevated)
                    )

                    // Magnet snap toggle button
                    IconButton(
                        onClick = onToggleMagnet,
                        modifier = Modifier.size(38.dp).testTag("toggle_magnet")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gesture,
                            contentDescription = "Magnet Snap",
                            tint = if (isMagnetEnabled) CyanAccent else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Visibility toggle
                    IconButton(
                        onClick = onToggleVisibility,
                        modifier = Modifier.size(38.dp).testTag("toggle_visibility")
                    ) {
                        Icon(
                            imageVector = if (isDrawingsHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle Drawings Visibility",
                            tint = if (isDrawingsHidden) GoldAccent else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Undo button
                    IconButton(
                        onClick = onUndo,
                        enabled = canUndo,
                        modifier = Modifier.size(38.dp).testTag("tool_undo")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = if (canUndo) TextPrimary else TextSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Redo button
                    IconButton(
                        onClick = onRedo,
                        enabled = canRedo,
                        modifier = Modifier.size(38.dp).testTag("tool_redo")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            tint = if (canRedo) TextPrimary else TextSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(
                modifier = Modifier
                    .height(1.dp)
                    .width(24.dp)
                    .background(ChartSurfaceElevated)
            )

            // Drawings Manager Button with badge
            IconButton(
                onClick = onOpenManager,
                modifier = Modifier.size(38.dp).testTag("open_drawings_manager")
            ) {
                BadgedBox(
                    badge = {
                        if (drawingsCount > 0) {
                            Badge(
                                containerColor = CyanAccent,
                                contentColor = Color.Black
                            ) {
                                Text(drawingsCount.toString(), fontSize = 9.sp)
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = "Drawings Manager",
                        tint = if (drawingsCount > 0) CyanAccent else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolIconButton(
    icon: ImageVector,
    contentDescription: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(vertical = 2.dp)
            .size(36.dp)
            .clip(CircleShape)
            .background(if (isSelected) CyanAccent.copy(alpha = 0.22f) else Color.Transparent)
            .border(
                width = if (isSelected) 1.5.dp else 0.dp,
                color = if (isSelected) CyanAccent else Color.Transparent,
                shape = CircleShape
            )
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isSelected) CyanAccent else TextSecondary,
            modifier = Modifier.size(19.dp)
        )
    }
}
