package org.fossify.musicplayer

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import org.fossify.commons.extensions.checkUseEnglish
import org.fossify.musicplayer.extensions.config
import org.fossify.musicplayer.helpers.AdMobHelper
import org.fossify.musicplayer.helpers.SimpleMediaController

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        applyPulseBeatDefaultTheme()
        checkUseEnglish()
        AdMobHelper.initialize(this)
        initController()
    }

    private fun applyPulseBeatDefaultTheme() {
        val prefs = getSharedPreferences("pulsebeat_branding", MODE_PRIVATE)
        if (!prefs.getBoolean("midnight_neon_v1_applied", false)) {
            val bg = 0xFF0B0E1A.toInt()
            val text = 0xFFF1F5F9.toInt()
            val primary = 0xFF8B5CF6.toInt()
            val accent = 0xFF06B6D4.toInt()
            config.apply {
                isSystemThemeEnabled = false
                isGlobalThemeEnabled = false
                backgroundColor = bg
                textColor = text
                primaryColor = primary
                accentColor = accent
                customBackgroundColor = bg
                customTextColor = text
                customPrimaryColor = primary
                customAccentColor = accent
                widgetBgColor = 0xE60B0E1A.toInt()
                widgetTextColor = text
            }
            prefs.edit().putBoolean("midnight_neon_v1_applied", true).apply()
        }
    }

    private fun initController() {
        SimpleMediaController.getInstance(applicationContext).createControllerAsync()
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStop(owner: LifecycleOwner) {
                    SimpleMediaController.destroyInstance()
                }
            }
        )
    }
}
