package com.hmju.youtubeplayerview

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.bumptech.glide.Glide
import com.hmju.youtubeplayer.YoutubePlayerView
import com.hmju.youtubeplayerview.util.Logger

class DummyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dummy)

        val youtubeView = findViewById<YoutubePlayerView>(R.id.youtubeView)
        youtubeView.setYoutubeUrl("https://www.youtube.com/watch?v=khmnEuo-oOg")

        Glide.with(this)
            .load(youtubeView.youtubeThumbNail)
            .into(findViewById(R.id.youtubeThumb))
    }

    override fun onDestroy() {
        super.onDestroy()
        Logger.d("onDestroy")
    }
}