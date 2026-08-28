package com.metahumanlegacy.game

import android.app.Application

class MetahumanApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NarrativeCodec.installAssetParts { path ->
            assets.open(path).use { it.readBytes() }
        }
    }
}
