package com.example.opengles20

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MainRenderer : GLSurfaceView.Renderer {
    lateinit var triangle: Triangle
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        triangle = Triangle()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {

    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClearColor(0.8f, 0.0f,0.0f,1.0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT )

        triangle.draw()
    }
}