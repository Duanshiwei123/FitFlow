package com.fitflow.app.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.fitflow.app.data.VideoFiles
import java.io.File

/**
 * 循环播放一段跟练示范视频（本地路径或网络链接）。
 * - 重复模式：单曲循环，适合一组动作的示范。
 * - 声音：保留视频原声（默认 1f）。
 */
@Composable
fun LoopVideoPlayer(
    uri: String,
    playing: Boolean,
    modifier: Modifier = Modifier,
    onError: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 1f
        }
    }

    LaunchedEffect(uri) {
        runCatching {
            val mediaUri =
                if (VideoFiles.isNetwork(uri)) Uri.parse(uri)
                else Uri.fromFile(File(uri))
            player.setMediaItem(MediaItem.fromUri(mediaUri))
            player.prepare()
            if (playing) player.play() else player.pause()
        }.onFailure { onError?.invoke() }
    }

    LaunchedEffect(playing) {
        if (playing) player.play() else player.pause()
    }

    DisposableEffect(Unit) {
        onDispose {
            player.release()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                setShutterBackgroundColor(android.graphics.Color.BLACK)
                this.player = player
            }
        }
    )
}
