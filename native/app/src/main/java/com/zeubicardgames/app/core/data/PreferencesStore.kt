package com.zeubicardgames.app.core.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore("zeubi_preferences")

data class UserPreferences(val sound: Boolean = true, val haptics: Boolean = true, val reducedMotion: Boolean = false, val onboardingDone: Boolean = false)

@Singleton
class PreferencesStore @Inject constructor(@ApplicationContext private val context: Context) {
    private object Keys { val sound = booleanPreferencesKey("sound"); val haptics = booleanPreferencesKey("haptics"); val reduced = booleanPreferencesKey("reduced_motion"); val onboarding = booleanPreferencesKey("onboarding") }
    val flow: Flow<UserPreferences> = context.dataStore.data.map { UserPreferences(it[Keys.sound] ?: true, it[Keys.haptics] ?: true, it[Keys.reduced] ?: false, it[Keys.onboarding] ?: false) }
    suspend fun setSound(v: Boolean) = context.dataStore.edit { it[Keys.sound] = v }
    suspend fun setHaptics(v: Boolean) = context.dataStore.edit { it[Keys.haptics] = v }
    suspend fun setReducedMotion(v: Boolean) = context.dataStore.edit { it[Keys.reduced] = v }
}
