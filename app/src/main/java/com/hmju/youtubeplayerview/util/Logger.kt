package com.hmju.youtubeplayerview.util

import android.util.Log

/**
 * Description :
 *
 * Created by hmju on 2021-04-27
 */
class Logger {
    companion object {
        val TAG = "LogUtil"

        @JvmStatic
        @JvmName("d")
        fun d(msg: String) {
            val ste = Thread.currentThread().stackTrace[4]
            val sb = StringBuilder()
            Log.d("[$TAG:$sb]", msg)
        }
    }
}