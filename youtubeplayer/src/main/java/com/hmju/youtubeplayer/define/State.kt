package com.hmju.youtubeplayer.define

/**
 * Description : Youtube 상태
 *
 * Created by hmju on 2021-04-27
 */
enum class State(val code: Int) {
    CUE(-1), // 영상 대기
    END(0), // 영상 종료
    PLAYING(1), // 영상 재생
    PAUSE(2), // 영상 일시 중지
    BUFFERING(3), // 영상 버퍼링
    UNKNOWN(5) // Invalid
}