package com.tkachenko.sortmusic.utils

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

object Utils {
    @SuppressLint("SimpleDateFormat")
    fun getDateByPattern(date: Long): String = SimpleDateFormat("dd.MM.yyyy в HH:mm").format(Date(date))

    @SuppressLint("SimpleDateFormat")
    fun getConvertMillisecondsToTimes(millis: Long): String = SimpleDateFormat("mm:ss").format(Date(millis))

    fun getTimeStringFromDouble(time: Double): String {
        val resultInt = time.roundToInt()
        val minutes = resultInt % 86400 % 3600 / 60
        val seconds = resultInt % 86400 % 3600 % 60

        return makeTimeString(minutes, seconds)
    }

    fun getTimeStringFromInt(time: Int): String {
        val minutes = time % 3600 / 60
        val seconds = time % 60
        return makeTimeString(minutes, seconds)
    }

    private fun makeTimeString(min: Int, sec: Int): String = String.format("%02d:%02d", min, sec)
}