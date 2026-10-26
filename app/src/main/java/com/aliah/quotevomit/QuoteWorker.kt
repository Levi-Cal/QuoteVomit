package com.aliah.quotevomit

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/** Background job: grab a new quote and push it to every widget. */
class QuoteWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        QuoteRepository.refresh(applicationContext)
        QuoteWidget.updateAll(applicationContext)
        Result.success()
    }
}

object QuoteScheduler {
    private const val HOURLY = "hourly_quote"
    private const val NOW = "quote_now"

    /** Every hour. KEEP = don't reset the timer if it's already scheduled. */
    fun scheduleHourly(context: Context) {
        val request = PeriodicWorkRequestBuilder<QuoteWorker>(1, TimeUnit.HOURS).build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(HOURLY, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun refreshNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<QuoteWorker>().build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(NOW, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancelAll(context: Context) {
        WorkManager.getInstance(context).apply {
            cancelUniqueWork(HOURLY)
            cancelUniqueWork(NOW)
        }
    }
}
