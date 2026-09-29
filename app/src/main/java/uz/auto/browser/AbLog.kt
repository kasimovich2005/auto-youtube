package uz.auto.browser

import android.util.Log

/**
 * Logcat helper. Everything is logged in debug builds only; release builds
 * log nothing, so URLs and other browsing data never reach Logcat there.
 *
 * Filter with: adb logcat -s AutoBrowser AndroidAuto WebView YouTube Navigation
 */
object AbLog {
    const val APP = "AutoBrowser"
    const val AUTO = "AndroidAuto"
    const val WEBVIEW = "WebView"
    const val YOUTUBE = "YouTube"
    const val NAV = "Navigation"

    fun d(tag: String, msg: String) {
        if (BuildConfig.DEBUG) Log.d(tag, msg)
    }

    fun w(tag: String, msg: String, t: Throwable? = null) {
        if (BuildConfig.DEBUG) Log.w(tag, msg, t)
    }
}
