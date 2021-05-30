package com.hmju.youtubeplayer.views

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.constraintlayout.widget.ConstraintLayout
import com.hmju.youtubeplayer.R
import com.hmju.youtubeplayer.YoutubePlayerView

/**
 * Description :
 *
 * Created by hmju on 2021-05-26
 */
internal class YoutubeThumbnailWebView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(ctx, attrs, defStyleAttr) {

    private val webView: WebView by lazy { findViewById(R.id.webView) }
    private val vPanel: View by lazy { findViewById(R.id.vPanel) }

    init {
        if (!isInEditMode) {
            // AddView
            LayoutInflater.from(context).inflate(R.layout.view_youtube_thumbnail, this, false)
                .apply {
                    attachViewToParent(
                        this,
                        0,
                        layoutParams
                    )
                }
        }
    }

    fun setThumbNail(url: String?) {
        if (!url.isNullOrEmpty()) {
            webView.settings.apply {
                @SuppressLint("SetJavaScriptEnabled")
                javaScriptEnabled = true
                mediaPlaybackRequiresUserGesture = false
                cacheMode = WebSettings.LOAD_NO_CACHE
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                if (YoutubePlayerView.DEBUG) {
                    WebView.setWebContentsDebuggingEnabled(true)
                }
            }
            webView.loadUrl(url)
            vPanel.setOnClickListener {
                callOnClick()
            }
        }
    }
}