package uz.auto.browser.car

import android.content.pm.ApplicationInfo
import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator
import uz.auto.browser.AbLog

/**
 * Entry point Android Auto binds to. Declared in the manifest with the
 * androidx.car.app.CarAppService action.
 */
class AutoBrowserCarAppService : CarAppService() {

    override fun createHostValidator(): HostValidator {
        val debuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        return if (debuggable) {
            // Lets the Desktop Head Unit and any host connect in debug builds.
            HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
        } else {
            HostValidator.Builder(applicationContext)
                .addAllowedHosts(androidx.car.app.R.array.hosts_allowlist_sample)
                .build()
        }
    }

    override fun onCreateSession(): Session {
        AbLog.d(AbLog.AUTO, "Android Auto connection detected: car session created")
        return AutoBrowserSession()
    }
}
