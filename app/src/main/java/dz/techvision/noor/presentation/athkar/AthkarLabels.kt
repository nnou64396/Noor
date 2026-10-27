package dz.techvision.noor.presentation.athkar

import androidx.annotation.StringRes
import dz.techvision.noor.R

/**
 * Maps corpus category slugs onto localized titles.
 *
 * Category ids come from the corpus, but no user-visible text is stored in the
 * corpus: titles resolve here so Arabic, English and French all work, and an id
 * the app has no title for degrades to a generic label instead of leaking a raw
 * slug onto the screen.
 */
@StringRes
fun athkarCategoryLabelRes(categoryId: String): Int = when (categoryId) {
    "morning" -> R.string.athkar_category_morning
    "evening" -> R.string.athkar_category_evening
    "after_prayer" -> R.string.athkar_category_after_prayer
    "before_sleep" -> R.string.athkar_category_before_sleep
    "general" -> R.string.athkar_category_general
    else -> R.string.athkar_category_other
}

