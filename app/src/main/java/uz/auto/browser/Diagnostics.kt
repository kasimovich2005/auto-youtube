package uz.auto.browser

import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.car.app.connection.CarConnection
import androidx.webkit.WebViewCompat

/**
 * Collects the values shown on About / Diagnostics. Only public SDK APIs are
 * used; values that Android does not expose publicly are reported as such.
 */
object Diagnostics {
    const val ANDROID_AUTO_PACKAGE = "com.google.android.projection.gearhead"

    fun isInternetConnected(context: Context): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    fun androidAutoVersion(context: Context): String? = try {
        context.packageManager.getPackageInfo(ANDROID_AUTO_PACKAGE, 0).versionName ?: "unknown"
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }

    fun webViewVersion(context: Context): String {
        val pkg = WebViewCompat.getCurrentWebViewPackage(context) ?: return "not available"
        return "${pkg.packageName} ${pkg.versionName}"
    }

    fun connectionName(type: Int?): String = when (type) {
        CarConnection.CONNECTION_TYPE_PROJECTION -> "connected (Android Auto projection)"
        CarConnection.CONNECTION_TYPE_NATIVE -> "native (Android Automotive OS)"
        CarConnection.CONNECTION_TYPE_NOT_CONNECTED -> "not connected"
        else -> "unknown"
    }

    /**
     * One UI's version is not available through a public Android API, so it
     * is only inferred from the Android version on Samsung devices.
     */
    private fun oneUi(): String {
        if (!Build.MANUFACTURER.equals("samsung", ignoreCase = true)) return "not a Samsung device"
        val inferred = when (Build.VERSION.SDK_INT) {
            Build.VERSION_CODES.R -> "3.x"
            Build.VERSION_CODES.S, Build.VERSION_CODES.S_V2 -> "4.x"
            Build.VERSION_CODES.TIRAMISU -> "5.x"
            34 -> "6.x"
            else -> "unknown"
        }
        return "$inferred (inferred from Android version)"
    }

    fun report(context: Context, carConnectionType: Int?): List<Pair<String, String>> {
        val pm = context.packageManager
        val aaVersion = androidAutoVersion(context)
        return listOf(
            "App version" to BuildConfig.VERSION_NAME,
            "Build type" to if (BuildConfig.DEBUG) "debug" else "release",
            "Device" to "${Build.MANUFACTURER} ${Build.MODEL}",
            "Android" to "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            "One UI" to oneUi(),
            "Android Auto" to (aaVersion?.let { "installed, $it" } ?: "not installed"),
            "Android Auto connection" to connectionName(carConnectionType),
            "WebView version" to webViewVersion(context),
            "Internet" to if (isInternetConnected(context)) "connected" else "disconnected",
            "Automotive environment" to
                if (pm.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)) "detected (Android Automotive OS)"
                else "not detected (phone)",
            "Parked state" to parkedState(carConnectionType),
            "Parked apps on Android Auto" to
                if (Build.VERSION.SDK_INT >= 35) "phone meets Android 15+ requirement (games only)"
                else "not supported: needs Android 15+, this phone has API ${Build.VERSION.SDK_INT}",
            "Browser on car screen" to
                "not supported on Android Auto (Browsers category is Android Automotive OS only)",
            "Video on car screen" to
                "not supported on Android Auto for this app (Video category is Android Automotive OS only)",
        )
    }

    private fun parkedState(type: Int?): String = when (type) {
        CarConnection.CONNECTION_TYPE_PROJECTION ->
            "unknown: not exposed to phone apps over Android Auto; video is blocked while connected"
        CarConnection.CONNECTION_TYPE_NATIVE -> "managed by Android Automotive OS"
        else -> "not in a car"
    }

    fun asText(items: List<Pair<String, String>>): String =
        items.joinToString("\n") { (k, v) -> "$k: $v" }
}
