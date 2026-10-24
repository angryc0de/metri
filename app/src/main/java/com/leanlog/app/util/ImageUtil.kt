package com.leanlog.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * 拍照 → 压缩 → 字节。
 *
 * 必须压：手机原图 4000px / 4MB，base64 后接近 6MB，
 * 既超过多数模型的限制，也会让用户在流量上传半天。
 */
object ImageUtil {

    private const val MAX_EDGE = 1024

    fun createCaptureUri(context: Context): Uri {
        val dir = File(context.cacheDir, "captures").apply { mkdirs() }
        val file = File(dir, "meal-${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun readJpegBytes(context: Context, uri: Uri): ByteArray? {
        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            val scale = maxOf(1, maxOf(bounds.outWidth, bounds.outHeight) / MAX_EDGE)

            val opts = BitmapFactory.Options().apply { inSampleSize = scale }
            val bitmap = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            } ?: return null

            ByteArrayOutputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                bitmap.recycle()
                out.toByteArray()
            }
        }.getOrNull()
    }
}
