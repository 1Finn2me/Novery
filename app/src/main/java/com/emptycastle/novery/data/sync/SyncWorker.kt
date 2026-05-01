package com.emptycastle.novery.data.sync

import android.app.Notification
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.emptycastle.novery.data.local.PreferencesManager
import com.emptycastle.novery.service.NotificationHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import java.util.concurrent.TimeUnit

/**
 * Background worker for manual and automatic sync jobs.
 */
class SyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    private val preferencesManager = PreferencesManager.getInstance(applicationContext)
    private val notifier = SyncNotifier(applicationContext)

    override suspend fun doWork(): Result {
        if (!syncMutex.tryLock()) {
            return Result.success()
        }

        return try {
            val trigger = inputData.getString(KEY_TRIGGER)
                ?.let { runCatching { SyncTrigger.valueOf(it) }.getOrNull() }
                ?: SyncTrigger.AUTO

            if (trigger != SyncTrigger.MANUAL &&
                !SyncManager.shouldTriggerSync(preferencesManager, trigger)
            ) {
                return Result.success()
            }
            if (isQueuedTrigger(trigger) && isInsideTriggerCooldown(preferencesManager)) {
                return Result.success()
            }

            val showNotifications = preferencesManager.getSyncSettings().showProgressNotifications
            val useForeground = trigger == SyncTrigger.MANUAL && showNotifications

            if (useForeground) {
                setForeground(createForegroundInfo("Preparing sync"))
            }

            SyncManager(applicationContext).sync(trigger)
                .fold(
                    onSuccess = { message ->
                        if (showNotifications) {
                            notifier.showSuccess(message)
                        }
                        if (trigger == SyncTrigger.MANUAL) {
                            schedule(applicationContext, forceUpdate = true)
                        }
                        Result.success()
                    },
                    onFailure = { error ->
                        if (showNotifications) {
                            notifier.showError(error.message ?: "Sync failed.")
                        }
                        Result.failure()
                    }
                )
        } catch (_: CancellationException) {
            SyncStatusTracker.finishCancelled()
            if (inputData.getString(KEY_TRIGGER) == SyncTrigger.MANUAL.name &&
                preferencesManager.getSyncSettings().showProgressNotifications
            ) {
                notifier.clear()
            }
            Result.success()
        } catch (error: Exception) {
            SyncStatusTracker.finishError(error.message ?: "Sync failed.")
            if (preferencesManager.getSyncSettings().showProgressNotifications) {
                notifier.showError(error.message ?: "Sync failed.")
            }
            Result.failure()
        } finally {
            syncMutex.unlock()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return createForegroundInfo("Preparing sync")
    }

    private fun createForegroundInfo(stage: String): ForegroundInfo {
        val notification: Notification = notifier.showProgress(stage)
        return ForegroundInfo(
            NotificationHelper.NOTIFICATION_ID_SYNC,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            }
        )
    }

    companion object {
        const val UNIQUE_PERIODIC_WORK = "novery_sync_periodic"
        const val UNIQUE_IMMEDIATE_WORK = "novery_sync_immediate"
        const val UNIQUE_TRIGGER_WORK = "novery_sync_triggered"
        const val TAG_SYNC = "novery_sync"
        private const val AUTO_TRIGGER_DELAY_MINUTES = 5L
        private const val MIN_TRIGGER_SYNC_INTERVAL_MINUTES = 15L
        private const val KEY_TRIGGER = "trigger"
        private val syncMutex = Mutex()
        private val SUPPORTED_PERIODIC_INTERVALS = setOf(
            30,
            60,
            180,
            360,
            720,
            1440,
            2880,
            10080
        )

        fun isRunning(context: Context): Boolean {
            val workManager = WorkManager.getInstance(context)
            val workInfos = workManager.getWorkInfosByTag(TAG_SYNC).get()
            return workInfos.any { it.isActiveSyncWork() }
        }

        fun schedule(context: Context, forceUpdate: Boolean = false) {
            val prefs = PreferencesManager.getInstance(context)
            val settings = prefs.getSyncSettings()
            val workManager = WorkManager.getInstance(context)
            val intervalMinutes = normalizedPeriodicInterval(settings.intervalMinutes)

            if (settings.service == SyncServiceType.NONE || !settings.googleDriveSignedIn) {
                workManager.cancelUniqueWork(UNIQUE_TRIGGER_WORK)
                workManager.cancelUniqueWork(UNIQUE_PERIODIC_WORK)
                return
            }

            if (intervalMinutes <= 0) {
                workManager.cancelUniqueWork(UNIQUE_PERIODIC_WORK)
                return
            }

            val request = PeriodicWorkRequestBuilder<SyncWorker>(
                intervalMinutes.toLong(),
                TimeUnit.MINUTES,
                periodicFlexMinutes(intervalMinutes).toLong(),
                TimeUnit.MINUTES
            )
                .setInputData(workDataOf(KEY_TRIGGER to SyncTrigger.AUTO.name))
                .setConstraints(syncConstraints(requireBatteryNotLow = true))
                .addTag(TAG_SYNC)
                .build()

            workManager.enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC_WORK,
                if (forceUpdate) {
                    ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
                } else {
                    ExistingPeriodicWorkPolicy.KEEP
                },
                request
            )
        }

        fun triggerNow(context: Context, trigger: SyncTrigger) {
            val prefs = PreferencesManager.getInstance(context)
            prefs.refreshSyncSettings()
            if (!SyncManager.shouldTriggerSync(prefs, trigger)) {
                return
            }

            if (trigger == SyncTrigger.MANUAL) {
                val request = OneTimeWorkRequestBuilder<SyncWorker>()
                    .setInputData(workDataOf(KEY_TRIGGER to trigger.name))
                    .setConstraints(syncConstraints(requireBatteryNotLow = false))
                    .addTag(TAG_SYNC)
                    .build()

                WorkManager.getInstance(context).enqueueUniqueWork(
                    UNIQUE_IMMEDIATE_WORK,
                    ExistingWorkPolicy.KEEP,
                    request
                )
                return
            }

            if (isInsideTriggerCooldown(prefs)) {
                return
            }

            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setInputData(workDataOf(KEY_TRIGGER to trigger.name))
                .setInitialDelay(AUTO_TRIGGER_DELAY_MINUTES, TimeUnit.MINUTES)
                .setConstraints(syncConstraints(requireBatteryNotLow = true))
                .addTag(TAG_SYNC)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_TRIGGER_WORK,
                ExistingWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            val workManager = WorkManager.getInstance(context)
            workManager.cancelUniqueWork(UNIQUE_IMMEDIATE_WORK)
            workManager.cancelUniqueWork(UNIQUE_TRIGGER_WORK)
            workManager.cancelUniqueWork(UNIQUE_PERIODIC_WORK)
        }

        private fun isInsideTriggerCooldown(preferencesManager: PreferencesManager): Boolean {
            val lastSyncTimestamp = preferencesManager.getSyncSettings().lastSyncTimestamp
            if (lastSyncTimestamp <= 0L) {
                return false
            }

            val elapsedMinutes = TimeUnit.MILLISECONDS.toMinutes(
                System.currentTimeMillis() - lastSyncTimestamp
            )
            return elapsedMinutes < MIN_TRIGGER_SYNC_INTERVAL_MINUTES
        }

        private fun isQueuedTrigger(trigger: SyncTrigger): Boolean {
            return trigger == SyncTrigger.APP_START ||
                    trigger == SyncTrigger.APP_RESUME ||
                    trigger == SyncTrigger.CHAPTER_OPEN ||
                    trigger == SyncTrigger.CHAPTER_READ
        }

        private fun periodicFlexMinutes(intervalMinutes: Int): Int {
            return intervalMinutes.coerceAtMost(10).coerceAtLeast(5)
        }

        private fun normalizedPeriodicInterval(intervalMinutes: Int): Int {
            return if (SUPPORTED_PERIODIC_INTERVALS.contains(intervalMinutes)) {
                intervalMinutes
            } else {
                0
            }
        }

        private fun syncConstraints(requireBatteryNotLow: Boolean): Constraints {
            val builder = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)

            if (requireBatteryNotLow) {
                builder.setRequiresBatteryNotLow(true)
            }

            return builder.build()
        }

        private fun WorkInfo.isActiveSyncWork(): Boolean {
            return state == WorkInfo.State.RUNNING || state == WorkInfo.State.ENQUEUED
        }
    }
}
