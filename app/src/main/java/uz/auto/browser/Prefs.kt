package uz.auto.browser

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager

/** Typed access to the Settings screen values and the recent-pages list. */
class Prefs(context: Context) {
    private val sp: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    val openYouTubeOnStartup get() = sp.getBoolean(KEY_OPEN_YOUTUBE, true)
    val darkMode get() = sp.getBoolean(KEY_DARK_MODE, true)
    val rememberLastPage get() = sp.getBoolean(KEY_REMEMBER_LAST, true)
    val desktopUserAgent get() = sp.getBoolean(KEY_DESKTOP_UA, false)
    val javaScript get() = sp.getBoolean(KEY_JAVASCRIPT, true)
    val cookies get() = sp.getBoolean(KEY_COOKIES, true)

    var lastUrl: String?
        get() = sp.getString(KEY_LAST_URL, null)
        set(value) = sp.edit { putString(KEY_LAST_URL, value) }

    /** Set by "Clear browsing data"; MainActivity clears WebView history once. */
    var historyClearPending: Boolean
        get() = sp.getBoolean(KEY_CLEAR_PENDING, false)
        set(value) = sp.edit { putBoolean(KEY_CLEAR_PENDING, value) }

    /** Most recent first. Defaults to YouTube and Google, as on the home screen. */
    val recent: List<String>
        get() {
            val raw = sp.getString(KEY_RECENT, null) ?: return listOf(UrlUtils.YOUTUBE, UrlUtils.GOOGLE)
            return raw.split('\n').filter { it.isNotBlank() }
        }

    fun addRecent(url: String) {
        if (!UrlUtils.isAllowed(url)) return
        val list = (listOf(url) + recent.filter { UrlUtils.label(it) != UrlUtils.label(url) })
            .take(MAX_RECENT)
        sp.edit { putString(KEY_RECENT, list.joinToString("\n")) }
    }

    fun clearHistory() {
        sp.edit {
            putString(KEY_RECENT, "")
            remove(KEY_LAST_URL)
            putBoolean(KEY_CLEAR_PENDING, true)
        }
    }

    companion object {
        const val KEY_OPEN_YOUTUBE = "open_youtube"
        const val KEY_DARK_MODE = "dark_mode"
        const val KEY_REMEMBER_LAST = "remember_last"
        const val KEY_DESKTOP_UA = "desktop_ua"
        const val KEY_JAVASCRIPT = "javascript"
        const val KEY_COOKIES = "cookies"
        private const val KEY_LAST_URL = "last_url"
        private const val KEY_RECENT = "recent"
        private const val KEY_CLEAR_PENDING = "clear_pending"
        private const val MAX_RECENT = 5
    }
}
