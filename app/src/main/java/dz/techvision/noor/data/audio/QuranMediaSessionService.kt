package dz.techvision.noor.data.audio

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dz.techvision.noor.MainActivity
import dz.techvision.noor.services.ServiceLocator

/**
 * Media3 [MediaSessionService] bridging the shared app-scoped player to the
 * system.
 *
 * The player is owned by [ServiceLocator] (one instance serves the in-app
 * controller and this session). Media3's [MediaSessionService] manages only
 * session/controller plumbing, never the player, so stopping the service
 * leaves playback state and the application play/pause controls intact for
 * the next time the user plays. media3 surfaces the media playback
 * foreground-service notification from here while audio is playing.
 */
@OptIn(UnstableApi::class)
class QuranMediaSessionService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        android.util.Log.v(DIAG, "onCreate: building session")
        mediaSession = MediaSession.Builder(this, ServiceLocator.quranAudioEngine.player)
            .setSessionActivity(openAppPendingIntent())
            .build()
        android.util.Log.v(DIAG, "onCreate: session=${mediaSession?.id}")
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        android.util.Log.v(DIAG, "onGetSession: controller=${controllerInfo.packageName}")
        return mediaSession
    }

    override fun onStartCommand(intent: android.content.Intent?, flags: Int, startId: Int): Int {
        android.util.Log.v(DIAG, "onStartCommand: action=${intent?.action}")
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        android.util.Log.v(DIAG, "onDestroy")
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    private companion object {
        const val DIAG = "NoorAudioDiag"
    }

    /** Tapping the (lockscreen/media) notification returns to the app in place. */
    private fun openAppPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
