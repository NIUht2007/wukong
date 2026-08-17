package com.tianqi.camera

import android.app.Application
import android.util.Log
import java.io.File

class TianqiApp : Application() {

    override fun onCreate() {
        super.onCreate()
        installCrashHandler()
    }

    /** 全局异常捕获：崩溃堆栈写本地文件，便于排查（不上传任何数据） */
    private fun installCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                val dir = File(filesDir, "crash").apply { mkdirs() }
                File(dir, "crash_${System.currentTimeMillis()}.log").writeText(
                    buildString {
                        append("thread: ${thread.name}\n")
                        append(Log.getStackTraceString(throwable))
                    }
                )
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
