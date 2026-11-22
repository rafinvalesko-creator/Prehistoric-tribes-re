package com.tribes.nativegame;

import android.app.Activity;
import android.os.Bundle;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends Activity {
    private GameView game;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(5894 | 1024 | 512 | 4096);
        game = new GameView(this);
        setContentView(game);
    }
    @Override protected void onPause() { if (game != null) game.save(); super.onPause(); }
    @Override public void onBackPressed() { if (game != null) { game.save(); game.togglePause(); } }

    static final class GameView extends View {
        static final float VW=1280, VH=720;
        final Paint p=new Paint(3);
        final Random random=new Random(17);
        final ArrayList<Person> people=new ArrayList<>();
        final ArrayList<Node> nodes=new ArrayList<>();
        final ArrayList<Building> buildings=new ArrayList<>();
        final ArrayList<UiButton> buttons=new ArrayList<>();
        final SharedPreferences prefs;
        float scale=1, ox=0, oy=0, lastX, lastY, downX, downY, camX=0, camY=0;
        boolean dragging=false, buildMenu=false, paused=false;
        int selected=-1, buildChoice=-1, food=90, wood=55, stone=25, tools=8, day=1;
        float dayTime=0, eventTimer=0, saveTimer=0;
        String notice="Племя просыпается. Назначь людям работу!";
        String[] buildNames={"Хижина","Склад","Кухня","Дом вождя","Тренировочная площадка","Тир","Сторожевая вышка","Тотем","Рыбацкая хижина","Мастерская","Ясли","Амбар","Хижина шамана"};
        int[] buildFood={0,0,10,0,0,0,0,10,5,0,15,0,10};
        int[] buildWood={18,24,20,40,30,24,35,16,22,30,26,20,28};
        int[] buildStone={4,8,6,12,10,8,15,12,4,12,4,8,18};
        String[] tasks={"Собирать ягоды","Рубить дерево","Добывать камень","Ловить рыбу","Охотиться","Строить","Отдыхать","Свободен"};
        long lastFrame=System.nanoTime();

        GameView(Context c) {
            super(c); prefs=c.getSharedPreferences("tribe_save",0);
            setLayerType(View.LAYER_TYPE_SOFTWARE,null);
            load();
            if (people.isEmpty()) newGame(); else if (nodes.isEmpty()) seedNodes();
        }
        void seedNodes() {
            nodes.add(new Node(210,170,0,90)); nodes.add(new Node(300,490,0,75)); nodes.add(new Node(760,190,0,85));
            nodes.add(new Node(160,350,1,130)); nodes.add(new Node(840,330,1,160)); nodes.add(new Node(900,490,1,100));
            nodes.add(new Node(780,470,2,100)); nodes.add(new Node(1010,250,2,130));
            nodes.add(new Node(1050,540,3,110)); nodes.add(new Node(1140,560,3,90)); nodes.add(new Node(930,170,4,80));
        }
        void newGame() {
            people.clear(); nodes.clear(); buildings.clear();
            people.add(new Person("Ог",410,280,true)); people.add(new Person("Уна",470,310,false));
            people.add(new Person("Тог",360,350,true)); people.add(new Person("Мира",520,360,false));
            people.add(new Person("Бор",600,300,true)); people.add(new Person("Ила",440,420,false));
            buildings.add(new Building(460,355,0,true)); buildings.add(new Building(570,430,1,true));
            nodes.add(new Node(210,170,0,90)); nodes.add(new Node(300,490,0,75)); nodes.add(new Node(760,190,0,85));
            nodes.add(new Node(160,350,1,130)); nodes.add(new Node(840,330,1,160)); nodes.add(new Node(900,490,1,100));
            nodes.add(new Node(780,470,2,100)); nodes.add(new Node(1010,250,2,130));
            nodes.add(new Node(1050,540,3,110)); nodes.add(new Node(1140,560,3,90));
            nodes.add(new Node(930,170,4,80));
            selected=0;
        }
        void load() {
            food=prefs.getInt("food",90); wood=prefs.getInt("wood",55); stone=prefs.getInt("stone",25); tools=prefs.getInt("tools",8);
            day=prefs.getInt("day",1);
            String saved=prefs.getString("state","");
            if(saved.length()==0) return;
            try {
                String[] rows=saved.split("\\|");
                if(rows.length<2) return;
                String[] ps=rows[0].split(";");
                for(String a:ps){String[] v=a.split(","); if(v.length>=6){Person q=new Person(v[0],Float.parseFloat(v[1]),Float.parseFloat(v[2]),v[3].equals("1"));q.hunger=Float.parseFloat(v[4]);q.energy=Float.parseFloat(v[5]);people.add(q);}}
                String[] bs=rows[1].split(";");
                for(String a:bs){String[] v=a.split(",");if(v.length>=4)buildings.add(new Building(Float.parseFloat(v[0]),Float.parseFloat(v[1]),Integer.parseInt(v[2]),v[3].equals("1")));}
                if(people.isEmpty()) newGame();
            } catch(Exception e){newGame();}
        }
        void save() {
            StringBuilder a=new StringBuilder(),b=new StringBuilder();
            for(Person q:people){if(a.length()>0)a.append(';');a.append(q.name).append(',').append(q.x).append(',').append(q.y).append(',').append(q.male?1:0).append(',').append(q.hunger).append(',').append(q.energy);}
            for(Building q:buildings){if(b.length()>0)b.append(';');b.append(q.x).append(',').append(q.y).append(',').append(q.type).append(',').append(q.done?1:0);}
            prefs.edit().putInt("food",food).putInt("wood",wood).putInt("stone",stone).putInt("tools",tools).putInt("day",day).putString("state",a+"|"+b).apply();
        }
        void togglePause(){paused=!paused;notice=paused?"Племя отдыхает.":"Продолжаем жить!";invalidate();}
        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            float sx=getWidth()/VW, sy=getHeight()/VH; scale=Math.min(sx,sy); ox=(getWidth()-VW*scale)/2; oy=(getHeight()-VH*scale)/2;
            c.drawColor(Color.rgb(24,35,30)); c.save(); c.translate(ox,oy); c.scale(scale,scale);
            long now=System.nanoTime(); float dt=Math.min(.06f,(now-lastFrame)/1e9f); lastFrame=now;
            if(!paused) update(dt);
            drawWorld(c); drawHud(c); drawActions(c);
            if(paused) drawOverlay(c,"ПАУЗА","Нажми ПРОДОЛЖИТЬ");
            c.restore();
            postInvalidateDelayed(33);
        }
        void update(float dt) {
            dayTime+=dt; if(dayTime>150){dayTime=0;day++;food=Math.max(0,food-people.size()*2);notice="День "+day+": племени нужен запас еды!";}
            saveTimer+=dt; if(saveTimer>12){saveTimer=0;save();}
            eventTimer+=dt;
            if(day>=3 && eventTimer>115){eventTimer=0;notice="Тревога! У границ замечены чужие охотники."; for(Person q:people)if(q.task==5)q.task=4;}
            for(Person q:people){
                q.hunger=Math.min(100,q.hunger+dt*(food>0?.65f:1.8f));
                q.energy=Math.max(0,Math.min(100,q.energy+(q.task==6?dt*5:-dt*.65f)));
                if(q.hunger>86 && food>0){food=Math.max(0,food-1);q.hunger=35;}
                if(q.hunger>99)q.health-=dt*2;
                if(q.health<=0){q.health=100;q.hunger=30;notice=q.name+" едва пережил голод.";}
                if(q.task==6){q.x+=(q.homeX-q.x)*dt*.25f;q.y+=(q.homeY-q.y)*dt*.25f;continue;}
                Node target=findNode(q.task,q.target);
                if(target!=null && target.amount>0){
                    float dx=target.x-q.x,dy=target.y-q.y,dist=(float)Math.sqrt(dx*dx+dy*dy);
                    if(dist>24){float step=Math.min(dist,dt*36);q.x+=dx/dist*step;q.y+=dy/dist*step;}
                    else {q.work+=dt; if(q.work>2.4f){q.work=0;target.amount=Math.max(0,target.amount-8);
                        if(q.task==0)food+=7; else if(q.task==1)wood+=6; else if(q.task==2)stone+=5; else if(q.task==3)food+=9; else if(q.task==4){food+=12;tools=Math.max(0,tools-1);}
                        if(target.amount==0)q.target=-1;
                    }}
                } else if(q.task>=0 && q.task<=4){q.target=-1;assignNearest(q,q.task);}
                else if(q.task==5){Building b=nearestUnfinished(q.x,q.y);if(b!=null){float dx=b.x-q.x,dy=b.y-q.y,d=(float)Math.sqrt(dx*dx+dy*dy);if(d>25){q.x+=dx/d*dt*32;q.y+=dy/d*dt*32;}else{b.progress+=dt;if(b.progress>20){b.done=true;notice="Постройка завершена!";q.task=7;}}}else q.task=7;}
                else if(q.task==7 && random.nextFloat()<dt*.08f){q.x+=random.nextFloat()*16-8;q.y+=random.nextFloat()*16-8;}
                q.x=Math.max(55,Math.min(1190,q.x));q.y=Math.max(115,Math.min(565,q.y));
            }
        }
        Node findNode(int task,int idx){if(idx>=0&&idx<nodes.size()){Node n=nodes.get(idx);if(n.kind==task&&n.amount>0)return n;}return null;}
        void assignNearest(Person q,int task){int best=-1;float d=Float.MAX_VALUE;for(int i=0;i<nodes.size();i++){Node n=nodes.get(i);if(n.kind==task&&n.amount>0){float z=(n.x-q.x)*(n.x-q.x)+(n.y-q.y)*(n.y-q.y);if(z<d){d=z;best=i;}}}q.target=best;if(best<0){q.task=7;notice="Поблизости нет подходящего места.";}}
        Building nearestUnfinished(float x,float y){Building best=null;float d=Float.MAX_VALUE;for(Building b:buildings)if(!b.done){float z=(b.x-x)*(b.x-x)+(b.y-y)*(b.y-y);if(z<d){d=z;best=b;}}return best;}
        void assign(int task){if(selected<0||selected>=people.size()){notice="Сначала выбери соплеменника.";return;}Person q=people.get(selected);q.task=task;q.target=-1;q.work=0;if(task>=0&&task<=4)assignNearest(q,task);if(task==6){q.homeX=460;q.homeY=380;}notice=q.name+": "+tasks[task]+".";}
        void build(int type,float x,float y){
            if(food<buildFood[type]||wood<buildWood[type]||stone<buildStone[type]){notice="Не хватает ресурсов для "+buildNames[type]+".";return;}
            food-=buildFood[type];wood-=buildWood[type];stone-=buildStone[type];
            buildings.add(new Building(x,y,type,false));if(selected>=0)people.get(selected).task=5;
            buildMenu=false;buildChoice=-1;notice="Начато строительство: "+buildNames[type]+".";
        }
        @Override public boolean onTouchEvent(MotionEvent e){
            float x=(e.getX()-ox)/scale,y=(e.getY()-oy)/scale;
            if(e.getAction()==MotionEvent.ACTION_DOWN){downX=lastX=x;downY=lastY=y;dragging=false;return true;}
            if(e.getAction()==MotionEvent.ACTION_MOVE){if(Math.abs(x-downX)+Math.abs(y-downY)>18){dragging=true;if(downY>88&&downY<605){camX=Math.max(0,Math.min(260,camX+(lastX-x)));camY=Math.max(0,Math.min(180,camY+(lastY-y)));}lastX=x;lastY=y;}return true;}
            if(e.getAction()==MotionEvent.ACTION_UP){if(!dragging)handleTap(x,y);return true;}return true;
        }
        void handleTap(float x,float y){
            if(paused){paused=false;return;}
            if(y<82&&x>1160){togglePause();return;}
            if(y>610){int idx=(int)((x-16)/156);if(idx>=0&&idx<8){switch(idx){case 0:assign(0);break;case 1:assign(1);break;case 2:assign(2);break;case 3:assign(3);break;case 4:assign(4);break;case 5:buildMenu=!buildMenu;buildChoice=-1;break;case 6:assign(6);break;case 7:assign(7);break;}}return;}
            if(buildMenu&&y>475&&y<608){int col=(int)((x-18)/178),row=(int)((y-480)/58),n=row*7+col;if(n>=0&&n<buildNames.length){buildChoice=n;notice=buildNames[n]+": "+buildWood[n]+" дерева, "+buildStone[n]+" камня";}return;}
            if(buildChoice>=0&&y>88&&y<605){build(buildChoice,x+camX,y-88+camY);return;}
            if(y>88&&y<605){float wx=x+camX,wy=y-88+camY;int hit=-1;float d=Float.MAX_VALUE;for(int i=0;i<people.size();i++){Person q=people.get(i);float z=(q.x-wx)*(q.x-wx)+(q.y-wy)*(q.y-wy);if(z<d){d=z;hit=i;}}if(d<1800){selected=hit;notice=people.get(hit).name+" — "+tasks[Math.max(0,Math.min(7,people.get(hit).task))];}else{selected=-1;notice="Выбери человека или назначь задачу.";} }
        }
        void drawWorld(Canvas c){
            p.setColor(Color.rgb(112,164,76));c.drawRect(0,88,VW,610,p);
            c.save();c.clipRect(0,88,VW,610);c.translate(-camX,88-camY);
            // layered terrain, paths and shoreline
            p.setColor(Color.rgb(139,180,91));c.drawOval(70,90,920,760,p);
            p.setColor(Color.rgb(164,190,103));for(int i=0;i<85;i++){float x=(i*137)%1250,y=(i*83)%650;c.drawCircle(x,y,2+(i%4),p);}
            p.setColor(Color.rgb(72,145,185));Path river=new Path();river.moveTo(980,0);river.cubicTo(900,180,1120,300,1000,700);river.lineTo(1210,700);river.cubicTo(1280,340,1050,170,1110,0);river.close();c.drawPath(river,p);
            p.setColor(Color.rgb(188,220,209));p.setStrokeWidth(6);c.drawLine(1010,80,1040,640,p);
            for(Node n:nodes)drawNode(c,n);
            for(Building b:buildings)drawBuilding(c,b);
            for(Person q:people)drawPerson(c,q,people.indexOf(q)==selected);
            c.restore();
        }
        void drawNode(Canvas c,Node n){
            if(n.amount<=0)return;
            if(n.kind==0){p.setColor(Color.rgb(52,126,48));c.drawCircle(n.x,n.y,22,p);p.setColor(Color.rgb(210,45,54));for(int i=0;i<5;i++)c.drawCircle(n.x-10+i*5,n.y-5+(i%2)*10,4,p);}
            if(n.kind==1){p.setColor(Color.rgb(112,75,43));c.drawRect(n.x-5,n.y-5,n.x+5,n.y+30,p);p.setColor(Color.rgb(32,105,49));c.drawCircle(n.x,n.y-12,25,p);c.drawCircle(n.x-16,n.y-5,17,p);}
            if(n.kind==2){p.setColor(Color.rgb(112,115,113));Path z=new Path();z.moveTo(n.x-20,n.y+15);z.lineTo(n.x-10,n.y-17);z.lineTo(n.x+5,n.y-24);z.lineTo(n.x+22,n.y+15);z.close();c.drawPath(z,p);}
            if(n.kind==3){p.setColor(Color.rgb(242,215,127));c.drawOval(n.x-18,n.y-8,n.x+18,n.y+9,p);p.setColor(Color.WHITE);c.drawCircle(n.x,n.y,3,p);}
            if(n.kind==4){p.setColor(Color.rgb(108,65,45));c.drawOval(n.x-17,n.y-12,n.x+18,n.y+12,p);p.setColor(Color.rgb(230,210,170));c.drawCircle(n.x+13,n.y-5,7,p);}
        }
        void drawBuilding(Canvas c,Building b){
            p.setColor(Color.argb(55,20,35,15));c.drawOval(b.x-37,b.y+18,b.x+37,b.y+30,p);
            p.setColor(b.done?new int[]{0xffa77a48,0xff8d7048,0xffa46f47,0xffbd8a4b,0xff99724c,0xff9c7145,0xff8a6846,0xffbd9a48,0xffa0784c,0xff7e674b,0xffbb9668,0xff997a4c,0xff80634b}[b.type]:Color.GRAY);
            c.drawRoundRect(b.x-29,b.y-15,b.x+29,b.y+20,7,7,p);
            p.setColor(Color.rgb(82,49,35));Path roof=new Path();roof.moveTo(b.x-38,b.y-12);roof.lineTo(b.x,b.y-42);roof.lineTo(b.x+38,b.y-12);roof.close();c.drawPath(roof,p);
            p.setColor(Color.rgb(58,39,29));c.drawRect(b.x-6,b.y+1,b.x+7,b.y+20,p);
            if(!b.done){p.setColor(Color.WHITE);p.setTextSize(15);c.drawText("Строится",b.x-30,b.y-48,p);}
        }
        void drawPerson(Canvas c,Person q,boolean isSelected){
            if(isSelected){p.setColor(Color.argb(120,255,236,91));c.drawCircle(q.x,q.y+3,26,p);}
            p.setColor(Color.argb(70,30,40,25));c.drawOval(q.x-12,q.y+10,q.x+13,q.y+17,p);
            p.setColor(q.male?Color.rgb(174,105,64):Color.rgb(211,151,106));c.drawRoundRect(q.x-10,q.y-5,q.x+10,q.y+15,8,8,p);
            p.setColor(Color.rgb(231,188,143));c.drawCircle(q.x,q.y-11,9,p);
            p.setColor(q.male?Color.rgb(67,42,27):Color.rgb(83,47,28));c.drawArc(q.x-10,q.y-21,q.x+10,q.y-3,180,180,true,p);
            p.setColor(Color.WHITE);p.setTextSize(12);p.setTextAlign(Paint.Align.CENTER);c.drawText(q.name,q.x,q.y+31,p);p.setTextAlign(Paint.Align.LEFT);
            if(q.hunger>75){p.setColor(Color.RED);c.drawCircle(q.x+13,q.y-23,4,p);}
        }
        void drawHud(Canvas c){
            p.setColor(Color.rgb(73,48,31));c.drawRoundRect(10,8,1270,78,18,18,p);
            p.setColor(Color.rgb(242,216,160));p.setTextSize(24);c.drawText("ПЛЕМЯ  •  День "+day,28,38,p);
            p.setTextSize(18);c.drawText("Еда  "+food,340,37,p);c.drawText("Дерево  "+wood,475,37,p);c.drawText("Камень  "+stone,650,37,p);c.drawText("Орудия  "+tools,820,37,p);
            p.setTextSize(14);p.setColor(Color.rgb(250,231,185));c.drawText("Людей: "+people.size()+"   "+notice,28,63,p);
            p.setColor(Color.rgb(160,73,48));c.drawRoundRect(1190,15,1254,66,12,12,p);p.setColor(Color.WHITE);p.setTextSize(18);c.drawText(paused?"▶":"Ⅱ",1210,48,p);
            if(selected>=0&&selected<people.size()){Person q=people.get(selected);p.setColor(Color.rgb(65,52,38));c.drawRoundRect(970,14,1178,68,10,10,p);p.setColor(Color.WHITE);p.setTextSize(15);c.drawText(q.name+"  "+tasks[Math.max(0,Math.min(7,q.task))],985,36,p);p.setTextSize(12);c.drawText("Сытость "+(int)(100-q.hunger)+"%  Силы "+(int)q.energy+"%",985,56,p);}
        }
        void drawActions(Canvas c){
            p.setColor(Color.rgb(63,48,37));c.drawRect(0,610,VW,VH,p);
            String[] labels={"Ягоды","Дерево","Камень","Рыба","Охота","Строить","Отдых","Свободен"};
            int[] colors={0xff557d3c,0xff557d3c,0xff747c7d,0xff347f9a,0xff9b633b,0xffa77c3e,0xff55718c,0xff77705e};
            for(int i=0;i<8;i++){float x=16+i*158;p.setColor(colors[i]);c.drawRoundRect(x,625,x+145,698,12,12,p);p.setColor(Color.WHITE);p.setTextSize(20);c.drawText(labels[i],x+18,669,p);}
            if(buildMenu){p.setColor(Color.argb(245,45,39,31));c.drawRoundRect(12,472,1268,606,16,16,p);for(int i=0;i<buildNames.length;i++){int col=i%7,row=i/7;float x=20+col*178,y=482+row*55;p.setColor(buildChoice==i?0xffc58c43:0xff766044);c.drawRoundRect(x,y,x+166,y+46,8,8,p);p.setColor(Color.WHITE);p.setTextSize(16);c.drawText(buildNames[i],x+8,y+20,p);p.setTextSize(12);c.drawText("Д:"+buildWood[i]+" К:"+buildStone[i]+" Е:"+buildFood[i],x+8,y+38,p);}}
            p.setColor(Color.rgb(240,225,187));p.setTextSize(12);c.drawText("Нажми на жителя, затем выбери задачу. Перетаскивай карту пальцем.",18,718,p);
        }
        void drawOverlay(Canvas c,String title,String sub){p.setColor(Color.argb(190,20,25,20));c.drawRect(0,0,VW,VH,p);p.setColor(Color.rgb(244,224,174));p.setTextAlign(Paint.Align.CENTER);p.setTextSize(54);c.drawText(title,640,330,p);p.setTextSize(23);c.drawText(sub,640,380,p);p.setTextAlign(Paint.Align.LEFT);}
    }
    static final class Person {
        String name;float x,y,hunger=20,energy=90,health=100,work=0,homeX,homeY;boolean male;int task=7,target=-1;
        Person(String n,float px,float py,boolean m){name=n;x=px;y=py;male=m;homeX=px;homeY=py;}
    }
    static final class Node {float x,y,amount;int kind;Node(float a,float b,int k,float n){x=a;y=b;kind=k;amount=n;}}
    static final class Building {float x,y,progress=0;int type;boolean done;Building(float a,float b,int t,boolean d){x=a;y=b;type=t;done=d;}}
    static final class UiButton {float x,y,w,h;String label;UiButton(float a,float b,float c,float d,String s){x=a;y=b;w=c;h=d;label=s;}}
}
