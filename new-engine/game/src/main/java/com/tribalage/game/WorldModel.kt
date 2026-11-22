package com.tribalage.game

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

enum class Ground { GRASS, DARK_GRASS, SAND, WATER, MUD }
enum class NodeKind { TREE, ROCK, BERRY, DEER }
enum class Role { GATHERER, HUNTER, BUILDER, GUARD, RAIDER }
enum class Structure { GREAT_HALL, HUT, GRANARY, WOODCAMP, QUARRY, HUNTER_CAMP, WATCHTOWER, BONFIRE, PALISADE }

data class MapTile(val x:Int, val y:Int, val ground:Ground, var node:NodeKind? = null, var amount:Int = 0)
data class Stock(var food:Int=145, var wood:Int=110, var stone:Int=70, var hides:Int=12) {
    fun canAfford(f:Int,w:Int,s:Int)=food>=f && wood>=w && stone>=s
}
data class StructureState(val id:Int, val type:Structure, val x:Float, val y:Float, var level:Int=1, var health:Float=100f)
data class Villager(
    val id:Int, val role:Role, var x:Float, var y:Float,
    var tx:Float, var ty:Float, var taskNode:Int=-1,
    var work:Float=0f, var health:Float=100f, var carrying:Int=0,
    var walkingPhase:Float=0f
)

class WorldModel {
    val width=24
    val height=16
    val tiles=Array(height) { y -> Array(width) { x -> makeTile(x,y) } }
    val buildings=mutableListOf<StructureState>()
    val people=mutableListOf<Villager>()
    val enemies=mutableListOf<Villager>()
    val stock=Stock()
    var day=1
        private set
    var population=12
        private set
    var morale=86
        private set
    var missionText="Развивайте поселение и переживите набеги"
    var raidWarning=0f
        private set
    var raidCount=0
        private set
    var elapsed=0f
        private set
    var season=0
        private set
    var firePulse=0f
        private set
    var selectedInfo="Племя Долины"
    private var dayClock=0f
    private var raidClock=95f
    private var growthClock=0f
    private var nextId=1
    private val rng=Random(38117)

    init {
        buildings += StructureState(nextId++,Structure.GREAT_HALL,11.5f,7.5f)
        buildings += StructureState(nextId++,Structure.HUT,9.4f,8.8f)
        buildings += StructureState(nextId++,Structure.HUT,13.3f,8.9f)
        buildings += StructureState(nextId++,Structure.HUT,12.3f,5.8f)
        buildings += StructureState(nextId++,Structure.GRANARY,10.2f,5.7f)
        buildings += StructureState(nextId++,Structure.WOODCAMP,7.0f,8.0f)
        buildings += StructureState(nextId++,Structure.QUARRY,15.2f,8.0f)
        buildings += StructureState(nextId++,Structure.HUNTER_CAMP,8.0f,5.5f)
        buildings += StructureState(nextId++,Structure.WATCHTOWER,14.5f,6.0f)
        buildings += StructureState(nextId++,Structure.BONFIRE,11.5f,9.6f)
        buildings += StructureState(nextId++,Structure.PALISADE,8.8f,7.8f)
        buildings += StructureState(nextId++,Structure.PALISADE,9.3f,10.2f)
        buildings += StructureState(nextId++,Structure.PALISADE,11.0f,11.0f)
        buildings += StructureState(nextId++,Structure.PALISADE,13.5f,10.6f)
        buildings += StructureState(nextId++,Structure.PALISADE,15.0f,8.4f)
        buildings += StructureState(nextId++,Structure.PALISADE,14.4f,5.2f)
        buildings += StructureState(nextId++,Structure.PALISADE,11.0f,4.4f)
        buildings += StructureState(nextId++,Structure.PALISADE,8.8f,5.1f)
        for (i in 0 until 7) {
            val x=10.4f+(i%4)*0.65f
            val y=9.9f+(i/4)*0.55f
            people += Villager(nextId++,if(i==0) Role.BUILDER else Role.GATHERER,x,y,x,y)
        }
        for (i in 0 until 3) people += Villager(nextId++,Role.HUNTER,8.5f+i*0.4f,6.6f,8.5f+i*0.4f,6.6f)
        for (i in 0 until 2) people += Villager(nextId++,Role.GUARD,12.4f+i*0.5f,9.4f,12.4f+i*0.5f,9.4f)
        for (i in 0 until 5) enemies += Villager(nextId++,Role.RAIDER,20f+(i%2)*0.6f,3.0f+(i/2)*0.7f,20f,3f)
    }

    private fun makeTile(x:Int,y:Int):MapTile {
        val r=Random(x*7919+y*104729+38117)
        val dx=x-11.5f
        val dy=y-7.5f
        val nearBase=abs(dx)<4.2f && abs(dy)<3.4f
        val river=abs(y-(2.0+0.11*x+0.7*kotlin.math.sin(x*0.42)))<0.72
        val ground=when {
            river && x<19 -> Ground.WATER
            !nearBase && r.nextInt(100)<9 -> Ground.SAND
            r.nextInt(100)<15 -> Ground.DARK_GRASS
            r.nextInt(100)<6 -> Ground.MUD
            else -> Ground.GRASS
        }
        val tile=MapTile(x,y,ground)
        if(ground!=Ground.WATER && !nearBase && x<20) {
            val chance=r.nextInt(100)
            when {
                chance<15 -> { tile.node=NodeKind.TREE; tile.amount=90+r.nextInt(90) }
                chance<19 -> { tile.node=NodeKind.ROCK; tile.amount=70+r.nextInt(80) }
                chance<23 -> { tile.node=NodeKind.BERRY; tile.amount=30+r.nextInt(30) }
                chance<25 -> { tile.node=NodeKind.DEER; tile.amount=45 }
            }
        }
        return tile
    }

    fun update(dtRaw:Float) {
        val dt=dtRaw.coerceIn(0f,0.05f)
        elapsed+=dt
        firePulse+=dt
        dayClock+=dt
        raidClock-=dt
        if(raidWarning>0f) raidWarning-=dt
        if(dayClock>=48f) {
            dayClock-=48f
            day++
            season=(day/6)%4
            morale=(morale+if(stock.food>40) 2 else -8).coerceIn(0,100)
            stock.food=max(0,stock.food-population/2)
            if(stock.food==0) morale=(morale-12).coerceAtLeast(0)
            missionText=when {
                day<3 -> "Соберите ресурсы и укрепите поселение"
                day<6 -> "Подготовьтесь к следующему набегу"
                else -> "Выживайте и расширяйте поселение"
            }
        }
        if(raidClock<=0f) {
            raidClock=125f
            raidCount++
            raidWarning=7f
            val count=min(3+raidCount,8)
            repeat(count) { i ->
                enemies += Villager(nextId++,Role.RAIDER,21f+(i%3)*0.45f,4.5f+(i/3)*0.45f,11.5f,8.5f)
            }
            missionText="НАБЕГ! Защитите поселение"
        }
        updateWorkers(dt)
        updateRaiders(dt)
        growthClock+=dt
        if(growthClock>36f && population<28 && stock.food>100 && morale>55) {
            growthClock=0f
            population++
            people += Villager(nextId++,Role.GATHERER,11.5f,8.5f,11.5f,8.5f)
        }
        if(stock.wood>0 && buildings.any{it.type==Structure.WOODCAMP}) stock.wood=min(999,stock.wood)
    }

    private fun updateWorkers(dt:Float) {
        for(p in people) {
            p.walkingPhase+=dt*7f
            if(p.role==Role.GUARD) {
                val homeX=12.4f+(p.id%3)*0.45f
                val homeY=9.3f
                if(distance(p.x,p.y,homeX,homeY)>0.25f) move(p,homeX,homeY,dt)
                continue
            }
            if(p.taskNode<0 || p.taskNode>=width*height) {
                val desired=if(p.role==Role.HUNTER) NodeKind.DEER else if(stock.wood<stock.stone) NodeKind.TREE else NodeKind.BERRY
                val target=findNode(p.x,p.y,desired) ?: findAnyNode(p.x,p.y)
                if(target!=null) { p.taskNode=target.y*width+target.x; p.tx=target.x.toFloat(); p.ty=target.y.toFloat() }
                else { p.tx=11.5f; p.ty=8.5f }
            }
            val tile=tiles[p.taskNode/width][p.taskNode%width]
            if(p.carrying==0) {
                if(distance(p.x,p.y,p.tx,p.ty)>0.22f) move(p,p.tx,p.ty,dt)
                else {
                    p.work+=dt
                    if(p.work>2.4f) {
                        p.work=0f
                        if(tile.amount>0) {
                            tile.amount-=1
                            p.carrying=if(tile.node==NodeKind.TREE) 3 else if(tile.node==NodeKind.ROCK) 2 else 2
                        } else p.taskNode=-1
                    }
                }
            } else {
                if(distance(p.x,p.y,11.5f,8.5f)>0.25f) move(p,11.5f,8.5f,dt)
                else {
                    when(tile.node) {
                        NodeKind.TREE -> stock.wood+=p.carrying
                        NodeKind.ROCK -> stock.stone+=p.carrying
                        NodeKind.DEER -> { stock.hides+=p.carrying; stock.food+=p.carrying }
                        else -> stock.food+=p.carrying
                    }
                    p.carrying=0
                    p.taskNode=-1
                }
            }
        }
    }

    private fun updateRaiders(dt:Float) {
        val targetX=11.5f
        val targetY=8.5f
        for(e in enemies) {
            if(distance(e.x,e.y,targetX,targetY)>0.65f) move(e,targetX,targetY,dt,1.05f)
            else {
                e.work+=dt
                if(e.work>2.1f) {
                    e.work=0f
                    val guard=people.filter{it.role==Role.GUARD}.minByOrNull{distance(it.x,it.y,e.x,e.y)}
                    if(guard!=null && distance(guard.x,guard.y,e.x,e.y)<1.6f) {
                        guard.health-=18f
                        e.health-=35f
                    } else {
                        val wall=buildings.firstOrNull{it.type==Structure.PALISADE}
                        if(wall!=null) wall.health-=8f else morale=(morale-4).coerceAtLeast(0)
                    }
                }
            }
        }
        enemies.removeAll{it.health<=0f || (it.x<12.4f && it.y>7.4f && it.y<10.5f && it.health<45f)}
    }

    private fun move(p:Villager,x:Float,y:Float,dt:Float,speed:Float=0.7f) {
        val dx=x-p.x; val dy=y-p.y
        val d=kotlin.math.sqrt(dx*dx+dy*dy)
        if(d<0.001f) return
        val step=min(d,speed*dt)
        p.x+=dx/d*step; p.y+=dy/d*step
    }
    private fun findNode(x:Float,y:Float,k:NodeKind):MapTile? =
        tiles.asSequence().flatten().filter{it.node==k && it.amount>0}.minByOrNull{distance(x,y,it.x.toFloat(),it.y.toFloat())}
    private fun findAnyNode(x:Float,y:Float):MapTile? =
        tiles.asSequence().flatten().filter{it.node!=null && it.amount>0}.minByOrNull{distance(x,y,it.x.toFloat(),it.y.toFloat())}
    private fun distance(x:Float,y:Float,xx:Float,yy:Float):Float {
        val dx=x-xx; val dy=y-yy
        return kotlin.math.sqrt(dx*dx+dy*dy)
    }

    fun visibleBuildings()=buildings.toList()
    fun addStructure(type:Structure,x:Float,y:Float) { buildings+=StructureState(nextId++,type,x,y) }
}
