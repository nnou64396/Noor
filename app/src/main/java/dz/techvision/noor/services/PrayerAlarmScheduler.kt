package dz.techvision.noor.services

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import dz.techvision.noor.domain.model.DailyPrayerSchedule
import dz.techvision.noor.domain.model.PrayerName
import java.time.ZoneId
import java.time.ZonedDateTime

object PrayerAlarmScheduler {
    const val EXTRA_PRAYER_NAME = "extra_prayer_name"

    fun scheduleAlarms(
        context: Context,
        schedule: DailyPrayerSchedule,
        notificationsEnabled: Boolean,
        prayerRemindersEnabled: Boolean,
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        cancelAlarms(context, schedule)

        if (!notificationsEnabled || !prayerRemindersEnabled) return

        val zoneId = runCatching { ZoneId.of(schedule.timezone) }.getOrDefault(ZoneId.systemDefault())
        val now = ZonedDateTime.now(zoneId)

        for (prayerTime in schedule.prayers) {
            if (!prayerTime.prayer.isObligatoryPrayer) continue
            val prayerDateTime = ZonedDateTime.of(schedule.date, prayerTime.time, zoneId)
            if (prayerDateTime.isAfter(now)) {
                val triggerAtMillis = prayerDateTime.toInstant().toEpochMilli()
                val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                    action = "dz.techvision.noor.ACTION_PRAYER_ALARM"
                    putExtra(EXTRA_PRAYER_NAME, prayerTime.prayer.name)
                }
                val requestCode = generateRequestCode(schedule.date.hashCode(), prayerTime.prayer.ordinal)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )

                runCatching {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (alarmManager.canScheduleExactAlarms()) {
                            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                        } else {
                            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                        }
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                    } else {
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                    }
                }
            }
        }
    }

    fun cancelAlarms(context: Context, schedule: DailyPrayerSchedule) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        for (prayerName in PrayerName.obligatoryPrayers) {
            val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                action = "dz.techvision.noor.ACTION_PRAYER_ALARM"
                putExtra(EXTRA_PRAYER_NAME, prayerName.name)
            }
            val requestCode = generateRequestCode(schedule.date.hashCode(), prayerName.ordinal)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            alarmManager.cancel(pendingIntent)
        }
    }

    private fun generateRequestCode(dateHash: Int, prayerOrdinal: Int): Int {
        return dateHash + prayerOrdinal
    }
}
