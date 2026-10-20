package com.music4music.tvguide

import android.app.LocaleManager
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.music4music.tvguide.data.local.prefs.UserPreferences
import com.music4music.tvguide.ui.navigation.TvGuideAppNavigation
import com.music4music.tvguide.ui.theme.DarkBackground
import com.music4music.tvguide.ui.theme.TvGuideTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Locale
import javax.inject.Inject

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.compose.runtime.CompositionLocalProvider
import com.music4music.tvguide.ui.theme.LocalDisplayDensity
import com.music4music.tvguide.ui.theme.LocalHighlightAccessibility
import com.music4music.tvguide.ui.theme.LocalFontSizeScale

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySavedLocale()
        enableEdgeToEdge()
        setContent {
            val themeMode by userPreferences.themeMode.collectAsStateWithLifecycle(initialValue = "dark")
            val fontSizeScale by userPreferences.fontSizeScale.collectAsStateWithLifecycle(initialValue = "normal")
            val displayDensity by userPreferences.displayDensity.collectAsStateWithLifecycle(initialValue = "comfort")
            val highlightAccessibility by userPreferences.highlightAccessibility.collectAsStateWithLifecycle(initialValue = true)
            val displayTimezone by userPreferences.displayTimezone.collectAsStateWithLifecycle(initialValue = "origin")
            val currentDisplayZone = androidx.compose.runtime.remember(displayTimezone) {
                com.music4music.tvguide.util.TimezoneUtils.resolveZoneId(displayTimezone)
            }

            TvGuideTheme(themeMode = themeMode, fontSizeScale = fontSizeScale) {
                val currentDensity = androidx.compose.ui.platform.LocalDensity.current
                val effectiveDensity = if (displayDensity == "compact") {
                    androidx.compose.ui.unit.Density(
                        density = currentDensity.density * 0.88f,
                        fontScale = currentDensity.fontScale * 0.92f
                    )
                } else {
                    currentDensity
                }

                CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalDensity provides effectiveDensity,
                    LocalDisplayDensity provides displayDensity,
                    LocalHighlightAccessibility provides highlightAccessibility,
                    LocalFontSizeScale provides fontSizeScale,
                    com.music4music.tvguide.ui.theme.LocalDisplayZone provides currentDisplayZone
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        TvGuideAppNavigation()
                    }
                }
            }
        }
    }

    private fun applySavedLocale() {
        try {
            val lang = runBlocking { userPreferences.appLanguage.first() }
            val locale = Locale.forLanguageTag(lang)
            Locale.setDefault(locale)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val localeManager = getSystemService(LocaleManager::class.java)
                val currentTags = localeManager?.applicationLocales?.toLanguageTags() ?: ""
                if (!currentTags.contains(lang)) {
                    localeManager?.applicationLocales = LocaleList.forLanguageTags(lang)
                }
            } else {
                val config = resources.configuration
                config.setLocale(locale)
                resources.updateConfiguration(config, resources.displayMetrics)
            }
        } catch (e: Exception) {
            Log.w("MainActivity", "Failed to apply saved locale", e)
        }
    }
}
