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
import kotlin.math.max

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
        setContentView(TribalWorldView(this))
    }
}

private class TribalWorldView(context: android.content.Context) : SurfaceView(context), SurfaceHolder.Callback, Runnable {
    private val model=WorldModel()
    private val renderer=WorldRenderer()
    @Volatile private var running=false
    private var loop:Thread?=null
    private var lastTick=System.nanoTime()

    init { holder.addCallback(this); isFocusable=true }
    override fun surfaceCreated(holder:SurfaceHolder) {
        loadSavedGame()
        running=true
        lastTick=System.nanoTime()
        loop=Thread(this,"tribal-world-loop").also{it.start()}
    }
    override fun surfaceChanged(holder:SurfaceHolder,format:Int,width:Int,height:Int) {}
    override fun surfaceDestroyed(holder:SurfaceHolder) {
        saveGame()
        running=false
        try { loop?.join(700) } catch (_:InterruptedException) {}
    }
    private fun saveGame() {
        try {
            context.getSharedPreferences("tribal_age_save",android.content.Context.MODE_PRIVATE)
                .edit().putString("world",model.saveState()).apply()
        } catch (_:Exception) {}
    }

    private fun loadSavedGame() {
        try {
            val data=context.getSharedPreferences("tribal_age_save",android.content.Context.MODE_PRIVATE)
                .getString("world",null)
            if(data!=null && !model.loadState(data)) {
                context.getSharedPreferences("tribal_age_save",android.content.Context.MODE_PRIVATE)
                    .edit().remove("world").apply()
            }
        } catch (_:Exception) {}
    }

    override fun run() {
        while(running) {
            val now=System.nanoTime()
            val dt=((now-lastTick)/1_000_000_000.0).toFloat().coerceIn(0f,0.05f)
            lastTick=now
            model.update(dt)
            val canvas=try{holder.lockCanvas()}catch(_:Exception){null}
            if(canvas!=null) {
                renderer.draw(canvas,width,height,model)
                holder.unlockCanvasAndPost(canvas)
            }
            try{Thread.sleep(16)}catch(_:InterruptedException){}
        }
    }
}

private class WorldRenderer {
    private val p=Paint(Paint.ANTI_ALIAS_FLAG)
    private val tilePath=Path()
    private val baseW=960f
    private val baseH=432f
    private val tileW=35f
    private val tileH=18f
    private fun sx(x:Float,y:Float)=480f+(x-y)*17.5f
    private fun sy(x:Float,y:Float)=65f+(x+y)*9f

    fun draw(c:Canvas,w:Int,h:Int,world:WorldModel) {
        val scale=min(w/baseW,h/baseH)
        val ox=(w-baseW*scale)/2f
        val oy=(h-baseH*scale)/2f
        c.save(); c.translate(ox,oy); c.scale(scale,scale)
        c.drawColor(0xff192923.toInt())
        p.style=Paint.Style.FILL
        drawGround(c,world)
        drawDecor(c,world)
        drawEnemyCamp(c)
        drawBuildings(c,world)
        drawUnits(c,world)
        drawLighting(c,world)
        drawInterface(c,world)
        c.restore()
    }

    private fun drawGround(c:Canvas,world:WorldModel) {
        for(y in 0 until world.height) for(x in 0 until world.width) {
            val cx=sx(x.toFloat(),y.toFloat())
            val cy=sy(x.toFloat(),y.toFloat())
            val path=diamond(cx,cy,tileW,tileH)
            p.color=when(world.tiles[y][x].ground) {
                Ground.GRASS -> intArrayOf(0xff668d48.toInt(),0xff70964c.toInt(),0xff789c50.toInt(),0xff628342.toInt())[(x*7+y*11)%4]
                Ground.DARK_GRASS -> 0xff526f3d.toInt()
                Ground.SAND -> 0xffb4a06b.toInt()
                Ground.WATER -> 0xff397d8a.toInt()
                Ground.MUD -> 0xff776b4c.toInt()
            }
            c.drawPath(path,p)
            p.color=0x303b492e
            p.style=Paint.Style.STROKE; p.strokeWidth=0.8f
            c.drawPath(path,p); p.style=Paint.Style.FILL
            if(world.tiles[y][x].ground==Ground.WATER) {
                p.color=0x558dc5c5
                c.drawLine(cx-8f,cy-1f,cx+5f,cy-1f,p)
                if((x+y)%3==0)c.drawLine(cx-2f,cy+3f,cx+9f,cy+3f,p)
            }
        }
    }

    private fun diamond(cx:Float,cy:Float,ww:Float,hh:Float)=Path().apply {
        moveTo(cx,cy-hh); lineTo(cx+ww,cy); lineTo(cx,cy+hh); lineTo(cx-ww,cy); close()
    }

    private fun drawDecor(c:Canvas,world:WorldModel) {
        for(y in 0 until world.height) for(x in 0 until world.width) {
            val tile=world.tiles[y][x]
            val cx=sx(x.toFloat(),y.toFloat())
            val cy=sy(x.toFloat(),y.toFloat())
            when(tile.node) {
                NodeKind.TREE -> drawTree(c,cx,cy,(x*3+y)%2==0,if(tile.amount<30)0.55f else 1f)
                NodeKind.ROCK -> drawRock(c,cx+2f,cy-1f)
                NodeKind.BERRY -> drawBerry(c,cx,cy)
                NodeKind.DEER -> drawDeer(c,cx,cy)
                null -> if(tile.ground!=Ground.WATER && (x*17+y*29)%19==0) {
                    p.color=0xffb5bd75.toInt(); c.drawOval(cx-1f,cy-4f,cx+2f,cy-1f,p)
                }
            }
        }
    }

    private fun drawTree(c:Canvas,x:Float,y:Float,pine:Boolean,health:Float) {
        p.color=0x5533281d; c.drawOval(x-9f,y-1f,x+10f,y+6f,p)
        p.color=0xff59432c.toInt(); c.drawRoundRect(x-3f,y-19f,x+3f,y+3f,2f,2f,p)
        val r=14f*health
        p.color=if(pine)0xff315c38.toInt() else 0xff47763c.toInt()
        c.drawCircle(x,y-23f,r,p); c.drawCircle(x-8f,y-18f,r*.68f,p); c.drawCircle(x+8f,y-17f,r*.7f,p)
        p.color=0x447fa65a; c.drawCircle(x-4f,y-28f,r*.42f,p)
        if(!pine) { p.color=0xff648c46.toInt(); c.drawCircle(x+4f,y-26f,r*.42f,p) }
    }
    private fun drawRock(c:Canvas,x:Float,y:Float) {
        p.color=0x55332922; c.drawOval(x-11f,y-1f,x+11f,y+5f,p)
        val path=Path().apply{moveTo(x-12f,y);lineTo(x-8f,y-10f);lineTo(x-1f,y-15f);lineTo(x+8f,y-12f);lineTo(x+13f,y-2f);lineTo(x+6f,y+2f);close()}
        p.color=0xff777c72.toInt();c.drawPath(path,p)
        p.color=0xffaeb1a1.toInt();c.drawLine(x-6f,y-9f,x-1f,y-13f,p);c.drawLine(x-1f,y-13f,x+3f,y-12f,p)
    }
    private fun drawBerry(c:Canvas,x:Float,y:Float) {
        p.color=0xff416a37.toInt();c.drawCircle(x-5f,y-5f,7f,p);c.drawCircle(x+3f,y-7f,7f,p);c.drawCircle(x+8f,y-2f,5f,p)
        p.color=0xffa8433b.toInt();c.drawCircle(x-5f,y-7f,2f,p);c.drawCircle(x+4f,y-9f,2f,p);c.drawCircle(x+8f,y-3f,2f,p)
    }
    private fun drawDeer(c:Canvas,x:Float,y:Float) {
        p.color=0x55332922;c.drawOval(x-11f,y+1f,x+13f,y+5f,p)
        p.color=0xff9a7047.toInt();c.drawOval(x-9f,y-10f,x+9f,y-2f,p);c.drawCircle(x+8f,y-10f,4f,p)
        p.color=0xff6d4b30.toInt();c.drawLine(x-5f,y-3f,x-7f,y+5f,p);c.drawLine(x+5f,y-3f,x+7f,y+5f,p)
        p.color=0xffe3d3ad.toInt();c.drawLine(x+8f,y-14f,x+11f,y-20f,p);c.drawLine(x+8f,y-14f,x+5f,y-20f,p)
    }

    private fun drawEnemyCamp(c:Canvas) {
        val x=sx(20.5f,2.5f)
        val y=sy(20.5f,2.5f)
        p.color=0x5533261b;c.drawOval(x-38f,y,x+38f,y+9f,p)
        p.color=0xff684b35.toInt()
        c.drawLine(x-30f,y+1f,x-4f,y-33f,p);c.drawLine(x+28f,y+1f,x-4f,y-33f,p)
        c.drawLine(x-30f,y+1f,x+28f,y+1f,p)
        p.color=0xff9a7147.toInt()
        val tent=Path().apply{moveTo(x-30f,y+1f);lineTo(x-4f,y-33f);lineTo(x+28f,y+1f);close()}
        c.drawPath(tent,p)
        p.color=0xff603e2c.toInt();c.drawRect(x-5f,y-15f,x+5f,y+1f,p)
        p.color=0xff8e3c31.toInt();c.drawLine(x+18f,y-31f,x+18f,y-55f,p)
        val flag=Path().apply{moveTo(x+18f,y-55f);lineTo(x+38f,y-48f);lineTo(x+18f,y-42f);close()}
        c.drawPath(flag,p)
        p.color=0xffb99b6c.toInt();c.drawCircle(x-34f,y-4f,4f,p);c.drawCircle(x+34f,y-4f,4f,p)
    }

    private fun drawBuildings(c:Canvas,world:WorldModel) {
        for(b in world.visibleBuildings().sortedBy{it.x+it.y}) {
            val x=sx(b.x,b.y); val y=sy(b.x,b.y)
            when(b.type) {
                Structure.GREAT_HALL -> drawGreatHall(c,x,y)
                Structure.HUT -> drawHut(c,x,y,1f)
                Structure.GRANARY -> drawGranary(c,x,y)
                Structure.WOODCAMP -> drawWoodcamp(c,x,y)
                Structure.QUARRY -> drawQuarry(c,x,y)
                Structure.HUNTER_CAMP -> drawHunterCamp(c,x,y)
                Structure.WATCHTOWER -> drawTower(c,x,y)
                Structure.BONFIRE -> drawFire(c,x,y,world.firePulse)
                Structure.PALISADE -> drawPalisade(c,x,y)
            }
            if(b.health<95f) {
                p.color=0xff401f1b.toInt();c.drawRect(x-13f,y-42f,x+13f,y-39f,p)
                p.color=0xffbd4b36.toInt();c.drawRect(x-13f,y-42f,x-13f+26f*(b.health/100f),y-39f,p)
            }
        }
    }
    private fun drawHut(c:Canvas,x:Float,y:Float,s:Float) {
        c.save();c.translate(x,y);c.scale(s,s)
        p.color=0x5533261b;c.drawOval(-27f,-2f,27f,8f,p)
        p.color=0xff8a6139.toInt();c.drawRect(-20f,-23f,20f,3f,p)
        val roof=Path().apply{moveTo(-27f,-20f);lineTo(0f,-46f);lineTo(27f,-20f);close()}
        p.color=0xffa87948.toInt();c.drawPath(roof,p)
        p.color=0xffd0a96a.toInt();c.drawLine(-20f,-20f,0f,-40f,p);c.drawLine(0f,-40f,20f,-20f,p)
        p.color=0xff523a29.toInt();c.drawRect(-5f,-15f,5f,3f,p)
        p.color=0xffc4a06a.toInt();c.drawLine(-19f,-3f,19f,-3f,p)
        c.restore()
    }
    private fun drawGreatHall(c:Canvas,x:Float,y:Float) {
        p.color=0x6633281e;c.drawOval(x-36f,y-3f,x+36f,y+9f,p)
        p.color=0xff775035.toInt();c.drawRect(x-28f,y-31f,x+28f,y+4f,p)
        val roof=Path().apply{moveTo(x-35f,y-29f);lineTo(x,y-61f);lineTo(x+35f,y-29f);close()}
        p.color=0xffb68a4f.toInt();c.drawPath(roof,p)
        p.color=0xffd2ad70.toInt();c.drawLine(x-27f,y-29f,x,y-55f,p);c.drawLine(x,y-55f,x+27f,y-29f,p)
        p.color=0xff493426.toInt();c.drawRect(x-7f,y-21f,x+7f,y+4f,p)
        p.color=0xffd9bc85.toInt();c.drawRect(x-31f,y+1f,x+31f,y+4f,p)
        p.color=0xffe7d5a8.toInt();c.drawCircle(x,y-39f,3f,p)
    }
    private fun drawGranary(c:Canvas,x:Float,y:Float) {
        p.color=0x5533261b;c.drawOval(x-20f,y,x+20f,y+6f,p)
        p.color=0xffa47b48.toInt();c.drawRect(x-15f,y-24f,x+15f,y+3f,p)
        val roof=Path().apply{moveTo(x-20f,y-22f);lineTo(x,y-39f);lineTo(x+20f,y-22f);close()}
        p.color=0xffd1aa65.toInt();c.drawPath(roof,p)
        p.color=0xff5a3d28.toInt();c.drawRect(x-4f,y-13f,x+4f,y+3f,p)
        p.color=0xffe1c18a.toInt();c.drawLine(x-12f,y-4f,x+12f,y-4f,p)
    }
    private fun drawWoodcamp(c:Canvas,x:Float,y:Float) {
        p.color=0x5533261b;c.drawOval(x-21f,y,x+22f,y+6f,p)
        p.color=0xff765132.toInt();c.drawRect(x-16f,y-14f,x+16f,y+2f,p)
        p.color=0xffa17a49.toInt();c.drawRect(x-19f,y-18f,x+19f,y-13f,p)
        p.color=0xff604329.toInt();c.drawLine(x-13f,y-19f,x+12f,y-19f,p);c.drawLine(x-10f,y-23f,x+14f,y-23f,p)
        p.color=0xffb18b59.toInt();c.drawRect(x-12f,y-12f,x+12f,y-9f,p)
    }
    private fun drawQuarry(c:Canvas,x:Float,y:Float) {
        drawRock(c,x-6f,y-2f);drawRock(c,x+9f,y-5f)
        p.color=0xff715338.toInt();c.drawRect(x-15f,y-10f,x+16f,y-7f,p)
        p.color=0xffc2a06b.toInt();c.drawLine(x-10f,y-13f,x+12f,y-13f,p)
    }
    private fun drawHunterCamp(c:Canvas,x:Float,y:Float) {
        p.color=0x5533261b;c.drawOval(x-20f,y,x+20f,y+6f,p)
        p.color=0xff67462e.toInt();c.drawLine(x-14f,y-3f,x,y-30f,p);c.drawLine(x+14f,y-3f,x,y-30f,p)
        p.color=0xffb18b58.toInt();c.drawLine(x-14f,y-3f,x+14f,y-3f,p)
        p.color=0xff8d6b45.toInt();c.drawLine(x-10f,y-8f,x+10f,y-8f,p)
    }
    private fun drawTower(c:Canvas,x:Float,y:Float) {
        p.color=0x5533261b;c.drawOval(x-17f,y,x+17f,y+6f,p)
        p.color=0xff805b39.toInt();c.drawRect(x-12f,y-39f,x+12f,y+2f,p)
        p.color=0xffbd9257.toInt();c.drawRect(x-17f,y-43f,x+17f,y-36f,p)
        p.color=0xff573b28.toInt();c.drawRect(x-3f,y-28f,x+3f,y-18f,p)
        p.color=0xffd6b77d.toInt();c.drawLine(x-10f,y-35f,x+10f,y-35f,p)
    }
    private fun drawFire(c:Canvas,x:Float,y:Float,t:Float) {
        p.color=0x6633281d;c.drawOval(x-17f,y,x+17f,y+7f,p)
        p.color=0xff5c402b.toInt();c.drawLine(x-13f,y+2f,x+13f,y-2f,p);c.drawLine(x-11f,y-2f,x+12f,y+3f,p)
        val f=sin(t*5f)*2f
        p.color=0xffe18a32.toInt();c.drawOval(x-7f,y-19f+f,x+7f,y+3f,p)
        p.color=0xffffd56d.toInt();c.drawOval(x-3f,y-13f+f,x+3f,y+1f,p)
        p.color=0x66ffb84c;c.drawCircle(x,y-10f,14f+sin(t*3f)*2f,p)
    }
    private fun drawPalisade(c:Canvas,x:Float,y:Float) {
        p.color=0xff6d4b2f.toInt()
        for(i in -2..2) {
            val xx=x+i*7f
            c.drawRect(xx-2f,y-23f,xx+2f,y+3f,p)
            val tip=Path().apply{moveTo(xx-2f,y-22f);lineTo(xx,y-29f);lineTo(xx+2f,y-22f);close()}
            c.drawPath(tip,p)
        }
        p.color=0xffb08a56.toInt();c.drawRect(x-18f,y-13f,x+18f,y-9f,p)
    }

    private fun drawUnits(c:Canvas,world:WorldModel) {
        for(unit in world.people.sortedBy{it.x+it.y}) drawPerson(c,sx(unit.x,unit.y),sy(unit.x,unit.y),unit.walkingPhase,false,unit.role,unit.health)
        for(unit in world.enemies.sortedBy{it.x+it.y}) drawPerson(c,sx(unit.x,unit.y),sy(unit.x,unit.y),unit.walkingPhase,true,unit.role,unit.health)
    }
    private fun drawPerson(c:Canvas,x:Float,y:Float,phase:Float,enemy:Boolean,role:Role,hp:Float) {
        val bob=sin(phase)*1.4f
        p.color=0x5533261b;c.drawOval(x-7f,y+4f,x+8f,y+8f,p)
        p.color=if(enemy)0xffa34235.toInt() else if(role==Role.GUARD)0xff9b7b48.toInt() else 0xff765038.toInt()
        c.drawRect(x-4f,y-3f+bob,x+4f,y+5f+bob,p)
        p.color=if(enemy)0xffc17c59.toInt() else 0xffc58d5e.toInt()
        c.drawCircle(x,y-9f+bob,4.2f,p)
        p.color=0xff3d2b22.toInt();c.drawArc(x-4.5f,y-14f+bob,x+4.5f,y-5f+bob,180f,180f,true,p)
        p.color=0xff4b3526.toInt()
        c.drawLine(x-2f,y+3f+bob,x-3f,y+9f+bob,p);c.drawLine(x+2f,y+3f+bob,x+3f,y+9f+bob,p)
        if(role==Role.GUARD || enemy) {
            p.color=0xff8a8a7a.toInt();c.drawLine(x+4f,y-2f+bob,x+10f,y-16f+bob,p)
            p.color=0xff68472c.toInt();c.drawLine(x+4f,y-1f+bob,x+8f,y+4f+bob,p)
        } else {
            p.color=0xffa88b62.toInt();c.drawLine(x+3f,y-2f+bob,x+8f,y-9f+bob,p)
        }
        if(hp<95f) {
            p.color=0xff3e2521.toInt();c.drawRect(x-6f,y-20f,x+6f,y-18f,p)
            p.color=0xffb64a37.toInt();c.drawRect(x-6f,y-20f,x-6f+12f*(hp/100f),y-18f,p)
        }
    }

    private fun drawLighting(c:Canvas,world:WorldModel) {
        val t=(world.elapsed%48f)/48f
        val darkness=if(t<0.25f || t>0.78f)0.26f else 0f
        if(darkness>0f) {
            p.color=(darkness*255).toInt().shl(24) or 0x00101c35
            c.drawRect(0f,54f,960f,380f,p)
        }
        if(world.raidWarning>0f) {
            val pulse=(sin(world.elapsed*8f)*0.5f+0.5f)
            p.color=(0x30+pulse*35).toInt().shl(24) or 0x00d94b32
            c.drawRect(0f,55f,960f,378f,p)
        }
    }

    private fun drawInterface(c:Canvas,world:WorldModel) {
        p.style=Paint.Style.FILL
        p.color=0xe51b2921.toInt();c.drawRoundRect(12f,8f,948f,51f,8f,8f,p)
        p.color=0xffd7c18c.toInt();p.typeface=Typeface.create("sans-serif-condensed",Typeface.BOLD);p.textSize=16f
        c.drawText("ПЛЕМЯ ДОЛИНЫ",25f,25f,p)
        p.textSize=10f;p.color=0xffa9b48c.toInt();c.drawText("ПОСЕЛЕНИЕ",26f,39f,p)
        p.textSize=14f;p.color=0xffe7d6ac.toInt()
        c.drawText("ДЕНЬ ${world.day} · ${world.seasonName}",180f,34f,p)
        drawResource(c,280f,30f,"ЕДА",world.stock.food,0xffd7b35e.toInt())
        drawResource(c,390f,30f,"ДЕРЕВО",world.stock.wood,0xffad7746.toInt())
        drawResource(c,525f,30f,"КАМЕНЬ",world.stock.stone,0xffaab0a5.toInt())
        drawResource(c,660f,30f,"ШКУРЫ",world.stock.hides,0xffb98c65.toInt())
        c.drawText("ЛЮДИ ${world.population}/${world.housingCapacity}",805f,34f,p)
        p.color=0xe51b2921.toInt();c.drawRoundRect(12f,382f,948f,423f,8f,8f,p)
        p.color=0xffe7d6ac.toInt();p.textSize=13f;c.drawText(world.missionText,27f,400f,p)
        p.color=0xffa9c38a.toInt();p.textSize=10f
        c.drawText("Жители: ${world.people.size}   Жильё: ${world.housingCapacity}   Стража: ${world.people.count{it.role==Role.GUARD}}   Набеги: ${world.raidCount}   Боевой дух: ${world.morale}",27f,415f,p)
        p.color=0xffd6a07a.toInt()
        c.drawText(world.lastBattleText,560f,415f,p)
        if(world.raidWarning>0f) {
            p.color=0xffffd18a.toInt();p.textSize=20f;p.typeface=Typeface.DEFAULT_BOLD
            c.drawText("ВРАЖЕСКИЙ НАБЕГ!",370f,80f,p)
        }
        p.color=0x99202d25.toInt();c.drawRoundRect(780f,55f,944f,78f,5f,5f,p)
        p.color=0xffd5c49c.toInt();p.textSize=10f;p.typeface=Typeface.DEFAULT
        c.drawText("АВТОНОМНАЯ СИМУЛЯЦИЯ",790f,70f,p)
    }
    private fun drawResource(c:Canvas,x:Float,y:Float,label:String,value:Int,color:Int) {
        p.color=color;c.drawCircle(x,y-4f,5f,p)
        p.color=0xffe7d6ac.toInt();p.textSize=12f;p.typeface=Typeface.DEFAULT_BOLD
        c.drawText("$label  $value",x+10f,y,p)
    }
}
