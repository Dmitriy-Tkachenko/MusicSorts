package com.tkachenko.sortmusic.models

import android.graphics.Bitmap
import android.net.Uri

data class Music(
    var image: Bitmap? = null,
    var title: String,
    var name: String,
    var artist: String,
    var duration: String,
    var uri: Uri,
    var extension: String,
    var isPlay: Boolean = false,
    var isPause: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as Music

        if (uri != other.uri) return false

        return true
    }

    override fun hashCode(): Int {
        var result = image.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + artist.hashCode()
        result = 31 * result + duration.hashCode()
        result = 31 * result + uri.hashCode()
        return result
    }
}