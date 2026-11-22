package com.tribalage.game

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random
import org.json.JSONArray
import org.json.JSONObject

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
    var walkingPhase:Float=0f, var taskBuildingId:Int=-1
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
    val housingCapacity:Int get() = 5 + buildings.count { it.type==Structure.HUT } * 5
    val seasonName:String get() = listOf("Весна","Лето","Осень","Зима")[season.coerceIn(0,3)]
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
    private var guardVolley=0f
    var lastBattleText="Поселение спокойно"
        private set
    var completedMissions=0
        private set
    var unlockedTechnologies=mutableSetOf<String>()
        private set
    private var missionStage=0
    private var technologyClock=0f
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
            val winterTax=if(season==3) population/2 else 0
            stock.food=max(0,stock.food-population/2-winterTax)
            if(stock.food==0) {
                morale=(morale-12).coerceAtLeast(0)
                people.filter { it.role!=Role.GUARD }.forEach { it.health=(it.health-5f).coerceAtLeast(0f) }
                lastBattleText="Голод ослабляет жителей!"
            } else if(stock.food>population*3) {
                morale=(morale+2).coerceAtMost(100)
            }
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
        updateProgression(dt)
        guardVolley+=dt
        if(guardVolley>=0.85f) {
            guardVolley=0f
            val guards=people.filter{it.role==Role.GUARD && it.health>0f}
            val target=enemies.minByOrNull{enemy -> guards.minOfOrNull{distance(it.x,it.y,enemy.x,enemy.y)} ?: 999f}
            if(target!=null && guards.any{distance(it.x,it.y,target.x,target.y)<5.2f}) {
                target.health-=18f
                lastBattleText="Стража атакует налётчиков"
                if(target.health<=0f) { stock.food+=4; stock.hides+=1; lastBattleText="Враг повержен: +4 еды, +1 шкура" }
            }
        }
        val fallen=people.filter{it.health<=0f}
        if(fallen.isNotEmpty()) {
            population=max(0,population-fallen.size)
            people.removeAll(fallen.toSet())
            morale=(morale-fallen.size*9).coerceAtLeast(0)
            lastBattleText="Потери племени: "+fallen.size
        }
        growthClock+=dt
        if(growthClock>36f && population<housingCapacity && stock.food>100 && morale>55) {
            growthClock=0f
            population++
            people += Villager(nextId++,Role.GATHERER,11.5f,8.5f,11.5f,8.5f)
            lastBattleText="В племени родился новый житель"
        }
        stock.food=stock.food.coerceIn(0,if("Хранение запасов" in unlockedTechnologies)1199 else 999)
        stock.wood=stock.wood.coerceIn(0,if("Хранение запасов" in unlockedTechnologies)1199 else 999)
        stock.stone=stock.stone.coerceIn(0,if("Хранение запасов" in unlockedTechnologies)1199 else 999)
        stock.hides=stock.hides.coerceIn(0,999)
    }

    private fun updateWorkers(dt:Float) {
        for(p in people) {
            p.walkingPhase+=dt*7f
            if(p.role==Role.GUARD) {
                val target=enemies.filter { it.health>0f }
                    .minByOrNull { distance(p.x,p.y,it.x,it.y) }
                if(target!=null && distance(p.x,p.y,target.x,target.y)<7.0f) {
                    if(distance(p.x,p.y,target.x,target.y)>1.05f) move(p,target.x,target.y,dt,0.92f)
                } else {
                    val homeX=12.4f+(p.id%3)*0.45f
                    val homeY=9.3f
                    if(distance(p.x,p.y,homeX,homeY)>0.25f) move(p,homeX,homeY,dt)
                }
                continue
            }
            if(p.role==Role.BUILDER) {
                val damaged=buildings.filter { it.health<100f }.minByOrNull { distance(p.x,p.y,it.x,it.y) }
                if(damaged==null) {
                    p.taskBuildingId=-1
                    if(distance(p.x,p.y,11.5f,8.5f)>0.3f) move(p,11.5f,8.5f,dt)
                } else {
                    p.taskBuildingId=damaged.id
                    if(distance(p.x,p.y,damaged.x,damaged.y)>0.28f) move(p,damaged.x,damaged.y,dt)
                    else {
                        p.work+=dt
                        if(p.work>=1.4f) {
                            p.work=0f
                            damaged.health=(damaged.health+5f).coerceAtMost(100f)
                            lastBattleText="Строитель ремонтирует поселение"
                        }
                    }
                }
                continue
            }
            if(p.taskNode<0 || p.taskNode>=width*height) {
                val desired=when {
                p.role==Role.HUNTER -> NodeKind.DEER
                stock.food<population*4 -> NodeKind.BERRY
                stock.wood<stock.stone -> NodeKind.TREE
                else -> NodeKind.ROCK
            }
                val target=findNode(p.x,p.y,desired) ?: findAnyNode(p.x,p.y)
                if(target!=null) { p.taskNode=target.y*width+target.x; p.tx=target.x.toFloat(); p.ty=target.y.toFloat() }
                else { p.tx=11.5f; p.ty=8.5f }
            }
            val tile=tiles[p.taskNode/width][p.taskNode%width]
            if(p.carrying==0) {
                if(distance(p.x,p.y,p.tx,p.ty)>0.22f) move(p,p.tx,p.ty,dt)
                else {
                    p.work+=dt
                    if(p.work>if("Орудия труда" in unlockedTechnologies) 1.8f else 2.4f) {
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

    private fun updateProgression(dt:Float) {
        technologyClock+=dt
        when(missionStage) {
            0 -> if(stock.wood>=140 && stock.stone>=85) {
                missionStage=1; completedMissions++
                missionText="Задание выполнено: запасы материалов собраны"
                lastBattleText="Новая цель: постройте ещё одну хижину"
            }
            1 -> if(buildings.count{it.type==Structure.HUT}>=4) {
                missionStage=2; completedMissions++
                missionText="Задание выполнено: жильё расширено"
                lastBattleText="Новая цель: укрепите оборону и переживите набег"
            }
            2 -> if(raidCount>=1 && enemies.isEmpty()) {
                missionStage=3; completedMissions++
                missionText="Задание выполнено: первый набег отражён"
                lastBattleText="Новая цель: развивайте технологии племени"
            }
            3 -> if("Орудия труда" in unlockedTechnologies && "Хранение запасов" in unlockedTechnologies) {
                missionStage=4; completedMissions++
                missionText="Главы обучения пройдены. Племя готово к развитию"
            }
        }
        if("Орудия труда" !in unlockedTechnologies && day>=3 && stock.wood>=65 && stock.stone>=35 && technologyClock>=12f) {
            stock.wood-=25; stock.stone-=15
            unlockedTechnologies.add("Орудия труда")
            technologyClock=0f
            lastBattleText="Открыта технология: Орудия труда"
        } else if("Хранение запасов" !in unlockedTechnologies && day>=5 && stock.wood>=80 && stock.stone>=45 && technologyClock>=18f) {
            stock.wood-=30; stock.stone-=20
            unlockedTechnologies.add("Хранение запасов")
            technologyClock=0f
            lastBattleText="Открыта технология: Хранение запасов"
        }
    }

    private fun updateRaiders(dt:Float) {
        val targetX=11.5f
        val targetY=8.5f
        for(e in enemies) {
            val nearestWall=buildings.filter{it.type==Structure.PALISADE && it.health>0f}
                .minByOrNull{distance(it.x,it.y,e.x,e.y)}
            val target=nearestWall ?: buildings.filter{it.type!=Structure.PALISADE}
                .minByOrNull{distance(it.x,it.y,e.x,e.y)}
            val attackX=target?.x ?: targetX
            val attackY=target?.y ?: targetY
            if(distance(e.x,e.y,attackX,attackY)>0.72f) {
                move(e,attackX,attackY,dt,1.05f)
            } else {
                e.work+=dt
                if(e.work>2.1f) {
                    e.work=0f
                    val guard=people.filter{it.role==Role.GUARD && it.health>0f}
                        .minByOrNull{distance(it.x,it.y,e.x,e.y)}
                    if(guard!=null && distance(guard.x,guard.y,e.x,e.y)<1.6f) {
                        guard.health-=14f
                        e.health-=26f
                        lastBattleText="Стража сражается с налётчиками"
                    } else if(target!=null) {
                        target.health-=if(target.type==Structure.PALISADE) 7f else 9f
                        lastBattleText=if(target.type==Structure.PALISADE) "Налётчики ломают частокол" else "Враги атакуют строение"
                        if(target.health<=0f) {
                            buildings.remove(target)
                            morale=(morale-8).coerceAtLeast(0)
                            lastBattleText=if(target.type==Structure.PALISADE) "Участок частокола разрушен!" else "Строение разрушено!"
                        }
                    } else {
                        morale=(morale-4).coerceAtLeast(0)
                    }
                }
            }
        }
        enemies.removeAll{it.health<=0f}
    }

    private fun move(p:Villager,x:Float,y:Float,dt:Float,speed:Float=0.7f) {
        val dx=x-p.x; val dy=y-p.y
        val d=kotlin.math.sqrt(dx*dx+dy*dy)
        if(d<0.001f) return
        val step=min(d,speed*dt)
        p.x+=dx/d*step; p.y+=dy/d*step
    }
    private fun findNode(x:Float,y:Float,k:NodeKind):MapTile? =
        tiles.asSequence().flatMap { row -> row.asSequence() }.filter{it.node==k && it.amount>0}.minByOrNull{distance(x,y,it.x.toFloat(),it.y.toFloat())}
    private fun findAnyNode(x:Float,y:Float):MapTile? =
        tiles.asSequence().flatMap { row -> row.asSequence() }.filter{it.node!=null && it.amount>0}.minByOrNull{distance(x,y,it.x.toFloat(),it.y.toFloat())}
    private fun distance(x:Float,y:Float,xx:Float,yy:Float):Float {
        val dx=x-xx; val dy=y-yy
        return kotlin.math.sqrt(dx*dx+dy*dy)
    }

    fun visibleBuildings()=buildings.toList()

    fun construct(type:Structure):Boolean {
        if(type==Structure.GREAT_HALL) return false
        val cost=when(type) {
            Structure.HUT -> intArrayOf(35,24,8)
            Structure.GRANARY -> intArrayOf(25,30,12)
            Structure.WOODCAMP -> intArrayOf(12,28,4)
            Structure.QUARRY -> intArrayOf(10,20,16)
            Structure.HUNTER_CAMP -> intArrayOf(16,22,5)
            Structure.WATCHTOWER -> intArrayOf(8,32,20)
            Structure.BONFIRE -> intArrayOf(12,18,4)
            Structure.PALISADE -> intArrayOf(5,15,5)
            else -> return false
        }
        if(!stock.canAfford(cost[0],cost[1],cost[2])) {
            lastBattleText="Недостаточно ресурсов для строительства"
            return false
        }
        val slots=listOf(
            7.0f to 10.8f, 9.0f to 12.0f, 11.0f to 12.1f, 13.2f to 12.0f,
            15.5f to 10.8f, 16.4f to 7.8f, 15.4f to 4.2f, 12.7f to 3.5f,
            9.4f to 3.8f, 6.8f to 6.0f, 6.0f to 8.8f, 8.0f to 13.0f,
            11.0f to 13.5f, 14.0f to 13.0f, 17.0f to 11.0f
        )
        val spot=slots.firstOrNull { (x,y) ->
            buildings.none { distance(it.x,it.y,x,y)<1.15f } &&
            tiles[y.toInt().coerceIn(0,height-1)][x.toInt().coerceIn(0,width-1)].ground!=Ground.WATER
        }
        if(spot==null) {
            lastBattleText="Нет свободного места для постройки"
            return false
        }
        stock.food-=cost[0]; stock.wood-=cost[1]; stock.stone-=cost[2]
        buildings+=StructureState(nextId++,type,spot.first,spot.second,health=45f)
        lastBattleText="Начато строительство: ${structureName(type)}"
        return true
    }

    fun trainGuard():Boolean {
        val foodCost=18
        val hideCost=2
        if(stock.food<foodCost || stock.hides<hideCost || people.count{it.role==Role.GUARD}>=8) {
            lastBattleText="Не хватает еды/шкур или достигнут предел стражи"
            return false
        }
        val recruit=people.firstOrNull { it.role==Role.GATHERER }
        if(recruit==null) {
            lastBattleText="Нет свободного жителя для обучения"
            return false
        }
        stock.food-=foodCost
        stock.hides-=hideCost
        people[people.indexOf(recruit)]=recruit.copy(role=Role.GUARD,taskNode=-1,carrying=0)
        lastBattleText="Житель обучен и вступил в стражу"
        return true
    }

    private fun structureName(type:Structure)=when(type) {
        Structure.HUT -> "хижина"
        Structure.GRANARY -> "амбар"
        Structure.WOODCAMP -> "лесной лагерь"
        Structure.QUARRY -> "каменоломня"
        Structure.HUNTER_CAMP -> "охотничий лагерь"
        Structure.WATCHTOWER -> "сторожевая башня"
        Structure.BONFIRE -> "костёр"
        else -> "постройка"
    }
    fun saveState():String {
        val root=JSONObject()
        root.put("day",day); root.put("population",population); root.put("morale",morale)
        root.put("raidCount",raidCount); root.put("elapsed",elapsed); root.put("season",season)
        root.put("dayClock",dayClock); root.put("raidClock",raidClock); root.put("growthClock",growthClock)
        root.put("nextId",nextId); root.put("guardVolley",guardVolley); root.put("missionStage",missionStage)
        root.put("technologyClock",technologyClock); root.put("completedMissions",completedMissions)
        root.put("missionText",missionText); root.put("lastBattleText",lastBattleText)
        root.put("food",stock.food); root.put("wood",stock.wood); root.put("stone",stock.stone); root.put("hides",stock.hides)
        val tech=JSONArray(); unlockedTechnologies.forEach { tech.put(it) }; root.put("technologies",tech)
        val structures=JSONArray()
        buildings.forEach { b ->
            structures.put(JSONObject().put("id",b.id).put("type",b.type.name).put("x",b.x)
                .put("y",b.y).put("level",b.level).put("health",b.health))
        }
        root.put("buildings",structures)
        fun encodeUnits(units:List<Villager>):JSONArray {
            val array=JSONArray()
            units.forEach { v ->
                array.put(JSONObject().put("id",v.id).put("role",v.role.name).put("x",v.x).put("y",v.y)
                    .put("tx",v.tx).put("ty",v.ty).put("taskNode",v.taskNode).put("work",v.work)
                    .put("health",v.health).put("carrying",v.carrying).put("walkingPhase",v.walkingPhase)
                    .put("taskBuildingId",v.taskBuildingId))
            }
            return array
        }
        root.put("people",encodeUnits(people)); root.put("enemies",encodeUnits(enemies))
        val resources=JSONArray()
        tiles.forEach { row -> row.forEach { tile ->
            resources.put(JSONObject().put("amount",tile.amount))
        } }
        root.put("tileAmounts",resources)
        return root.toString()
    }

    fun loadState(data:String):Boolean = try {
        val root=JSONObject(data)
        day=root.optInt("day",1).coerceAtLeast(1)
        population=root.optInt("population",12).coerceAtLeast(0)
        morale=root.optInt("morale",86).coerceIn(0,100)
        raidCount=root.optInt("raidCount",0).coerceAtLeast(0)
        elapsed=root.optDouble("elapsed",0.0).toFloat().coerceAtLeast(0f)
        season=root.optInt("season",0).coerceIn(0,3)
        dayClock=root.optDouble("dayClock",0.0).toFloat().coerceIn(0f,48f)
        raidClock=root.optDouble("raidClock",95.0).toFloat().coerceAtLeast(0f)
        growthClock=root.optDouble("growthClock",0.0).toFloat().coerceAtLeast(0f)
        nextId=root.optInt("nextId",1).coerceAtLeast(1)
        guardVolley=root.optDouble("guardVolley",0.0).toFloat().coerceAtLeast(0f)
        missionStage=root.optInt("missionStage",0).coerceIn(0,4)
        technologyClock=root.optDouble("technologyClock",0.0).toFloat().coerceAtLeast(0f)
        completedMissions=root.optInt("completedMissions",0).coerceAtLeast(0)
        missionText=root.optString("missionText","Развивайте поселение и переживите набеги")
        lastBattleText=root.optString("lastBattleText","Поселение спокойно")
        stock.food=root.optInt("food",145).coerceAtLeast(0)
        stock.wood=root.optInt("wood",110).coerceAtLeast(0)
        stock.stone=root.optInt("stone",70).coerceAtLeast(0)
        stock.hides=root.optInt("hides",12).coerceAtLeast(0)
        unlockedTechnologies.clear()
        val tech=root.optJSONArray("technologies") ?: JSONArray()
        for(i in 0 until tech.length()) unlockedTechnologies.add(tech.getString(i))
        buildings.clear()
        val savedBuildings=root.getJSONArray("buildings")
        for(i in 0 until savedBuildings.length()) {
            val b=savedBuildings.getJSONObject(i)
            buildings.add(StructureState(b.getInt("id"),Structure.valueOf(b.getString("type")),
                b.getDouble("x").toFloat(),b.getDouble("y").toFloat(),b.optInt("level",1),
                b.optDouble("health",100.0).toFloat().coerceIn(0f,100f)))
        }
        fun decodeUnits(array:JSONArray):MutableList<Villager> {
            val result=mutableListOf<Villager>()
            for(i in 0 until array.length()) {
                val v=array.getJSONObject(i)
                result.add(Villager(v.getInt("id"),Role.valueOf(v.getString("role")),
                    v.getDouble("x").toFloat(),v.getDouble("y").toFloat(),
                    v.getDouble("tx").toFloat(),v.getDouble("ty").toFloat(),
                    v.optInt("taskNode",-1),v.optDouble("work",0.0).toFloat(),
                    v.optDouble("health",100.0).toFloat().coerceIn(0f,100f),
                    v.optInt("carrying",0),v.optDouble("walkingPhase",0.0).toFloat(),
                    v.optInt("taskBuildingId",-1)))
            }
            return result
        }
        people.clear(); people.addAll(decodeUnits(root.getJSONArray("people")))
        enemies.clear(); enemies.addAll(decodeUnits(root.getJSONArray("enemies")))
        val savedAmounts=root.optJSONArray("tileAmounts")
        if(savedAmounts!=null) {
            var index=0
            tiles.forEach { row -> row.forEach { tile ->
                if(index<savedAmounts.length()) tile.amount=savedAmounts.getJSONObject(index).optInt("amount",tile.amount).coerceAtLeast(0)
                index++
            } }
        }
        raidWarning=0f
        true
    } catch(_:Exception) { false }

    fun addStructure(type:Structure,x:Float,y:Float) {
        buildings+=StructureState(nextId++,type,x,y)
        if(type==Structure.HUT) lastBattleText="Построено новое жильё"
    }
}
