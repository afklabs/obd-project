package com.example.androidautoobd2.utils

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "obd_preferences"
        private const val KEY_UPDATE_FREQUENCY = "update_frequency"
        private const val KEY_ENABLED_PARAMS_PREFIX = "param_enabled_"
        private const val DEFAULT_UPDATE_FREQUENCY = 1000
    }

    fun getUpdateFrequency(): Int {
        return prefs.getInt(KEY_UPDATE_FREQUENCY, DEFAULT_UPDATE_FREQUENCY)
    }

    fun setUpdateFrequency(frequency: Int) {
        prefs.edit().putInt(KEY_UPDATE_FREQUENCY, frequency).apply()
    }

    fun isParameterEnabled(pid: String): Boolean {
        return prefs.getBoolean("$KEY_ENABLED_PARAMS_PREFIX$pid", true)
    }

    fun setParameterEnabled(pid: String, enabled: Boolean) {
        prefs.edit().putBoolean("$KEY_ENABLED_PARAMS_PREFIX$pid", enabled).apply()
    }
}