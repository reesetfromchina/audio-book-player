package com.ive.audiobookplayer.utils

import android.annotation.SuppressLint

@SuppressLint("DefaultLocale")
object TimeFormatter{
    fun formatTime(ms: Long) : String{
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
}