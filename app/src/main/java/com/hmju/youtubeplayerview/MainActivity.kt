package com.hmju.youtubeplayerview

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.hmju.youtubeplayerview.util.Logger

class MainActivity : AppCompatActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)
		Logger.d("Activity onCreate")

//		val youtubeView = findViewById<YoutubePlayerView>(R.id.youtubeView)
//		youtubeView.setYoutubeUrl("https://www.youtube.com/watch?v=khmnEuo-oOg")
////        youtubeView.setYoutubeUrl("https://www.youtube.com/watch?v=hlWiI4xVXKY")
//		youtubeView.listener = object : YoutubePlayerView.Companion.SimpleYoutubeListener() {
//
//
//			override fun onEnterFullScreen() {
//				findViewById<TextView>(R.id.tvTT).visibility = View.GONE
//			}
//
//			override fun onExitFullScreen() {
//				findViewById<TextView>(R.id.tvTT).visibility = View.VISIBLE
//
//			}
//		}

	}

	fun moveDummy(v : View){
		startActivity(Intent(this,DummyActivity::class.java))
	}
}