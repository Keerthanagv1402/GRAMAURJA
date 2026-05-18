package com.gramaUrja.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrefsManager @Inject constructor(@ApplicationContext context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("grama_urja_prefs", Context.MODE_PRIVATE)

    var selectedZoneId: String?
        get() = prefs.getString(KEY_ZONE_ID, null)
        set(value) = prefs.edit { putString(KEY_ZONE_ID, value) }

    var selectedZoneName: String?
        get() = prefs.getString(KEY_ZONE_NAME, null)
        set(value) = prefs.edit { putString(KEY_ZONE_NAME, value) }

    var deviceId: String
        get() = prefs.getString(KEY_DEVICE_ID, "") ?: ""
        set(value) = prefs.edit { putString(KEY_DEVICE_ID, value) }

    var onboardingShown: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING, false)
        set(value) = prefs.edit { putBoolean(KEY_ONBOARDING, value) }

    companion object {
        private const val KEY_ZONE_ID    = "zone_id"
        private const val KEY_ZONE_NAME  = "zone_name"
        private const val KEY_DEVICE_ID  = "device_id"
        private const val KEY_ONBOARDING = "onboarding_shown"
    }
}
