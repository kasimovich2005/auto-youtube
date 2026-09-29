package uz.auto.browser

import android.content.Intent
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewDatabase
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, SettingsFragment())
                .commit()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)

            findPreference<Preference>(Prefs.KEY_DARK_MODE)?.setOnPreferenceChangeListener { _, value ->
                AutoBrowserApp.applyNightMode(value as Boolean)
                true
            }
            findPreference<Preference>("clear_data")?.setOnPreferenceClickListener {
                clearBrowsingData()
                true
            }
            findPreference<Preference>("diagnostics")?.setOnPreferenceClickListener {
                startActivity(Intent(requireContext(), DiagnosticsActivity::class.java))
                true
            }
        }

        private fun clearBrowsingData() {
            val ctx = requireContext()
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
            WebStorage.getInstance().deleteAllData()
            WebViewDatabase.getInstance(ctx).clearHttpAuthUsernamePassword()
            // Cache is shared by all WebViews of the app.
            WebView(ctx).apply {
                clearCache(true)
                destroy()
            }
            Prefs(ctx).clearHistory()
            AbLog.d(AbLog.APP, "Browsing data cleared")
            Toast.makeText(ctx, R.string.data_cleared, Toast.LENGTH_SHORT).show()
        }
    }
}
