package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

data class SavedScreenshotResult(
    val filePath: String,
    val width: Int,
    val height: Int
)

object ScreenshotStorage {

    private const val SCREENSHOT_FILE_NAME = "macro_reference_screenshot.jpg"
    private const val MAX_LONG_EDGE_PX = 1920

    /**
     * Copies and safely downsamples a Photo Picker Uri into internal app storage so it persists
     * across app restarts without requiring broad storage permissions and remains memory-safe
     * on low-RAM devices like Nokia C30.
     */
    suspend fun importScreenshot(context: Context, uri: Uri): Result<SavedScreenshotResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val resolver = context.contentResolver

                // Step 1: Decode original dimensions without allocating pixel memory
                val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                resolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, boundsOptions)
                } ?: error("Unable to read selected image.")

                val origWidth = boundsOptions.outWidth
                val origHeight = boundsOptions.outHeight
                if (origWidth <= 0 || origHeight <= 0) {
                    error("Selected file is not a valid image.")
                }

                // Step 2: Compute power-of-two sample size for low-RAM safety
                val longEdge = max(origWidth, origHeight)
                var sampleSize = 1
                while (longEdge / sampleSize > MAX_LONG_EDGE_PX) {
                    sampleSize *= 2
                }

                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                val bitmap = resolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, decodeOptions)
                } ?: error("Failed to decode selected screenshot.")

                val outFile = File(context.filesDir, SCREENSHOT_FILE_NAME)
                val tempFile = File(context.filesDir, "$SCREENSHOT_FILE_NAME.tmp")
                try {
                    FileOutputStream(tempFile).use { fos ->
                        val ok = bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
                        if (!ok) error("Could not save screenshot locally.")
                    }
                    if (outFile.exists()) outFile.delete()
                    if (!tempFile.renameTo(outFile)) {
                        error("Could not finalize saved screenshot.")
                    }
                    SavedScreenshotResult(
                        filePath = outFile.absolutePath,
                        width = bitmap.width,
                        height = bitmap.height
                    )
                } finally {
                    bitmap.recycle()
                    if (tempFile.exists()) tempFile.delete()
                }
            }
        }

    fun isScreenshotValid(path: String): Boolean {
        if (path.isBlank()) return false
        val file = File(path)
        return file.exists() && file.isFile && file.length() > 0L
    }
}
