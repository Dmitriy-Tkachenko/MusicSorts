package com.tkachenko.sortmusic.player

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.tkachenko.sortmusic.utils.Utils
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "MusicFileData"

class MusicFileData(private val context: Context) {
    private val mediaMetadataRetriever = MediaMetadataRetriever()

    fun getTitle(uri: Uri): String {
        mediaMetadataRetriever.setDataSource(context, uri)
        var title: String? = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
        if (title == null) {
            val file = uri.path?.let { File(it) }
            val name = file?.nameWithoutExtension ?: "Неизвестно"
            title = name
        }
        return title
    }

    fun getName(uri: Uri): String {
        val file = uri.path?.let { File(it) }
        return file?.nameWithoutExtension ?: "Неизвестно"
    }

    fun getImage(uri: Uri): Bitmap? {
        mediaMetadataRetriever.setDataSource(context, uri)
        val art: ByteArray? = mediaMetadataRetriever.embeddedPicture
        var bitmap: Bitmap? = null
        if (art != null) {
            bitmap = BitmapFactory.decodeByteArray(art, 0, art.size)
        }
        return bitmap
    }

    fun getArtist(uri: Uri): String {
        mediaMetadataRetriever.setDataSource(context, uri)
        return mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Неизвестно"
    }

    fun getDuration(uri: Uri): String {
        mediaMetadataRetriever.setDataSource(context, uri)
        return Utils.getConvertMillisecondsToTimes(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0)
    }

    fun getExtension(uri: Uri): String {
        val file = uri.path?.let { File(it) }
        return file?.extension ?: ""
    }
}