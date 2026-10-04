package com.example.ui.theme

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.example.data.model.AppLanguage
import java.util.Locale

/**
 * Applies the selected language to everything drawn inside it.
 *
 * The language selector previously only stored a value: it was sent to the AI
 * endpoints so explanations came back in Tamil or Hindi, but the app's own
 * chrome stayed English, because nothing ever changed the locale that
 * `stringResource` reads from.
 *
 * This overrides the configuration for the composition subtree rather than
 * recreating the Activity. Two reasons: `AppCompatDelegate.setApplicationLocales`
 * would mean adding AppCompat purely for this, and a recreation would throw away
 * scroll positions and half-typed questions every time someone changed language.
 *
 * Note this affects resources only. Locale-sensitive formatting done through
 * `Locale.getDefault()` elsewhere is unchanged, which is deliberate: dates and
 * currency should follow the device, not the reading language someone picked for
 * scheme text.
 */
@Composable
fun ProvideAppLanguage(
    language: AppLanguage,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    val localizedContext = remember(language, configuration) {
        context.withLocale(language.toLocale())
    }
    val localizedConfiguration = remember(language, configuration) {
        Configuration(configuration).apply { setLocale(language.toLocale()) }
    }

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedConfiguration,
        content = content
    )
}

private fun AppLanguage.toLocale(): Locale = when (this) {
    AppLanguage.TAMIL -> Locale("ta", "IN")
    AppLanguage.HINDI -> Locale("hi", "IN")
    AppLanguage.ENGLISH -> Locale("en", "IN")
}

private fun Context.withLocale(locale: Locale): Context {
    val configuration = Configuration(resources.configuration).apply {
        setLocale(locale)
        setLayoutDirection(locale)
    }
    return createConfigurationContext(configuration)
}
