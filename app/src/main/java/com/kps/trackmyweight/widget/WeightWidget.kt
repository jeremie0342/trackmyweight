package com.kps.trackmyweight.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.kps.trackmyweight.MainActivity

private val InkBg = ColorProvider(Color(0xFF141416))
private val InkText = ColorProvider(Color(0xFFF5F5F7))
private val InkTextMuted = ColorProvider(Color(0xFF8E8E93))

class WeightWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs: Preferences = currentState()
            val lastKg = prefs[LAST_WEIGHT_KEY]
            val lastDate = prefs[LAST_DATE_KEY].orEmpty()
            val goalKg = prefs[GOAL_WEIGHT_KEY]

            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(InkBg)
                    .padding(16.dp)
                    .clickable(actionStartActivity<MainActivity>()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = "Dernière pesée",
                    style = TextStyle(color = InkTextMuted, fontSize = 11.sp),
                )
                Spacer(GlanceModifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = lastKg?.let { "%.1f".format(it) } ?: "—",
                        style = TextStyle(color = InkText, fontSize = 32.sp, fontWeight = FontWeight.Bold),
                    )
                    Text(
                        text = " kg",
                        style = TextStyle(color = InkTextMuted, fontSize = 14.sp),
                    )
                }
                if (lastDate.isNotEmpty()) {
                    Text(
                        text = lastDate,
                        style = TextStyle(color = InkTextMuted, fontSize = 11.sp),
                    )
                }
                Spacer(GlanceModifier.height(6.dp))
                if (goalKg != null && lastKg != null) {
                    val delta = lastKg - goalKg
                    val text = if (delta >= 0) {
                        "+%.1f kg vs objectif".format(delta)
                    } else {
                        "%.1f kg vs objectif".format(delta)
                    }
                    Text(text = text, style = TextStyle(color = InkTextMuted, fontSize = 11.sp))
                }
                Spacer(GlanceModifier.height(8.dp))
                Text(
                    text = "+ Ajouter une pesée",
                    style = TextStyle(color = ColorProvider(Color(0xFF5EEAD4)), fontSize = 12.sp, fontWeight = FontWeight.Medium),
                    modifier = GlanceModifier.clickable(actionStartActivity<QuickWeightEntryActivity>()),
                )
            }
        }
    }

    companion object {
        val LAST_WEIGHT_KEY = floatPreferencesKey("last_weight_kg")
        val LAST_DATE_KEY = stringPreferencesKey("last_weight_date")
        val GOAL_WEIGHT_KEY = floatPreferencesKey("goal_weight_kg")

        suspend fun refresh(context: Context) {
            WeightWidget().updateAll(context)
        }
    }
}

class WeightWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeightWidget()
}
