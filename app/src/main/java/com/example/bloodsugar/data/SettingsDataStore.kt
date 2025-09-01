package com.example.bloodsugar.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(context: Context) {

    private val dataStore = context.dataStore

    private object PreferencesKeys {
        val BREAKFAST_COEFFICIENT = floatPreferencesKey("breakfast_coefficient")
        val DINNER_COEFFICIENT = floatPreferencesKey("dinner_coefficient")
        val SUPPER_COEFFICIENT = floatPreferencesKey("supper_coefficient")
        val CARBS_PER_BU = floatPreferencesKey("carbs_per_bu")
        val DAILY_CARBS_GOAL = floatPreferencesKey("daily_carbs_goal")
        val INSULIN_DOSE_ACCURACY = floatPreferencesKey("insulin_dose_accuracy")
        val POST_MEAL_NOTIFICATION_ENABLED = androidx.datastore.preferences.core.booleanPreferencesKey("post_meal_notification_enabled")
        val POST_MEAL_NOTIFICATION_DELAY = androidx.datastore.preferences.core.intPreferencesKey("post_meal_notification_delay")
        val TREND_NOTIFICATION_ENABLED = androidx.datastore.preferences.core.booleanPreferencesKey("trend_notification_enabled")
        val TREND_NOTIFICATION_LOW_THRESHOLD = floatPreferencesKey("trend_notification_low_threshold")
        val TREND_NOTIFICATION_HIGH_THRESHOLD = floatPreferencesKey("trend_notification_high_threshold")
    }

    val breakfastCoefficient: Flow<Float> = dataStore.data.map {
        it[PreferencesKeys.BREAKFAST_COEFFICIENT] ?: 0f
    }

    val dinnerCoefficient: Flow<Float> = dataStore.data.map {
        it[PreferencesKeys.DINNER_COEFFICIENT] ?: 0f
    }

    val supperCoefficient: Flow<Float> = dataStore.data.map {
        it[PreferencesKeys.SUPPER_COEFFICIENT] ?: 0f
    }

    val carbsPerBu: Flow<Float> = dataStore.data.map {
        it[PreferencesKeys.CARBS_PER_BU] ?: 10f
    }

    val dailyCarbsGoal: Flow<Float> = dataStore.data.map {
        it[PreferencesKeys.DAILY_CARBS_GOAL] ?: 200f
    }

    val insulinDoseAccuracy: Flow<Float> = dataStore.data.map {
        it[PreferencesKeys.INSULIN_DOSE_ACCURACY] ?: 0.5f
    }

    val postMealNotificationEnabled: Flow<Boolean> = dataStore.data.map {
        it[PreferencesKeys.POST_MEAL_NOTIFICATION_ENABLED] ?: false
    }

    val postMealNotificationDelay: Flow<Int> = dataStore.data.map {
        it[PreferencesKeys.POST_MEAL_NOTIFICATION_DELAY] ?: 120
    }

    val trendNotificationEnabled: Flow<Boolean> = dataStore.data.map {
        it[PreferencesKeys.TREND_NOTIFICATION_ENABLED] ?: false
    }

    val trendNotificationLowThreshold: Flow<Float> = dataStore.data.map {
        it[PreferencesKeys.TREND_NOTIFICATION_LOW_THRESHOLD] ?: 4.0f
    }

    val trendNotificationHighThreshold: Flow<Float> = dataStore.data.map {
        it[PreferencesKeys.TREND_NOTIFICATION_HIGH_THRESHOLD] ?: 10.0f
    }

    suspend fun saveSettings(
        breakfast: Float, dinner: Float, supper: Float,
        carbsPerBu: Float, dailyCarbsGoal: Float, insulinDoseAccuracy: Float,
        postMealEnabled: Boolean, postMealDelay: Int,
        trendNotificationsEnabled: Boolean, trendLowThreshold: Float, trendHighThreshold: Float
    ) {
        dataStore.edit {
            it[PreferencesKeys.BREAKFAST_COEFFICIENT] = breakfast
            it[PreferencesKeys.DINNER_COEFFICIENT] = dinner
            it[PreferencesKeys.SUPPER_COEFFICIENT] = supper
            it[PreferencesKeys.CARBS_PER_BU] = carbsPerBu
            it[PreferencesKeys.DAILY_CARBS_GOAL] = dailyCarbsGoal
            it[PreferencesKeys.INSULIN_DOSE_ACCURACY] = insulinDoseAccuracy
            it[PreferencesKeys.POST_MEAL_NOTIFICATION_ENABLED] = postMealEnabled
            it[PreferencesKeys.POST_MEAL_NOTIFICATION_DELAY] = postMealDelay
            it[PreferencesKeys.TREND_NOTIFICATION_ENABLED] = trendNotificationsEnabled
            it[PreferencesKeys.TREND_NOTIFICATION_LOW_THRESHOLD] = trendLowThreshold
            it[PreferencesKeys.TREND_NOTIFICATION_HIGH_THRESHOLD] = trendHighThreshold
        }
    }
}