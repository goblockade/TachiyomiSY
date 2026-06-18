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

class SystemAvifDecoder(
    private val source: SourceFetchResult,
) : Decoder {

    override suspend fun decode(): DecodeResult? = runInterruptible {
        val sourceData = source.source.source().peek().readByteArray()

        try {
            val src = ImageDecoder.createSource(ByteBuffer.wrap(sourceData))
            val drawable = ImageDecoder.decodeDrawable(src)
            return@runInterruptible DecodeResult(image = drawable.asImage(), isSampled = false)
        } catch (_: Exception) { }

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
