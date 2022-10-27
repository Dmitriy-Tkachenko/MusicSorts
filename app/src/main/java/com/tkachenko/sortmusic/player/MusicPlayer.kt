package com.tkachenko.sortmusic.player

import android.content.Context
import android.media.*
import android.net.Uri

private const val TAG = "MusicPlayer"

class MusicPlayer {
    private var mediaPlayer: MediaPlayer? = null

    fun createAudioPlayer() {
        mediaPlayer = MediaPlayer()
    }

    fun playStart(context: Context, uri: Uri) {
        playStop()
        mediaPlayer?.apply {
            setDataSource(context, uri)
            prepare()
            start()
        }
    }

    fun playContinue() {
        mediaPlayer?.start()
    }

    fun playPause() {
        mediaPlayer?.pause()
    }

    fun playStop() {
        mediaPlayer?.stop()
        mediaPlayer?.reset()
    }

    fun playRewind(progress: Int) {
        mediaPlayer?.seekTo(progress * 1000)
    }

    fun onCompletion(onCompletionListener: MediaPlayer.OnCompletionListener) {
        mediaPlayer?.setOnCompletionListener(onCompletionListener)
    }

    fun getDuration() = mediaPlayer?.duration

    fun getCurrentPosition() = mediaPlayer?.currentPosition
}