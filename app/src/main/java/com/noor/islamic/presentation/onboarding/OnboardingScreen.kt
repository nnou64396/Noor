package com.noor.islamic.presentation.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.noor.islamic.R
import com.noor.islamic.domain.model.Language
import com.noor.islamic.ui.components.NoorDotsIndicator
import com.noor.islamic.ui.components.NoorIslamicMotif
import com.noor.islamic.ui.components.NoorFullLogo
import com.noor.islamic.ui.components.NoorPrimaryButton
import com.noor.islamic.ui.components.NoorSettingRow
import com.noor.islamic.ui.theme.NoorDimens
import com.noor.islamic.ui.theme.noorLargeTitle
import kotlinx.coroutines.launch

private const val ONBOARDING_PAGE_COUNT = 3

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

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box {
            NoorIslamicMotif(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.secondary,
                alpha = 0.04f,
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
                )

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
                )

                Spacer(Modifier.height(NoorDimens.Xl))
            }
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
        NoorFullLogo()
        Spacer(Modifier.height(NoorDimens.Lg))

        Text(
            text = stringResource(R.string.onboarding_tagline),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
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
        Spacer(Modifier.height(NoorDimens.Xl))

        Text(
            text = stringResource(R.string.onboarding_features_title),
            style = MaterialTheme.typography.noorLargeTitle,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
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

@Composable
private fun FeatureRow(icon: ImageVector, title: String, description: String) {
    NoorSettingRow(
        title = title,
        subtitle = description,
        icon = icon,
        onClick = null,
    )
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
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(NoorDimens.Xl))

        Row(
            horizontalArrangement = Arrangement.spacedBy(NoorDimens.Sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Language.entries.forEach { language ->
                FilterChip(
                    selected = language == selectedLanguage,
                    onClick = { onLanguageSelected(language) },
                    label = {
                        Text(
                            text = stringResource(language.displayNameRes),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                )
            }
        }
        Spacer(Modifier.height(NoorDimens.Xxl))
    }
}