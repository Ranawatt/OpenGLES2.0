package com.example.opengles20

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.opengles.GL

class Triangle {
    var vertexBuffer: FloatBuffer
    var vertices = floatArrayOf(
        0.0f, 0.5f, 0.0f,
        -0.5f, -0.5f, 0.0f,
        0.5f, -0.5f, 0.0f
    )
    private var shaderProgram: Int
    var color = floatArrayOf(0.0f, 0.6f, 1.0f, 1.0f)
    val vertexShaderCode: String = "attribute vec4 vPosition" +
        "void main() { " +
            "gl_position = vPosition" +
        "}"

    private val fragmentShaderCode = "Precision medium pFloat " +
        "uniform vec4 vColor" +
        "void main() { " +
           "gl_fragColor = vColor " +
        "}"

    constructor(){
        var bb = ByteBuffer.allocateDirect(vertices.size * 4)
        bb.order(ByteOrder.nativeOrder())

        vertexBuffer = bb.asFloatBuffer()
        vertexBuffer.put(vertices)
        vertexBuffer.position(0)

        var vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        var fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER,fragmentShaderCode)

        shaderProgram = GLES20.glCreateProgram()
        GLES20.glAttachShader(shaderProgram, vertexShader)
        GLES20.glAttachShader(shaderProgram, fragmentShader)
        GLES20.glLinkProgram(shaderProgram)

    }

    companion object {
        fun loadShader(type: Int, shaderCode: String): Int {
            var shader = GLES20.glCreateShader(type)
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
            return shader
        }
    }

    fun draw() {
        GLES20.glUseProgram(shaderProgram)
        var positionAttrib = GLES20.glGetAttribLocation(shaderProgram, "vPosition")
        GLES20.glEnableVertexAttribArray(positionAttrib)

        GLES20.glVertexAttribPointer(positionAttrib, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        var colorUniform = GLES20.glGetUniformLocation(shaderProgram, "vColor")
        GLES20.glUniform4fv(colorUniform,1, color, 0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0,vertices.size / 3)
        GLES20.glDisableVertexAttribArray(positionAttrib)
    }

}