package com.hmju.youtubeplayer.model

import com.hmju.youtubeplayer.define.Constants
import org.json.JSONObject
import java.io.Serializable

/**
 * Description : https://developers.google.com/youtube/player_parameters?hl=ko#Parameters
 * 참고한 데이터 클래스
 * 사용하지 않고 기본값으로 설정된 것들은 설정 X
 * Created by hmju on 2021-04-27
 */
class Options : JSONObject(), Serializable {
    companion object {
        private const val AUTO_PLAY = "autoplay" // 자동 재생 O -> 1, 자동 재생 X -> 0
        private const val CONTROLS = "controls" // 자체 컨트롤러 표시 X -> 0, 자체 컨트롤러 표시 O -> 1
        private const val END = "end" // 몇초뒤 동영상 중지
        private const val FS = "fs" // 전체 화면 버튼 유무 -> 무조건 안보이게 처리 (0)
        private const val IV_LOAD_POLICY = "iv_load_policy" // 동영상 특수 효과 1 -> 표시 O, 3 -> 표시 X
        private const val LOOP = "loop" // 반복 재생 0 -> 반복 재생 X, 1 -> 반복 재생
        private const val LOGO = "modestbranding" // 로그 표시 O -> 0, 로고 표시 X -> 1
        private const val START = "start" // 시작 지점
        private const val REL = "rel" // 관련 영상 표시 O -> 1, 표시 X -> 0
        private const val PLAY_INLINE = "playsinline" // iOS 해당.
        private const val PROGRESS_COLOR = "color"
        private const val ORIGIN = "origin"
        private const val ENABLE_JS_API = "enablejsapi" // JavaScript Api 제어
        private const val CC_LOAD_POLICY = "cc_load_policy"
        private const val CC_LANG_PREF = "cc_lang_pref"
        private const val DISABLE_KB = "disablekb"
    }

    var isAutoPlay: Boolean = false // 자동 재생 유무
        set(value) {
            if (value) {
                put(AUTO_PLAY, 1)
            } else {
                put(AUTO_PLAY, 0)
            }

            field = value
        }

    var isControl: Boolean = false // 웹뷰 자체 컨트롤러 표시 유무
        set(value) {
            if (value) {
                put(CONTROLS, 1)
            } else {
                put(CONTROLS, 0)
            }
            field = value
        }

    var end: Float = 0F // 종료 시점
        set(value) {
            put(END, value)
            field = value
        }

    var start: Float = 0F // 시작 시점
        set(value) {
            put(START, value)
            field = value
        }

    //    var isLogo: Boolean = false // 로고 표시 유무
//        set(value) {
//            if (value) {
//                put(LOGO, 0)
//            } else {
//                put(LOGO, 1)
//            }
//            field = value
//        }
    var isLoop: Boolean = false // 연속 재생 유무
        set(value) {
            if (value) {
                put(LOOP, 1)
            } else {
                put(LOOP, 0)
            }
            field = value
        }
    var isEffect: Boolean = false // 특수효과 표시 유무
        set(value) {
            if (value) {
                put(IV_LOAD_POLICY, 1)
            } else {
                put(IV_LOAD_POLICY, 3)
            }
            field = value
        }
    var isRel: Boolean = false // 관련 영상 표시 유무
        set(value) {
            if (value) {
                put(REL, 1)
            } else {
                put(REL, 0)
            }
            field = value
        }
    var loadPolicy: Int = 0
        set(value) {
            put(CC_LOAD_POLICY, value)
            field = value
        }
    var lanPref: String? = null
        set(value) {
            if (value != null) {
                put(CC_LANG_PREF, value)
            }
            field = value
        }

    init {
        reset()
    }

    /**
     * 변수값  초기화.
     */
    fun reset() {
        isAutoPlay = false
        isControl = false
        isLoop = false
        isEffect = true
        isRel = false
        loadPolicy = 0
        put(LOGO, 1)
        put(FS, 0)
        put(PROGRESS_COLOR, "white")
        put(ORIGIN, Constants.BASE_URL)
        put(PLAY_INLINE, 0)
        put(ENABLE_JS_API, 1)
        put(DISABLE_KB, 1) // 키보드 컨트롤 비 활성화
    }

    fun copy(copy: Options?) {
        reset()
        if (copy != null) {
            isAutoPlay = copy.isAutoPlay
            isControl = copy.isControl
            isLoop = copy.isLoop
            isEffect = copy.isEffect
            isRel = copy.isRel
            loadPolicy = copy.loadPolicy
        }
    }
}