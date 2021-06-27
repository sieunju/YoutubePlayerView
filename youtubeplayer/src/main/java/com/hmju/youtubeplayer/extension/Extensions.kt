package com.hmju.youtubeplayer.extension

import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.fragment.app.FragmentActivity


// MultiPle Null Check.
inline fun <A, B, R> multiNullCheck(a: A?, b: B?, function: (A, B) -> R): R? {
    return if (a != null && b != null) {
        function(a, b)
    } else {
        null
    }
}

// MultiPle Null Check.
inline fun <A, B, C, R> multiNullCheck(a: A?, b: B?, c: C?, function: (A, B, C) -> R): R? {
    return if (a != null && b != null && c != null) {
        function(a, b, c)
    } else {
        null
    }
}

/**
 * Convert To Time
 * "0:00"
 */
fun Float.convertToTime(): String {
    val min = (this / 60).toInt()
    val sec = (this % 60).toInt()
    return String.format("%d:%02d", min, sec)
}

/**
 * 네비게이션바 및 상태바 숨김 처리.
 */
fun FragmentActivity.hideSystemUi() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        window.insetsController?.run {
            hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    } else {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }
}

/**
 * 네비게이션바 및 상태바 보임 처리.
 */
fun FragmentActivity.showSystemUi() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        window.insetsController?.run {
            show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        }
    } else {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }
}