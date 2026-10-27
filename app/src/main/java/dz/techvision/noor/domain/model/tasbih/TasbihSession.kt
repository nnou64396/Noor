package dz.techvision.noor.domain.model.tasbih

/**
 * The user's Tasbih session, restored across app restarts.
 *
 * [count] is the progress within the current target and is clamped to
 * `0..target`; [totalCount] is the lifetime number of taps, which is kept
 * separately so resetting the round does not erase the running total.
 */
data class TasbihSession(
    val count: Int,
    val target: Int,
    val totalCount: Int = 0,
) {
    val isComplete: Boolean get() = target > 0 && count >= target
    val remaining: Int get() = (target - count).coerceAtLeast(0)

    companion object {
        /** Traditional presets; a custom target is always accepted too. */
        val PRESETS: List<Int> = listOf(33, 99, 100)

        const val DEFAULT_TARGET = 33
        const val MIN_TARGET = 1
        const val MAX_TARGET = 10_000

        val Empty = TasbihSession(count = 0, target = DEFAULT_TARGET, totalCount = 0)

        /** Clamps anything from storage or input into a valid session. */
        fun sanitized(count: Int, target: Int, totalCount: Int): TasbihSession {
            val safeTarget = target.coerceIn(MIN_TARGET, MAX_TARGET)
            return TasbihSession(
                count = count.coerceIn(0, safeTarget),
                target = safeTarget,
                totalCount = totalCount.coerceAtLeast(0),
            )
        }
    }
}

