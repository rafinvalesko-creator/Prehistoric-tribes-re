package com.ancienttribes.game
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import kotlin.math.hypot
import kotlin.random.Random
private data class TribeUnit(var x:Float,var y:Float,var tx:Float,var ty:Float,val kind:Int=0,var selected:Boolean=false)
private data class Tree(val x:Float,val y:Float,val r:Float)
class TribeGameView(context:Context):View(context){
 private val p=Paint(Paint.ANTI_ALIAS_FLAG)
 private val units=mutableListOf<TribeUnit>()
 private val trees=mutableListOf<Tree>()
 private var food=120; private var wood=80; private var stone=25
 private var selected:TribeUnit?=null
 private var message="Коснись воина, затем укажи место"
 private var messageUntil=0L
 private var last=System.nanoTime()
 private var cameraX=0f; private var cameraY=0f
 private val worldW=1800f; private val worldH=1200f
 private val hut=RectF(770f,490f,940f,620f)
 private val map=RectF()
 private val buttons=mutableListOf<Pair<RectF,String>>()
 private var downX=0f; private var downY=0f; private var dragging=false
 private val rnd=Random(14)
 init{
  repeat(115){trees.add(Tree(50f+rnd.nextFloat()*1700f,80f+rnd.nextFloat()*1040f,12f+rnd.nextFloat()*13f))}
  units.add(TribeUnit(830f,660f,830f,660f));units.add(TribeUnit(890f,675f,890f,675f));units.add(TribeUnit(955f,650f,955f,650f));units.add(TribeUnit(800f,700f,800f,700f,1))
  isFocusable=true
 }
 override fun onDraw(c:Canvas){
  super.onDraw(c)
  val w=width.toFloat();val h=height.toFloat();val hudH=h*.14f
  map.set(0f,hudH,w,h-h*.13f)
  val scale=minOf(map.width()/worldW,map.height()/worldH)
  val ox=(map.width()-worldW*scale)/2f;val oy=map.top+(map.height()-worldH*scale)/2f
  c.save();c.clipRect(map);c.drawColor(0xff648b45.toInt())
  c.translate(map.left+ox-cameraX*scale,map.top+oy-cameraY*scale);c.scale(scale,scale);drawWorld(c);c.restore()
  drawHud(c,w,h,hudH)
  if(System.currentTimeMillis()<messageUntil){p.color=0xee17251a.toInt();c.drawRoundRect(w*.27f,h*.82f,w*.73f,h*.91f,18f,18f,p);text(c,message,w*.5f,h*.875f,18f,0xffffffff.toInt(),true)}
  val now=System.nanoTime();val dt=((now-last)/1e9).toFloat().coerceIn(0f,.05f);last=now
  units.forEach{u->val dx=u.tx-u.x;val dy=u.ty-u.y;val d=hypot(dx,dy);if(d>2f){val step=minOf(d,95f*dt);u.x+=dx/d*step;u.y+=dy/d*step}}
  postInvalidateOnAnimation()
 }
 private fun drawWorld(c:Canvas){
  p.color=0xff729b50.toInt();c.drawRect(0f,0f,worldW,worldH,p)
  repeat(32){i->val x=((i*317)%1680+40).toFloat();val y=((i*191)%1080+40).toFloat();p.color=if(i%2==0)0x227b5b35 else 0x227fc56b;c.drawOval(x,y,x+100f,y+45f,p)}
  p.color=0xffb9a071.toInt();p.strokeWidth=42f;p.style=Paint.Style.STROKE
  val path=Path().apply{moveTo(900f,560f);cubicTo(1050f,610f,1120f,770f,1280f,810f);moveTo(860f,550f);cubicTo(700f,420f,520f,390f,380f,250f)}
  c.drawPath(path,p);p.style=Paint.Style.FILL
  trees.forEach{t->p.color=0xff65452c.toInt();c.drawRect(t.x-4,t.y,t.x+4,t.y+18,p);p.color=0xff285b32.toInt();c.drawCircle(t.x,t.y,t.r,p);p.color=0xff4d8540.toInt();c.drawCircle(t.x-5,t.y-5,t.r*.55f,p)}
  p.color=0xff777b79.toInt();c.drawCircle(330f,820f,34f,p);c.drawCircle(355f,800f,24f,p);c.drawCircle(310f,800f,20f,p)
  p.color=0xff386e35.toInt();c.drawCircle(1430f,300f,35f,p)
  repeat(8){i->p.color=0xffb93d46.toInt();c.drawCircle(1410f+(i%4)*13f,285f+(i/4)*18f,4f,p)}
  p.color=0x55000000;c.drawOval(hut.left,hut.bottom-9,hut.right,hut.bottom+10,p)
  p.color=0xffa47b4b.toInt();c.drawRect(hut.left+20,hut.top+35,hut.right-20,hut.bottom,p)
  p.color=0xff765032.toInt();val roof=Path().apply{moveTo(hut.left,hut.top+40);lineTo(hut.centerX(),hut.top-25);lineTo(hut.right,hut.top+40);close()};c.drawPath(roof,p)
  p.color=0xffd9bd83.toInt();c.drawRect(hut.centerX()-13,hut.bottom-48,hut.centerX()+13,hut.bottom,p)
  text(c,"СТАН",hut.centerX(),hut.top+72,22f,0xffffffff.toInt(),true)
  units.forEach{u->
   p.color=0x55000000;c.drawOval(u.x-17,u.y+12,u.x+17,u.y+22,p)
   p.color=if(u.kind==1)0xffa6a6a6.toInt() else 0xffd7b17a.toInt();c.drawCircle(u.x,u.y,14f,p)
   p.color=if(u.kind==1)0xff5b626a.toInt() else 0xff8c4b2b.toInt();c.drawRect(u.x-9,u.y+8,u.x+9,u.y+23,p)
   p.color=0xff362b20.toInt();c.drawCircle(u.x,u.y-2,7f,p)
   if(u.selected){p.color=0xffffff55.toInt();p.style=Paint.Style.STROKE;p.strokeWidth=3f;c.drawCircle(u.x,u.y+2,23f,p);p.style=Paint.Style.FILL}
  }
  p.color=0x88604b30.toInt();p.style=Paint.Style.STROKE;p.strokeWidth=8f;c.drawRect(4f,4f,worldW-4,worldH-4,p);p.style=Paint.Style.FILL
 }
 private fun drawHud(c:Canvas,w:Float,h:Float,top:Float){
  p.color=0xff26382b.toInt();c.drawRect(0f,0f,w,top,p);p.color=0xff344a36.toInt();c.drawRect(0f,h-h*.13f,w,h,p)
  text(c,"ДРЕВНИЕ ПЛЕМЕНА",24f,top*.43f,20f,0xfff2dfb4.toInt(),true)
  text(c,"Еда  $food",w*.43f,top*.43f,18f,0xffffe4a6.toInt(),true);text(c,"Дерево  $wood",w*.58f,top*.43f,18f,0xffd9bd83.toInt(),true);text(c,"Камень  $stone",w*.75f,top*.43f,18f,0xffd2d5d3.toInt(),true)
  buttons.clear();listOf("Рабочий","Воин","Дом").forEachIndexed{i,s->
   val r=RectF(w*.35f+i*w*.11f,h-h*.105f,w*.35f+i*w*.11f+w*.095f,h-h*.025f);buttons.add(r to s)
   p.color=if(i==1)0xff8a623e.toInt() else 0xff4c6846.toInt();c.drawRoundRect(r,10f,10f,p);p.color=0xffc8b18a.toInt();p.style=Paint.Style.STROKE;p.strokeWidth=2f;c.drawRoundRect(r,10f,10f,p);p.style=Paint.Style.FILL
   text(c,s,r.centerX(),r.centerY()+6f,16f,0xffffffff.toInt(),true)
  }
  text(c,"Выбери отряд и коснись земли для движения",w*.5f,h-h*.145f,14f,0xffe6e4d8.toInt(),false)
 }
 private fun text(c:Canvas,s:String,x:Float,y:Float,size:Float,color:Int,bold:Boolean){p.color=color;p.textSize=size;p.typeface=android.graphics.Typeface.create("sans-serif",if(bold)1 else 0);p.textAlign=Paint.Align.CENTER;c.drawText(s,x,y,p)}
 override fun onTouchEvent(e:MotionEvent):Boolean{
  val w=width.toFloat();val h=height.toFloat();val top=h*.14f
  when(e.action){
   MotionEvent.ACTION_DOWN->{downX=e.x;downY=e.y;dragging=false;return true}
   MotionEvent.ACTION_MOVE->{if(hypot(e.x-downX,e.y-downY)>22f)dragging=true;return true}
   MotionEvent.ACTION_UP->{
    if(dragging&&downY>top&&downY<h*.87f){cameraX=(cameraX+(downX-e.x)*1.5f).coerceIn(0f,worldW*.55f);cameraY=(cameraY+(downY-e.y)*1.5f).coerceIn(0f,worldH*.55f);return true}
    if(e.y>h*.87f){buttons.firstOrNull{it.first.contains(e.x,e.y)}?.let{action(it.second)};return true}
    if(e.y<top||e.y>h*.87f)return true
    val scale=minOf(w/worldW,(h-top-h*.13f)/worldH);val ox=(w-worldW*scale)/2f;val oy=top+((h-top-h*.13f)-worldH*scale)/2f
    val wx=(e.x-ox)/scale+cameraX;val wy=(e.y-oy)/scale+cameraY
    val hit=units.minByOrNull{hypot(it.x-wx,it.y-wy)}
    if(hit!=null&&hypot(hit.x-wx,hit.y-wy)<45f){units.forEach{it.selected=false};hit.selected=true;selected=hit;notify("Отряд выбран")}
    else if(selected!=null){selected!!.tx=wx.coerceIn(20f,worldW-20f);selected!!.ty=wy.coerceIn(20f,worldH-20f);notify("Отряд получил приказ")}
    return true
   }
  }
  return true
 }
 private fun action(name:String){when(name){
  "Рабочий"->{if(food>=40){food-=40;units.add(TribeUnit(900f,650f,900f,650f));notify("Новый рабочий готов")}else notify("Не хватает еды")}
  "Воин"->{if(food>=55&&wood>=20){food-=55;wood-=20;units.add(TribeUnit(900f,650f,900f,650f,1));notify("Воин присоединился к племени")}else notify("Нужно 55 еды и 20 дерева")}
  "Дом"->{if(wood>=60){wood-=60;notify("Место для нового жилища отмечено")}else notify("Нужно 60 дерева")}
 }}
 private fun notify(s:String){message=s;messageUntil=System.currentTimeMillis()+1800}
}
