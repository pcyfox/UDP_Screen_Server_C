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
import com.pcyfox.screen.R
import com.pcyfox.screen.service.ScreenRecorderService
import kotlinx.android.synthetic.main.activity_screen_record.*


class ScreenRecordDemoActivity : AppCompatActivity(), View.OnClickListener {

    private val WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 200
    private val RECORD_AUDIO_REQUEST_CODE = 201
    private val RECORD_CAMERA_REQUEST_CODE = 202
    private val REQUEST_CODE = 202
    private val isDisableAudio = true;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_screen_record)
        requestSDPermission()
        requestAudioPermission();
        requestCameraPermission()
    }

    private fun initTestVideo() {
        vv_test.setVideoPath("/sdcard/test.mp4")
        vv_test.setOnPreparedListener {
            vv_test.start()
            it.isLooping = true
        }
    }


    override fun onClick(view: View) {
        when (view.id) {
            R.id.btn_start_screen -> {
                startActivityForResult(
                    (getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager).createScreenCaptureIntent(),
                    REQUEST_CODE
                )
                initTestVideo()
            }

        }
    }


    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        ScreenRecorderService.start(this, resultCode, data, 1935)
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


    private fun requestAudioPermission(): Boolean {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    RECORD_AUDIO_REQUEST_CODE
                )
                return false
            }
        }
        return true
    }

    private fun requestCameraPermission(): Boolean {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    RECORD_CAMERA_REQUEST_CODE
                )
                return false
            }
        }
        return true
    }
}
