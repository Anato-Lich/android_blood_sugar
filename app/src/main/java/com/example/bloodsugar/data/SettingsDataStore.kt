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

    suspend fun saveCoefficients(breakfast: Float, dinner: Float, supper: Float) {
        dataStore.edit {
            it[PreferencesKeys.BREAKFAST_COEFFICIENT] = breakfast
            it[PreferencesKeys.DINNER_COEFFICIENT] = dinner
            it[PreferencesKeys.SUPPER_COEFFICIENT] = supper
        }
    }

    suspend fun saveCarbsPerBu(carbs: Float) {
        dataStore.edit {
            it[PreferencesKeys.CARBS_PER_BU] = carbs
        }
    }

    suspend fun saveDailyCarbsGoal(goal: Float) {
        dataStore.edit {
            it[PreferencesKeys.DAILY_CARBS_GOAL] = goal
        }
    }

    suspend fun saveInsulinDoseAccuracy(accuracy: Float) {
        dataStore.edit {
            it[PreferencesKeys.INSULIN_DOSE_ACCURACY] = accuracy
        }
    }

    suspend fun savePostMealNotificationSettings(enabled: Boolean, delay: Int) {
        dataStore.edit {
            it[PreferencesKeys.POST_MEAL_NOTIFICATION_ENABLED] = enabled
            it[PreferencesKeys.POST_MEAL_NOTIFICATION_DELAY] = delay
        }
    }
}