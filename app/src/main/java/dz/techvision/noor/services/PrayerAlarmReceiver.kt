package dz.techvision.noor.services

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dz.techvision.noor.MainActivity
import dz.techvision.noor.R
import dz.techvision.noor.domain.model.PrayerName
import dz.techvision.noor.presentation.common.labelRes
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class PrayerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "dz.techvision.noor.ACTION_PRAYER_ALARM") return

        val settingsRepo = ServiceLocator.settingsRepository
        val (notificationsEnabled, prayerRemindersEnabled) = runBlocking {
            val enabled = settingsRepo.notificationsEnabled.first()
            val reminders = settingsRepo.prayerRemindersEnabled.first()
            enabled to reminders
        }

        if (!notificationsEnabled || !prayerRemindersEnabled) return

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val prayerNameStr = intent.getStringExtra(PrayerAlarmScheduler.EXTRA_PRAYER_NAME) ?: return
        val prayerName = runCatching { PrayerName.valueOf(prayerNameStr) }.getOrNull() ?: return

        val prayerLabel = context.getString(prayerName.labelRes)

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_PRAYER)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.home_greeting))
            .setContentText("حان الآن وقت صلاة $prayerLabel")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(prayerName.ordinal + 1000, notification)
        }
    }
}
