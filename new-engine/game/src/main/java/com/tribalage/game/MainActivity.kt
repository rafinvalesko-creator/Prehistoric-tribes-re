package com.tribalage.game

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.content.pm.ActivityInfo
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.min

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
        setContentView(TribalWorldView())
    }
}

private class TribalWorldView : SurfaceView, SurfaceHolder.Callback, Runnable {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    @Volatile private var running = false
    private var loop: Thread? = null
    private var startedAt = System.nanoTime()
    private var lastTick = startedAt
    private var food = 120
    private var wood = 85
    private var stone = 42
    private var population = 8
    private var day = 1
    private var phase = 0f

    private val grass = intArrayOf(0xff668b43.toInt(), 0xff72964b.toInt(), 0xff789d50.toInt(), 0xff628340.toInt())
    private val map = Array(15) { x -> IntArray(9) { y -> ((x * 13 + y * 7 + x * y) % 4) } }

    init { holder.addCallback(this); isFocusable = true }
    constructor() : super(AppContextHolder.context)

    override fun surfaceCreated(holder: SurfaceHolder) { running = true; loop = Thread(this, "tribal-render").also { it.start() } }
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
    override fun surfaceDestroyed(holder: SurfaceHolder) { running = false; try { loop?.join(500) } catch (_: InterruptedException) {} }

    override fun run() {
        while (running) {
            val now = System.nanoTime()
            val dt = ((now - lastTick) / 1_000_000_000.0).toFloat().coerceIn(0f, 0.1f)
            lastTick = now
            phase += dt
            if (phase >= 5f) {
                phase -= 5f
                food += 2
                wood += 1
                if (food > 180 && population < 12) { population++; food -= 18 }
                if (food > 0) food--
            }
            val canvas = try { holder.lockCanvas() } catch (_: Exception) { null }
            if (canvas != null) {
                drawWorld(canvas)
                holder.unlockCanvasAndPost(canvas)
            }
            try { Thread.sleep(16) } catch (_: InterruptedException) {}
        }
    }

    private fun drawWorld(c: Canvas) {
        val scale = min(width / 960f, height / 432f)
        val ox = (width - 960f * scale) / 2f
        val oy = (height - 432f * scale) / 2f
        c.save()
        c.translate(ox, oy); c.scale(scale, scale)
        c.drawColor(0xff172b24.toInt())
        paint.style = Paint.Style.FILL
        paint.color = 0xffb9a16b.toInt()
        c.drawRect(0f, 0f, 960f, 432f, paint)
        drawTerrain(c)
        drawSettlement(c)
        drawPeople(c)
        drawAtmosphere(c)
        drawHud(c)
        c.restore()
    }

    private fun iso(x: Int, y: Int): Pair<Float, Float> {
        val px = 480f + (x - y) * 29f
        val py = 225f + (x + y) * 15f
        return px to py
    }

    private fun drawTerrain(c: Canvas) {
        for (x in 0 until 15) for (y in 0 until 9) {
            val (px, py) = iso(x, y)
            val p = Path().apply {
                moveTo(px, py - 16f); lineTo(px + 30f, py); lineTo(px, py + 16f); lineTo(px - 30f, py); close()
            }
            paint.color = grass[map[x][y]]
            c.drawPath(p, paint)
            paint.color = 0x22502d1c
            paint.style = Paint.Style.STROKE; paint.strokeWidth = 1f; c.drawPath(p, paint); paint.style = Paint.Style.FILL
            if ((x * 17 + y * 23) % 11 == 0 && x !in 5..9) drawTree(c, px, py - 10f, (x + y) % 2 == 0)
            if ((x * 7 + y * 19) % 23 == 0) drawStone(c, px + 5f, py - 4f)
            if ((x * 5 + y * 3) % 29 == 0) drawBush(c, px - 8f, py - 4f)
        }
        // Winding stream
        val stream = Path().apply {
            moveTo(80f, 365f); cubicTo(220f, 315f, 190f, 270f, 330f, 250f)
            cubicTo(440f, 225f, 430f, 165f, 555f, 155f)
        }
        paint.color = 0xff4b9aab.toInt(); paint.style = Paint.Style.STROKE; paint.strokeWidth = 13f; c.drawPath(stream, paint)
        paint.color = 0xff8dc7c2.toInt(); paint.strokeWidth = 3f; c.drawPath(stream, paint); paint.style = Paint.Style.FILL
    }

    private fun drawTree(c: Canvas, x: Float, y: Float, pine: Boolean) {
        paint.color = 0xff59412b.toInt(); c.drawRect(x - 3f, y - 17f, x + 3f, y + 2f, paint)
        paint.color = if (pine) 0xff315b35.toInt() else 0xff426f38.toInt()
        c.drawCircle(x, y - 23f, 12f, paint); c.drawCircle(x - 7f, y - 18f, 8f, paint)
        paint.color = 0x334d8a45; c.drawCircle(x - 4f, y - 28f, 5f, paint)
    }
    private fun drawStone(c: Canvas, x: Float, y: Float) {
        paint.color = 0xff77786c.toInt()
        val p = Path().apply { moveTo(x - 9f,y); lineTo(x - 5f,y - 9f); lineTo(x + 4f,y - 11f); lineTo(x + 11f,y - 2f); lineTo(x + 7f,y + 2f); close() }
        c.drawPath(p, paint)
    }
    private fun drawBush(c: Canvas, x: Float, y: Float) {
        paint.color = 0xff426b37.toInt(); c.drawCircle(x, y, 7f, paint); c.drawCircle(x + 6f, y - 2f, 5f, paint)
        paint.color = 0xffa54b3b.toInt(); c.drawCircle(x + 2f, y - 2f, 2f, paint)
    }

    private fun drawSettlement(c: Canvas) {
        val (cx, cy) = iso(7, 4)
        // central meeting ground
        paint.color = 0xffb79a62.toInt(); c.drawOval(cx - 83f, cy - 17f, cx + 83f, cy + 19f, paint)
        drawHut(c, cx - 48f, cy - 28f, 1.0f)
        drawHut(c, cx + 35f, cy - 5f, 0.82f)
        drawGranary(c, cx + 3f, cy - 47f)
        drawCampfire(c, cx - 4f, cy + 15f)
        // palisade sections
        paint.color = 0xff795331.toInt()
        for (i in 0..8) {
            val x = cx - 94f + i * 23f
            c.drawRect(x, cy + 17f, x + 5f, cy + 36f, paint)
        }
        paint.color = 0xffb48a4d.toInt(); c.drawRect(cx - 97f, cy + 15f, cx + 98f, cy + 19f, paint)
    }

    private fun drawHut(c: Canvas, x: Float, y: Float, s: Float) {
        c.save(); c.translate(x, y); c.scale(s, s)
        paint.color = 0xff8b6239.toInt()
        c.drawRect(-22f, -22f, 22f, 3f, paint)
        paint.color = 0xffc19a5d.toInt()
        val roof = Path().apply { moveTo(-27f,-20f); lineTo(0f,-43f); lineTo(27f,-20f); close() }; c.drawPath(roof, paint)
        paint.color = 0xff543a28.toInt(); c.drawRect(-5f, -15f, 5f, 3f, paint)
        paint.color = 0xffe0bd7b.toInt(); c.drawLine(-19f,-18f,0f,-36f,paint)
        c.restore()
    }
    private fun drawGranary(c: Canvas, x: Float, y: Float) {
        paint.color = 0xffa77b45.toInt(); c.drawRect(x - 13f, y - 19f, x + 13f, y + 2f, paint)
        paint.color = 0xffd3a85d.toInt()
        val roof = Path().apply { moveTo(x-17f,y-18f); lineTo(x,y-30f); lineTo(x+17f,y-18f); close() }; c.drawPath(roof,paint)
        paint.color = 0xff59412c.toInt(); c.drawRect(x-3f,y-10f,x+3f,y+2f,paint)
    }
    private fun drawCampfire(c: Canvas, x: Float, y: Float) {
        paint.color = 0xff563d2a.toInt(); c.drawLine(x-9f,y+4f,x+9f,y-1f,paint); c.drawLine(x-8f,y-1f,x+8f,y+4f,paint)
        paint.color = 0xffe4a33d.toInt(); c.drawOval(x-5f,y-13f,x+5f,y+2f,paint)
        paint.color = 0xffffd36a.toInt(); c.drawOval(x-2f,y-9f,x+2f,y,paint)
    }

    private fun drawPeople(c: Canvas) {
        val (cx, cy) = iso(7, 4)
        for (i in 0 until population) {
            val a = i * 2.399f
            val x = cx + cos(a) * (48f + (i % 3) * 8f)
            val y = cy + sin(a) * (17f + (i % 3) * 3f) - 5f
            drawTribesman(c, x, y, phase, i)
        }
    }
    private fun drawTribesman(c: Canvas, x: Float, y: Float, t: Float, index: Int) {
        val bob = sin(t * 4f + index) * 1.6f
        paint.color = 0x55332211; c.drawOval(x-7f,y+8f,x+7f,y+12f,paint)
        paint.color = if (index % 4 == 0) 0xffa95e39.toInt() else 0xffc58c5c.toInt()
        c.drawOval(x-5f,y-13f+bob,x+5f,y-2f+bob,paint)
        paint.color = 0xff3f3027.toInt(); c.drawOval(x-5f,y-17f+bob,x+5f,y-7f+bob,paint)
        paint.color = 0xff765038.toInt(); c.drawRect(x-5f,y-3f+bob,x+5f,y+7f+bob,paint)
        paint.color = 0xffd7c29a.toInt(); c.drawRect(x-5f,y+1f+bob,x+5f,y+4f+bob,paint)
        paint.color = 0xff5b3b28.toInt(); c.drawLine(x+3f,y-2f+bob,x+10f,y-13f+bob,paint)
    }

    private fun drawAtmosphere(c: Canvas) {
        val glow = (sin((System.nanoTime() - startedAt) / 8.0e9) * 0.5 + 0.5).toFloat()
        paint.color = (0x18000000 + (glow * 0x08000000).toInt())
        c.drawRect(0f, 52f, 960f, 400f, paint)
    }

    private fun drawHud(c: Canvas) {
        paint.color = 0xdd18251d.toInt(); c.drawRoundRect(14f, 10f, 946f, 54f, 9f, 9f, paint)
        paint.color = 0xffd9c28c.toInt(); paint.textSize = 16f; paint.typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
        c.drawText("ПЛЕМЯ ДОЛИНЫ", 30f, 37f, paint)
        paint.textSize = 14f; paint.typeface = Typeface.DEFAULT_BOLD
        c.drawText("ДЕНЬ $day", 200f, 37f, paint)
        c.drawText("Еда  $food", 320f, 37f, paint)
        c.drawText("Дерево  $wood", 435f, 37f, paint)
        c.drawText("Камень  $stone", 580f, 37f, paint)
        c.drawText("Люди  $population/20", 735f, 37f, paint)
        paint.color = 0xcc18251d.toInt(); c.drawRoundRect(14f, 382f, 946f, 420f, 8f, 8f, paint)
        paint.color = 0xffe5d2a6.toInt(); paint.textSize = 13f
        c.drawText("ПОСЕЛЕНИЕ", 30f, 406f, paint)
        paint.color = 0xffa9c38a.toInt(); c.drawText("Хижины  2     Амбар  1     Костёр  1     Частокол", 155f, 406f, paint)
        paint.color = 0xffd6b67d.toInt(); c.drawText("Автономная симуляция • управление будет добавлено позже", 575f, 406f, paint)
    }
}
