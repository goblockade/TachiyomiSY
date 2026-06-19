package eu.kanade.tachiyomi.data.coil

import coil3.ImageLoader
import coil3.decode.Decoder
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import com.github.awxkee.avifcoil.decoder.animation.AnimatedAvifDecoder
import okio.ByteString.Companion.encodeUtf8

/**
 * Matches only animated AVIF (ftypavis brand), NOT static AVIF (ftypavif).
 * Static AVIF falls through to [com.github.awxkee.avifcoil.decoder.HeifDecoder].
 */
class AnimatedAvifFactory(
    private val preheatFrames: Int = 6,
    private val exceptionLogger: ((Exception) -> Unit)? = null,
) : Decoder.Factory {

    override fun create(
        result: SourceFetchResult,
        options: Options,
        imageLoader: ImageLoader,
    ): Decoder? {
        return if (result.source.source().rangeEquals(4, AVIS)) {
            AnimatedAvifDecoder(
                source = result,
                options = options,
                preheatFrames = preheatFrames,
                exceptionLogger = exceptionLogger,
            )
        } else null
    }

    companion object {
        private val AVIS = "ftypavis".encodeUtf8()
    }
}
