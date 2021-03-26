package com.pcyfox.screen.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.blankj.utilcode.util.ScreenUtils
import com.pcyfox.screen.ScreenDisplay


/**
 * Basic RTMP/RTSP service streaming implementation with camera2
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class ScreenRecorderService : Service() {
    override fun onCreate() {
        super.onCreate()
        Log.e(TAG, "RTP Display service create")
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelId, NotificationManager.IMPORTANCE_HIGH)
            notificationManager?.createNotificationChannel(channel)
        }
        keepAliveTrick()
    }

    private fun keepAliveTrick() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            val notification = NotificationCompat.Builder(this, channelId)
                    .setOngoing(true)
                    .setContentTitle("")
                    .setContentText("").build()
            startForeground(1, notification)
        } else {
            startForeground(1, Notification())
        }
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "RTP Display service started")
        if (resultCode != null) {
            startStreamRtp(true)
        }
        return START_STICKY
    }

    companion object {
        private const val TAG = "ScreenRecordService"
        private const val channelId = "ScreenRecordServiceChannel"
        private const val notifyId = 1234567
        private var notificationManager: NotificationManager? = null
        private var serverDisplay: ScreenDisplay? = null
        private var resultCode: Int? = null
        private var data: Intent? = null

        fun start(context: Context, resultCode: Int, intent: Intent?, endpoint: Int) {
            Companion.resultCode = resultCode
            init(context, resultCode, intent)
            val startIntent = Intent()
            startIntent.setClass(context, ScreenRecorderService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(startIntent)
            } else {
                context.startService(startIntent)
            }
        }


        fun init(context: Context, resultCode: Int, intent: Intent?) {
            serverDisplay = ScreenDisplay(context)
            serverDisplay?.setIntentResult(resultCode, intent)
        }

        fun setData(resultCode: Int, data: Intent) {
            Companion.resultCode = resultCode
            Companion.data = data
        }

        fun isStreaming(): Boolean {
            return if (serverDisplay == null) {
                false
            } else {
                serverDisplay!!.isStreaming
            }
        }


        fun stopStream() {
            serverDisplay?.stopRecord()
            serverDisplay?.stopStream()
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        Log.e(TAG, "RTP Display service destroy")
        stopStream()
    }


    private fun startStreamRtp(isDisableAudio: Boolean) {
        //val w = ScreenUtils.getAppScreenWidth()
        //val h = ScreenUtils.getAppScreenHeight()
        val w = 1920
        val h = 1080
        val r = w * h * 0.6
        serverDisplay?.run {
            if (!serverDisplay!!.isStreaming) {
                val prepareAudio = serverDisplay!!.prepareAudio(16 * 1024, 8000, true, false, false);
                val prepareVideo = serverDisplay!!.prepareVideo(w, h, 25, r.toInt(), 0, ScreenUtils.getScreenDensityDpi())
                if (isDisableAudio && prepareVideo || (!isDisableAudio && prepareAudio && prepareVideo)) {
                    if (isDisableAudio) {
                        serverDisplay?.disableAudio()
                    }
                    serverDisplay!!.startStream()
                    if (isDisableAudio) {
                        serverDisplay?.disableAudio()
                    }
                }
            } else {
                serverDisplay!!.stopStream()
            }
        }
    }
}
