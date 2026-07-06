package tachiyomi.domain.library.model

import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.category.model.Category

enum class FilterKey {
    DOWNLOADED,
    UNREAD,
    STARTED,
    BOOKMARKED,
    COMPLETED,
    INTERVAL_CUSTOM,
    LEWD,
}

/**
 * Encodes/decodes library filter states into/from Category.flags (Long).
 *
 * Bit layout:
 *   Bits 0-6: reserved for sort (LibrarySortMode)
 *   Bits 7-8:  filterDownloaded
 *   Bits 9-10: filterUnread
 *   Bits 11-12: filterStarted
 *   Bits 13-14: filterBookmarked
 *   Bits 15-16: filterCompleted
 *   Bits 17-18: filterIntervalCustom
 *   Bits 19-20: filterLewd
 *   Bit  21:    filterMode (true=OR, false=AND)
 *
 * Each 2-bit TriState field: 00=DISABLED, 01=ENABLED_IS, 10=ENABLED_NOT
 */
object LibraryFilterFlags {

    // Shift positions (each 2 bits)
    private const val SHIFT_DOWNLOADED = 7
    private const val SHIFT_UNREAD = 9
    private const val SHIFT_STARTED = 11
    private const val SHIFT_BOOKMARKED = 13
    private const val SHIFT_COMPLETED = 15
    private const val SHIFT_INTERVAL_CUSTOM = 17
    private const val SHIFT_LEWD = 19
    private const val SHIFT_MODE = 21

    const val MASK_DOWNLOADED = 0b11L shl SHIFT_DOWNLOADED
    const val MASK_UNREAD = 0b11L shl SHIFT_UNREAD
    const val MASK_STARTED = 0b11L shl SHIFT_STARTED
    const val MASK_BOOKMARKED = 0b11L shl SHIFT_BOOKMARKED
    const val MASK_COMPLETED = 0b11L shl SHIFT_COMPLETED
    const val MASK_INTERVAL_CUSTOM = 0b11L shl SHIFT_INTERVAL_CUSTOM
    const val MASK_LEWD = 0b11L shl SHIFT_LEWD
    const val MASK_MODE = 0b1L shl SHIFT_MODE

    fun getDownloaded(flags: Long): TriState = decode(flags, SHIFT_DOWNLOADED)
    fun getUnread(flags: Long): TriState = decode(flags, SHIFT_UNREAD)
    fun getStarted(flags: Long): TriState = decode(flags, SHIFT_STARTED)
    fun getBookmarked(flags: Long): TriState = decode(flags, SHIFT_BOOKMARKED)
    fun getCompleted(flags: Long): TriState = decode(flags, SHIFT_COMPLETED)
    fun getIntervalCustom(flags: Long): TriState = decode(flags, SHIFT_INTERVAL_CUSTOM)
    fun getLewd(flags: Long): TriState = decode(flags, SHIFT_LEWD)
    fun getMode(flags: Long): Boolean = ((flags shr SHIFT_MODE) and 0b1L) == 1L

    fun setDownloaded(flags: Long, value: TriState): Long = set(flags, value, SHIFT_DOWNLOADED, MASK_DOWNLOADED)
    fun setUnread(flags: Long, value: TriState): Long = set(flags, value, SHIFT_UNREAD, MASK_UNREAD)
    fun setStarted(flags: Long, value: TriState): Long = set(flags, value, SHIFT_STARTED, MASK_STARTED)
    fun setBookmarked(flags: Long, value: TriState): Long = set(flags, value, SHIFT_BOOKMARKED, MASK_BOOKMARKED)
    fun setCompleted(flags: Long, value: TriState): Long = set(flags, value, SHIFT_COMPLETED, MASK_COMPLETED)
    fun setIntervalCustom(flags: Long, value: TriState): Long = set(flags, value, SHIFT_INTERVAL_CUSTOM, MASK_INTERVAL_CUSTOM)
    fun setLewd(flags: Long, value: TriState): Long = set(flags, value, SHIFT_LEWD, MASK_LEWD)
    fun setMode(flags: Long, value: Boolean): Long = (flags and MASK_MODE.inv()) or (if (value) 1L shl SHIFT_MODE else 0L)

    /**
     * Toggle a TriState filter: DISABLED → ENABLED_IS → ENABLED_NOT → DISABLED
     */
    fun toggleDownloaded(flags: Long): Long = toggle(flags, SHIFT_DOWNLOADED, MASK_DOWNLOADED)
    fun toggleUnread(flags: Long): Long = toggle(flags, SHIFT_UNREAD, MASK_UNREAD)
    fun toggleStarted(flags: Long): Long = toggle(flags, SHIFT_STARTED, MASK_STARTED)
    fun toggleBookmarked(flags: Long): Long = toggle(flags, SHIFT_BOOKMARKED, MASK_BOOKMARKED)
    fun toggleCompleted(flags: Long): Long = toggle(flags, SHIFT_COMPLETED, MASK_COMPLETED)
    fun toggleIntervalCustom(flags: Long): Long = toggle(flags, SHIFT_INTERVAL_CUSTOM, MASK_INTERVAL_CUSTOM)
    fun toggleLewd(flags: Long): Long = toggle(flags, SHIFT_LEWD, MASK_LEWD)

    private fun decode(flags: Long, shift: Int): TriState = when ((flags shr shift) and 0b11) {
        1L -> TriState.ENABLED_IS
        2L -> TriState.ENABLED_NOT
        else -> TriState.DISABLED
    }

    private fun set(flags: Long, value: TriState, shift: Int, mask: Long): Long {
        val encoded = when (value) {
            TriState.DISABLED -> 0L
            TriState.ENABLED_IS -> 1L
            TriState.ENABLED_NOT -> 2L
        }
        return (flags and mask.inv()) or (encoded shl shift)
    }

    private fun toggle(flags: Long, shift: Int, mask: Long): Long {
        val current = decode(flags, shift)
        val next = current.next()
        return set(flags, next, shift, mask)
    }
}

val Category?.filterDownloaded: TriState
    get() = this?.flags?.let { LibraryFilterFlags.getDownloaded(it) } ?: TriState.DISABLED

val Category?.filterUnread: TriState
    get() = this?.flags?.let { LibraryFilterFlags.getUnread(it) } ?: TriState.DISABLED

val Category?.filterStarted: TriState
    get() = this?.flags?.let { LibraryFilterFlags.getStarted(it) } ?: TriState.DISABLED

val Category?.filterBookmarked: TriState
    get() = this?.flags?.let { LibraryFilterFlags.getBookmarked(it) } ?: TriState.DISABLED

val Category?.filterCompleted: TriState
    get() = this?.flags?.let { LibraryFilterFlags.getCompleted(it) } ?: TriState.DISABLED

val Category?.filterIntervalCustom: TriState
    get() = this?.flags?.let { LibraryFilterFlags.getIntervalCustom(it) } ?: TriState.DISABLED

val Category?.filterLewd: TriState
    get() = this?.flags?.let { LibraryFilterFlags.getLewd(it) } ?: TriState.DISABLED

val Category?.filterMode: Boolean
    get() = this?.flags?.let { LibraryFilterFlags.getMode(it) } ?: false
