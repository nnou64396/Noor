package com.noor.islamic.presentation.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.noor.islamic.R
import com.noor.islamic.domain.model.Language
import com.noor.islamic.ui.components.NoorDotsIndicator
import com.noor.islamic.ui.components.NoorFullLogo
import com.noor.islamic.ui.components.NoorIslamicMotif
import com.noor.islamic.ui.components.NoorOrnamentalDivider
import com.noor.islamic.ui.components.NoorPrimaryButton
import com.noor.islamic.ui.components.heroAccent
import com.noor.islamic.ui.components.heroBrush
import com.noor.islamic.ui.theme.NoorDimens
import com.noor.islamic.ui.theme.NoorHeroBottom
import com.noor.islamic.ui.theme.NoorHeroTop
import com.noor.islamic.ui.theme.noorCaption
import com.noor.islamic.ui.theme.noorLabel
import com.noor.islamic.ui.theme.noorLargeTitle
import com.noor.islamic.ui.theme.noorSecondary
import com.noor.islamic.ui.theme.noorTitle
import kotlinx.coroutines.launch

private const val ONBOARDING_PAGE_COUNT = 3

/**
 * First-run experience.
 *
 * Onboarding deliberately sits on the same deep blue gradient as the launcher
 * icon and the splash screen, so the app introduces itself with the identity
 * the user just tapped. Type is white (and gold for the smallest accents),
 * which keeps contrast high regardless of the user's light/dark preference —
 * this screen is intentionally theme-independent.
 */
@Composable
fun OnboardingScreen(
    selectedLanguage: Language,
    onLanguageSelected: (Language) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { ONBOARDING_PAGE_COUNT })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage >= ONBOARDING_PAGE_COUNT - 1

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(heroBrush()),
    ) {
        NoorIslamicMotif(
            modifier = Modifier.fillMaxSize(),
            color = Color.White,
            alpha = 0.05f,
        )

        Column(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
            ) { page ->
                when (page) {
                    0 -> BrandPage()
                    1 -> FeaturesPage()
                    else -> LanguagePage(
                        selectedLanguage = selectedLanguage,
                        onLanguageSelected = onLanguageSelected,
                    )
                }
            }

            NoorDotsIndicator(
                count = ONBOARDING_PAGE_COUNT,
                currentIndex = pagerState.currentPage,
                onDarkSurface = true,
            )

            // Inverted against the dark backdrop, so the CTA stays the brightest
            // element on the screen.
            NoorPrimaryButton(
                text = stringResource(R.string.action_continue),
                onClick = {
                    if (isLastPage) {
                        onContinue()
                    } else {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
                modifier = Modifier.padding(horizontal = NoorDimens.Lg),
                testTag = "onboarding_continue",
                containerColor = Color.White,
                contentColor = NoorHeroBottom,
            )

            Spacer(Modifier.height(NoorDimens.Xl))
        }
    }
}

@Composable
private fun BrandPage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(NoorDimens.Lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(Modifier.height(NoorDimens.Xl))

        NoorFullLogo(onDarkSurface = true)

        Spacer(Modifier.height(NoorDimens.Lg))

        NoorOrnamentalDivider(
            modifier = Modifier.widthIn(max = 180.dp),
            color = Color.White.copy(alpha = 0.28f),
            accent = heroAccent(),
        )

        Spacer(Modifier.height(NoorDimens.Lg))

        Text(
            text = stringResource(R.string.onboarding_tagline),
            style = MaterialTheme.typography.noorSecondary,
            color = Color.White.copy(alpha = 0.82f),
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = NoorDimens.ReadableWidthMax),
        )
    }
}

@Composable
private fun FeaturesPage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(NoorDimens.Lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NoorDimens.Md),
    ) {
        Spacer(Modifier.height(NoorDimens.Lg))

        Text(
            text = stringResource(R.string.onboarding_features_title),
            style = MaterialTheme.typography.noorLargeTitle,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = NoorDimens.ReadableWidthMax),
        )
        Spacer(Modifier.height(NoorDimens.Sm))

        FeatureRow(
            icon = Icons.AutoMirrored.Rounded.MenuBook,
            title = stringResource(R.string.nav_quran),
            description = stringResource(R.string.feat_quran_desc),
        )
        FeatureRow(
            icon = Icons.Rounded.Schedule,
            title = stringResource(R.string.prayer_title),
            description = stringResource(R.string.feat_prayer_desc),
        )
        FeatureRow(
            icon = Icons.Rounded.Explore,
            title = stringResource(R.string.nav_qibla),
            description = stringResource(R.string.feat_qibla_desc),
        )
        FeatureRow(
            icon = Icons.Rounded.Star,
            title = stringResource(R.string.nav_athkar),
            description = stringResource(R.string.feat_athkar_desc),
        )

        Spacer(Modifier.height(NoorDimens.Lg))
    }
}

/** A translucent white card — the shared `NoorSettingRow` cannot be used here
 *  because it derives its colours from the app theme rather than the backdrop. */
@Composable
private fun FeatureRow(icon: ImageVector, title: String, description: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = NoorDimens.ReadableWidthMax),
        shape = MaterialTheme.shapes.medium,
        color = Color.White.copy(alpha = 0.08f),
        contentColor = Color.White,
        border = BorderStroke(NoorDimens.Hairline, Color.White.copy(alpha = 0.16f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = NoorDimens.Md, vertical = NoorDimens.Md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NoorDimens.Md),
        ) {
            Surface(
                color = Color.White.copy(alpha = 0.14f),
                shape = CircleShape,
                contentColor = heroAccent(),
                modifier = Modifier.size(NoorDimens.Md + 10.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(NoorDimens.Md),
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.noorTitle,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.noorCaption,
                    color = Color.White.copy(alpha = 0.74f),
                )
            }
        }
    }
}

@Composable
private fun LanguagePage(
    selectedLanguage: Language,
    onLanguageSelected: (Language) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(NoorDimens.Lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(Modifier.height(NoorDimens.Xl))

        Text(
            text = stringResource(R.string.onboarding_language_title),
            style = MaterialTheme.typography.noorLargeTitle,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = NoorDimens.ReadableWidthMax),
        )
        Spacer(Modifier.height(NoorDimens.Xl))

        Row(
            horizontalArrangement = Arrangement.spacedBy(NoorDimens.Sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Language.entries.forEach { language ->
                OnboardingLanguageChip(
                    label = stringResource(language.displayNameRes),
                    selected = language == selectedLanguage,
                    onClick = { onLanguageSelected(language) },
                )
            }
        }
        Spacer(Modifier.height(NoorDimens.Xxl))
    }
}

/** A language choice rendered for the dark onboarding backdrop. */
@Composable
private fun OnboardingLanguageChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = CircleShape,
        color = if (selected) Color.White else Color.White.copy(alpha = 0.10f),
        contentColor = if (selected) NoorHeroBottom else Color.White,
        border = BorderStroke(
            NoorDimens.Hairline,
            if (selected) Color.White else Color.White.copy(alpha = 0.28f),
        ),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.noorLabel,
            color = if (selected) NoorHeroBottom else Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = NoorDimens.Md, vertical = 10.dp),
        )
    }
}
