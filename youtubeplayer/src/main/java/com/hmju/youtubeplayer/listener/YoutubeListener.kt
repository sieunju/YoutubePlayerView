package com.hmju.youtubeplayer.listener

import com.hmju.youtubeplayer.define.PlayQuality
import com.hmju.youtubeplayer.define.State

/**
 * Description : Youtube 관련 Listener
 *
 * Created by hmju on 2021-04-27
 */
internal interface YoutubeListener {
    fun onState(state: State)
    fun onQualityChanged(quality: PlayQuality)
    fun onDuration(duration : Float)
    fun onError(err : String)
    fun onEnterFullScreen()
    fun onExitFullScreen()
}