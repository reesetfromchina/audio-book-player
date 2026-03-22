package com.ive.audiobookplayer

import android.app.Application
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache

class AudiobookApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val imageLoader = ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.20)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("cover_cache"))
                    .maxSizeBytes(50L * 1024 * 1024)
                    .build()
            }
            .build()
        coil.Coil.setImageLoader(imageLoader)
    }
}