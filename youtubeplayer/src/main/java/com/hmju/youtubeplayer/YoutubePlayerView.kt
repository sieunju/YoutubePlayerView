package com.hmju.youtubeplayer

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
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
import com.hmju.youtubeplayer.extension.convertToTime
import com.hmju.youtubeplayer.extension.hideSystemUi
import com.hmju.youtubeplayer.extension.multiNullCheck
import com.hmju.youtubeplayer.extension.showSystemUi
import com.hmju.youtubeplayer.listener.YoutubeListener
import com.hmju.youtubeplayer.model.Options
import com.hmju.youtubeplayer.utility.ConnectionLiveData
import com.hmju.youtubeplayer.views.YoutubeThumbnailView
import com.hmju.youtubeplayer.views.YoutubeWebView
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
		const val DEBUG = false
		const val WHAT_TIMER = 1                // ?????? ???????????? ??????
		const val WHAT_CONTROL_HIDE = 2         // ???????????? ??? ?????????
		const val WHAT_CONTROL_SHOW = 3         // ???????????? ??? ?????????

		fun LogD(msg: String) {
			if (DEBUG) {
				Log.d(TAG, msg)
			}
		}

		open class SimpleYoutubeListener : YoutubeListener {
			override fun onState(state: State) {}
			override fun onQualityChanged(quality: PlayQuality) {}
			override fun onDuration(duration: Float) {}
			override fun onError(err: String) {}
			override fun onEnterFullScreen() {} // ??????????????? YoutubePlayerView ??? ????????? ????????? GONE ??????
			override fun onExitFullScreen() {} // ?????? ?????? ????????? YoutubePlayerView ??? ????????? ????????? VISIBLE ??????
		}
	}

	private val activity: FragmentActivity by lazy {
		if (ctx is FragmentActivity) {
			ctx
		} else {
			throw IllegalArgumentException("?????? ??? ????????? ?????? ??????????????? FragmentActivity ??? ??????????????? ?????????.")
		}
	}
	var listener: SimpleYoutubeListener? = null // Youtube Listener
	var youtubeId: String? = null
	val options: Options by lazy { Options() } // Youtube Options

	private val lifecycleRegistry: LifecycleRegistry by lazy { LifecycleRegistry(this) }
	private val connectionLiveData: ConnectionLiveData by lazy { ConnectionLiveData(ctx) }
	private val youtubeState: MutableLiveData<State> by lazy { MutableLiveData<State>() } // Youtube ?????????
	private val controllerLiveData: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
	private val fullScreenLiveData: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() } // ?????? ??????
	private val youtubeThumbNail: MutableLiveData<String> by lazy { MutableLiveData<String>() }

	var thumbNail: String = ""
		get() = youtubeThumbNail.value ?: ""
		private set

	private val messageHandler: MessageHandler by lazy { MessageHandler(activity) }

	private var thumbNailSkip: Boolean = false // ????????? ????????? ????????? ?????? ?????? ?????? flag
	private var isLogoVisible: Boolean = false
	private var isFullScreenVisible: Boolean = false
	private var isShareVisible: Boolean = false

	@IdRes
	private var thumbnailId: Int = -1

	// [s] View Variable
	private val container: ConstraintLayout by lazy { findViewById(R.id.container) }
	private val imgPlayAndPause: AppCompatImageView by lazy { findViewById(R.id.imgPlayAndPause) }
	private val youtubeFrame: FrameLayout by lazy { findViewById(R.id.youtubeFrame) }
	private val progressBar: ProgressBar by lazy { findViewById(R.id.progressBar) }
	private val youtubeController: ConstraintLayout by lazy { findViewById(R.id.youtubeController) }
	private val seekBar: AppCompatSeekBar by lazy { findViewById(R.id.seekBar) }
	private var vThumbnail: View? = null
	private val tvProgress: AppCompatTextView by lazy { findViewById(R.id.tvProgressTime) } // ???????????? ??????
	private val tvRemain: AppCompatTextView by lazy { findViewById(R.id.tvRemainTime) } // ?????? ??????
	private val imgLogo: AppCompatImageView by lazy { findViewById(R.id.imgLogo) }
	private val imgFullScreen: AppCompatImageView by lazy { findViewById(R.id.imgFullScreen) }
	private val imgShare: AppCompatImageView by lazy { findViewById(R.id.imgShare) } // ????????????
	private var youtubeWebView: YoutubeWebView? = null
	// [e] View Variable

	init {
		if (!isInEditMode) {
			// ????????? ??????
			ctx.obtainStyledAttributes(attrs, R.styleable.YoutubePlayerView).run {
				try {
					options.apply {
						isAutoPlay = this@run.getBoolean(
								R.styleable.YoutubePlayerView_youtubeIsAutoPlay,
								false
						)
						isLoop =
								this@run.getBoolean(R.styleable.YoutubePlayerView_youtubeIsLoop, false)
						isEffect =
								this@run.getBoolean(R.styleable.YoutubePlayerView_youtubeIsEffect, true)
						isRel = this@run.getBoolean(
								R.styleable.YoutubePlayerView_youtubeIsRelation,
								false
						)
						loadPolicy =
								this@run.getInt(R.styleable.YoutubePlayerView_youtubeLoadPolicy, 0)
						val langPref =
								this@run.getString(R.styleable.YoutubePlayerView_youtubeLangPref)
						if (!langPref.isNullOrEmpty()) {
							lanPref = langPref
						}
					}

					isLogoVisible = getBoolean(R.styleable.YoutubePlayerView_youtubeIsLogo, true)
					isFullScreenVisible = getBoolean(R.styleable.YoutubePlayerView_youtubeIsFullscreen, true)
					isShareVisible = getBoolean(R.styleable.YoutubePlayerView_youtubeIsShare, true)

					thumbnailId = getResourceId(
							R.styleable.YoutubePlayerView_youtubeThumbnailId,
							NO_ID
					)
					thumbNailSkip = getBoolean(R.styleable.YoutubePlayerView_youtubeSkipThumbNail, false)
				} catch (ex: Exception) {
				}

				recycle()
			}

			// AddView
			val root = LayoutInflater.from(context).inflate(R.layout.view_youtube_player, this, false)
			addView(root, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

			activity.lifecycle.addObserver(this)
		}
	}

	override fun getLifecycle() = lifecycleRegistry

	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		// Youtube ????????? ?????? 16:9 ????????? View ??????
		if (activity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
			super.onMeasure(widthMeasureSpec, heightMeasureSpec)
		} else {
			val ratioSize = MeasureSpec.getSize(widthMeasureSpec).toFloat() * (9F / 16F)
			val height = MeasureSpec.makeMeasureSpec(ratioSize.toInt(), MeasureSpec.EXACTLY)
			super.onMeasure(widthMeasureSpec, height)
		}
	}

	override fun addView(child: View?, params: ViewGroup.LayoutParams?) {
		super.addView(child, params)
		if (child == null) return
		if (thumbNailSkip) return

		if (thumbnailId != NO_ID &&
				vThumbnail == null &&
				child.id == thumbnailId
		) {
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
			R.id.imgFullScreen -> {
				if (youtubeState.value == State.PLAYING) {
					fullScreenLiveData.postValue(fullScreenLiveData.value != true)
				}
			}
			R.id.imgShare -> moveShared()
		}
	}

	override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
		runCatching {
			tvProgress.text = progress.toFloat().convertToTime()
		}
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
		// FullScreen ?????????????????? ????????? ??????
		if (fullScreenLiveData.value != true) {
			stopVideo()
		}
	}

	/**
	 * init View And Set Listener
	 */
	private fun initView() {
		imgLogo.visibility = if (isLogoVisible) VISIBLE else GONE
		imgFullScreen.visibility = if (isFullScreenVisible) VISIBLE else GONE
		imgShare.visibility = if (isShareVisible) VISIBLE else GONE

		if (thumbnailId == NO_ID && !thumbNailSkip) {
			vThumbnail = YoutubeThumbnailView(context).run {
				this@YoutubePlayerView.addView(this, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
				this.setOnClickListener { startVideo() }
				return@run this
			}
		}

		imgLogo.setOnClickListener(this)
		imgFullScreen.setOnClickListener(this)
		imgPlayAndPause.setOnClickListener(this)
		imgShare.setOnClickListener(this)
		seekBar.setOnSeekBarChangeListener(this)
		findViewById<View>(R.id.background).setOnClickListener(this)
	}

	/**
	 * ????????? ????????? ????????? ????????? ?????? ??????.
	 */
	private fun bringToThumbnail() {
		vThumbnail?.let {
			this.removeView(it)
			this.addView(it)
		}
	}

	@OnLifecycleEvent(Lifecycle.Event.ON_ANY)
	private fun onStateEvent(owner: LifecycleOwner, event: Lifecycle.Event) {
		lifecycleRegistry.handleLifecycleEvent(event)
//		LogD("onLifecycle Event $event")
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
				// ????????? ??????.
				onDestroy()
			}
			else -> {
			}
		}
	}

	/**
	 * Live Data ??????
	 */
	private fun registerLiveData() {
		// ???????????? ??????
		connectionLiveData.observe(this, {
			if (!it) {
				// ????????? ?????? ?????? ?????? ??????
				pauseVideo()
			}
		})

		// ?????? ??????
		youtubeState.observe(this, {
			LogD("youtubeState $it")

			// ?????????????????? -> ?????? ?????? ?????? ??????
			when (it) {
				State.CUE -> {
					// ?????? ??????
					progressBar.visibility = VISIBLE
					vThumbnail?.visibility = GONE
				}
				State.END -> {
					vThumbnail?.visibility = VISIBLE
				}
				State.PLAYING -> {
					// ?????? ??????
					progressBar.visibility = GONE
					imgPlayAndPause.visibility = VISIBLE
					imgPlayAndPause.setImageResource(R.drawable.ic_pause)
					controllerHide(2500)
				}
				State.PAUSE -> {
					// ?????? ?????? ??????
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

		// ???????????? ???????????? ?????? / ?????? ??????
		controllerLiveData.observe(this, { visible ->
			youtubeController.visibility = if (visible) VISIBLE else GONE
		})

		// ?????? ?????? ?????? true -> ?????? ??????, false -> ?????? ?????? ??????
		// ???????????? ?????? ??????????????? Activity configChanges -> orientation|screenSize ?????? ?????????.
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
					(vThumbnail as YoutubeThumbnailView).setThumbNail(url)
				}.onFailure {
					LogD("Error $it")
				}
			}
		})
	}

	/**
	 * ????????? ??????
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
	 * ????????? ?????? ?????? ?????? ??????
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
	 * ?????? ?????? ??????
	 * @param duration ????????? ?????? ??????
	 */
	private fun setVideoTime(currTime: Float, duration: Float) {
		activity.runOnUiThread {
			runCatching {
				seekBar.max = duration.toInt()
				tvProgress.text = currTime.convertToTime()
				tvRemain.text = duration.convertToTime()
			}
		}
	}

	/**
	 * ????????? ????????? ?????? ??? WebView ????????? ?????? ??????
	 */
	private fun onDestroy() {
		if (youtubeWebView != null) {
			youtubeWebView?.destroy()
			youtubeWebView = null
		}
		stopHandler()
	}

	/**
	 * ????????? ?????? ?????? ??????.
	 */
	private fun stopHandler() {
		messageHandler.removeMessages(WHAT_TIMER)
		messageHandler.removeMessages(WHAT_CONTROL_HIDE)
		messageHandler.removeMessages(WHAT_CONTROL_SHOW)
		messageHandler.removeCallbacksAndMessages(null)
		messageHandler.act.clear()
	}

	/**
	 * ???????????? ?????? ????????? ??????
	 */
	private fun controllerShow() {
		messageHandler.removeMessages(WHAT_CONTROL_SHOW)
		messageHandler.sendEmptyMessage(WHAT_CONTROL_SHOW)
	}

	/**
	 * ???????????? ?????? ???????????? ??????
	 * @param delay ????????? ????????? ?????? ????????? ???
	 */
	private fun controllerHide(delay: Long = 0) {
		messageHandler.removeMessages(WHAT_CONTROL_HIDE)
		messageHandler.sendEmptyMessageDelayed(WHAT_CONTROL_HIDE, delay)
	}

	/**
	 * ?????? ?????? ?????? ??????
	 * @param delay ????????? ??????
	 */
	private fun timerProgress(delay: Long = 1000) {
		messageHandler.removeMessages(WHAT_TIMER)
		messageHandler.sendEmptyMessageDelayed(WHAT_TIMER, delay)
	}

	/**
	 * ????????? ????????? ??????
	 */
	private fun moveYoutubePage() {
		runCatching {
			val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://youtu.be/$youtubeId"))
			activity.startActivity(intent)
		}
	}

	/**
	 * ????????? ????????????
	 */
	private fun moveShared() {
		runCatching {
			val intent = Intent(Intent.ACTION_SEND).apply {
				type = "text/plain"
				putExtra(Intent.EXTRA_TEXT, "https://youtube.com/watch?v=$youtubeId&feature=share")
			}
			activity.startActivity(Intent.createChooser(intent, "??? ????????? ????????????"))
		}
	}

	// [s] Public Function

	/**
	 * ????????? ?????? ?????? ????????? ??? ?????? ??????.
	 * @param url URI OR ????????? ????????? ???
	 */
	fun setYoutubeUrl(url: String) {
		youtubeId = if (url.startsWith("http")) {
			val uri = Uri.parse(url)
			// Type -> https://www.youtube.com/watch?v=khmnEuo-oOg
			uri.getQueryParameter("v") ?: run {
				// Type -> https://youtu.be/...
				uri.lastPathSegment?.trim()
			}
		} else {
			// Youtube Id
			url.trim()
		}

		// ???????????? ?????????(320x180) : mqdefault.jpg
		// ????????? ?????????(480x360) : hqdefault.jpg
		// ??????????????? ?????????(640x480) : sddefault.jpg
		youtubeThumbNail.value = "https://img.youtube.com/vi/$youtubeId/mqdefault.jpg"
	}

	// [e] Public Function

	/**
	 * ????????? ??????
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
	 * ????????? ??????
	 */
	fun playVideo() {
		youtubeWebView?.playVideo()
	}

	/**
	 * ????????? ?????? ??????
	 */
	fun pauseVideo() {
		youtubeWebView?.pauseVideo()
	}

	/**
	 * ???????????? ?????? ??????
	 */
	fun fetchCurrentTime(callBack: (Float) -> Unit) {
		youtubeWebView?.fetchCurrentTime(callBack)
	}

	/**
	 * ?????? ????????? ?????? ?????? ??????
	 */
	fun fetchPlayQuality(callBack: (PlayQuality) -> Unit) {
		youtubeWebView?.fetchPlayQuality(callBack)
	}

	/**
	 * ?????? ??????????????? ?????? ?????? ??????
	 */
	fun fetchAvailablePlayQualities(callBack: (Array<PlayQuality>) -> Unit) {
		youtubeWebView?.fetchAvailablePlayQualities(callBack)
	}

	/**
	 * ????????? ???????????? ?????? ??????.
	 */
	fun enterFullScreen() {
//		listener?.onEnterFullScreen()
		container.layoutParams = container.layoutParams.also { lp ->
			lp.width = ViewGroup.LayoutParams.MATCH_PARENT
			lp.height = ViewGroup.LayoutParams.MATCH_PARENT
		}
		with(activity) {
			requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
			hideSystemUi()
		}
		removeView(container)
		(activity.window.decorView as ViewGroup).addView(container)
	}

	/**
	 * ???????????? ????????? ????????? ?????? ??????.
	 */
	fun exitFullScreen() {
		//		listener?.onExitFullScreen()
		container.layoutParams = container.layoutParams.also { lp ->
			lp.width = ViewGroup.LayoutParams.MATCH_PARENT
			lp.height = ViewGroup.LayoutParams.MATCH_PARENT
		}
		with(activity) {
			requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
			showSystemUi()
		}
		(activity.window.decorView as ViewGroup).removeView(container)
		addView(container)

		bringToThumbnail()
		playVideo()
	}

	// [e] Public Function

	// ????????? ???????????? ?????? ?????? ?????????
	inner class JavaScriptInterface {

		/**
		 * ????????? ?????? ?????? ????????? ???????????????
		 * ???????????? ??????
		 */
		@JavascriptInterface
		fun onReady() {

			multiNullCheck(youtubeId, youtubeWebView) { id, view ->
				// ??????????????? ????????? ????????? Stop ????????????????????? ?????? ????????? ?????? ??????.
				view.loadVideo(id)
			}
		}

		/**
		 * ????????? ?????? ????????????
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
		 * ????????? ?????? ?????? ?????? ????????????
		 * @param state
		 * small, medium, large, hd720, hd1080, highres, default -> Youtube ?????? ????????? ?????? ??????.
		 */
		@JavascriptInterface
		fun onQualityChanged(state: String) {
			runCatching {
				val parseQuality = PlayQuality.valueOf(state)
				listener?.onQualityChanged(parseQuality)
			}
		}

		/**
		 * ????????? ?????? ??? ??????
		 * @param currTime ???????????? ??????
		 * @param duration ?????? ??? ??????
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
		youtubeThumbNail.value = ss.youtubeThumb
		options.copy(ss.options)
		isFullScreenVisible = ss.isFullScreenVisible
		isLogoVisible = ss.isLogoVisible
		isShareVisible = ss.isShareVisible
		initView()
	}

	internal class SavedState : BaseSavedState {

		var youtubeId: String? = null // Youtube ????????? ???
		var youtubeThumb: String? = null // Youtube ????????? ??????
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