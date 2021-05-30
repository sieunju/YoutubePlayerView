package com.hmju.youtubeplayer.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.hmju.youtubeplayer.R

/**
 * Description : 유튜브 썸네일 레이아웃
 *
 * Created by juhongmin on 5/8/21
 */
internal class YoutubeThumbnailView @JvmOverloads constructor(
		ctx: Context,
		attrs: AttributeSet? = null,
		defStyleAttr: Int = 0
) : ConstraintLayout(ctx, attrs, defStyleAttr) {


	init {
		if (!isInEditMode) {

			// AddView
			LayoutInflater.from(context).inflate(R.layout.view_youtube_thumbnail, this, false).apply {
				attachViewToParent(this, 0, layoutParams)
			}
		}
	}
}