package io.github.bijojp.weekendvibes.imports

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import android.webkit.WebViewClient
import io.github.bijojp.weekendvibes.domain.eventpage.PageCheck
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONTokener
import kotlin.coroutines.resume

/**
 * The fallback for pages a plain fetch can't read (ADR 0002): loads the page in a hidden WebView,
 * Android's own Chrome engine, and takes its HTML once event data shows up or time runs out.
 * BookMyShow turns away anything that isn't a real browser, but lets this through.
 */
internal object PageRenderer {
    private const val TIMEOUT_MILLIS = 20_000L
    private const val POLL_MILLIS = 1_000L

    /** The rendered page, or whatever it had when time ran out; null if nothing loaded at all. */
    suspend fun render(context: Context, url: String): Fetched? = withContext(Dispatchers.Main) {
        var latest: Fetched? = null
        val done = withTimeoutOrNull(TIMEOUT_MILLIS) {
            suspendCancellableCoroutine { continuation ->
                val handler = Handler(Looper.getMainLooper())
                val web = newWebView(context)
                fun finish(result: Fetched?) {
                    handler.removeCallbacksAndMessages(null)
                    if (continuation.isActive) continuation.resume(result)
                    handler.post { web.destroy() }
                }
                val poll = object : Runnable {
                    override fun run() {
                        web.evaluateJavascript("document.documentElement.outerHTML") { result ->
                            val html = runCatching { JSONTokener(result).nextValue() as? String }.getOrNull()
                            if (html != null) {
                                latest = Fetched(web.url ?: url, 200, html)
                                if (PageCheck.hasEventData(html)) return@evaluateJavascript finish(latest)
                            }
                            if (continuation.isActive) handler.postDelayed(this, POLL_MILLIS)
                        }
                    }
                }
                continuation.invokeOnCancellation {
                    handler.removeCallbacksAndMessages(null)
                    handler.post { web.destroy() }
                }
                web.loadUrl(url)
                handler.postDelayed(poll, POLL_MILLIS)
            }
        }
        done ?: latest
    }

    // JavaScript is needed for the browser checks and for pages that build themselves in script.
    // Nothing from the app is exposed to the page: no JavaScript interface, no file or content access.
    @SuppressLint("SetJavaScriptEnabled")
    private fun newWebView(context: Context) = WebView(context.applicationContext).apply {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.blockNetworkImage = true
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.mediaPlaybackRequiresUserGesture = true
        webViewClient = WebViewClient()
    }
}
