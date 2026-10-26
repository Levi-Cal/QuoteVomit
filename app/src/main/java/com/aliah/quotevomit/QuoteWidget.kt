package com.aliah.quotevomit

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews

/** The home-screen widget itself. Tap it to get a new quote. */
class QuoteWidget : AppWidgetProvider() {

    override fun onEnabled(context: Context) {
        // First widget placed: start the hourly job and grab a fresh quote
        QuoteScheduler.scheduleHourly(context)
        QuoteScheduler.refreshNow(context)
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { manager.updateAppWidget(it, buildViews(context)) }
        QuoteScheduler.scheduleHourly(context) // safety net; KEEP means no duplicates
    }

    override fun onDisabled(context: Context) {
        // Last widget removed: stop running in the background
        QuoteScheduler.cancelAll(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REROLL) {
            QuoteScheduler.refreshNow(context)
        }
    }

    companion object {
        const val ACTION_REROLL = "com.aliah.quotevomit.REROLL"

        /** Quotes get quotation marks; tips and facts don't. */
        fun displayText(q: Quote): String =
            if (q.label == "QUOTE") "“${q.text}”" else q.text

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, QuoteWidget::class.java))
            ids.forEach { manager.updateAppWidget(it, buildViews(context)) }
        }

        private fun buildViews(context: Context): RemoteViews {
            val q = QuoteRepository.load(context)
            val views = RemoteViews(context.packageName, R.layout.widget_quote)
            views.setTextViewText(R.id.quote_label, q.label)
            views.setTextViewText(R.id.quote_text, displayText(q))
            if (q.author.isBlank()) {
                views.setViewVisibility(R.id.quote_author, View.GONE)
            } else {
                views.setViewVisibility(R.id.quote_author, View.VISIBLE)
                views.setTextViewText(R.id.quote_author, "— ${q.author}")
            }
            views.setTextViewText(R.id.quote_footer, "${q.source} · tap to reroll")

            // Pretty, unrelated background: a photo if we have one, else a pastel gradient
            val photo = BackgroundRepository.loadPhoto(context)
            if (photo != null) {
                views.setImageViewBitmap(R.id.bg_image, photo)
            } else {
                views.setImageViewResource(R.id.bg_image, BackgroundRepository.gradientRes(context))
            }

            val reroll = Intent(context, QuoteWidget::class.java).setAction(ACTION_REROLL)
            val pending = PendingIntent.getBroadcast(
                context, 0, reroll,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pending)
            return views
        }
    }
}
