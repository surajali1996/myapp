package com.example.ui.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatLineSpacing
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Drawing
import com.example.model.DrawingType
import com.example.ui.theme.CandleBearRedBright
import com.example.ui.theme.CandleBullGreenBright
import com.example.ui.theme.ChartSurfaceElevated
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

private val QUICK_COLORS = listOf(
    0xFF00E5FF, // Cyan
    0xFFFFD600, // Gold
    0xFF00E676, // Emerald
    0xFFFF5252, // Coral Red
    0xFFD500F9, // Purple
    0xFFFFFFFF  // White
)

@Composable
fun DrawingQuickToolbar(
    selectedDrawing: Drawing?,
    onToggleLock: (String) -> Unit,
    onDelete: (String) -> Unit,
    onChangeColor: (String, Long) -> Unit,
    onChangeStroke: (String, Float) -> Unit,
    onChangeText: (String, String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showTextDialog by remember { mutableStateOf(false) }
    var editTextValue by remember { mutableStateOf("") }

    if (showTextDialog && selectedDrawing != null) {
        AlertDialog(
            onDismissRequest = { showTextDialog = false },
            title = { Text("Edit Note Text", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = editTextValue,
                    onValueChange = { editTextValue = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_note_input")
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onChangeText(selectedDrawing.id, editTextValue)
                        showTextDialog = false
                    },
                    modifier = Modifier.testTag("save_note_button")
                ) {
                    Text("Save", color = CyanAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTextDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = ChartSurfaceElevated
        )
    }

    AnimatedVisibility(
        visible = selectedDrawing != null,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier
    ) {
        if (selectedDrawing != null) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = ChartSurfaceElevated.copy(alpha = 0.96f),
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (selectedDrawing.isLocked) GoldAccent.copy(alpha = 0.7f) else CyanAccent.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("drawing_quick_toolbar")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    // Header row: Type badge, lock status, and actions
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(selectedDrawing.colorHex))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = selectedDrawing.type.title,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (selectedDrawing.isLocked) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "• LOCKED",
                                    color = GoldAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Lock / Unlock toggle button
                            IconButton(
                                onClick = { onToggleLock(selectedDrawing.id) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("toolbar_lock_button")
                            ) {
                                Icon(
                                    imageVector = if (selectedDrawing.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = if (selectedDrawing.isLocked) "Unlock Drawing" else "Lock Drawing",
                                    tint = if (selectedDrawing.isLocked) GoldAccent else TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Text Edit button (if note)
                            if (selectedDrawing.type == DrawingType.TEXT_NOTE) {
                                IconButton(
                                    onClick = {
                                        editTextValue = selectedDrawing.text
                                        showTextDialog = true
                                    },
                                    modifier = Modifier.size(36.dp).testTag("toolbar_edit_text_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Text",
                                        tint = CyanAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Stroke width toggle button
                            IconButton(
                                onClick = {
                                    val nextStroke = when {
                                        selectedDrawing.strokeWidth < 2f -> 2.5f
                                        selectedDrawing.strokeWidth < 3.5f -> 4.5f
                                        else -> 1.5f
                                    }
                                    onChangeStroke(selectedDrawing.id, nextStroke)
                                },
                                modifier = Modifier.size(36.dp).testTag("toolbar_stroke_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FormatLineSpacing,
                                    contentDescription = "Change Stroke Width",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Delete button
                            IconButton(
                                onClick = { onDelete(selectedDrawing.id) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("toolbar_delete_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Drawing",
                                    tint = CandleBearRedBright,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Close / Deselect button
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(36.dp).testTag("toolbar_dismiss_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Deselect",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Row of color chips & interaction hint
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Color chips
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            QUICK_COLORS.forEach { colorHex ->
                                val isSelected = selectedDrawing.colorHex == colorHex
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(Color(colorHex))
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) Color.White else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { onChangeColor(selectedDrawing.id, colorHex) }
                                )
                            }
                        }

                        // Helpful movement hint
                        Text(
                            text = if (selectedDrawing.isLocked) {
                                "🔒 Locked (tap unlock to edit)"
                            } else {
                                "✥ Drag handles or body"
                            },
                            color = if (selectedDrawing.isLocked) GoldAccent else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
