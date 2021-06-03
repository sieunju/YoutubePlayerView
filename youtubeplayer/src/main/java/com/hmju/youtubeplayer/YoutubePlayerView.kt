package com.hmju.youtubeplayer

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.TypedArray
import android.net.Uri
import android.os.*
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.webkit.JavascriptInterface
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.SeekBar
import androidx.annotation.IdRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.*
import com.hmju.youtubeplayer.define.PlayQuality
import com.hmju.youtubeplayer.define.State
import com.hmju.youtubeplayer.listener.YoutubeListener
import com.hmju.youtubeplayer.model.Options
import com.hmju.youtubeplayer.utility.ConnectionLiveData
import com.hmju.youtubeplayer.views.YoutubeThumbnailWebView
import com.hmju.youtubeplayer.views.YoutubeWebView
import com.hmju.youtubeplayerview.extension.convertToTime
import com.hmju.youtubeplayerview.extension.multiNullCheck
import java.lang.ref.WeakReference
import java.util.*

/**
 * Description : Youtube Player View
 *
 * Created by hmju on 2021-04-27
 */
class YoutubePlayerView @JvmOverloads constructor(
        ctx: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayout(ctx, attrs, defStyleAttr), LifecycleOwner, LifecycleObserver,
        View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    companion object {
        const val TAG = "YoutubeLib"
        const val DEBUG = true
        const val WHAT_TIMER = 1                // 현재 진행중인 시간
        const val WHAT_CONTROL_HIDE = 2         // 컨트롤러 뷰 숨기기
        const val WHAT_CONTROL_SHOW = 3         // 컨트롤러 뷰 보이게

        fun LogD(msg: String) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, msg)
            }
        }

        open class SimpleYoutubeListener : YoutubeListener {
            override fun onState(state: State) {}
            override fun onQualityChanged(quality: PlayQuality) {}
            override fun onDuration(duration: Float) {}
            override fun onError(err: String) {}
            override fun onEnterFullScreen() {} // 전체화면시 YoutubePlayerView 를 제외한 나머지 GONE 처리
            override fun onExitFullScreen() {} // 전체 화면 해제시 YoutubePlayerView 를 제외한 나머지 VISIBLE 처리
        }
    }

    private val activity: FragmentActivity by lazy {
        if (ctx is FragmentActivity) {
            ctx
        } else {
            throw IllegalArgumentException("해당 뷰 사용시 현재 액티비티가 FragmentActivity 로 상속되어야 합니다.")
        }
    }
    var listener: SimpleYoutubeListener? = null // Youtube Listener
    var youtubeId: String? = null
    val options: Options by lazy { Options() } // Youtube Options

    private val lifecycleRegistry: LifecycleRegistry by lazy { LifecycleRegistry(this) }
    private val connectionLiveData: ConnectionLiveData by lazy { ConnectionLiveData(ctx) }
    private val youtubeState: MutableLiveData<State> by lazy { MutableLiveData<State>() } // Youtube 상태값
    private val controllerLiveData: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    private val fullScreenLiveData: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() } // 전체 화면
    private val _youtubeThumbNail: MutableLiveData<String> by lazy { MutableLiveData<String>() }
    val youtubeThumbNail: LiveData<String> get() = _youtubeThumbNail
    private val messageHandler: MessageHandler by lazy { MessageHandler(activity) }

    private var isLogoVisible: Boolean = false
    private var isFullScreenVisible: Boolean = false
    private var isShareVisible: Boolean = false

    @IdRes
    private var thumbnailId: Int = -1

    // [s] View Variable
    private lateinit var container: ConstraintLayout
    private val imgPlayAndPause: AppCompatImageView by lazy { findViewById(R.id.imgPlayAndPause) }
    private val youtubeFrame: FrameLayout by lazy { findViewById(R.id.youtubeFrame) }
    private val progressBar: ProgressBar by lazy { findViewById(R.id.progressBar) }
    private val youtubeController: ConstraintLayout by lazy { findViewById(R.id.youtubeController) }
    private val seekBar: AppCompatSeekBar by lazy { findViewById(R.id.seekBar) }
    private var vThumbnail: View? = null
    private val tvProgress: AppCompatTextView by lazy { findViewById(R.id.tvProgressTime) } // 진행중인 시간
    private val tvRemain: AppCompatTextView by lazy { findViewById(R.id.tvRemainTime) } // 남은 시간
    private val imgLogo: AppCompatImageView by lazy { findViewById(R.id.imgLogo) }
    private val imgFullScreen: AppCompatImageView by lazy { findViewById(R.id.imgFullScreen) }
    private val imgShare: AppCompatImageView by lazy { findViewById(R.id.imgShare) } // 공유하기
    private var youtubeWebView: YoutubeWebView? = null
    // [e] View Variable

    init {
        if (!isInEditMode) {
            // 속성값 세팅
            attrs?.run {
                val attr: TypedArray =
                        ctx.obtainStyledAttributes(this, R.styleable.YoutubePlayerView)

                try {
                    options.apply {
                        isAutoPlay = attr.getBoolean(
                                R.styleable.YoutubePlayerView_youtube_is_auto_play,
                                false
                        )
                        isControl = attr.getBoolean(
                                R.styleable.YoutubePlayerView_youtube_is_web_control,
                                false
                        )
                        isLoop =
                                attr.getBoolean(R.styleable.YoutubePlayerView_youtube_is_loop, false)
                        isEffect =
                                attr.getBoolean(R.styleable.YoutubePlayerView_youtube_is_effect, true)
                        isRel = attr.getBoolean(
                                R.styleable.YoutubePlayerView_youtube_is_relation,
                                false
                        )
                        loadPolicy =
                                attr.getInt(R.styleable.YoutubePlayerView_youtube_load_policy, 0)
                        val langPref =
                                attr.getString(R.styleable.YoutubePlayerView_youtube_lang_pref)
                        if (!langPref.isNullOrEmpty()) {
                            lanPref = langPref
                        }

                        val endTime = attr.getFloat(R.styleable.YoutubePlayerView_youtube_end, -1F)
                        if (endTime != -1F) {
                            end = endTime
                        }
                    }

                    isLogoVisible =
                            attr.getBoolean(R.styleable.YoutubePlayerView_youtube_is_logo, true)
                    isFullScreenVisible =
                            attr.getBoolean(R.styleable.YoutubePlayerView_youtube_is_fullscreen, true)
                    isShareVisible =
                            attr.getBoolean(R.styleable.YoutubePlayerView_youtube_is_share, true)

                    thumbnailId = attr.getResourceId(
                            R.styleable.YoutubePlayerView_youtube_thumbnail_id,
                            NO_ID
                    )
                } finally {
                    attr.recycle()
                }
            }

            // AddView
            LayoutInflater.from(context).inflate(R.layout.view_youtube_player, this, false).apply {
                attachViewToParent(this, 0, layoutParams)
                container = this as ConstraintLayout
            }

            activity.lifecycle.addObserver(this)
        }
    }

    override fun getLifecycle() = lifecycleRegistry

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Youtube 정책에 맞게 16:9 비율로 View 표현
        if (activity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        } else {
            val ratioSize = MeasureSpec.getSize(widthMeasureSpec).toFloat() * (9F / 16F)
            val height = MeasureSpec.makeMeasureSpec(ratioSize.toInt(), MeasureSpec.EXACTLY)
            super.onMeasure(widthMeasureSpec, height)
        }
    }

    override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
        container.addView(child, index, params)
        if (thumbnailId != NO_ID &&
                vThumbnail == null &&
                child != null &&
                child.id == thumbnailId) {
            child.setOnClickListener { startVideo() }
            vThumbnail = child
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.imgPlayAndPause -> {
                if (youtubeState.value == State.PLAYING) {
                    pauseVideo()
                } else {
                    playVideo()
                }
            }
            R.id.background -> {
                if (youtubeState.value == State.PLAYING || youtubeState.value == State.PAUSE) {
                    if (controllerLiveData.value == true) {
                        controllerHide()
                    } else {
                        controllerShow()
                    }
                }
            }
            R.id.imgLogo -> moveYoutubePage()
            R.id.imgFullScreen -> fullScreenLiveData.postValue(fullScreenLiveData.value != true)
            R.id.imgShare -> moveShared()
        }
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        tvProgress.text = progress.toFloat().convertToTime()
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {
        messageHandler.removeMessages(WHAT_CONTROL_HIDE)
        messageHandler.removeMessages(WHAT_TIMER)
    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {
        youtubeWebView?.seekTo(seekBar?.progress?.toFloat())
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopVideo()
    }

    /**
     * init View And Set Listener
     */
    private fun initView() {
        imgLogo.visibility = if (isLogoVisible) VISIBLE else GONE
        imgFullScreen.visibility = if (isFullScreenVisible) VISIBLE else GONE
        imgShare.visibility = if (isShareVisible) VISIBLE else GONE

        // 썸네일 레이아웃 없는 경우
        if (thumbnailId == NO_ID) {
            vThumbnail = YoutubeThumbnailWebView(context)
            addView(vThumbnail, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
            vThumbnail?.setOnClickListener { startVideo() }
        }

        imgLogo.setOnClickListener(this)
        imgFullScreen.setOnClickListener(this)
        imgPlayAndPause.setOnClickListener(this)
        imgShare.setOnClickListener(this)
        seekBar.setOnSeekBarChangeListener(this)
        if (options.isControl) {
            findViewById<View>(R.id.background).visibility = GONE
        } else {
            findViewById<View>(R.id.background).setOnClickListener(this)
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_ANY)
    private fun onStateEvent(owner: LifecycleOwner, event: Lifecycle.Event) {
        lifecycleRegistry.handleLifecycleEvent(event)
        when (event) {
            Lifecycle.Event.ON_CREATE -> {
                registerLiveData()
                initView()
            }
            Lifecycle.Event.ON_PAUSE -> {
                pauseVideo()
            }
            Lifecycle.Event.ON_STOP -> {
                stopVideo()
                stopHandler()
            }
            Lifecycle.Event.ON_DESTROY -> {
                // 핸들러 해제.
                onDestroy()
            }
            else -> {
            }
        }
    }

    /**
     * Live Data 등록
     */
    private fun registerLiveData() {
        // 네트워크 통신
        connectionLiveData.observe(this, {
            if (!it) {
                // 유튜브 중지 또는 일시 중지
                pauseVideo()
            }
        })

        // 재생 버튼
        youtubeState.observe(this, {
            LogD("youtubeState $it")

            // 재생중일떄는 -> 일시 정지 화면 노출
            when (it) {
                State.CUE -> {
                    // 영상 대기
                    progressBar.visibility = VISIBLE
                    vThumbnail?.visibility = GONE
                }
                State.END -> {
                    vThumbnail?.visibility = VISIBLE
                }
                State.PLAYING -> {
                    // 영상 재생
                    progressBar.visibility = GONE
                    imgPlayAndPause.visibility = VISIBLE
                    imgPlayAndPause.setImageResource(R.drawable.ic_pause)
                    controllerHide(2500)
                }
                State.PAUSE -> {
                    // 영상 일시 중지
                    controllerShow()
                    imgPlayAndPause.setImageResource(R.drawable.ic_play)
                }
                State.BUFFERING -> {
                    controllerShow()
                }
                State.UNKNOWN -> {
                    progressBar.visibility = GONE
                    vThumbnail?.visibility = VISIBLE
                }
                else -> {
                }
            }
        })

        // 컨트롤러 레이아웃 표시 / 숨김 처리
        controllerLiveData.observe(this, { visible ->
            youtubeController.visibility = if (visible) VISIBLE else GONE
        })

        // 전체 화면 처리 true -> 전체 화면, false -> 전체 화면 해제
        // 전체화면 기능 사용하려면 Activity configChanges -> orientation|screenSize 설정 해야함.
        fullScreenLiveData.observe(this, { isFullScreen ->
            if (isFullScreen) {
                enterFullScreen()
                imgFullScreen.setImageResource(R.drawable.ic_fullscreen_exit)
            } else {
                exitFullScreen()
                imgFullScreen.setImageResource(R.drawable.ic_fullscreen_enter)
            }
        })

        youtubeThumbNail.observe(this, { url ->
            if (thumbnailId == -1) {
                runCatching {
                    (vThumbnail as YoutubeThumbnailWebView).setThumbNail(url)
                }.onFailure {
                    LogD("Error $it")
                }
            }
        })
    }

    /**
     * 동영상 재생
     */
    fun startVideo() {
        // Network Check
        if (connectionLiveData.value == false) return

        youtubeFrame.removeAllViews()
        imgPlayAndPause.visibility = GONE

        if (youtubeWebView != null) {
            youtubeWebView?.destroy()
            youtubeWebView = null
        }

        youtubeWebView = YoutubeWebView(context).run {
            initialize(options)
            removeJavascriptInterface("youtubePlayerBridge")
            addJavascriptInterface(JavaScriptInterface(), "youtubePlayerBridge")
            return@run this
        }

        youtubeFrame.addView(
                youtubeWebView,
                LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
        youtubeState.postValue(State.CUE)
    }

    /**
     * 유튜브 재생 상태 가공 함수
     */
    private fun parseState(state: Int) =
            when (state) {
                State.CUE.code -> State.CUE
                State.END.code -> State.END
                State.PLAYING.code -> State.PLAYING
                State.PAUSE.code -> State.PAUSE
                State.BUFFERING.code -> State.BUFFERING
                else -> State.UNKNOWN
            }

    /**
     * 종료 시간 처리
     * @param duration 동영상 재생 시간
     */
    private fun setVideoTime(currTime: Float, duration: Float) {
        activity.runOnUiThread {
            seekBar.max = duration.toInt()
            tvProgress.text = currTime.convertToTime()
            tvRemain.text = duration.convertToTime()
        }
    }

    /**
     * 핸들러 메모리 해제 및 WebView 메모리 해제 처리
     */
    private fun onDestroy() {
        if (youtubeWebView != null) {
            youtubeWebView?.destroy()
            youtubeWebView = null
        }
        stopHandler()
    }

    /**
     * 헨들러 멈춤 처리 함수.
     */
    private fun stopHandler() {
        messageHandler.removeMessages(WHAT_TIMER)
        messageHandler.removeMessages(WHAT_CONTROL_HIDE)
        messageHandler.removeMessages(WHAT_CONTROL_SHOW)
        messageHandler.removeCallbacksAndMessages(null)
        messageHandler.act.clear()
    }

    /**
     * 컨트롤러 화면 보이게 처리
     */
    private fun controllerShow() {
        messageHandler.removeMessages(WHAT_CONTROL_SHOW)
        messageHandler.sendEmptyMessage(WHAT_CONTROL_SHOW)
    }

    /**
     * 컨트롤러 화면 안보이게 처리
     * @param delay 몇초뒤 보이게 하는 딜레이 값
     */
    private fun controllerHide(delay: Long = 0) {
        messageHandler.removeMessages(WHAT_CONTROL_HIDE)
        messageHandler.sendEmptyMessageDelayed(WHAT_CONTROL_HIDE, delay)
    }

    /**
     * 진행 시간 처리 함수
     * @param delay 몇초뒤 실행
     */
    private fun timerProgress(delay: Long = 1000) {
        messageHandler.removeMessages(WHAT_TIMER)
        messageHandler.sendEmptyMessageDelayed(WHAT_TIMER, delay)
    }

    /**
     * Window FullScreen
     * 버전 별로 분기 처리.
     * @param decorView View
     */
    private fun hideSystemUi(decorView: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.window.insetsController?.run {
                hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            decorView.systemUiVisibility =
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        }
    }

    /**
     * Window FullScreen 해제
     * 버번 별로 분기 처리
     * @param decorView View.
     */
    private fun showSystemUi(decorView: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.window.insetsController?.run {
                show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            }
        } else {
            @Suppress("DEPRECATION")
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
    }

    /**
     * 유튜브 앱으로 보기
     */
    private fun moveYoutubePage() {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://youtu.be/$youtubeId"))
            activity.startActivity(intent)
        }
    }

    /**
     * 유튜브 공유하기
     */
    private fun moveShared() {
        runCatching {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "https://youtube.com/watch?v=$youtubeId&feature=share")
            }
            activity.startActivity(Intent.createChooser(intent, "이 영상을 공유하기"))
        }
    }

    // [s] Public Function

    /**
     * 유튜브 주소 또는 아이디 값 처리 함수.
     * @param url URI OR 유튜브 아이디 값
     */
    fun setYoutubeUrl(url: String) {
        youtubeId = if (url.startsWith("http")) {
            val uri = Uri.parse(url)
            // Type -> https://www.youtube.com/watch?v=khmnEuo-oOg
            uri.getQueryParameter("v") ?: run {
                // Type -> https://youtu.be/...
                uri.lastPathSegment
            }
        } else {
            // Youtube Id
            url
        }

        // 중간품질 썸네일(320x180) : mqdefault.jpg
        // 고품질 썸네일(480x360) : hqdefault.jpg
        // 표준해상도 썸네일(640x480) : sddefault.jpg
        _youtubeThumbNail.value = "https://img.youtube.com/vi/$youtubeId/mqdefault.jpg"
    }

    // [e] Public Function

    /**
     * 동영상 중지
     */
    fun stopVideo() {

        if (youtubeWebView != null) {
            youtubeWebView?.destroy()
            youtubeWebView = null
        }

        youtubeFrame.removeAllViews()
        youtubeState.postValue(State.UNKNOWN)
    }

    /**
     * 동영상 재생
     */
    fun playVideo() {
        youtubeWebView?.playVideo()
    }

    /**
     * 동영상 일시 중지
     */
    fun pauseVideo() {
        youtubeWebView?.pauseVideo()
    }

    /**
     * 재생중인 시간 조회
     */
    fun fetchCurrentTime(callBack: (Float) -> Unit) {
        youtubeWebView?.fetchCurrentTime(callBack)
    }

    /**
     * 현재 동영상 재생 품질 조회
     */
    fun fetchPlayQuality(callBack: (PlayQuality) -> Unit) {
        youtubeWebView?.fetchPlayQuality(callBack)
    }

    /**
     * 현재 사용가능한 재생 품질 조회
     */
    fun fetchAvailablePlayQualities(callBack: (Array<PlayQuality>) -> Unit) {
        youtubeWebView?.fetchAvailablePlayQualities(callBack)
    }

    /**
     * 동영상 풀스크린 처리 함수.
     */
    fun enterFullScreen() {
        this.layoutParams = this.layoutParams.also { lp ->
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT
            lp.height = ViewGroup.LayoutParams.MATCH_PARENT
        }

        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        val decorView = activity.window.decorView as ViewGroup
        hideSystemUi(decorView)
        listener?.onEnterFullScreen()
    }

    /**
     * 풀스크린 동영상 나가기 처리 함수.
     */
    fun exitFullScreen() {
        this.layoutParams = this.layoutParams.also { lp ->
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
        }

        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        val decorView = activity.window.decorView as ViewGroup
        showSystemUi(decorView)
        listener?.onExitFullScreen()
    }

    // [e] Public Function

    // 유튜브 웹뷰에서 전달 받는 클래스
    inner class JavaScriptInterface {

        /**
         * 유튜브 재생 준비 완료된 상태인경우
         * 콜백하는 함수
         */
        @JavascriptInterface
        fun onReady() {

            multiNullCheck(youtubeId, youtubeWebView) { id, view ->
                // 생명주기에 따라서 알아서 Stop 처리하기때문에 바로 동영상 재생 한다.
                view.loadVideo(id)
            }
        }

        /**
         * 유튜브 상태 콜백함수
         * @param state YT.PlayerState
         */
        @JavascriptInterface
        fun onStateChanged(state: Int) {
            parseState(state).run {
                youtubeState.postValue(this)
                listener?.onState(this)
            }
        }

        /**
         * 유튜브 재생 품질 상태 콜백함수
         * @param state
         * small, medium, large, hd720, hd1080, highres, default -> Youtube 에서 적절한 재생 품질.
         */
        @JavascriptInterface
        fun onQualityChanged(state: String) {
            try {
                val parseQuality = PlayQuality.valueOf(state)
                listener?.onQualityChanged(parseQuality)
            } catch (ex: Exception) {
            }
        }

        /**
         * 유튜브 재생 총 시간
         * @param currTime 진행중인 시간
         * @param duration 영상 총 시간
         */
        @JavascriptInterface
        fun onDuration(currTime: Float, duration: Float) {
            setVideoTime(currTime, duration)
            listener?.onDuration(duration)
        }

        @JavascriptInterface
        fun onError(error: String) {
            listener?.onError(error)
        }
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val state = SavedState(superState)
        state.youtubeId = youtubeId
        state.youtubeThumb = youtubeThumbNail.value
        state.options = options
        state.isFullScreenVisible = isFullScreenVisible
        state.isLogoVisible = isLogoVisible
        state.isShareVisible = isShareVisible
        return state
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        val ss = state as SavedState
        super.onRestoreInstanceState(ss.superState)
        youtubeId = ss.youtubeId
        _youtubeThumbNail.value = ss.youtubeThumb
        options.copy(ss.options)
        isFullScreenVisible = ss.isFullScreenVisible
        isLogoVisible = ss.isLogoVisible
        isShareVisible = ss.isShareVisible
        initView()
    }

    internal class SavedState : BaseSavedState {

        var youtubeId: String? = null // Youtube 아이디 값
        var youtubeThumb: String? = null // Youtube 썸네일 주소
        var options: Options? = null // Youtube Option
        var isFullScreenVisible: Boolean = false
        var isLogoVisible: Boolean = false
        var isShareVisible: Boolean = false

        constructor(superState: Parcelable?) : super(superState)

        constructor(state: Parcel) : super(state) {
            youtubeId = state.readString()
            youtubeThumb = state.readString()
            options = state.readSerializable() as Options?
            isFullScreenVisible = state.readInt() != 0
            isLogoVisible = state.readInt() != 0
            isShareVisible = state.readInt() != 0
        }

        override fun writeToParcel(out: Parcel?, flags: Int) {
            super.writeToParcel(out, flags)
            out?.run {
                writeString(youtubeId)
                writeString(youtubeThumb)
                writeSerializable(options)
                writeInt(if (isFullScreenVisible) 1 else 0)
                writeInt(if (isLogoVisible) 1 else 0)
                writeInt(if (isShareVisible) 1 else 0)
            }
        }

        @JvmField
        val CREATOR = object : Parcelable.Creator<SavedState> {
            override fun createFromParcel(source: Parcel) = SavedState(source)

            override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
        }
    }

    @SuppressLint("HandlerLeak")
    inner class MessageHandler(activity: Activity) : Handler(Looper.getMainLooper()) {

        val act = WeakReference(activity)

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            if (act.get() == null) return
            LogD("Message Handler ${msg.what}")
            when (msg.what) {
                WHAT_TIMER -> {
                    fetchCurrentTime { currTime ->
                        if (controllerLiveData.value == true) {
                            timerProgress()
                        }

                        seekBar.progress = currTime.toInt()
                    }
                }
                WHAT_CONTROL_HIDE -> {
                    ObjectAnimator.ofFloat(youtubeController, ALPHA, 1.0F, 0F).apply {
                        interpolator = AccelerateInterpolator()
                        duration = 500
                        doOnStart { controllerLiveData.value = true }
                        doOnEnd { controllerLiveData.value = false }
                        start()
                    }
                }
                WHAT_CONTROL_SHOW -> {
                    ObjectAnimator.ofFloat(youtubeController, ALPHA, 0F, 1.0F).apply {
                        interpolator = DecelerateInterpolator()
                        duration = 500
                        doOnStart {
                            controllerLiveData.value = true
                            timerProgress(0)
                        }
                        doOnEnd {
                            controllerHide(2500)
                        }
                        start()
                    }
                }
            }
        }
    }
}