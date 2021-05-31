package com.hmju.youtubeplayer.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.annotation.IntRange
import com.hmju.youtubeplayer.R
import com.hmju.youtubeplayer.YoutubePlayerView
import com.hmju.youtubeplayer.define.Constants
import com.hmju.youtubeplayer.define.PlayQuality
import com.hmju.youtubeplayer.model.Options
import org.json.JSONArray

/**
 * Description : 실제 유튜브 동영상 플레이 하는 WebView
 *
 * Created by hmju on 2021-04-27
 */
class YoutubeWebView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : WebView(ctx, attrs, defStyleAttr) {

    private val uiThreadHandler = Handler(Looper.getMainLooper())
    private var options: Options? = null

    init {
        if (!isInEditMode) {
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            settings.apply {
                @SuppressLint("SetJavaScriptEnabled")
                javaScriptEnabled = true
                mediaPlaybackRequiresUserGesture = false
                cacheMode = WebSettings.LOAD_NO_CACHE
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                if (YoutubePlayerView.DEBUG) {
                    setWebContentsDebuggingEnabled(true)
                }
            }
            setBackgroundColor(Color.BLACK)
        }
    }

    internal fun initialize(options: Options) {
        this.options = options
        val htmlPage = resources.openRawResource(R.raw.youtube)
            .bufferedReader()
            .use { it.readText() }
            .replace("<<playerOptions>>", options.toString())

        loadDataWithBaseURL(Constants.BASE_URL, htmlPage, "text/html", "UTF-8", null)
    }

    internal fun loadVideo(videoId: String, startSec: Float = 0F) {
        uiThreadHandler.post { loadUrl("javascript:loadVideo('$videoId', $startSec)") }
    }

    internal fun cueVideo(videoId: String, startSec: Float = 0F) {
        uiThreadHandler.post { loadUrl("javascript:cueVideo('$videoId', $startSec)") }
    }

    /**
     * 재생중인 시간 조회
     */
    internal fun fetchCurrentTime(callBack: (Float) -> Unit) {
        evaluateJavascript("javascript:currentTime()") {
            if (!it.isNullOrEmpty() && it != "null") {
                callBack.invoke(it.toFloat())
            }
        }
    }

    /**
     * 동영상 재생 품질 조회
     */
    internal fun fetchPlayQuality(callBack: (PlayQuality) -> Unit) {
        evaluateJavascript("javascript:getPlayQuality()") {
            try {
                callBack.invoke(PlayQuality.valueOf(it.replace("\'", "")))
            } catch (ex: Exception) {
            }
        }
    }

    /**
     * 현재 사용가능한 재생 품질 조회
     */
    internal fun fetchAvailablePlayQualities(callBack: (Array<PlayQuality>) -> Unit) {
        evaluateJavascript("javascript:getAvailablePlayQualities()") { result ->
            try {
                val jsonArray = JSONArray(result)
                val qualityArr = Array(jsonArray.length()) { PlayQuality.invalid }

                for (index in 0 until jsonArray.length()) {
                    qualityArr[index] = PlayQuality.valueOf(jsonArray[index].toString())
                }

                callBack.invoke(qualityArr.filter { it != PlayQuality.invalid }.toTypedArray())
            } catch (ex: Exception) {
            }
        }
    }

    /**
     * 동영상 재생
     */
    fun playVideo() {
        uiThreadHandler.post { loadUrl("javascript:playVideo()") }
    }

    /**
     * 동영상 일시 중지
     */
    fun pauseVideo() {
        uiThreadHandler.post { loadUrl("javascript:pauseVideo()") }
    }

    /**
     * 동영상 해당 초로 이동
     */
    fun seekTo(time: Float?) {
        if (time != null) {
            uiThreadHandler.post { loadUrl("javascript:seekTo('$time')") }
        }
    }

    /**
     * 음소거
     */
    fun mute() {
        uiThreadHandler.post { loadUrl("javascript:mute()") }
    }

    /**
     * 음소거 해제
     */
    fun unMute() {
        uiThreadHandler.post { loadUrl("javascript:unMute()") }
    }

    /**
     * 볼륨 조절
     */
    fun setVolume(@IntRange(from = 0, to = 100) volume: Int) {
        uiThreadHandler.post { loadUrl("javascript:setVolume('$volume')") }
    }
}