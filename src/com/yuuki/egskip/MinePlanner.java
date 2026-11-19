package com.yuuki.egskip;
import java.util.*;
/** Planner uses only the server snapshot. Resource labels are explicit user calibration. */
public final class MinePlanner {
 public static final String[] LABELS={"待标记", "金币矿·小", "金币矿·中", "金币矿·大", "魔石矿·小", "魔石矿·中", "魔石矿·大", "钻石", "镐子", "炸药/钻头等道具", "宝箱（未分级）", "普通蛋", "蓝蛋", "金蛋", "忽略", "魔晶", "加速药水", "宝箱·低级", "宝箱·中级", "宝箱·高级", "强化石", "裂缝石·剩1镐", "完整石砖·2镐"};
 public static final int UNKNOWN=0,COIN_S=1,COIN_M=2,COIN_L=3,MAGIC_S=4,MAGIC_M=5,MAGIC_L=6,DIAMOND=7,PICK=8,TOOL=9,CHEST=10,EGG=11,BLUE=12,GOLD=13,IGNORE=14,CRYSTAL=15,POTION=16,CHEST_S=17,CHEST_M=18,CHEST_L=19,ENHANCE=20,CRACKED=21,HARD=22;
 // Append category IDs: existing saved resource classifications keep their meaning.
 public static final String[] CATEGORY_NAMES={"矿石","道具","宝箱","宠物","地形"};
 public static final int[][] CATEGORY_KINDS={{COIN_S,COIN_M,COIN_L,MAGIC_S,MAGIC_M,MAGIC_L,DIAMOND,CRYSTAL},{PICK,TOOL,POTION,ENHANCE},{CHEST_S,CHEST_M,CHEST_L},{EGG,BLUE,GOLD,IGNORE},{CRACKED,HARD}};
 public static int groupForKind(int kind){if(kind==CHEST)return 2;for(int g=0;g<CATEGORY_KINDS.length;g++)for(int k:CATEGORY_KINDS[g])if(k==kind)return g;return 0;}
 public static final String[] PRIORITY_NAMES={"不追/跳过","低优先（两镐内）","普通（四镐内）","优先","最高优先","提醒并暂停"};
 public static boolean priorityEditable(int kind){return kind>UNKNOWN&&kind<LABELS.length&&kind!=CRACKED&&kind!=HARD&&kind!=GOLD;}
 public static int defaultPriority(int k){if(k==GOLD)return 5;if(k==CRACKED||k==HARD)return 0;if(k==COIN_S||k==COIN_M||k==MAGIC_S||k==POTION||k==IGNORE||ignoredEgg(k))return 0;if(k==MAGIC_M||chest(k))return 1;return 4;}
 public static int priority(int k,java.util.Map<Integer,Integer> values){Integer v=values.get(k);return priorityEditable(k)&&v!=null?Math.max(0,Math.min(5,v)):defaultPriority(k);}
 public static String priorityLabel(int k,java.util.Map<Integer,Integer> values){return k==GOLD?"固定暂停（所有金蛋）":PRIORITY_NAMES[priority(k,values)];}
 static boolean avoided(int k){return avoided(k,java.util.Collections.<Integer,Integer>emptyMap());}
 static boolean avoided(int k,java.util.Map<Integer,Integer> p){return k==POTION&&priority(k,p)==0;}
 public static class Cell { public int index,id,ground,res,unlocked; public int row(){return (id-1)/6;} public int col(){return (id-1)%6;} }
 public static class MapData { public String nonce; public int depth,snap,base,picks,lineTools,areaTools; public int goldPriority58=5; public boolean fastDescent=false,allowTools=true,fallbackPicks=false; public List<Cell> cells=new ArrayList<Cell>(); }
 public static class Plan { public int index=-1,id,tool; public String note; public boolean alert; public String alertKey=""; Plan(String n){note=n;} public String encode(String nonce){return nonce+"\n"+index+"\n"+id+"\n"+tool+"\n";} }
 public static MapData parse(String text){
  String[] ls=text.trim().split("\\n");if(ls.length<4||!ls[ls.length-1].trim().equals("END46"))throw new IllegalArgumentException("矿图未就绪");
  MapData m=new MapData();m.nonce=ls[0].trim();if(!m.nonce.matches("[0-9:-]{1,100}"))throw new IllegalArgumentException("矿图标记无效");
  String[] h=ls[1].trim().split(",");if(h.length!=6)throw new IllegalArgumentException("矿图头不完整");m.depth=Integer.parseInt(h[0]);m.snap=Integer.parseInt(h[1]);m.base=Integer.parseInt(h[2]);m.picks=Integer.parseInt(h[3]);m.lineTools=Integer.parseInt(h[4]);m.areaTools=Integer.parseInt(h[5]);
  Set<Integer> ids=new HashSet<Integer>();for(int j=2;j<ls.length-1;j++){if(ls[j].startsWith("退出码:"))break;String[] v=ls[j].trim().split(",");if(v.length!=5)throw new IllegalArgumentException("矿格不完整");Cell c=new Cell();c.index=Integer.parseInt(v[0]);c.id=Integer.parseInt(v[1]);c.ground=Integer.parseInt(v[2]);c.res=Integer.parseInt(v[3]);c.unlocked=Integer.parseInt(v[4]);if(c.id<1||c.index<0||!ids.add(c.id))throw new IllegalArgumentException("重复或无效矿格");m.cells.add(c);}
  if(m.cells.isEmpty()||m.cells.size()>4096)throw new IllegalArgumentException("矿图范围无效");return m;
 }
 static int kind(Cell c,java.util.Map<Integer,Integer> labels){Integer k=labels.get(c.res);return c.res==0?IGNORE:k==null?UNKNOWN:k;}
 static boolean mineral(int k){return k>=COIN_S&&k<=MAGIC_L;}
 static boolean chest(int k){return k==CHEST||k==CHEST_S||k==CHEST_M||k==CHEST_L;}
 static boolean ignoredEgg(int k){return k==EGG||k==BLUE;}
 static int cost(Cell c){return c.ground==0?0:c.ground==1?1:c.ground==2?2:1000000;}
 static int cost(Cell c,java.util.Map<Integer,Integer> labels){int k=kind(c,labels);return k==CRACKED?1:k==HARD?2:cost(c);}
 static boolean terrain(int k){return k==CRACKED||k==HARD;}
 static boolean hundred(Cell c){return (c.row()+1)%100==0;}
 static boolean paused(Cell c,int k,java.util.Map<Integer,Integer> p){return priority(k,p)==5&&!terrain(k)&&c.res!=0;}
 static int weight(Cell c,int k,java.util.Map<Integer,Integer> p){
  if(terrain(k)||k==UNKNOWN)return 0;if(mineral(k)&&hundred(c))return 160;
  int tier=priority(k,p);if(tier==0||tier==5)return 0;
  if(p.containsKey(k)){switch(tier){case 1:return 35;case 2:return 90;case 3:return 180;default:return 280;}}
  switch(k){case GOLD:return 400;case DIAMOND:return 250;case MAGIC_L:case CRYSTAL:return 210;case COIN_L:return 180;case PICK:case TOOL:case ENHANCE:return 150;case MAGIC_M:case CHEST_L:return 45;case CHEST_S:return 20;case CHEST:case CHEST_M:return 30;default:return 0;}
 }
 static boolean required(Cell c,int k,java.util.Map<Integer,Integer> p){return !terrain(k)&&weight(c,k,p)>0&&(priority(k,p)==4||mineral(k)&&hundred(c));}
 static boolean withinBudget(Cell c,int k,int distance,java.util.Map<Integer,Integer> p){if(mineral(k)&&hundred(c))return true;int tier=priority(k,p);return tier!=1&&tier!=2||distance<=(tier==1?2:4);}
 static List<Cell> neighbors(Cell c,java.util.Map<Integer,Cell> byId){List<Cell> r=new ArrayList<Cell>();for(int d:new int[]{-6,6,-1,1}){Cell v=byId.get(c.id+d);if(v!=null&&(Math.abs(d)==6||v.row()==c.row()))r.add(v);}return r;}
 static boolean footprint(MapData m,Cell center,Cell c,int tool){return tool==1028?c.index/6>=m.snap&&c.index/6<m.snap+6&&(c.col()==center.col()||c.index/6==m.snap&&Math.abs(c.col()-center.col())==1):Math.abs(c.row()-center.row())+Math.abs(c.col()-center.col())<3;}
 static Plan alert(String text,String key){Plan p=new Plan(text);p.alert=true;p.alertKey=key;return p;}
 static Plan goldStop75(MapData m,java.util.Map<Integer,Integer> labels){
  for(Cell c:m.cells)if(c.res==9003||kind(c,labels)==GOLD)return alert("发现金蛋（本百层第"+(c.row()%100+1)+"排第"+(c.col()+1)+"列），已暂停全部自动采矿；请手动处理，处理后自动继续","gold75:"+c.id);
  return null;
 }
 public static Plan choose(MapData m,java.util.Map<Integer,Integer> labels){return choose(m,labels,java.util.Collections.<Integer,Integer>emptyMap());}
 public static Plan choose(MapData m,java.util.Map<Integer,Integer> labels,java.util.Map<Integer,Integer> priorities){
  Plan gold75=goldStop75(m,labels);if(gold75!=null)return gold75;
  for(Cell c:m.cells){int k=kind(c,labels);if(c.unlocked==1&&paused(c,k,priorities))return alert(k==GOLD?"发现金蛋，已暂停，请手动捕捉；处理后自动继续":"发现"+LABELS[k]+"，按设置暂停；处理后自动继续","pause:"+c.id+":"+k);}
  for(Cell c:m.cells){int k=kind(c,labels);if(c.res!=0&&k==UNKNOWN&&c.unlocked==1)return alert("矿格待标记：请用亮框确认图案后选择类别","unknown:"+c.res);}
  int n=m.cells.size(),INF=1000000;int[] dist=new int[n],first=new int[n],previous=new int[n];boolean[] used=new boolean[n];Arrays.fill(dist,INF);Arrays.fill(first,-1);Arrays.fill(previous,-1);
  java.util.Map<Integer,Cell> byId=new HashMap<Integer,Cell>();java.util.Map<Integer,Integer> pos=new HashMap<Integer,Integer>();int openRow=-1;
  for(int i=0;i<n;i++){Cell c=m.cells.get(i);int k=kind(c,labels);byId.put(c.id,c);pos.put(c.id,i);if(cost(c,labels)==0&&c.unlocked==1)openRow=Math.max(openRow,c.row());if(c.unlocked==1&&cost(c,labels)<INF&&!avoided(k,priorities)){
   dist[i]=cost(c,labels);boolean skipOpen=cost(c,labels)==0&&(c.res==0||weight(c,k,priorities)==0&&!paused(c,k,priorities));first[i]=skipOpen?-1:i;
  }}
  if(openRow<0){int shallow=INF;for(Cell c:m.cells)if(c.unlocked==1)shallow=Math.min(shallow,c.row());openRow=shallow==INF?0:shallow-1;}
  for(int step=0;step<n;step++){int u=-1;for(int i=0;i<n;i++)if(!used[i]&&(u<0||dist[i]<dist[u]))u=i;if(u<0||dist[u]>=INF)break;used[u]=true;Cell c=m.cells.get(u);if(paused(c,kind(c,labels),priorities))continue;
   for(Cell v:neighbors(c,byId)){int j=pos.get(v.id),k=kind(v,labels);if(avoided(k,priorities)||cost(v,labels)>=INF||paused(v,k,priorities))continue;int nd=dist[u]+cost(v,labels);if(nd<dist[j]){dist[j]=nd;previous[j]=u;first[j]=first[u]>=0?first[u]:(cost(v,labels)>0||weight(v,k,priorities)>0?j:-1);}}
  }
  int goal=-1;double best=-1;boolean mandatory=false;
  for(int i=0;i<n;i++){Cell c=m.cells.get(i);int k=kind(c,labels),w=weight(c,k,priorities);if(w==0||dist[i]>=INF||first[i]<0||m.cells.get(first[i]).unlocked!=1||!withinBudget(c,k,dist[i],priorities))continue;
   boolean req=required(c,k,priorities);double score=(w>=150&&m.depth-c.row()>=4?1000000.0+w:w)/(1.0+dist[i]);if(m.fastDescent)score=(req?1000000.0:0)+w/(1.0+dist[i]);
   if(goal<0||score>best||score==best&&c.id<m.cells.get(goal).id){goal=i;best=score;mandatory=req;}
  }
  // Descend toward the next hundred-row boundary using the cheapest visible
  // route. Required loot/checkpoint minerals are handled before advancing.
  boolean descending=false;int bottom=-1,descent=-1;double advanceScore=-1;int boundary=(m.depth/100+1)*100-1;int checkpointRow=INF;for(Cell c:m.cells)if(mineral(kind(c,labels))&&hundred(c)&&weight(c,kind(c,labels),priorities)>0)checkpointRow=Math.min(checkpointRow,c.row());
  if(m.fastDescent&&!mandatory){for(int i=0;i<n;i++){Cell c=m.cells.get(i);if(dist[i]>=INF||first[i]<0||m.cells.get(first[i]).unlocked!=1||avoided(kind(c,labels),priorities)||c.row()>boundary)continue;
    if(c.row()<=openRow)continue;
    if(descent<0||(long)dist[i]*(bottom-openRow)<(long)dist[descent]*(c.row()-openRow)||(long)dist[i]*(bottom-openRow)==(long)dist[descent]*(c.row()-openRow)&&(dist[i]<dist[descent]||dist[i]==dist[descent]&&c.id<m.cells.get(descent).id)){bottom=c.row();descent=i;}}
   if(descent>=0){advanceScore=100.0*Math.max(1,bottom-openRow)/(1+dist[descent]);if(goal<0||best<advanceScore){goal=descent;best=advanceScore;descending=true;}}
  }
  Cell chosen=goal>=0?m.cells.get(first[goal]):null;int routeCost=goal>=0?dist[goal]:0;
  // Free rewards are collected directly; consumables never replace a free
  // pickup or become an automatic substitute for an empty pick inventory.
  if(chosen!=null&&cost(chosen,labels)==0&&chosen.res!=0){Plan free=new Plan("免费拾取"+LABELS[kind(chosen,labels)]);free.index=chosen.index;free.id=chosen.id;free.tool=0;return free;}
  if(m.picks<1)return new Plan("镐子不足，等待恢复；不会自动改用炸药或地钻");
  // A hard corridor still merits a tool when its continuation is unavoidable
  // from this same first action. Do not inflate it with a different branch.
  int toolGoal=goal;if(descending&&chosen!=null&&cost(chosen,labels)>=2)for(int i=0;i<n;i++)if(first[i]==first[goal]&&dist[i]<INF&&m.cells.get(i).row()<=boundary&&(toolGoal<0||m.cells.get(i).row()>m.cells.get(toolGoal).row()))toolGoal=i;
  if(toolGoal>=0)routeCost=dist[toolGoal];
  Set<Integer> route=new HashSet<Integer>();int routeHard=0;
  if(toolGoal>=0)for(int j=toolGoal;j>=0;j=previous[j]){Cell c=m.cells.get(j);route.add(c.id);if(cost(c,labels)>=2&&cost(c,labels)<INF)routeHard++;}
  Plan batch=null;double batchScore=0;
  if(m.allowTools)for(Cell center:m.cells){if(center.unlocked!=1||cost(center,labels)>=INF||avoided(kind(center,labels),priorities))continue;
   for(int tool:new int[]{1028,1029}){if(tool==1028&&(m.lineTools<=0||center.index/6!=m.snap)||tool==1029&&m.areaTools<=0)continue;int gain=0,targets=0,saved=0,paidTargets=0,lootWork=0,routeWork=0,routeStone=0,relevantWork=0,deepest=openRow;boolean forbidden=false,requiredHit=false,opensRoute=false;
    for(Cell c:m.cells){if(!footprint(m,center,c,tool))continue;if(chosen!=null&&chosen.id==c.id)opensRoute=true;int k=kind(c,labels);if(paused(c,k,priorities)||avoided(k,priorities)||k==UNKNOWN&&c.res!=0){forbidden=true;break;}int w=weight(c,k,priorities);if(w>0){gain+=w;targets++;if(cost(c,labels)>0&&cost(c,labels)<INF){paidTargets++;lootWork+=cost(c,labels);}if(required(c,k,priorities))requiredHit=true;}if(cost(c,labels)<INF){int hits=cost(c,labels);saved+=hits;if(route.contains(c.id)){routeWork+=hits;if(hits>=2)routeStone++;}if(route.contains(c.id)||w>0)relevantWork+=hits;if(hits>0)deepest=Math.max(deepest,c.row());}}
    if(m.fastDescent){if(checkpointRow<INF&&deepest>checkpointRow)for(Cell c:m.cells)if(c.row()==checkpointRow&&mineral(kind(c,labels))&&weight(c,kind(c,labels),priorities)>0&&!footprint(m,center,c,tool)){forbidden=true;break;}
     if(deepest>boundary&&checkpointRow==INF)forbidden=true;}
    // A deep empty footprint is not a reason to spend a rare tool. Count
    // work on actual reward cells and the shortest chosen route, not unrelated
    // bricks elsewhere in the blast radius.
    boolean cluster=paidTargets>=3&&lootWork>=3||paidTargets>=2&&lootWork>=4;
    boolean difficult=routeCost>=8&&routeHard>=3&&routeStone>=2&&routeWork>=4&&routeWork*2>=routeCost&&opensRoute;
    if(forbidden||(!cluster&&!difficult))continue;
    if(m.fastDescent&&mandatory&&!requiredHit&&!opensRoute)continue;
    double score=relevantWork-1+Math.min(gain,1000)/10000.0;
    if(score>batchScore){batchScore=score;String why=cluster?"多资源集中："+paidTargets+"个需挖目标，目标共需"+lootWork+"镐":"困难路线：最省镐路线需"+routeCost+"镐，其中道具覆盖"+routeWork+"镐";batch=new Plan(why+"；用1个道具，预计少操作"+Math.max(0,relevantWork-1)+"次");batch.index=center.index;batch.id=center.id;batch.tool=tool;}
   }
  }if(batch!=null)return batch;
  if(chosen==null){for(Cell c:m.cells)if(required(c,kind(c,labels),priorities))return alert("必拿目标暂不可达，请手动开路或调整优先级；地图更新后继续","path:"+c.id);}
  if(chosen==null){for(Cell c:m.cells){int k=kind(c,labels);if(c.unlocked==1&&(c.res==0||terrain(k))&&cost(c,labels)>0&&cost(c,labels)<INF&&(chosen==null||cost(c,labels)<cost(chosen,labels)||cost(c,labels)==cost(chosen,labels)&&c.id>chosen.id))chosen=c;}}
  if(chosen==null)return new Plan("暂无可达目标，等待地图更新或手动开路");
  int tool=cost(chosen,labels)==0&&chosen.res!=0?0:1026;if(tool==1026&&m.picks<1)return new Plan("镐子不足，等待恢复");
  String target=descending?"冲百层，向下开路":goal<0?"沿低成本地块开路":"前往"+LABELS[kind(m.cells.get(goal),labels)];Plan p=new Plan(target+"，预计镐成本"+routeCost);p.index=chosen.index;p.id=chosen.id;p.tool=tool;return p;
 }
}
