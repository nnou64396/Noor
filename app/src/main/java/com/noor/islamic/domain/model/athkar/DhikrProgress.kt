package com.noor.islamic.domain.model.athkar

/**
 * The single place that turns a [DhikrCount] into a number the app may act on.
 *
 * `AthkarViewModel`, `AthkarCategoryViewModel` and `HomePracticeViewModel` all
 * need the same answers to "is this finished?", "how many are left?" and "what
 * happens on the next tap?". Centralising them keeps one set of rules, and makes
 * an unspecified count resolve to `null` — "there is no target" — instead of
 * to some substituted default.
 */
object DhikrProgress {

    /**
     * The count the source prescribes, or null when it prescribes none.
     *
     * A non-positive declared count also yields null. That state is rejected by
     * the validator and cannot reach the UI, so treating it as "no target" only
     * keeps a bad value from becoming a real one; it never invents a number.
     */
    fun targetOf(count: DhikrCount): Int? =
        (count as? DhikrCount.Prescribed)?.value?.takeIf { it >= 1 }

    /**
     * Whether the user has finished this dhikr.
     *
     * Only a prescribed count can ever be complete. Counting an unspecified dhikr
     * any number of times does not make it complete, so no tap is presented as
     * having discharged a religious requirement.
     */
    fun isComplete(count: DhikrCount, completedRepetitions: Int): Boolean {
        val target = targetOf(count) ?: return false
        return completedRepetitions >= target
    }

    /** Repetitions still to do, or null when there is no target to work towards. */
    fun remainingOf(count: DhikrCount, completedRepetitions: Int): Int? {
        val target = targetOf(count) ?: return null
        return (target - completedRepetitions).coerceAtLeast(0)
    }

    /** Progress toward the target, or null when there is nothing to progress toward. */
    fun fractionOf(count: DhikrCount, completedRepetitions: Int): Float? {
        val target = targetOf(count) ?: return null
        return (completedRepetitions.toFloat() / target).coerceIn(0f, 1f)
    }

    /**
     * The value to store after one more tap.
     *
     * A prescribed count caps at its own target, so a fast double-tap cannot
     * overshoot what the source asks for. An unspecified count simply increases
     * and is never given a ceiling, because inventing one is exactly the failure
     * this type exists to prevent.
     */
    fun nextCount(count: DhikrCount, completedRepetitions: Int): Int {
        if (completedRepetitions == Int.MAX_VALUE) return Int.MAX_VALUE
        val incremented = completedRepetitions + 1
        val target = targetOf(count) ?: return incremented
        return incremented.coerceAtMost(target)
    }

    /**
     * A stored value brought back in line with what the corpus currently declares.
     *
     * Used when reading a counter that was written by an earlier corpus or an
     * older app version, so a value above the current target cannot keep a
     * completed dhikr looking unfinished. An unspecified count has no ceiling to
     * apply and keeps the user's real total.
     */
    fun clampStored(count: DhikrCount, storedRepetitions: Int): Int {
        val atLeastZero = storedRepetitions.coerceAtLeast(0)
        val target = targetOf(count) ?: return atLeastZero
        return atLeastZero.coerceAtMost(target)
    }
}
