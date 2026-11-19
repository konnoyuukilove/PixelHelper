package com.yuuki.egskip;
import java.util.*;
/** Hundred-layer templates, immutable tile fingerprints and recorded waypoints. */
final class MineRoute56 {
 static class Tile {int ground,res;Tile(MinePlanner.Cell c){ground=c.ground;res=c.res;}}
 static class Route {String group;int source;TreeMap<Integer,Tile> tiles=new TreeMap<Integer,Tile>();List<MineReplay.Step> steps=new ArrayList<MineReplay.Step>();}
 static int row(MinePlanner.Cell c){return (c.id-1)/6;}static int block(int row){return row/100*100;}static int pos(MinePlanner.Cell c){return row(c)%100*6+(c.id-1)%6;}
 static boolean geometry(MinePlanner.MapData m){if(m.cells.isEmpty())return false;MinePlanner.Cell c=m.cells.get(0);int offset=row(c)-c.index/6;return Math.abs(offset+m.snap-m.depth)<=1;}
 static class Cache {int revision,count;List<Route> routes;}static final WeakHashMap<MineReplay,Cache> caches=new WeakHashMap<MineReplay,Cache>();
 static List<Route> routes(MineReplay db){Cache cached=caches.get(db);if(cached!=null&&cached.revision==db.revision&&cached.count==db.steps.size())return cached.routes;LinkedHashMap<String,Route> all=new LinkedHashMap<String,Route>();for(MineReplay.Step s:db.steps){MineReplay.Group g=db.groups.get(s.group);if(!db.acceptsGroup(s.group)||!s.enabled||g!=null&&!g.enabled||!geometry(s.before))continue;MinePlanner.Cell target=MineReplay.target(s.before,s.rel);if(target==null)continue;int source=block(row(target));String key=s.group+":"+source;Route r=all.get(key);if(r==null){r=new Route();r.group=s.group;r.source=source;all.put(key,r);}r.steps.add(s);}
  // Only intact observed tiles form the template: dug holes and unlock flags
  // describe progress and must never become the map's permanent identity.
  for(MineReplay.Step s:db.steps)if(geometry(s.before))for(MinePlanner.Cell c:s.before.cells)if(row(c)%100!=99&&c.ground>0){Route r=all.get(s.group+":"+block(row(c)));if(r==null)continue;int p=pos(c);Tile old=r.tiles.get(p);if(old==null||c.ground>old.ground)r.tiles.put(p,new Tile(c));}
  for(Route r:all.values())Collections.sort(r.steps,new Comparator<MineReplay.Step>(){public int compare(MineReplay.Step a,MineReplay.Step b){long x=Long.parseLong(a.before.nonce.split(":")[0]),y=Long.parseLong(b.before.nonce.split(":")[0]);if(x!=y)return Long.compare(x,y);String[] u=a.id.split(":"),v=b.id.split(":");return Long.compare(Long.parseLong(u[u.length-1]),Long.parseLong(v[v.length-1]));}});Cache save=new Cache();save.revision=db.revision;save.count=db.steps.size();save.routes=new ArrayList<Route>(all.values());caches.put(db,save);return save.routes;
 }
 static MinePlanner.Cell cellAt(Map<Integer,MinePlanner.Cell> map,int start,int position){return map.get((start+position/6)*6+position%6+1);}
 static MinePlanner.Plan pause(String message){return MineReplay.pause(message);}
 static boolean sameRoute(Route a,Route b){List<String> x=new ArrayList<String>(),y=new ArrayList<String>();for(MineReplay.Step s:a.steps){MinePlanner.Cell c=MineReplay.target(s.before,s.rel);x.add(pos(c)+":"+s.tool);}for(MineReplay.Step s:b.steps){MinePlanner.Cell c=MineReplay.target(s.before,s.rel);y.add(pos(c)+":"+s.tool);}return x.equals(y);}
 static MinePlanner.Plan choose(MineReplay db,MinePlanner.MapData m,MinePlanner.MapData world){return choose(db,m,world,world);}
 static MinePlanner.Plan choose(MineReplay db,MinePlanner.MapData m,MinePlanner.MapData world,MinePlanner.MapData original){
  return chooseInternal70(db,m,world,original,false);
 }
 // Production playback uses recorded pre-states. The waypoint planner remains
 // separate for explicit pick-only fallback and compatibility checks.
 static MinePlanner.Plan chooseRecorded70(MineReplay db,MinePlanner.MapData m,MinePlanner.MapData world,MinePlanner.MapData original){return chooseInternal70(db,m,world,original,true);}
 static MinePlanner.Plan chooseInternal70(MineReplay db,MinePlanner.MapData m,MinePlanner.MapData world,MinePlanner.MapData original,boolean strict){
  MinePlanner.Plan gold75=MinePlanner.goldStop75(m,Collections.<Integer,Integer>emptyMap());if(gold75!=null)return gold75;
  if(!geometry(m)){if(routes(db).isEmpty())return db.choose(m);return new MinePlanner.Plan("道具结算/矿图滚动中，等待坐标稳定");}String shape=MineReplay.shape(m,true);if(!db.sentNonce.isEmpty()){if(db.sentNonce.equals(m.nonce))return new MinePlanner.Plan("复刻等待上一笔回包");if(db.sentState.equals(shape)){if(strict&&System.currentTimeMillis()-db.sentAt70<2500)return new MinePlanner.Plan("回包已确认，等待矿图更新；不重复消耗");db.clearSent70();return pause("回包后矿格未变化，复刻已暂停；请检查日志后重连");}db.clearSent70();}
  MinePlanner.Plan boundary=previousBoundaryReward77(db,m);
  if(boundary!=null)return boundary;
  if(strict)return recorded70(db,m,world);
  int start=block(m.depth);Map<Integer,MinePlanner.Cell> all=new HashMap<Integer,MinePlanner.Cell>();for(MinePlanner.Cell c:world.cells)all.put(c.id,c);for(MinePlanner.Cell c:m.cells)all.put(c.id,c);
  // Every hundredth row's four magic stones are independent of the random map.
  for(MinePlanner.Cell c:m.cells)if(row(c)%100==99&&c.unlocked==1&&c.ground==0&&c.res>=102&&c.res<=104)return plan(c,0,"百层奖励：先收完魔石");
  Map<Integer,MinePlanner.Cell> identity=new HashMap<Integer,MinePlanner.Cell>();for(MinePlanner.Cell c:original.cells)identity.put(c.id,c);for(MinePlanner.Cell c:m.cells)if(!identity.containsKey(c.id)||identity.get(c.id).ground==0&&c.ground>0)identity.put(c.id,c);
  List<Route> matches=new ArrayList<Route>();Map<Route,Integer> scores=new HashMap<Route,Integer>();for(Route r:routes(db)){int score=0,prefix=0,resources=0;boolean mismatch=false;for(Map.Entry<Integer,Tile> e:r.tiles.entrySet()){MinePlanner.Cell c=cellAt(identity,start,e.getKey()),live=cellAt(all,start,e.getKey());Tile t=e.getValue();if(live!=null&&live.ground>0&&(live.ground>t.ground||live.res!=t.res)){mismatch=true;break;}if(c==null||c.ground==0)continue;if(c.ground>t.ground||c.res!=t.res){mismatch=true;break;}score++;if(c.res>0)resources++;if(e.getKey()<48)prefix++;}if(!mismatch&&score>=24&&(prefix>=4||resources>=3)){matches.add(r);scores.put(r,score);}}
  if(matches.isEmpty())return pause("未识别本百层地图，复刻已暂停。首排相同还需核对后续矿格；请选择分组补录");
  Collections.sort(matches,new Comparator<Route>(){public int compare(Route a,Route b){return Integer.compare(b.tiles.size(),a.tiles.size());}});Route chosen=matches.get(0);
  // Conflicting maps with the same prefix need more client data, not a guessed route.
  for(int n=1;n<matches.size();n++){Route other=matches.get(n);boolean contradiction=false;for(Map.Entry<Integer,Tile> e:chosen.tiles.entrySet()){Tile t=other.tiles.get(e.getKey());if(t!=null&&(t.ground!=e.getValue().ground||t.res!=e.getValue().res)){contradiction=true;break;}}if(contradiction)return pause("地图首排相同，后续矿格还不足以区分，复刻已暂停；请继续观察/补录");}
  String title=db.groups.containsKey(chosen.group)?db.groups.get(chosen.group).name:chosen.group;
  // A pet interaction is a separate game operation and must not be replaced by a pick.
  for(MinePlanner.Cell c:m.cells)if(row(c)%100==99&&c.res==9003&&c.unlocked==1&&m.goldPriority58>0)return pause("百层高等级宠物待处理，请手动捕捉后点继续复刻");
  boolean skippedTools57=false;for(int stepNo=0;stepNo<chosen.steps.size();stepNo++){MineReplay.Step s=chosen.steps.get(stepNo);MinePlanner.Cell recorded=MineReplay.target(s.before,s.rel);int position=pos(recorded);if(position/6==99)continue;MinePlanner.Cell target=cellAt(all,start,position);if(target==null)continue;
   if(s.tool!=1028&&s.tool!=1029&&target.ground==0&&target.res==0)continue; // Recorded backtracking is intentional; depth alone never completes an action.
   if(target.res!=0&&target.res!=recorded.res)return pause("录制目标资源与当前矿格不同，复刻已暂停");
   if(s.tool==1028||s.tool==1029){
    boolean unavailable=!m.allowTools||s.tool==1028&&m.lineTools<=0||s.tool==1029&&m.areaTools<=0;
    if(unavailable&&m.fallbackPicks){skippedTools57=true;continue;} // Next recorded pick remains the waypoint; path() fills the missing blast with picks.
    // Restore dropped range-tool records, but never infer a missing explosion.
    int needed=rangeNeeded(s,stepNo+1<chosen.steps.size()?chosen.steps.get(stepNo+1):null,all,start);if(needed==0)continue;if(needed<0)return pause("录制道具的结果数据不完整，请补录这一小段，避免重复消耗");
    MinePlanner.Cell fresh=find(m,target.id);if(fresh==null)return pause("录制道具落点尚未出现在当前视窗，复刻已暂停");
    if(!m.allowTools)return pause("记录需要"+MineReplay.toolName(s.tool)+"，请先开启道具开关或补录");
    if(s.tool==1028&&m.lineTools<=0||s.tool==1029&&m.areaTools<=0)return pause("录制道具库存不足，复刻已暂停");
    // Collect exposed rewards first. Intact stones are harvested by the recorded tool; only high pets need interception.
    MinePlanner.Plan protectedLoot=guardRange58(m,all,fresh,s.tool);if(protectedLoot!=null)return protectedLoot;
    return plan(fresh,s.tool,"地图已识别："+title+" · 原录制"+MineReplay.toolName(s.tool));
   }
   if(target.res>=9001&&target.res<=9003)continue;
   MinePlanner.Cell direct=find(m,target.id);if(direct!=null&&direct.unlocked==1){if(direct.ground==0&&direct.res>0)return plan(direct,0,"地图已识别："+title+" · 拾取录制目标");if(direct.ground>0){if(m.picks<=0)return pause("镐子不足，等待自然恢复；保持开启会自动继续，不自动购买");return plan(direct,1026,"地图已识别："+title+" · 按百层相对位置用镐");}}
   MinePlanner.Cell first=path(m,all,target);if(first!=null){if(m.picks<=0)return pause("镐子不足，等待自然恢复；保持开启会自动继续，不自动购买");return plan(first,1026,"地图已识别："+title+" · 用镐补齐未录的中间路段");}
   return pause("已识别地图，但当前无法到达下一录制落点；请补录这一小段");
  }
  return pause(skippedTools57?"范围道具已禁用或用完，本段缺少后续镐路线；请补录镐路线后继续":"本百层已录步骤执行完；下一段没有录制动作，请补录或等待新地图");
 }
 // Compare progress only inside this hundred-layer map. Neighbouring maps
 // are independently randomized; reward quality is not map identity.
 static MinePlanner.Plan previousBoundaryReward77(MineReplay db,MinePlanner.MapData m){
  // Match only recorded reward-row progress; next maps are independently random.
  Map<Integer,MinePlanner.Cell> live=new HashMap<Integer,MinePlanner.Cell>();int rewardRow=-1;
  for(MinePlanner.Cell c:m.cells){live.put(c.id,c);if(row(c)%100==99&&row(c)<m.depth&&m.depth-row(c)<=6&&c.res>=102&&c.res<=104)rewardRow=row(c);}
  if(rewardRow<0)return null;
  MineReplay.Step chosen=null;MinePlanner.Cell target=null;int best=-1;
  for(Route route:routes(db))for(MineReplay.Step step:route.steps){
   MinePlanner.Cell old=MineReplay.target(step.before,step.rel);
   if(old==null||row(old)%100!=99||old.res<102||old.res>104||step.tool!=0&&step.tool!=1026)continue;
   int shift=(rewardRow-row(old))*6;MinePlanner.Cell cell=live.get(old.id+shift);
   if(cell==null||cell.res<102||cell.res>104||cell.ground!=old.ground)continue;
   int overlap=0;boolean matches=true;
   for(MinePlanner.Cell before:step.before.cells){if(row(before)!=row(old))continue;MinePlanner.Cell now=live.get(before.id+shift);if(now==null)continue;overlap++;if(!sameTile74(now,before)){matches=false;break;}}
   if(!matches||overlap<4)continue;
   int score=(shift==0?2:0)+(!db.playGroup70.isEmpty()&&db.playGroup70.equals(step.group)&&db.playSource70==route.source?4:0);
   if(score<best)continue;chosen=step;target=cell;best=score;
  }
  if(chosen==null)return null;
  if(target.unlocked!=1)return new MinePlanner.Plan("录制的百层奖励尚未解锁，等待矿图更新");
  if(chosen.tool==1026&&m.picks<=0)return pause("百层奖励尚未挖完，镐子不足，等待恢复");
  return plan(target,chosen.tool,"先完成当前路线的百层奖励，再识别下一张随机地图");
 }
 static boolean sameTile74(MinePlanner.Cell a,MinePlanner.Cell b){
  // Catching a pet is a separate manual interaction, at any map row. Its
  // cleared reward cell must not invalidate the demonstrated mining route.
  if(b.res>=9001&&b.res<=9003){
   // Pet tier is randomized inside ordinary maps too, not just reward rows.
   if(a.res>=9001&&a.res<=9003&&a.ground==b.ground)return true;
   if(a.res==0&&a.ground==0&&a.ground<=b.ground)return true;
  }
  if(a.ground!=b.ground)return false;if(a.res==b.res)return true;
  if(row(a)%100!=99||row(b)%100!=99)return false;
  return a.res>=102&&a.res<=104&&b.res>=102&&b.res<=104||a.res>=9001&&a.res<=9003&&b.res>=9001&&b.res<=9003;
 }
 static MinePlanner.Plan recorded70(MineReplay db,MinePlanner.MapData m,MinePlanner.MapData world){
  Map<Integer,MinePlanner.Cell> live=new HashMap<Integer,MinePlanner.Cell>(),all=new HashMap<Integer,MinePlanner.Cell>();for(MinePlanner.Cell c:world.cells)all.put(c.id,c);for(MinePlanner.Cell c:m.cells){live.put(c.id,c);all.put(c.id,c);}
  MineReplay.Step selected=null;MinePlanner.Cell target=null;int ordinal=0,total=0,coverage=0;boolean exactSource=false;
  Map<String,Integer> lengths=new HashMap<String,Integer>();for(MineReplay.Step s:db.steps)if(s.enabled){String k=s.group+":"+block(s.before.depth);Integer n=lengths.get(k);lengths.put(k,n==null?1:n+1);}
  if(db.playStart70!=block(m.depth)){db.playGroup70="";db.playSource70=-1;}
  for(MineReplay.Step s:db.steps){
   if(!db.playGroup70.isEmpty()&&(!db.playGroup70.equals(s.group)||db.playSource70!=block(s.before.depth)))continue;
   MineReplay.Group g=db.groups.get(s.group);if(!db.acceptsGroup(s.group)||!s.enabled||g!=null&&!g.enabled||!geometry(s.before))continue;
   int delta=block(m.depth)-block(s.before.depth);if(m.depth!=s.before.depth+delta)continue;
   int overlap=0,intact=0;boolean mismatch=false;
   for(MinePlanner.Cell old:s.before.cells){if(block(row(old))!=block(s.before.depth)&&row(old)%100!=99)continue;MinePlanner.Cell c=live.get(old.id+delta*6);if(c==null)continue;overlap++;if(!sameTile74(c,old)){mismatch=true;break;}if(c.ground>0)intact++;}
   if(mismatch||overlap<24||intact<6)continue;
   MinePlanner.Cell old=MineReplay.target(s.before,s.rel),c=live.get(old.id+delta*6);if(c==null||!sameTile74(c,old))continue;if(old.res>=9001&&old.res<=9003&&c.res==0&&c.ground==0)continue;
   // Prefer this exact segment when available; otherwise keep the fuller
   // demonstrated segment rather than replacing it with a partial later take.
   boolean sameSource=delta==0;int length=lengths.get(s.group+":"+block(s.before.depth));if(selected!=null&&(exactSource&&!sameSource||exactSource==sameSource&&length<coverage))continue;
   selected=s;target=c;exactSource=sameSource;coverage=length;
  }
  if(selected==null&&m.fallbackPicks&&(!m.allowTools||m.lineTools<=0||m.areaTools<=0)){
   MinePlanner.Plan fallback=chooseInternal70(db,m,world,world,false);fallback.note="道具不可用，镐替代模式："+fallback.note;return fallback;
  }
  if(selected==null)return pause("当前矿格状态没有对应录制步骤；已暂停，不自动绕路或消耗道具");
  for(MineReplay.Step s:db.steps)if(s.group.equals(selected.group)&&s.enabled){total++;if(s==selected)ordinal=total;}
  String title=db.groups.containsKey(selected.group)?db.groups.get(selected.group).name:selected.group;
  if(block(row(target))==block(m.depth)&&row(target)%100!=99){db.playGroup70=selected.group;db.playSource70=block(selected.before.depth);db.playStart70=block(m.depth);}
  int tool=selected.tool;
  if(tool==1028||tool==1029){
   if(!m.allowTools||tool==1028&&m.lineTools<=0||tool==1029&&m.areaTools<=0){
    if(m.fallbackPicks){
     // Only the user's explicit fallback setting permits an alternative route.
     // Use the next recorded pick as goal, never spend another kind of tool.
     int selectedAt=db.steps.indexOf(selected);for(int n=selectedAt+1;n<db.steps.size();n++){MineReplay.Step next=db.steps.get(n);if(!next.group.equals(selected.group))break;if(!next.enabled||next.tool!=1026)continue;int delta=m.depth-selected.before.depth;MinePlanner.Cell old=MineReplay.target(next.before,next.rel),goal=all.get(old.id+delta*6);if(goal==null)continue;MinePlanner.Cell first=path(m,all,goal);if(first==null)continue;if(m.picks<=0)return pause("镐子不足，等待自然恢复；不自动购买或改用其他道具");return plan(first,1026,"道具不可用，按已开启的镐替代设置开路");}
    }
    return pause("记录需要"+MineReplay.toolName(tool)+"，当前已关闭或库存不足；开启道具/补充库存后继续");
   }
   MinePlanner.Plan guard=guardRange58(m,all,target,tool);if(guard!=null)return guard;
  }else{
   if(target.unlocked!=1)return new MinePlanner.Plan("录制落点尚未解锁，等待游戏更新；不另选路线");
   if(tool==1026&&m.picks<=0)return pause("镐子不足，等待自然恢复；保持开启会自动继续，不自动购买");
  }
  return plan(target,tool,"按记录复刻："+title+" · "+ordinal+"/"+total+" · "+MineReplay.toolName(tool));
 }
 static MinePlanner.Plan guardRange58(MinePlanner.MapData m,Map<Integer,MinePlanner.Cell> all,MinePlanner.Cell center,int tool){
  MinePlanner.Plan gold75=MinePlanner.goldStop75(m,Collections.<Integer,Integer>emptyMap());if(gold75!=null)return gold75;
  for(MinePlanner.Cell c:all.values())if(MinePlanner.footprint(m,center,c,tool)&&(c.res==9003&&m.goldPriority58>0||row(c)%100==99&&c.res>=102&&c.res<=104)){
   MinePlanner.Cell live=find(m,c.id);if(live==null)continue;
   String location="本百层第"+(row(c)%100+1)+"排第"+(c.col()+1)+"列";
   if(c.res!=9003){if(c.ground==0&&c.unlocked==1)return plan(c,0,"先收取范围内百层魔石："+location);continue;}
   if(c.res==9003&&c.ground==0&&c.unlocked==1)return pause("发现金蛋："+location+"，请手动捕捉；处理后自动继续");
   if(c.unlocked==1&&c.ground>0&&m.picks>0)return plan(c,1026,"先用镐打开范围内"+(c.res==9003?"金蛋":"百层魔石")+"："+location);
   MinePlanner.Cell first=path(m,all,c);if(first!=null&&m.picks>0)return plan(first,1026,"用镐接近范围内"+(c.res==9003?"金蛋":"百层魔石")+"："+location);
   return pause((c.res==9003?"金蛋":"百层魔石")+"在"+MineReplay.toolName(tool)+"范围内："+location+"；当前无法自动接近，请手动处理或关闭范围道具改用镐");
  }return null;
 }
 static int rangeNeeded(MineReplay.Step s,MineReplay.Step next,Map<Integer,MinePlanner.Cell> now,int start){int changed=0,needed=0;Map<Integer,MinePlanner.Cell> after=new HashMap<Integer,MinePlanner.Cell>();if(next!=null)for(MinePlanner.Cell c:next.before.cells)after.put(c.id,c);MinePlanner.Cell center=MineReplay.target(s.before,s.rel);for(MinePlanner.Cell c:s.before.cells){if(block(row(c))!=block(row(center)))continue;MinePlanner.Cell post=after.get(c.id);if(post==null||c.ground==post.ground&&c.res==post.res)continue;changed++;MinePlanner.Cell cur=cellAt(now,start,pos(c));if(cur==null)return -1;if(cur.ground>post.ground||cur.res!=post.res&&cur.res!=0)needed++;}if(changed>0)return needed>0?1:0;MinePlanner.Cell cur=cellAt(now,start,pos(center));if(cur==null)return -1;if(center.ground>0)return cur.ground>0?1:0;return -1;}
 static MinePlanner.Cell find(MinePlanner.MapData m,int id){for(MinePlanner.Cell c:m.cells)if(c.id==id)return c;return null;}
 static MinePlanner.Plan plan(MinePlanner.Cell c,int tool,String note){MinePlanner.Plan p=new MinePlanner.Plan(note);p.index=c.index;p.id=c.id;p.tool=tool;return p;}
 static class Node implements Comparable<Node>{int id,cost,first;Node(int id,int cost,int first){this.id=id;this.cost=cost;this.first=first;}public int compareTo(Node n){return Integer.compare(cost,n.cost);}}
 static MinePlanner.Cell path(MinePlanner.MapData current,Map<Integer,MinePlanner.Cell> cells,MinePlanner.Cell goal){
  Map<Integer,Integer> best=new HashMap<Integer,Integer>();PriorityQueue<Node> queue=new PriorityQueue<Node>();for(MinePlanner.Cell c:current.cells)if(c.unlocked==1&&c.ground<=2&&(c.res<9001||c.res>9003)){int cost=c.ground==0?0:c.ground==1?1:2;int first=cost>0?c.id:-1;Integer old=best.get(c.id);if(old==null||cost<old){best.put(c.id,cost);queue.add(new Node(c.id,cost,first));}}
  while(!queue.isEmpty()){Node n=queue.poll();if(n.cost!=best.get(n.id))continue;if(n.id==goal.id)return n.first<0?null:find(current,n.first);MinePlanner.Cell c=cells.get(n.id);if(c==null)continue;for(int d:new int[]{-6,6,-1,1}){MinePlanner.Cell t=cells.get(n.id+d);if(t==null||Math.abs(d)==1&&row(c)!=row(t)||t.ground>2||t.res>=9001&&t.res<=9003)continue;int work=t.ground==0?0:t.ground==1?1:2,cost=n.cost+work;Integer old=best.get(t.id);if(old==null||cost<old){int first=n.first>=0?n.first:work>0?t.id:-1;best.put(t.id,cost);queue.add(new Node(t.id,cost,first));}}}
  return null;
 }
}
