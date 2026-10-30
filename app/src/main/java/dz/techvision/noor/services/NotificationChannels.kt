package dz.techvision.noor.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import dz.techvision.noor.R

object NotificationChannels {
    const val CHANNEL_PRAYER = "noor_prayer_reminders"
    const val CHANNEL_MEDIA = "noor_media_playback"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            val prayerChannel = NotificationChannel(
                CHANNEL_PRAYER,
                context.getString(R.string.notification_prayer_reminders),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Prayer time reminders and notifications"
            }

            val mediaChannel = NotificationChannel(
                CHANNEL_MEDIA,
                "Quran Audio Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Media controls for audio recitation foreground service"
            }

            notificationManager.createNotificationChannels(
                listOf(prayerChannel, mediaChannel)
            )
        }
    }
}
