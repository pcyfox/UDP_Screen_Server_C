package com.pcyfox.screen.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.blankj.utilcode.constant.PermissionConstants
import com.blankj.utilcode.util.PermissionUtils
import com.blankj.utilcode.util.ScreenUtils
import com.pcyfox.screen.R
import com.pcyfox.screen.Sender
import com.pcyfox.screen.service.ScreenRecorderService
import kotlinx.android.synthetic.main.activity_screen_record.*
import kotlin.math.min


class ScreenRecordDemoActivity : AppCompatActivity(), View.OnClickListener {
    private val WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 200
    private val REQUEST_CODE = 202
    private val isDisableAudio = true;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_screen_record)
        PermissionUtils.permission(
            PermissionConstants.STORAGE,
            PermissionConstants.ACTIVITY_RECOGNITION
        ).request()
        initTestVideo()
    }

    private fun initTestVideo() {
        vv_test.setVideoPath("/sdcard/test.mp4")
        vv_test.setOnPreparedListener {
            vv_test.start()
            it.isLooping = true
        }
        val w = ScreenUtils.getAppScreenWidth()
        val h = ScreenUtils.getAppScreenHeight()
        et_w.setText(w.toString())
        et_h.setText(h.toString())
        val r = (w * h * 0.8).toInt()
        et_bitrate.setText(r.toString())
    }


    override fun onClick(view: View) {
        when (view.id) {
            R.id.btn_start_screen -> {
                startActivityForResult(
                    (getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager).createScreenCaptureIntent(),
                    REQUEST_CODE
                )
            }

        }
    }


    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val w = Integer.parseInt(et_w.text.toString())
        val h = Integer.parseInt(et_h.text.toString())
        val r = Integer.parseInt(et_bitrate.text.toString())
        val fps = Integer.parseInt(et_fps.text.toString())

        ScreenRecorderService.start(
            this,
            resultCode,
            data!!,
            w,
            h,
            fps,
            r.toInt(),
            Sender.BROADCAST_IP,
            Sender.TARGET_PORT,
        )
    }


    private fun requestSDPermission(): Boolean {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    WRITE_EXTERNAL_STORAGE_REQUEST_CODE
                )
                return false
            }
        }
        return true
    }


}
