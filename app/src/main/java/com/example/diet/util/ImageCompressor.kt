package com.example.diet.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min

/**
 * 上传前压缩图片：长边限制 + JPEG 重编码。
 * 视觉模型的图片 token 与像素/体积正相关，本地压缩可显著降低调用成本。
 */
object ImageCompressor {

    private const val MAX_EDGE = 768
    private const val QUALITY = 70
    private const val SKIP_BELOW_BYTES = 150 * 1024

    fun compress(bytes: ByteArray): ByteArray {
        if (bytes.isEmpty()) return bytes
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        val width = options.outWidth
        val height = options.outHeight
        if (width <= 0 || height <= 0) return bytes

        val maxEdge = max(width, height)
        if (maxEdge <= MAX_EDGE && bytes.size <= SKIP_BELOW_BYTES) return bytes

        val sample = max(1, maxEdge / MAX_EDGE)
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions) ?: return bytes

        val scale = min(1f, MAX_EDGE.toFloat() / max(decoded.width, decoded.height))
        val target = if (scale >= 1f) {
            decoded
        } else {
            Bitmap.createScaledBitmap(
                decoded,
                (decoded.width * scale).toInt().coerceAtLeast(1),
                (decoded.height * scale).toInt().coerceAtLeast(1),
                true
            )
        }

        val stream = ByteArrayOutputStream()
        target.compress(Bitmap.CompressFormat.JPEG, QUALITY, stream)
        if (target !== decoded) target.recycle()
        decoded.recycle()

        val compressed = stream.toByteArray()
        return if (compressed.isNotEmpty() && compressed.size < bytes.size) compressed else bytes
    }
}
