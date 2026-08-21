package dev.shorthouse.coinwatch

import android.app.Application
import timber.log.Timber

class BaseApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            Timber.d("Timber DebugTree planted")
        }
    }
}
