package com.ive.audiobookplayer.utils

import android.annotation.SuppressLint

@SuppressLint("DefaultLocale")
object TimeFormatter{
    fun formatTime(ms: Long) : String{
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) /60
        val seconds = totalSeconds % 60
        return if (hours>0){
            "%d:%02d:%02d".format(hours, minutes, seconds)
        }else{
            "%02d:%02d".format(minutes, seconds)
        }
    }
}