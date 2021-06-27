package com.hmju.youtubeplayerview

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.hmju.youtubeplayer.YoutubePlayerView
import com.hmju.youtubeplayer.define.State
import com.hmju.youtubeplayerview.util.Logger

class MainActivity : AppCompatActivity() {
	private val youtubePlayerView : YoutubePlayerView by lazy { findViewById(R.id.youtubeView) }

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		youtubePlayerView.setYoutubeUrl("https://www.youtube.com/watch?v=k3rUzmGK1Hs")
		youtubePlayerView.listener = object : YoutubePlayerView.Companion.SimpleYoutubeListener() {
			override fun onState(state: State) {
				Logger.d("onState $state")
			}

			override fun onEnterFullScreen() {
				Logger.d("onEnterFullScreen")
			}

			override fun onExitFullScreen() {
				Logger.d("onExitFullScreen")
			}
		}
	}
}