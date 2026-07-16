package com.kps.trackmyweight.widget

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.kps.trackmyweight.data.repository.GoalRepository
import com.kps.trackmyweight.data.repository.WeightRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Écrit le poids courant + objectif dans le state du widget puis force une mise à jour.
 * À appeler après chaque log de poids.
 */
@Singleton
class WeightWidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
    private val weightRepo: WeightRepository,
    private val goalRepo: GoalRepository,
) {
    suspend fun refresh() {
        val last = weightRepo.observeLast().first()
        val goal = goalRepo.observeActive().first()
        val manager = GlanceAppWidgetManager(context)
        manager.getGlanceIds(WeightWidget::class.java).forEach { id ->
            updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { prefs ->
                prefs.toMutablePreferences().apply {
                    if (last != null) {
                        this[WeightWidget.LAST_WEIGHT_KEY] = last.weightKg
                        this[WeightWidget.LAST_DATE_KEY] = last.date.toString()
                    }
                    if (goal != null) {
                        this[WeightWidget.GOAL_WEIGHT_KEY] = goal.targetWeightKg
                    }
                }
            }
        }
        WeightWidget.refresh(context)
    }
}
