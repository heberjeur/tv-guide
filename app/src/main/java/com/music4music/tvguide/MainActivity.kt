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
            TvGuideTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TvGuideAppNavigation()
                }
            }
        }
    }

    private fun applySavedLocale() {
        try {
            val lang = runBlocking { userPreferences.appLanguage.first() }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val localeManager = getSystemService(LocaleManager::class.java)
                val currentTags = localeManager?.applicationLocales?.toLanguageTags() ?: ""
                if (!currentTags.contains(lang)) {
                    localeManager?.applicationLocales = LocaleList.forLanguageTags(lang)
                }
            } else {
                val locale = Locale(lang)
                Locale.setDefault(locale)
                val config = resources.configuration
                config.setLocale(locale)
                resources.updateConfiguration(config, resources.displayMetrics)
            }
        } catch (e: Exception) {
            Log.w("MainActivity", "Failed to apply saved locale", e)
        }
    }
}
