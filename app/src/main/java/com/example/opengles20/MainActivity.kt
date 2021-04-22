package com.example.opengles20

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ConfigurationInfo
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val am: ActivityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info: ConfigurationInfo = am.deviceConfigurationInfo
        var supportES2 = info.reqGlEsVersion >= 0x20000
//        setContentView(R.layout.activity_main)
        if (supportES2){
            var mainRenderer = MainRenderer()
            var mainSurfaceView = MainSurfaceView(this)
            mainSurfaceView.setEGLContextClientVersion(2)
            mainSurfaceView.setRenderer(mainRenderer)
            this.setContentView(mainSurfaceView)
        }else{
            Log.e("OpenGles 2","Your device doesn't support ES2. " + info.reqGlEsVersion)
        }
    }
}