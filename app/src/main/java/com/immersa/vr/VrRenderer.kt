package com.immersa.vr

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class VrRenderer(private val apps: List<String>) : GLSurfaceView.Renderer {
    @Volatile private var yaw = 0f
    @Volatile private var pitch = 0f
    private var width = 1
    private var height = 1
    private var program = 0
    private val textures = mutableListOf<Int>()
    private val projection = FloatArray(16)
    private val view = FloatArray(16)
    private val model = FloatArray(16)
    private val vp = FloatArray(16)
    private val mvp = FloatArray(16)
    private val coords = floatArrayOf(-1f,-1f,0f,0f,1f, 1f,-1f,0f,1f,1f, -1f,1f,0f,0f,0f, 1f,1f,0f,1f,0f)
    private val buffer = ByteBuffer.allocateDirect(coords.size*4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(coords); position(0) }
    fun setHead(y: Float, p: Float) { yaw=y; pitch=p }
    private fun angle(i: Int) = (i % 6 - 2.5f) * 0.30f
    fun gazeTarget(y: Float, p: Float): Int {
        val row = if (p < -0.10f) 0 else if (p > 0.10f) 1 else return -1
        return (row*6 until minOf(row*6+6, apps.size)).firstOrNull { abs(y-angle(it)) < 0.12f } ?: -1
    }
    private fun shader(type: Int, source: String): Int {
        val id = GLES20.glCreateShader(type)
        GLES20.glShaderSource(id, source)
        GLES20.glCompileShader(id)
        return id
    }
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.02f,0.035f,0.085f,1f)
        val v = shader(GLES20.GL_VERTEX_SHADER, "attribute vec3 aPos; attribute vec2 aUv; uniform mat4 uMvp; varying vec2 uv; void main(){gl_Position=uMvp*vec4(aPos,1.);uv=aUv;}")
        val f = shader(GLES20.GL_FRAGMENT_SHADER, "precision mediump float; varying vec2 uv; uniform sampler2D tex; void main(){gl_FragColor=texture2D(tex,uv);}")
        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program,v)
        GLES20.glAttachShader(program,f)
        GLES20.glLinkProgram(program)
        GLES20.glDeleteShader(v)
        GLES20.glDeleteShader(f)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA,GLES20.GL_ONE_MINUS_SRC_ALPHA)
        textures.clear()
        listOf("IMMERSA" to "SPATIAL VR ALPHA").plus(apps.map { it.take(21) to "OLHE PARA ABRIR" }).plus(listOf("+" to "")).forEach { (title,subtitle) ->
            val bitmap = Bitmap.createBitmap(512,256,Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint(3)
            paint.color = Color.rgb(13,24,50)
            canvas.drawRoundRect(8f,8f,504f,248f,20f,20f,paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 5f
            paint.color = Color.rgb(42,205,240)
            canvas.drawRoundRect(8f,8f,504f,248f,20f,20f,paint)
            paint.style = Paint.Style.FILL
            paint.color = Color.WHITE
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = if (title.length > 15) 28f else 42f
            canvas.drawText(title,256f,120f,paint)
            paint.color = Color.rgb(115,211,243)
            paint.textSize = 22f
            canvas.drawText(subtitle,256f,175f,paint)
            val id = IntArray(1)
            GLES20.glGenTextures(1,id,0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,id[0])
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_CLAMP_TO_EDGE)
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D,0,bitmap,0)
            bitmap.recycle()
            textures.add(id[0])
        }
    }
    override fun onSurfaceChanged(gl: GL10?, w: Int, h: Int) { width=w; height=h }
    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glUseProgram(program)
        val pos = GLES20.glGetAttribLocation(program,"aPos")
        val uv = GLES20.glGetAttribLocation(program,"aUv")
        buffer.position(0)
        GLES20.glVertexAttribPointer(pos,3,GLES20.GL_FLOAT,false,20,buffer)
        GLES20.glEnableVertexAttribArray(pos)
        buffer.position(3)
        GLES20.glVertexAttribPointer(uv,2,GLES20.GL_FLOAT,false,20,buffer)
        GLES20.glEnableVertexAttribArray(uv)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(program,"tex"),0)
        for (eye in 0..1) {
            GLES20.glViewport(eye*width/2,0,width/2,height)
            Matrix.perspectiveM(projection,0,85f,(width/2f)/height,0.1f,30f)
            Matrix.setIdentityM(view,0)
            Matrix.rotateM(view,0,Math.toDegrees(pitch.toDouble()).toFloat(),1f,0f,0f)
            Matrix.rotateM(view,0,-Math.toDegrees(yaw.toDouble()).toFloat(),0f,1f,0f)
            Matrix.translateM(view,0,if (eye==0) 0.032f else -0.032f,0f,0f)
            Matrix.multiplyMM(vp,0,projection,0,view,0)
            draw(0,0f,1.5f,-4.5f,2.5f,0.5f)
            apps.forEachIndexed { i,_ ->
                val a = angle(i)
                draw(i+1,sin(a)*4.1f,if (i<6) 0.45f else -0.55f,-cos(a)*4.1f,1.15f,0.65f)
            }
            draw(textures.lastIndex,0f,0f,-2f,0.06f,0.06f)
        }
    }
    private fun draw(index: Int,x: Float,y: Float,z: Float,w: Float,h: Float) {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,textures[index])
        Matrix.setIdentityM(model,0)
        Matrix.translateM(model,0,x,y,z)
        Matrix.scaleM(model,0,w/2,h/2,1f)
        Matrix.multiplyMM(mvp,0,vp,0,model,0)
        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(program,"uMvp"),1,false,mvp,0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4)
    }
}
