package uz.auto.browser

import android.net.Uri
import java.net.URLEncoder

object UrlUtils {
    const val YOUTUBE = "https://www.youtube.com"
    const val GOOGLE = "https://www.google.com"

    /** True only for http:// and https:// URLs with a host. */
    fun isAllowed(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val uri = Uri.parse(url)
        val scheme = uri.scheme?.lowercase() ?: return false
        return (scheme == "http" || scheme == "https") && !uri.host.isNullOrBlank()
    }

    /**
     * Turns what the user typed into a URL:
     *  - "https://…" / "http://…" is used as is;
     *  - "youtube.com" becomes "https://youtube.com";
     *  - anything else becomes a Google search.
     * Returns null for other schemes (javascript:, file:, intent:, …).
     */
    fun fromUserInput(input: String): String? {
        val text = input.trim()
        if (text.isEmpty()) return null
        val scheme = Uri.parse(text).scheme?.lowercase()
        if (scheme == "http" || scheme == "https") return if (isAllowed(text)) text else null
        val looksLikeHost = !text.contains(' ') && text.contains('.') &&
            !text.startsWith(".") && !text.endsWith(".")
        if (scheme != null && !looksLikeHost) return null
        if (looksLikeHost) {
            val candidate = "https://$text"
            if (isAllowed(candidate)) return candidate
        }
        return "$GOOGLE/search?q=" + URLEncoder.encode(text, "UTF-8")
    }

    fun isYouTube(url: String?): Boolean {
        val host = url?.let { Uri.parse(it).host?.lowercase() } ?: return false
        return host == "youtube.com" || host.endsWith(".youtube.com") || host == "youtu.be"
    }

    /** Short label for the recent list: host without "www." / "m.". */
    fun label(url: String): String {
        val host = Uri.parse(url).host ?: return url
        return host.removePrefix("www.").removePrefix("m.")
    }
}
