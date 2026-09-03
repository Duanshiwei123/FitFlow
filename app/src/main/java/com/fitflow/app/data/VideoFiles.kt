package com.fitflow.app.data

import android.content.Context
import android.net.Uri
import java.io.File

/** 跟练视频文件管理：从系统选择器导入到应用私有目录，或使用网络链接 */
object VideoFiles {

    fun isNetwork(uri: String?): Boolean =
        uri != null && (uri.startsWith("http://") || uri.startsWith("https://"))

    /** uri 是否指向存在文件（网络 uri 视为可用，交给播放器错误回调） */
    fun usable(uri: String?): Boolean {
        if (uri.isNullOrBlank()) return false
        if (isNetwork(uri)) return true
        return File(uri).exists()
    }

    /** 拷贝 content:// 视频到 filesDir/videos/，返回文件绝对路径；失败返回 null */
    fun import(context: Context, uri: Uri): String? {
        return try {
            val dir = File(context.filesDir, "videos").apply { mkdirs() }
            val out = File(dir, "vid_${System.currentTimeMillis()}.mp4")
            context.contentResolver.openInputStream(uri)?.use { input ->
                out.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            out.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    /** 删除一个本地视频文件（网络链接不处理）。供移除视频时释放空间 */
    fun deleteIfLocal(uri: String?) {
        if (uri.isNullOrBlank() || isNetwork(uri)) return
        runCatching { File(uri).delete() }
    }
}
