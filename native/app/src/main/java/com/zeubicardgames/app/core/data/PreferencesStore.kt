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

data class UserPreferences(
    val sound: Boolean = true,
    val haptics: Boolean = true,
    val reducedMotion: Boolean = false,
    val onboardingDone: Boolean = false,
    val favoriteCardIds: Set<String> = emptySet(),
)

@Singleton
class PreferencesStore @Inject constructor(@ApplicationContext private val context: Context) {
    private object Keys {
        val sound = booleanPreferencesKey("sound")
        val haptics = booleanPreferencesKey("haptics")
        val reduced = booleanPreferencesKey("reduced_motion")
        val onboarding = booleanPreferencesKey("onboarding")
        val favorites = stringSetPreferencesKey("favorite_card_ids")
    }

    val flow: Flow<UserPreferences> = context.dataStore.data.map { values ->
        UserPreferences(
            sound = values[Keys.sound] ?: true,
            haptics = values[Keys.haptics] ?: true,
            reducedMotion = values[Keys.reduced] ?: false,
            onboardingDone = values[Keys.onboarding] ?: false,
            favoriteCardIds = values[Keys.favorites]?.toSet() ?: emptySet(),
        )
    }

    suspend fun setSound(v: Boolean) = context.dataStore.edit { it[Keys.sound] = v }
    suspend fun setHaptics(v: Boolean) = context.dataStore.edit { it[Keys.haptics] = v }
    suspend fun setReducedMotion(v: Boolean) = context.dataStore.edit { it[Keys.reduced] = v }

    suspend fun toggleFavorite(cardId: String) {
        if (cardId.isBlank()) return
        context.dataStore.edit { values ->
            val next = (values[Keys.favorites] ?: emptySet()).toMutableSet()
            if (!next.add(cardId)) next.remove(cardId)
            values[Keys.favorites] = next
        }
    }
}
