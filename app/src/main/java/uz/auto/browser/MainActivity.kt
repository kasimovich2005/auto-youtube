package uz.auto.browser

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.car.app.connection.CarConnection
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.google.android.material.button.MaterialButton
import uz.auto.browser.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: Prefs
    private lateinit var webView: WebView

    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    private var orientationBeforeFullscreen = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

    /** Android Auto is projecting from this phone. */
    private var carProjectionActive = false
    private var appliedDesktopUa: Boolean? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = Prefs(this)

        createWebView()
        setupToolbar()
        setupHome()
        setupBackHandling()
        observeCarConnection()

        val restored = savedInstanceState?.let { webView.restoreState(it) } != null &&
            webView.url != null
        if (restored) {
            showBrowser()
        } else {
            val start = intent?.dataString?.takeIf { UrlUtils.isAllowed(it) }
                ?: prefs.lastUrl?.takeIf { prefs.rememberLastPage && UrlUtils.isAllowed(it) }
                ?: UrlUtils.YOUTUBE.takeIf { prefs.openYouTubeOnStartup }
            if (start != null) load(start) else showHome()
        }
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
        applySettings()
        if (prefs.historyClearPending) {
            webView.clearHistory()
            prefs.historyClearPending = false
            showHome()
        }
        renderRecent()
    }

    override fun onPause() {
        webView.onPause()
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onDestroy() {
        (webView.parent as? ViewGroup)?.removeView(webView)
        webView.destroy()
        super.onDestroy()
    }

    // ---------------------------------------------------------------- WebView

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView() {
        webView = WebView(this)
        binding.webContainer.removeAllViews()
        binding.webContainer.addView(
            webView,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
        with(webView.settings) {
            domStorageEnabled = true
            @Suppress("DEPRECATION")
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = false
            // Popups: no new windows, no window.open() without a click.
            setSupportMultipleWindows(false)
            javaScriptCanOpenWindowsAutomatically = false
            // No local file or content provider access from web pages.
            allowFileAccess = false
            allowContentAccess = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        }
        // No addJavascriptInterface(): the app has no JavaScript bridge.
        webView.webViewClient = BrowserClient()
        webView.webChromeClient = ChromeClient()
        webView.setDownloadListener { _, _, _, _, _ ->
            AbLog.d(AbLog.WEBVIEW, "Download blocked")
            toast(R.string.downloads_unsupported)
        }
        appliedDesktopUa = null
        applySettings()
        AbLog.d(AbLog.WEBVIEW, "WebView initialized: ${Diagnostics.webViewVersion(this)}")
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun applySettings() {
        val s = webView.settings
        s.javaScriptEnabled = prefs.javaScript
        CookieManager.getInstance().apply {
            setAcceptCookie(prefs.cookies)
            setAcceptThirdPartyCookies(webView, prefs.cookies)
        }
        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(s, prefs.darkMode)
        }
        val desktop = prefs.desktopUserAgent
        if (appliedDesktopUa != desktop) {
            val changed = appliedDesktopUa != null
            s.userAgentString = userAgent(desktop)
            appliedDesktopUa = desktop
            AbLog.d(AbLog.WEBVIEW, "User agent: ${s.userAgentString}")
            if (changed && webView.url != null) webView.reload()
        }
    }

    /**
     * The default WebView user agent marks itself with "; wv" and
     * "Version/4.0", which some sites treat as an embedded view. Removing
     * these tokens makes it read as regular Chrome for Android; the desktop
     * variant uses the same Chrome version on Linux.
     */
    private fun userAgent(desktop: Boolean): String {
        val base = WebSettings.getDefaultUserAgent(this)
        val chrome = Regex("Chrome/[\\d.]+").find(base)?.value ?: "Chrome/120.0.0.0"
        return if (desktop) {
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) $chrome Safari/537.36"
        } else {
            base.replace("; wv", "").replace(Regex("Version/[\\d.]+ "), "")
        }
    }

    private fun load(url: String) {
        if (!UrlUtils.isAllowed(url)) {
            toast(R.string.invalid_url)
            return
        }
        AbLog.d(if (UrlUtils.isYouTube(url)) AbLog.YOUTUBE else AbLog.APP, "Loading $url")
        showBrowser()
        webView.loadUrl(url)
    }

    private inner class BrowserClient : WebViewClient() {

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val uri = request.url
            val scheme = uri.scheme?.lowercase()
            if (scheme == "http" || scheme == "https") return false
            // intent:// links: never start another app; follow a web fallback if there is one.
            if (scheme == "intent") {
                val fallback = try {
                    Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME)
                        .getStringExtra("browser_fallback_url")
                } catch (e: Exception) {
                    null
                }
                if (fallback != null && UrlUtils.isAllowed(fallback)) {
                    AbLog.d(AbLog.NAV, "intent:// redirected to web fallback")
                    view.loadUrl(fallback)
                    return true
                }
            }
            AbLog.d(AbLog.NAV, "Blocked external scheme: $scheme")
            if (request.hasGesture()) toast(R.string.external_blocked)
            return true
        }

        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
            binding.errorView.isVisible = false
            binding.progress.isVisible = true
            updateToolbar(url)
        }

        override fun onPageFinished(view: WebView, url: String) {
            binding.progress.isVisible = false
            updateToolbar(url)
            if (UrlUtils.isAllowed(url)) {
                prefs.addRecent(url)
                if (prefs.rememberLastPage) prefs.lastUrl = url
            }
            applyDrivingGuard()
            AbLog.d(if (UrlUtils.isYouTube(url)) AbLog.YOUTUBE else AbLog.APP, "Page loaded")
        }

        override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) {
            // YouTube is a single-page app: navigation inside it changes the
            // URL without a new page load.
            updateToolbar(url)
            applyDrivingGuard()
        }

        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
            if (!request.isForMainFrame) return
            val url = request.url.toString()
            val tag = if (UrlUtils.isYouTube(url)) AbLog.YOUTUBE else AbLog.WEBVIEW
            AbLog.w(tag, "Load error ${error.errorCode}: ${error.description}")
            val message = when {
                !Diagnostics.isInternetConnected(this@MainActivity) -> R.string.error_no_internet
                UrlUtils.isYouTube(url) -> R.string.error_youtube
                else -> R.string.error_generic
            }
            showError(message)
        }

        override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
            AbLog.w(AbLog.WEBVIEW, "Renderer gone, crashed=${detail.didCrash()}; recreating WebView")
            val url = view.url
            if (view == webView) {
                (webView.parent as? ViewGroup)?.removeView(webView)
                webView.destroy()
                createWebView()
                if (url != null && UrlUtils.isAllowed(url)) load(url) else showHome()
            }
            return true
        }
    }

    private inner class ChromeClient : WebChromeClient() {

        override fun onProgressChanged(view: WebView, newProgress: Int) {
            binding.progress.setProgressCompat(newProgress, true)
            binding.progress.isVisible = newProgress < 100
        }

        override fun onShowCustomView(view: View, callback: CustomViewCallback) {
            if (carProjectionActive) {
                AbLog.d(AbLog.AUTO, "Fullscreen video refused: Android Auto connected")
                callback.onCustomViewHidden()
                return
            }
            if (customView != null) {
                callback.onCustomViewHidden()
                return
            }
            AbLog.d(AbLog.YOUTUBE, "Video playback requested: fullscreen")
            customView = view
            customViewCallback = callback
            binding.fullscreenContainer.addView(view)
            binding.fullscreenContainer.isVisible = true
            orientationBeforeFullscreen = requestedOrientation
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            setSystemBarsHidden(true)
        }

        override fun onHideCustomView() = exitFullscreen()

        override fun onPermissionRequest(request: PermissionRequest) {
            // Camera / microphone / protected media are never granted.
            AbLog.d(AbLog.WEBVIEW, "Denied web permission request: ${request.resources.joinToString()}")
            request.deny()
        }

        override fun onGeolocationPermissionsShowPrompt(
            origin: String,
            callback: GeolocationPermissions.Callback
        ) {
            callback.invoke(origin, false, false)
        }
    }

    private fun exitFullscreen() {
        val view = customView ?: return
        binding.fullscreenContainer.removeView(view)
        binding.fullscreenContainer.isVisible = false
        customView = null
        customViewCallback?.onCustomViewHidden()
        customViewCallback = null
        requestedOrientation = orientationBeforeFullscreen
        setSystemBarsHidden(false)
    }

    private fun setSystemBarsHidden(hidden: Boolean) {
        WindowCompat.setDecorFitsSystemWindows(window, !hidden)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        if (hidden) {
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    // ------------------------------------------------------ Android Auto state

    private fun observeCarConnection() {
        CarConnection(this).type.observe(this) { type ->
            val projecting = type == CarConnection.CONNECTION_TYPE_PROJECTION
            AbLog.d(AbLog.AUTO, "Car connection: ${Diagnostics.connectionName(type)}")
            if (projecting && !carProjectionActive) AbLog.d(AbLog.APP, "Android Auto connection detected")
            carProjectionActive = projecting
            binding.drivingBanner.isVisible = projecting
            if (projecting) exitFullscreen()
            applyDrivingGuard()
        }
    }

    /**
     * Android Auto does not tell phone apps whether the car is parked, so
     * while Android Auto is connected the app treats the car as possibly
     * moving: it pauses any <video>/<audio> on the page and refuses
     * fullscreen. This uses evaluateJavascript only; no JS bridge exists.
     */
    private fun applyDrivingGuard() {
        val block = carProjectionActive
        if (!block && webView.url == null) return
        val js = """
            (function(){
              window.__abBlock = $block;
              if (!window.__abGuard) {
                window.__abGuard = true;
                document.addEventListener('play', function(e){
                  if (window.__abBlock) { try { e.target.pause(); } catch (x) {} }
                }, true);
              }
              if (window.__abBlock) {
                document.querySelectorAll('video,audio').forEach(function(v){ try { v.pause(); } catch (x) {} });
              }
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    // ---------------------------------------------------------------- toolbar

    private fun setupToolbar() = with(binding) {
        btnBack.setOnClickListener { goBack() }
        btnForward.setOnClickListener {
            if (webView.canGoForward()) {
                AbLog.d(AbLog.NAV, "Forward")
                showBrowser()
                webView.goForward()
            }
        }
        btnRefresh.setOnClickListener {
            AbLog.d(AbLog.NAV, "Refresh")
            if (webView.url == null) showHome() else {
                showBrowser()
                webView.reload()
            }
        }
        btnHome.setOnClickListener {
            AbLog.d(AbLog.NAV, "Home")
            showHome()
        }
        btnMenu.setOnClickListener { showMenu(it) }
        urlBar.setOnEditorActionListener { _, actionId, event ->
            val enter = event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN
            if (actionId == EditorInfo.IME_ACTION_GO || enter) {
                submitUrl()
                true
            } else {
                false
            }
        }
        errorRetry.setOnClickListener {
            errorView.isVisible = false
            webView.reload()
        }
        updateToolbar(null)
    }

    private fun submitUrl() {
        val url = UrlUtils.fromUserInput(binding.urlBar.text.toString())
        if (url == null) {
            toast(R.string.invalid_url)
            return
        }
        hideKeyboard()
        binding.urlBar.clearFocus()
        load(url)
    }

    private fun updateToolbar(url: String?) = with(binding) {
        if (!urlBar.hasFocus()) urlBar.setText(url ?: webView.url ?: "")
        btnBack.isEnabled = webView.canGoBack() || !homeView.isVisible
        btnForward.isEnabled = webView.canGoForward()
        btnBack.alpha = if (btnBack.isEnabled) 1f else 0.35f
        btnForward.alpha = if (btnForward.isEnabled) 1f else 0.35f
    }

    private fun showMenu(anchor: View) {
        PopupMenu(this, anchor).apply {
            menu.add(0, 1, 0, R.string.home)
            menu.add(0, 2, 1, R.string.settings)
            menu.add(0, 3, 2, R.string.about_diagnostics)
            setOnMenuItemClickListener {
                when (it.itemId) {
                    1 -> showHome()
                    2 -> startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                    3 -> startActivity(Intent(this@MainActivity, DiagnosticsActivity::class.java))
                }
                true
            }
        }.show()
    }

    // ------------------------------------------------------------ home screen

    private fun setupHome() = with(binding) {
        homeYoutube.setOnClickListener { load(UrlUtils.YOUTUBE) }
        homeGoogle.setOnClickListener { load(UrlUtils.GOOGLE) }
        homeEnter.setOnClickListener {
            urlBar.setText("")
            urlBar.requestFocus()
            getSystemService(InputMethodManager::class.java)
                ?.showSoftInput(urlBar, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun renderRecent() = with(binding) {
        recentList.removeAllViews()
        val recent = prefs.recent
        recentTitle.isVisible = recent.isNotEmpty()
        for (url in recent) {
            val item = MaterialButton(
                this@MainActivity, null, androidx.appcompat.R.attr.borderlessButtonStyle
            ).apply {
                text = "•  ${UrlUtils.label(url)}"
                textSize = 20f
                isAllCaps = false
                textAlignment = View.TEXT_ALIGNMENT_VIEW_START
                minHeight = (56 * resources.displayMetrics.density).toInt()
                setOnClickListener { load(url) }
            }
            recentList.addView(
                item,
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            )
        }
    }

    private fun showHome() {
        exitFullscreen()
        binding.homeView.isVisible = true
        binding.errorView.isVisible = false
        binding.progress.isVisible = false
        binding.urlBar.setText("")
        renderRecent()
        updateToolbar("")
    }

    private fun showBrowser() {
        binding.homeView.isVisible = false
    }

    private fun showError(message: Int) {
        binding.errorText.setText(message)
        binding.errorView.isVisible = true
        binding.homeView.isVisible = false
    }

    // ------------------------------------------------------------- navigation

    private fun goBack(): Boolean {
        when {
            customView != null -> exitFullscreen()
            binding.homeView.isVisible -> return false
            webView.canGoBack() -> {
                AbLog.d(AbLog.NAV, "Back (web history)")
                binding.errorView.isVisible = false
                webView.goBack()
            }
            else -> {
                AbLog.d(AbLog.NAV, "Back (no web history) -> Home")
                showHome()
            }
        }
        return true
    }

    private fun setupBackHandling() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!goBack()) {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.dataString?.let { if (UrlUtils.isAllowed(it)) load(it) }
    }

    // ---------------------------------------------------------------- helpers

    private fun hideKeyboard() {
        getSystemService(InputMethodManager::class.java)
            ?.hideSoftInputFromWindow(binding.urlBar.windowToken, 0)
    }

    private fun toast(res: Int) = Toast.makeText(this, res, Toast.LENGTH_SHORT).show()
}
