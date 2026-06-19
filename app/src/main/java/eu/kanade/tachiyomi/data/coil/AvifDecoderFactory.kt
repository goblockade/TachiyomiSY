package eu.kanade.tachiyomi.data.coil

import android.graphics.ImageDecoder
import android.os.Build
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DecodeResult
import coil3.decode.Decoder
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import kotlinx.coroutines.runInterruptible
import okio.ByteString.Companion.encodeUtf8
import java.nio.ByteBuffer

/**
 * Decodes **animated** AVIF (ftypavis) using Android's built-in [ImageDecoder].
 * - API 33+: [ImageDecoder.decodeDrawable] returns [android.graphics.drawable.AnimatedImageDrawable].
 * - API 30-32: [ImageDecoder.decodeBitmap] as fallback.
 * - API < 30: returns null, falls through to [com.github.awxkee.avifcoil.decoder.HeifDecoder].
 *
 * Only matches `ftypavis` (animated brand). Static AVIF (ftypavif) falls through
 * to [com.github.awxkee.avifcoil.decoder.HeifDecoder] which is more battle-tested.
 */
class SystemAvifDecoder(
    private val source: SourceFetchResult,
) : Decoder {

    override suspend fun decode(): DecodeResult? = runInterruptible {
        val sourceData = source.source.source().readByteArray()

        // API 33+: decodeDrawable returns AnimatedImageDrawable for animated AVIF
        if (Build.VERSION.SDK_INT >= 33) {
            try {
                val src = ImageDecoder.createSource(ByteBuffer.wrap(sourceData))
                val drawable = ImageDecoder.decodeDrawable(src)
                return@runInterruptible DecodeResult(image = drawable.asImage(), isSampled = false)
            } catch (_: Exception) { }
        }

        // API 30-32: fallback to static decode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val src = ImageDecoder.createSource(ByteBuffer.wrap(sourceData))
                val bitmap = ImageDecoder.decodeBitmap(src)
                return@runInterruptible DecodeResult(image = bitmap.asImage(), isSampled = false)
            } catch (_: Exception) { }
        }

        null
    }

    class Factory : Decoder.Factory {
        override fun create(
            result: SourceFetchResult,
            options: Options,
            imageLoader: ImageLoader,
        ): Decoder? {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
            return if (result.source.source().rangeEquals(4L, AVIS)) {
                SystemAvifDecoder(result)
            } else {
                null
            }
        }

        companion object {
            private val AVIS = "ftypavis".encodeUtf8()
        }
    }
}
