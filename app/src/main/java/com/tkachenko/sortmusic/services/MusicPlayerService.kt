package com.tkachenko.sortmusic.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.decodeBitmap
import androidx.core.net.toUri
import androidx.lifecycle.MutableLiveData
import com.tkachenko.sortmusic.R
import com.tkachenko.sortmusic.activities.MusicActivity
import com.tkachenko.sortmusic.player.MusicPlayer
import com.tkachenko.sortmusic.utils.Utils
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate
import kotlin.math.roundToInt

private const val TAG = "MusicPlayerService"
private const val NOTIFICATION_ID = 101
private const val CHANNEL_ID = "SERVICE_PLAYER_CHANNEL"
private const val CHANNEL_NAME = "SERVICE_PLAYER"

class MusicPlayerService: Service() {
    private val myBinder: IBinder = MyBinder()
    private val audioNotePlayer: MusicPlayer by lazy { MusicPlayer() }
    private var timer: Timer? = null
    private val maxProgressMutableLiveData: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    private val progressMutableLiveData: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val maxProgressLiveData = maxProgressMutableLiveData
    val progressLiveData = progressMutableLiveData

    private var callback: Callback? = null

    private val pendingIntent by lazy {
        PendingIntent.getActivity(this, 0, Intent(this, MusicActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    private val notificationBuilder: NotificationCompat.Builder by lazy {
        NotificationCompat.Builder(this, CHANNEL_ID).apply {
            setContentTitle("")
            setContentText("00:00")
            setSmallIcon(R.drawable.ic_stat_music)
            setLargeIcon(icon)
            setContentIntent(pendingIntent)
            addAction(R.drawable.ic_stat_pause, "Пауза", pendingIntent)
            priority = NotificationCompat.PRIORITY_MAX
        }
    }
    private var isShowNotification = false
    private var icon: Bitmap? = null

    fun attachCallback(callback: Callback) {
        this.callback = callback
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.d(TAG, "onBind")
        return myBinder
    }

    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)
        Log.d(TAG, "onRebind")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "onUnbind")
        return super.onUnbind(intent)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopPlay()
        stopTimer()
        stopNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onCreate() {
        super.onCreate()
        audioNotePlayer.createAudioPlayer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")

        if (intent?.action != null) {
            if (intent.action.equals(START_PLAY, ignoreCase = true)) {
                val uri: String = intent.getStringExtra(URI)!!
                val title = intent.getStringExtra(TITLE).toString()
                val artist = intent.getStringExtra(ARTIST).toString()

                val mediaMetadataRetriever = MediaMetadataRetriever()
                mediaMetadataRetriever.setDataSource(this, uri.toUri())
                val art: ByteArray? = mediaMetadataRetriever.embeddedPicture
                val bitmap: Bitmap?
                if (art != null) {
                    bitmap = BitmapFactory.decodeByteArray(art, 0, art.size)
                    icon = bitmap
                }

                startPlay(uri)
                startTimer()
                startNotification()
                updateContentTitle(title, artist)
            }
            if (intent.action.equals(STOP_PLAY, ignoreCase = true)) {
                stopPlay()
                stopTimer()
                stopNotification()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    stopForeground(false)
                }
                stopSelf()
            }
            if (intent.action.equals(CONTINUE_PLAY, ignoreCase = true)) {
                continuePlay()
            }
            if (intent.action.equals(PAUSE_PLAY, ignoreCase = true)) {
                pausePlay()
            }
            if (intent.action.equals(REWIND_PLAY, ignoreCase = true)) {
                val progress: Int = intent.getIntExtra(PROGRESS, 0)
                Log.d(TAG, progress.toString())
                rewindPlay(progress)
            }
        }

        return START_STICKY
    }

    private fun startPlay(uri: String) {
        audioNotePlayer.playStart(this, uri.toUri())
        audioNotePlayer.onCompletion {
            maxProgressLiveData.postValue(0)
            callback?.onCompletion()
            it?.reset()
            it?.stop()
            stopTimer()
            stopNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                stopForeground(false)
            }
            stopSelf()
        }
    }

    private fun stopPlay() {
        maxProgressLiveData.postValue(0)
        audioNotePlayer.playStop()
    }

    private fun continuePlay() {
        audioNotePlayer.playContinue()
        audioNotePlayer.onCompletion {
            maxProgressLiveData.postValue(0)
            callback?.onCompletion()
            it?.reset()
            it?.stop()
            stopTimer()
            stopNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                stopForeground(false)
            }
            stopSelf()
        }
    }

    private fun pausePlay() {
        audioNotePlayer.playPause()
    }

    private fun rewindPlay(progress: Int) {
        audioNotePlayer.playRewind(progress)
    }

    private fun startTimer() {
        timer = Timer()
        timer?.scheduleAtFixedRate(TimeTask(), 0, 1000)
    }

    private fun stopTimer() {
        timer?.cancel()
        timer = null
    }

    private inner class TimeTask : TimerTask() {
        private val duration: Int? = audioNotePlayer.getDuration()?.div(1000)

        init {
            maxProgressMutableLiveData.postValue(duration)
        }

        override fun run() {
            val currentProgress = audioNotePlayer.getCurrentPosition()?.toDouble()?.div(1000)?.roundToInt()
            if (currentProgress != null) {
                if (currentProgress >= 0)
                    progressMutableLiveData.postValue(currentProgress)

                if (isShowNotification) {
                    updateNotification("${Utils.getTimeStringFromInt(currentProgress)} / ${Utils.getTimeStringFromInt(duration!!)}")
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel() = NotificationChannel(
        CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT
    ).apply {
        setSound(null, null)
    }

    private fun startNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(createChannel())
            startForeground(NOTIFICATION_ID, notificationBuilder.build())
        } else {
            notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
        }
        isShowNotification = true
    }

    private fun stopNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.deleteNotificationChannel(CHANNEL_ID)
        }
        notificationManager.cancel(NOTIFICATION_ID)
        notificationBuilder.clearPeople()
        isShowNotification = false
    }

    private fun updateNotification(notificationText: String? = null) {
        notificationText.let { notificationBuilder.setContentText(it) }
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
    }

    private fun updateContentTitle(title: String, artist: String) {
        notificationBuilder.setContentTitle("$title - $artist")
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
    }

    inner class MyBinder: Binder() {
        val service: MusicPlayerService
            get() = this@MusicPlayerService
    }

    interface Callback {
        fun onCompletion()
    }

    companion object {
        const val START_PLAY = "START_PLAY"
        const val STOP_PLAY = "STOP_PLAY"
        const val CONTINUE_PLAY = "CONTINUE_PLAY"
        const val PAUSE_PLAY = "PAUSE_PLAY"
        const val REWIND_PLAY = "REWIND_PLAY"
        const val PROGRESS = "PROGRESS"
        const val URI = "URI"
        const val TITLE = "TITLE"
        const val ARTIST = "ARTIST"
    }
}