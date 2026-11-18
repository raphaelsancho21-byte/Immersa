package com.immersa.vr

import android.app.Activity
import android.content.Intent
import android.content.pm.ResolveInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : Activity(), SensorEventListener {
    private lateinit var sensors: SensorManager
    private lateinit var surface: GLSurfaceView
    private lateinit var renderer: VrRenderer
    private var sensor: Sensor? = null
    private val matrix = FloatArray(9)
    private val remapped = FloatArray(9)
    private val orientation = FloatArray(3)
    private var origin: Float? = null
    private var target = -1
    private var focusedAt = 0L
    private var lastLaunch = 0L
    private var apps: List<ResolveInfo> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        sensors = getSystemService(SENSOR_SERVICE) as SensorManager
        sensor = sensors.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: sensors.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        apps = packageManager.queryIntentActivities(launcher, 0)
            .filter { it.activityInfo.packageName != packageName }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }
            .take(12)
        renderer = VrRenderer(apps.map { it.loadLabel(packageManager).toString() })
        surface = GLSurfaceView(this).apply {
            setEGLContextClientVersion(2)
            setRenderer(renderer)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_UP) {
                    origin = null
                    Toast.makeText(this@MainActivity, "Visão recentralizada", Toast.LENGTH_SHORT).show()
                }
                true
            }
        }
        setContentView(surface)
        if (sensor == null) Toast.makeText(this, "Sensor de rotação indisponível", Toast.LENGTH_LONG).show()
    }

    override fun onResume() {
        super.onResume()
        surface.onResume()
        sensor?.let { sensors.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
    }
    override fun onPause() {
        sensors.unregisterListener(this)
        surface.onPause()
        super.onPause()
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    override fun onSensorChanged(event: SensorEvent) {
        SensorManager.getRotationMatrixFromVector(matrix, event.values)
        SensorManager.remapCoordinateSystem(matrix, SensorManager.AXIS_X, SensorManager.AXIS_Z, remapped)
        SensorManager.getOrientation(remapped, orientation)
        val heading = orientation[0]
        if (origin == null) origin = heading
        val delta = heading - origin!!
        val yaw = atan2(sin(delta.toDouble()), cos(delta.toDouble())).toFloat()
        val pitch = orientation[1]
        renderer.setHead(yaw, pitch)
        val current = renderer.gazeTarget(yaw, pitch)
        val now = SystemClock.elapsedRealtime()
        if (current != target) { target = current; focusedAt = now }
        if (current >= 0 && now - focusedAt >= 1800 && now - lastLaunch >= 3500) {
            lastLaunch = now
            focusedAt = now
            val info = apps.getOrNull(current) ?: return
            runOnUiThread {
                try {
                    startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                        .setClassName(info.activityInfo.packageName, info.activityInfo.name))
                } catch (_: Exception) {
                    Toast.makeText(this, "Não foi possível abrir o app", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
