package com.fitflow.app.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.fitflow.app.data.Exercise
import com.fitflow.app.data.Mode
import com.fitflow.app.data.Store
import com.fitflow.app.data.VideoFiles
import com.fitflow.app.data.randomId
import com.fitflow.app.ui.theme.Caccent
import com.fitflow.app.ui.theme.Cbg
import com.fitflow.app.ui.theme.Cdanger
import com.fitflow.app.ui.theme.Cline
import com.fitflow.app.ui.theme.Cmuted
import com.fitflow.app.ui.theme.Csurface
import com.fitflow.app.ui.theme.Csurface2
import com.fitflow.app.ui.theme.Csurface3
import com.fitflow.app.ui.theme.Ctext
import androidx.compose.foundation.lazy.itemsIndexed

/** 动作库：内置 + 我的自定义动作，支持给动作绑定本地视频或网络链接 */
@Composable
fun LibraryScreen() {
    val ctx = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    val all = remember(tick) { Store.loadLibrary(ctx) }
    val customs = remember(tick) { Store.loadCustom(ctx) }

    var creating by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Exercise?>(null) }
    var importing by remember { mutableStateOf(false) }
    var previewUri by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun toast(msg: String) {
        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
    }

    // 本地视频选择：兼容「单击即返回」和「长按多选后点添加」，统一取第一个
    val videoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        editing?.let { ex ->
            val uri = uris.firstOrNull()
            if (uri == null) {
                toast("未获取到视频，请重新选择")
                return@let
            }
            importing = true
            scope.launch {
                val path = withContext(Dispatchers.IO) { VideoFiles.import(ctx, uri) }
                importing = false
                if (path == null) {
                    toast("视频导入失败，请换一个文件试试")
                    return@launch
                }
                // 替换前删除旧本地文件
                if (!VideoFiles.isNetwork(ex.videoUri)) VideoFiles.deleteIfLocal(ex.videoUri)
                Store.bindVideo(ctx, ex.id, path)
                tick++
                editing = null           // 绑完直接关掉弹窗，列表会显示 ▶ 预览
                toast("视频已添加 ✓")
            }
        }
    }

    Column(Modifier.fillMaxSize().background(Cbg).statusBarsPadding()) {
        TopBar(title = "动作库", actions = {
            IconButton(onClick = { creating = true }) { Icon(Icons.Default.Add, "新建动作", tint = Ctext) }
        })
        if (all.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("还没有动作，点右上角 + 新建", color = Cmuted)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp)
            ) {
                itemsIndexed(all) { _, ex ->
                    ExerciseCard(
                        ex = ex,
                        onClick = { editing = ex },
                        onPreview = { previewUri = ex.videoUri }
                    )
                }
            }
        }
    }

    // 新建动作
    if (creating) {
        PromptDialog(
            title = "新建动作",
            initial = "",
            confirmText = "创建",
            onDismiss = { creating = false },
            onConfirm = { name ->
                creating = false
                if (name.isBlank()) return@PromptDialog
                Store.addCustom(
                    ctx,
                    Exercise(
                        id = randomId(), name = name.trim(), figure = "generic",
                        mode = Mode.TIME, value = 30, sets = 3, rest = 30, tempo = 1.0,
                        tip = "", videoUri = null, builtin = false
                    )
                )
                tick++
                // 创建后自动进入编辑，方便立刻绑视频
                val created = Store.loadCustom(ctx).lastOrNull()
                if (created != null) editing = created
            }
        )
    }

    // 编辑/绑视频 详情
    editing?.let { ex ->
        var deleting by remember { mutableStateOf(false) }
        var linkMode by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { editing = null },
            containerColor = Csurface,
            titleContentColor = Ctext,
            textContentColor = Ctext,
            title = { Text(ex.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip(if (ex.builtin) "内置" else "我的动作", accent = ex.builtin)
                        Chip(if (VideoFiles.usable(ex.videoUri)) "已绑定视频" else "无视频",
                            accent = VideoFiles.usable(ex.videoUri))
                    }
                    Text(if (ex.mode == Mode.REPS) "${ex.value} 个 × ${ex.sets} 组 · 组休 ${ex.rest} 秒"
                    else "${ex.value} 秒 × ${ex.sets} 组 · 组休 ${ex.rest} 秒",
                        style = MaterialTheme.typography.bodySmall, color = Cmuted)
                    if (ex.videoUri != null) {
                        val head = if (VideoFiles.isNetwork(ex.videoUri)) "链接：" else "本地文件："
                        Text(head + fileName(ex.videoUri!!),
                            style = MaterialTheme.typography.bodySmall, color = Cmuted, maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (ex.videoUri != null) {
                        TextButtonText("移除视频", Cmuted) {
                            if (!VideoFiles.isNetwork(ex.videoUri)) VideoFiles.deleteIfLocal(ex.videoUri)
                            Store.bindVideo(ctx, ex.id, null)
                            editing = null; tick++
                        }
                    }
                    TextButtonText("粘贴链接", Caccent) { linkMode = true }
                    TextButtonText(
                        if (importing) "导入中…" else "选择本地视频",
                        Caccent,
                        enabled = !importing
                    ) {
                        if (!importing) videoPicker.launch("video/*")
                    }
                    TextButtonText("完成", Ctext, enabled = !importing) { if (!importing) editing = null }
                }
            },
            dismissButton = {
                if (!ex.builtin) {
                    TextButtonText("删除动作", Cdanger) { deleting = true }
                } else {
                    Text("内置动作不可删除", style = MaterialTheme.typography.labelMedium, color = Cmuted)
                }
            }
        )

        // 删除确认
        if (deleting) {
            ConfirmDialog(
                title = "删除动作",
                message = "确定删除「${ex.name}」吗？已加入的计划不受影响，但视频文件会一并移除。",
                danger = true,
                onDismiss = { deleting = false },
                onConfirm = {
                    deleting = false
                    if (!VideoFiles.isNetwork(ex.videoUri)) VideoFiles.deleteIfLocal(ex.videoUri)
                    Store.removeCustom(ctx, ex.id)
                    editing = null
                    tick++
                }
            )
        }

        // 粘贴链接
        if (linkMode) {
            PromptDialog(
                title = "粘贴视频链接（http/https）",
                initial = "",
                confirmText = "绑定",
                onDismiss = { linkMode = false },
                onConfirm = { url ->
                    linkMode = false
                    val t = url.trim()
                    if (t.startsWith("http://") || t.startsWith("https://")) {
                        if (!VideoFiles.isNetwork(ex.videoUri)) VideoFiles.deleteIfLocal(ex.videoUri)
                        Store.bindVideo(ctx, ex.id, t)
                        tick++
                        editing = null
                        toast("链接已添加 ✓")
                    } else {
                        toast("链接需以 http:// 或 https:// 开头")
                    }
                }
            )
        }
    }

    // 全屏视频预览
    previewUri?.let { uri ->
        PreviewVideoDialog(uri = uri, onDismiss = { previewUri = null })
    }
}

@Composable
private fun TextButtonText(text: String, color: Color, enabled: Boolean = true, onClick: () -> Unit) {
    androidx.compose.material3.TextButton(onClick = onClick, enabled = enabled) { Text(text, color = color) }
}

@Composable
private fun ExerciseCard(ex: Exercise, onClick: () -> Unit, onPreview: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(Csurface, RoundedCornerShape(16.dp))
        .border(1.dp, Cline, RoundedCornerShape(16.dp))
        .clickable(onClick = onClick).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp, 44.dp)) { StickFigure(ex.figure, 0.25f) }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(ex.name, style = MaterialTheme.typography.titleMedium, color = Ctext,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(if (ex.builtin) "内置动作 · 可绑定自己的跟练视频"
                else "我的动作 · 长按卡片拖动改变列表顺序",
                    style = MaterialTheme.typography.bodySmall, color = Cmuted)
            }
            if (VideoFiles.usable(ex.videoUri)) {
                // 视频 chip：单独可点击，触发预览（不让 Card 整体 onClick 触发）
                Box(Modifier.clickable(onClick = onPreview)) {
                    Chip("▶ 预览", accent = true)
                }
            } else {
                Chip("无视频")
            }
        }
    }
}

/** 视频预览全屏弹窗：使用 ExoPlayer 循环播放一段视频 */
@Composable
private fun PreviewVideoDialog(uri: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Csurface,
        titleContentColor = Ctext,
        textContentColor = Ctext,
        title = { Text("预览视频") },
        text = {
            LoopVideoPlayer(uri = uri, playing = true,
                modifier = Modifier.fillMaxWidth().aspectRatio(9f / 16f))
        },
        confirmButton = {
            TextButtonText("关闭", Ctext) { onDismiss() }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    )
}

private fun fileName(uri: String): String {
    if (com.fitflow.app.data.VideoFiles.isNetwork(uri)) {
        return uri.takeLast(60)
    }
    return uri.substringAfterLast('/')
}
