package com.hmju.youtubeplayerview

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.hmju.youtubeplayer.YoutubePlayerView
import com.hmju.youtubeplayerview.util.Logger
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlin.random.Random

class DummyActivity : AppCompatActivity() {

    private val youtubeArr = arrayListOf<String>(
        "https://www.youtube.com/watch?v=khmnEuo-oOg",
        "https://www.youtube.com/watch?v=GV2asy4FpIA",
        "https://www.youtube.com/watch?v=1T2pwVGUhu4",
        "https://www.youtube.com/watch?v=W0A0BTCl6U0",
        "https://www.youtube.com/watch?v=bRBeNiO4qTM"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dummy)

        Observable.fromCallable {
            val tmpList = arrayListOf<String>()
            for (index in 0 until 1000) {
                tmpList.add(youtubeArr[Random.nextInt(youtubeArr.size)])
            }
            return@fromCallable tmpList
        }.subscribeOn(Schedulers.io())
            .subscribe({
                findViewById<RecyclerView>(R.id.rvContents).adapter = Adapter(it)
            }, {})
    }

    override fun onDestroy() {
        super.onDestroy()
        Logger.d("onDestroy")
    }

    inner class Adapter(private val dataList: ArrayList<String>) :
        RecyclerView.Adapter<ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(parent)

        override fun onBindViewHolder(holder: ViewHolder, pos: Int) {
            holder.youtubeView.setYoutubeUrl(dataList[pos])
            Glide.with(holder.youtubeThumb)
                .load(holder.youtubeView.thumbNail)
                .into(holder.youtubeThumb)
        }

        override fun getItemCount() = dataList.size
    }

    inner class ViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_view_holder, parent, false)
    ) {
        val youtubeView: YoutubePlayerView by lazy { itemView.findViewById(R.id.youtubeView) }
        val youtubeThumb: AppCompatImageView by lazy { itemView.findViewById(R.id.imgThumb) }

        init {
//            youtubeThumb.setOnClickListener {
//                youtubeView.startVideo()
//            }
        }
    }
}