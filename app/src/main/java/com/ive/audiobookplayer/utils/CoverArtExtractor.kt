package com.ive.audiobookplayer.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log

class CoverArtExtractor(private val context: Context) {

    // return a bitmap from embedded metadata if available
    fun extractCover(audioUri: Uri, targetSize: Int = 128): Bitmap? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, audioUri)
            val art = retriever.embeddedPicture
            retriever.release()
            Log.d("Cover", "extractCover for $audioUri — art found: ${art != null}")
            art?.let { decodeScaled(it, targetSize) }
        } catch (e: Exception) {
            Log.e("Cover", "extractCover failed: ${e.message}")
            null
        }
    }


    //decode byte array scaled down to targetSize
    private fun decodeScaled(bytes: ByteArray, targetSize: Int) : Bitmap?{
        // first pass just read dimensions without allocating pixels
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

        //calculate how much to shrink
        options.inSampleSize = calculateSampleSize(options.outWidth, options.outHeight, targetSize)
        options.inJustDecodeBounds = false

        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }

    private fun calculateSampleSize(width: Int, height: Int, targetSize: Int) : Int {
        var sampleSize = 1
        while( width / sampleSize > targetSize || height/sampleSize > targetSize){
            sampleSize*=2
        }
        return sampleSize
    }
}