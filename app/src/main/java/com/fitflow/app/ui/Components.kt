package com.fitflow.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fitflow.app.ui.theme.Caccent
import com.fitflow.app.ui.theme.Cdanger
import com.fitflow.app.ui.theme.Cline
import com.fitflow.app.ui.theme.Cmuted
import com.fitflow.app.ui.theme.Csurface
import com.fitflow.app.ui.theme.Csurface2
import com.fitflow.app.ui.theme.Csurface3
import com.fitflow.app.ui.theme.Ctext

/** 顶栏：可选返回键 + 标题 + 右侧操作 */
@Composable
fun TopBar(title: String, onBack: (() -> Unit)? = null, actions: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "返回", tint = Ctext) }
        } else {
            Spacer(Modifier.width(48.dp))
        }
        Text(title, style = MaterialTheme.typography.titleMedium, color = Ctext,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        if (actions != null) {
            actions()
        } else {
            Spacer(Modifier.width(48.dp))
        }
    }
}

/** 主/次/危险 按钮 */
@Composable
fun FfButton(text: String, onClick: () -> Unit, primary: Boolean = false,
             danger: Boolean = false, modifier: Modifier = Modifier) {
    val bg = when { primary -> Caccent; danger -> Color(0x1AFF5470); else -> Csurface2 }
    val fg = when { primary -> Color(0xFF10130A); danger -> Cdanger; else -> Ctext }
    Box(modifier.clip(RoundedCornerShape(12.dp)).background(bg)
        .border(BorderStroke(if (primary || danger) 0.dp else 1.dp, Cline), RoundedCornerShape(12.dp))
        .clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = fg)
    }
}

/** 胶囊标签 */
@Composable
fun Chip(text: String, accent: Boolean = false) {
    val border = if (accent) Color(0x40C6FF3D) else Cline
    val bg = if (accent) Color(0x1AC6FF3D) else Csurface2
    val fg = if (accent) Caccent else Cmuted
    Box(Modifier.clip(CircleShape).background(bg).border(BorderStroke(1.dp, border), CircleShape)
        .padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = fg)
    }
}

/** 分段选择：如 按个数/按时间 */
@Composable
fun SegmentRow(labels: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Csurface2)
        .border(BorderStroke(1.dp, Cline), RoundedCornerShape(12.dp)).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        labels.forEachIndexed { i, label ->
            val on = i == selectedIndex
            Box(Modifier.weight(1f).clip(RoundedCornerShape(9.dp))
                .background(if (on) Csurface3 else Color.Transparent)
                .clickable { onSelect(i) }.padding(vertical = 8.dp),
                contentAlignment = Alignment.Center) {
                Text(label, color = if (on) Caccent else Cmuted, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** 整数值编辑行：- n + */
@Composable
fun NumberField(label: String, value: Int, unit: String, min: Int = 0, max: Int = 9999,
                step: Int = 1, onChange: (Int) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Cmuted)
        Spacer(Modifier.size(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StepButton("-") { onChange(((value - step).coerceAtLeast(min))) }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$value", style = MaterialTheme.typography.titleLarge,
                    color = Ctext, fontWeight = FontWeight.ExtraBold)
                Text(unit, style = MaterialTheme.typography.labelMedium, color = Cmuted)
            }
            StepButton("+") { onChange(((value + step).coerceAtMost(max))) }
        }
    }
}

@Composable
fun StepButton(text: String, onClick: () -> Unit) {
    Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(Csurface2)
        .clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(text, color = Ctext, style = MaterialTheme.typography.titleMedium)
    }
}

/** 输入弹窗 */
@Composable
fun PromptDialog(title: String, initial: String, confirmText: String = "保存",
                 onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(onDismissRequest = onDismiss, containerColor = Csurface,
        titleContentColor = Ctext, textContentColor = Ctext,
        title = { Text(title) },
        text = {
            OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = Ctext),
                colors = tfColors())
        },
        confirmButton = { TextButton(onClick = { onConfirm(text.trim()) }) { Text(confirmText, color = Caccent) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = Cmuted) } }
    )
}

/** 确认弹窗 */
@Composable
fun ConfirmDialog(title: String, message: String, danger: Boolean = false,
                  onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, containerColor = Csurface,
        titleContentColor = Ctext, textContentColor = Ctext,
        title = { Text(title) }, text = { Text(message) },
        confirmButton = {
            TextButton(onClick = { onConfirm() }) {
                Text(if (danger) "删除" else "确定", color = if (danger) Cdanger else Caccent)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = Cmuted) } }
    )
}

@androidx.compose.runtime.Composable
fun tfColors() = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Caccent, unfocusedBorderColor = Cline, cursorColor = Caccent,
    focusedTextColor = Ctext, unfocusedTextColor = Ctext,
    focusedLabelColor = Cmuted, unfocusedLabelColor = Cmuted
)
