package com.hmju.youtubeplayer.views

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.constraintlayout.widget.ConstraintLayout
import com.hmju.youtubeplayer.R
import java.net.URL
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Description :
 *
 * Created by hmju on 2021-05-26
 */
internal class YoutubeThumbnailView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(ctx, attrs, defStyleAttr) {

    companion object {
        val executor: ExecutorService by lazy { Executors.newCachedThreadPool() }
    }

    private val imgView: AppCompatImageView by lazy { findViewById(R.id.imgThumb) }
    private val imgPlay: AppCompatImageView by lazy { findViewById(R.id.imgPlay) }
    private val vPanel: View by lazy { findViewById(R.id.vPanel) }
    private val uiThreadHandler = Handler(Looper.getMainLooper())

    init {
        if (!isInEditMode) {
            // AddView
            val view =
                LayoutInflater.from(context).inflate(R.layout.view_youtube_thumbnail, this, false)
            addView(
                view,
                LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
        }
    }

    fun setThumbNail(url: String?) {
        if (!url.isNullOrEmpty()) {
            runCatching {
                executor.submit {
                    val bytes = URL(url).readBytes()
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    uiThreadHandler.post {
                        imgView.setImageBitmap(bitmap)
                        imgPlay.visibility = View.VISIBLE
                    }
                }
            }
            vPanel.setOnClickListener {
                callOnClick()
            }
        }
    }
}