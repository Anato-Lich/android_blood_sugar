package com.example.bloodsugar.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
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
        val POST_MEAL_NOTIFICATION_ENABLED = booleanPreferencesKey("post_meal_notification_enabled")
        val POST_MEAL_NOTIFICATION_DELAY = intPreferencesKey("post_meal_notification_delay")
        val TREND_NOTIFICATION_ENABLED = booleanPreferencesKey("trend_notification_enabled")
        val TREND_NOTIFICATION_LOW_THRESHOLD = floatPreferencesKey("trend_notification_low_threshold")
        val TREND_NOTIFICATION_HIGH_THRESHOLD = floatPreferencesKey("trend_notification_high_threshold")
        val TREND_NOTIFICATION_TIME_WINDOW = intPreferencesKey("trend_notification_time_window")

        const val BREAKFAST_COEFFICIENT_DEFAULT = 0f
        const val DINNER_COEFFICIENT_DEFAULT = 0f
        const val SUPPER_COEFFICIENT_DEFAULT = 0f
        const val CARBS_PER_BU_DEFAULT = 10f
        const val DAILY_CARBS_GOAL_DEFAULT = 200f
        const val INSULIN_DOSE_ACCURACY_DEFAULT = 0.5f
        const val POST_MEAL_NOTIFICATION_ENABLED_DEFAULT = false
        const val POST_MEAL_NOTIFICATION_DELAY_DEFAULT = 120
        const val TREND_NOTIFICATION_ENABLED_DEFAULT = false
        const val TREND_NOTIFICATION_LOW_THRESHOLD_DEFAULT = 4.0f
        const val TREND_NOTIFICATION_HIGH_THRESHOLD_DEFAULT = 10.0f
        const val TREND_NOTIFICATION_TIME_WINDOW_DEFAULT = 60
    }

    val breakfastCoefficient: Flow<Float> = dataStore.data.map {
        it[PreferencesKeys.BREAKFAST_COEFFICIENT] ?: PreferencesKeys.BREAKFAST_COEFFICIENT_DEFAULT
    }

    val dinnerCoefficient: Flow<Float> = dataStore.data.map {
        it[PreferencesKeys.DINNER_COEFFICIENT] ?: PreferencesKeys.DINNER_COEFFICIENT_DEFAULT
    }

    val supperCoefficient: Flow<Float> = dataStore.data.map {
        it[PreferencesKeys.SUPPER_COEFFICIENT] ?: PreferencesKeys.SUPPER_COEFFICIENT_DEFAULT
    }

    val carbsPerBu: Flow<Float> = dataStore.data.map {
        it[PreferencesKeys.CARBS_PER_BU] ?: PreferencesKeys.CARBS_PER_BU_DEFAULT
    }

    val dailyCarbsGoal: Flow<Float> = dataStore.data.map {
        it[PreferencesKeys.DAILY_CARBS_GOAL] ?: PreferencesKeys.DAILY_CARBS_GOAL_DEFAULT
    }

    val insulinDoseAccuracy: Flow<Float> = dataStore.data.map {
        it[PreferencesKeys.INSULIN_DOSE_ACCURACY] ?: PreferencesKeys.INSULIN_DOSE_ACCURACY_DEFAULT
    }

    val postMealNotificationEnabled: Flow<Boolean> = dataStore.data.map {
        it[PreferencesKeys.POST_MEAL_NOTIFICATION_ENABLED] ?: PreferencesKeys.POST_MEAL_NOTIFICATION_ENABLED_DEFAULT
    }

    val postMealNotificationDelay: Flow<Int> = dataStore.data.map {
        it[PreferencesKeys.POST_MEAL_NOTIFICATION_DELAY] ?: PreferencesKeys.POST_MEAL_NOTIFICATION_DELAY_DEFAULT
    }

    val trendNotificationEnabled: Flow<Boolean> = dataStore.data.map {
        it[PreferencesKeys.TREND_NOTIFICATION_ENABLED] ?: PreferencesKeys.TREND_NOTIFICATION_ENABLED_DEFAULT
    }

    val trendNotificationLowThreshold: Flow<Float> = dataStore.data.map {
        it[PreferencesKeys.TREND_NOTIFICATION_LOW_THRESHOLD] ?: PreferencesKeys.TREND_NOTIFICATION_LOW_THRESHOLD_DEFAULT
    }

    val trendNotificationHighThreshold: Flow<Float> = dataStore.data.map {
        it[PreferencesKeys.TREND_NOTIFICATION_HIGH_THRESHOLD] ?: PreferencesKeys.TREND_NOTIFICATION_HIGH_THRESHOLD_DEFAULT
    }

    val trendNotificationTimeWindow: Flow<Int> = dataStore.data.map {
        it[PreferencesKeys.TREND_NOTIFICATION_TIME_WINDOW] ?: PreferencesKeys.TREND_NOTIFICATION_TIME_WINDOW_DEFAULT
    }

    suspend fun saveSettings(
        breakfast: Float, dinner: Float, supper: Float,
        carbsPerBu: Float, dailyCarbsGoal: Float, insulinDoseAccuracy: Float,
        postMealEnabled: Boolean, postMealDelay: Int,
        trendNotificationsEnabled: Boolean, trendLowThreshold: Float, trendHighThreshold: Float,
        trendTimeWindow: Int
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
            it[PreferencesKeys.TREND_NOTIFICATION_TIME_WINDOW] = trendTimeWindow
        }
    }
}
