package com.fitflow.app.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
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

    /** 尽量从系统 content uri 获取原始文件名（带扩展名），拿不到时返回 null */
    private fun queryDisplayName(context: Context, uri: Uri): String? = try {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME),
            null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    } catch (_: Exception) {
        null
    }

    /** 拷贝 content:// 视频到 filesDir/videos/，保留原扩展名；返回文件绝对路径；失败返回 null */
    fun import(context: Context, uri: Uri): String? {
        return try {
            val dir = File(context.filesDir, "videos").apply { mkdirs() }

            // 优先用系统返回的真实文件名（含扩展名），例如 xxx.mp4 / xxx.mov / xxx.webm
            val display = queryDisplayName(context, uri)
            val safeName = display
                ?.substringAfterLast('/')
                ?.replace(Regex("[^A-Za-z0-9._\\-]"), "_")
                .orEmpty()
            val name = if (safeName.isNotBlank() && safeName.contains('.')) safeName
                       else "vid_${System.currentTimeMillis()}.mp4"
            val out = File(dir, name)

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
