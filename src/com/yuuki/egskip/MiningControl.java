package com.yuuki.egskip;
import android.content.*;
import java.util.*;
import java.io.*;
/** Pure planning runs in the helper's existing serial worker; game objects stay on native main thread. */
final class MiningControl {
 static MinePlanner.MapData pendingMap63,expiredMap63;static int pendingIndex63,pendingGrid63,pendingTool63;static boolean sent63;
 static void clearReceipt63(){pendingMap63=null;expiredMap63=null;sent63=false;}
 static boolean matchesReceipt63(String[] v,MinePlanner.MapData m){try{return m!=null&&v.length==5&&v[0].equals(m.nonce)&&Integer.parseInt(v[2])==pendingIndex63&&Integer.parseInt(v[3])==pendingGrid63&&Integer.parseInt(v[4])==pendingTool63;}catch(NumberFormatException ignored){return false;}}
 static void receipt63(Context c,String raw)throws Exception{
  String[] v=raw.trim().split("\\n");
  if(pendingMap63==null){if(matchesReceipt63(v,expiredMap63)&&v[1].equals("ACK")){demos(c).issued(expiredMap63);expiredMap63=null;planned="";}return;}
  if(!matchesReceipt63(v,pendingMap63))return;
  if(v[1].equals("TIMEOUT")){expiredMap63=pendingMap63;pendingMap63=null;sent63=false;demos(c).reset();planned="";return;}
  if(v[1].equals("SENT")){sent63=true;return;}
  if(v[1].equals("ACK")){demos(c).issued(pendingMap63);clearReceipt63();planned="";}
 }
 static String execution62="",planNote62="";
 static void execution62(String raw){execution62="";String[] lines=raw.trim().split("\\n",2);if(lines.length!=2)return;try{long age=System.currentTimeMillis()/1000-Long.parseLong(lines[0].trim());if(age>=-5&&age<=12)execution62=lines[1].trim();}catch(Exception ignored){}}
 static String executionNote62(){return execution62.isEmpty()?"已规划，等待游戏执行："+planNote62:execution62+(planNote62.isEmpty()?"":" · "+planNote62);}
 static String manualAlert="",lastMineAlert="";
 static String pendingPause57="";static long pauseSince57;static boolean mineAlert57;
 static void clearPause57(){pendingPause57="";if(mineAlert57){newAlert="";mineAlert57=false;}}
 static boolean stablePause57(String reason,long now){if(!reason.equals(pendingPause57)){pendingPause57=reason;pauseSince57=now;return false;}return now-pauseSince57>=1500;}

 static Map<Integer,Integer> priorities(Context c){Map<Integer,Integer> r=new HashMap<Integer,Integer>();for(Map.Entry<String,?> e:c.getSharedPreferences("mine_priority50",0).getAll().entrySet())try{int kind=Integer.parseInt(e.getKey()),v=((Number)e.getValue()).intValue();if(MinePlanner.priorityEditable(kind)&&v>=0&&v<=5)r.put(kind,v);}catch(Exception ignored){}return r;}
 static void priority(Context c,int kind,int value){if(MinePlanner.priorityEditable(kind)&&value>=0&&value<=5)c.getSharedPreferences("mine_priority50",0).edit().putInt(String.valueOf(kind),value).apply();planned="";}
 static void resetPriorities(Context c){c.getSharedPreferences("mine_priority50",0).edit().clear().apply();planned="";}
 static String gradeRaw="";static MinePlanner.MapData latest;static String planned="",note="打开矿井后等待地图",refine="",lastAlert="";static String newAlert="";
 static Map<Integer,Integer> labels(Context c){Map<Integer,Integer> r=new HashMap<Integer,Integer>();for(Map.Entry<String,?> e:c.getSharedPreferences("mine_labels47",0).getAll().entrySet())try{int k=((Number)e.getValue()).intValue();if(k>0&&k<MinePlanner.LABELS.length)r.put(Integer.parseInt(e.getKey()),k);}catch(Exception ignored){}return r;}
 static volatile MineReplay demos;static volatile int demoCount;static String demoError="";
 static File demoFile(Context c){return new File(c.getFilesDir(),"mine-demos53.txt");}
 static MineReplay demos(Context c)throws Exception{if(demos==null){try{demos=MineReplay.read(demoFile(c));}catch(Exception e){demoError="记录文件损坏，请从导出或备份恢复";throw e;}demoCount=demos.steps.size();}return demos;}
 static String replayGroup(Context c){
  return c.getSharedPreferences("floating",0).getString("mine_replay_group","");
 }
 static String replayTitle(Context c){
  String id=replayGroup(c);
  if(id.isEmpty())return "全部启用分组";
  MineReplay db=demos;
  MineReplay.Group group=db==null?null:db.group(id);
  return group==null?"分组不可用":group.name;
 }
 static void startReplay(Context c,String id)throws Exception{
  MineReplay db=demos(c);
  if(!id.isEmpty()&&(db.group(id)==null||!db.group(id).enabled||db.count(id)==0))
   throw new IOException("请选择有记录且已启用的分组");
  c.getSharedPreferences("floating",0).edit().putString("mine_replay_group",id).commit();
  startMode(c,2);
 }
 static MineReplay replay(Context c)throws Exception{
  MineReplay db=demos(c);String selected=replayGroup(c);
  if(!db.replayGroup.equals(selected)){db.replayGroup=selected;db.revision++;db.reset();}
  return db;
 }
 static MineReplay copy(Context c)throws Exception{return MineReplay.decode(demos(c).encode());}
 static String activeGroup(Context c)throws Exception{MineReplay db=demos(c);String id=String.valueOf(c.getSharedPreferences("floating",0).getLong("mine_group54",0));if(db.groups.containsKey(id))return id;if(!db.groups.isEmpty()){id=db.groups.keySet().iterator().next();c.getSharedPreferences("floating",0).edit().putLong("mine_group54",Long.parseLong(id)).commit();return id;}return newGroup(c,"地图 1");}
 static String groupTitle(Context c){MineReplay db=demos;String id=String.valueOf(c.getSharedPreferences("floating",0).getLong("mine_group54",0));MineReplay.Group g=db==null?null:db.group(id);return g==null?"未选择地图":g.name;}
 static String newGroup(Context c,String name)throws Exception{MineReplay db=copy(c);long value=System.currentTimeMillis();while(db.groups.containsKey(String.valueOf(value)))value++;String id=String.valueOf(value);db.addGroup(id,name);db.save(demoFile(c));demos=db;demoCount=db.steps.size();c.getSharedPreferences("floating",0).edit().putLong("mine_group54",value).commit();planned="";return id;}
 static void selectGroup(Context c,String id)throws Exception{if(!demos(c).groups.containsKey(id))throw new IOException("地图已不存在");c.getSharedPreferences("floating",0).edit().putLong("mine_group54",Long.parseLong(id)).commit();planned="";}
 static void editGroup(Context c,String id,String name,boolean enabled,boolean delete)throws Exception{mode(c,3);MineReplay db=copy(c);MineReplay.Group g=db.group(id);if(g==null)throw new IOException("地图已不存在");if(delete){for(Iterator<MineReplay.Step> it=db.steps.iterator();it.hasNext();)if(it.next().group.equals(id))it.remove();db.groups.remove(id);db.retiredGroups.add(id);}else{if(name.trim().isEmpty()||name.length()>120)throw new IOException("名称需1至120字");g.name=name.trim();g.enabled=enabled;}db.save(demoFile(c));demos=db;demoCount=db.steps.size();planned="";}
 static void startMode(Context c,int value)throws Exception{
  mode(c,value);
  RootOps.Result off=RootOps.patchFeatures64(c,2097152,0,"矿井显式重连，只关闭矿井开关");if(!off.ok)throw new IOException(off.text);
  RootOps.Result clean=RootOps.miningShell(c,": > "+RootOps.q(RootOps.FLAGS+"/mine-alert.txt")+" || exit 4\n: > "+RootOps.q(RootOps.FLAGS+"/mine-full56.txt")+" || exit 4\n: > "+RootOps.q(RootOps.FLAGS+"/mine-snapshot.txt")+" || exit 4\n");if(!clean.ok)throw new IOException(clean.text);
  world56.clear();origin56.clear();fullRaw56="";fullReadAt78=0;mode(c,value);
  RootOps.Result r=RootOps.patchFeatures64(c,2097152,2097152,"矿井录制/复刻重连，只开启矿井开关");if(!r.ok)throw new IOException(r.text);
  note=value==1?"记录已启用，等待第一笔确认操作":"复刻正在重连新矿图，已有采矿请求仍等待确认";
 }
 static MinePlanner.MapData full56;static String fullRaw56="";static final TreeMap<Integer,MinePlanner.Cell> world56=new TreeMap<Integer,MinePlanner.Cell>(),origin56=new TreeMap<Integer,MinePlanner.Cell>();static boolean recovering56;
 static MinePlanner.MapData origin56(){MinePlanner.MapData m=new MinePlanner.MapData();m.cells.addAll(origin56.values());return m;}
 static MinePlanner.MapData world56(MinePlanner.MapData current){for(MinePlanner.Cell cell:current.cells)world56.put(cell.id,cell);MinePlanner.MapData w=new MinePlanner.MapData();w.cells.addAll(world56.values());return w;}
 static volatile String recorderStatus="尚未收到矿图；请开启录制后在矿井操作";
 static int mode(Context c){return c.getSharedPreferences("floating",0).getInt("mine_mode53",3);}
 static String modeText(Context c){return mode(c)==3?"录制与复刻已暂停 · "+demoCount+"步":mode(c)==1?"手动记录 · "+demoCount+"步":mode(c)==2?(manualAlert.isEmpty()?"自动复刻 · ":"复刻暂停 · ")+demoCount+"步":"原有选路";}
 static void mode(Context c,int value)throws Exception{
  clearPause57();clearReceipt63();
  if(value<0||value>3)throw new IllegalArgumentException();demos(c);
  String path=RootOps.FLAGS+"/mine-record-mode";String script=": > "+RootOps.q(RootOps.FLAGS+"/mine-plan.txt")+" || exit 4\nrm -f "+RootOps.q(RootOps.FLAGS+"/mine-mark.txt")+"\n";
  if(value==1)script+="printf '%s' "+RootOps.q(activeGroup(c)+":"+System.currentTimeMillis())+" > "+RootOps.q(path+".tmp")+" && chmod 644 "+RootOps.q(path+".tmp")+" && mv -f "+RootOps.q(path+".tmp")+" "+RootOps.q(path)+" || exit 4\n";else script+="rm -f "+RootOps.q(path)+" || exit 4\n";
  script+=value==3?": > "+RootOps.q(RootOps.FLAGS+"/mine-paused62")+" || exit 4\n":"rm -f "+RootOps.q(RootOps.FLAGS+"/mine-paused62")+" || exit 4\n";
  RootOps.Result r=RootOps.miningShell(c,script);if(!r.ok)throw new IOException(r.text);c.getSharedPreferences("floating",0).edit().putInt("mine_mode53",value).commit();planned="";demos(c).reset();manualAlert="";lastMineAlert="";note=modeText(c);
 }
 static String eventRead(Context c){long offset=c.getSharedPreferences("mine_recorder53",0).getLong("offset",0);String path=RootOps.q(RootOps.FLAGS+"/mine-demo-events.log");return "printf '\n__EVENTS53__\n'\nif [ -f "+path+" ]; then n=$(wc -c < "+path+"); if [ \"$n\" -lt "+offset+" ]; then printf '__RESET53__\n'; tail -c +1 "+path+" | head -c 131072; else tail -c +"+(offset+1)+" "+path+" | head -c 131072; fi; fi\nprintf '\n__EVENT_END53__\n'\n";}
 static void ingest(Context c,String chunk)throws Exception{int end=chunk.indexOf("__EVENT_END53__");if(end<0)return;chunk=chunk.substring(0,end);if(chunk.startsWith("\n"))chunk=chunk.substring(1);boolean reset=chunk.startsWith("__RESET53__\n");if(reset)chunk=chunk.substring(12);recovering56=chunk.length()>=131072;MineReplay.Batch b=MineReplay.events(chunk);if(b.consumed==0)return;MineReplay db=demos(c);MineReplay pending=MineReplay.decode(db.encode());boolean changed=false;for(MineReplay.Step s:b.steps)changed|=pending.add(s);if(changed){pending.save(demoFile(c));db.steps.clear();db.steps.addAll(pending.steps);db.groups.clear();db.groups.putAll(pending.groups);db.revision++;if(mode(c)==1)manualAlert="";planned="";}long offset=reset?0:c.getSharedPreferences("mine_recorder53",0).getLong("offset",0);c.getSharedPreferences("mine_recorder53",0).edit().putLong("offset",offset+b.consumed).commit();demoCount=db.steps.size();}
 static void saveStep(Context c,MineReplay.Step edited,boolean delete)throws Exception{MineReplay db=demos(c);MineReplay replacement=MineReplay.decode(db.encode());boolean found=false;for(int i=0;i<replacement.steps.size();i++)if(replacement.steps.get(i).id.equals(edited.id)){if(delete)replacement.steps.remove(i);else{MineReplay.validate(edited);replacement.steps.set(i,edited);}found=true;break;}if(!found)throw new IOException("该记录已不存在，请刷新列表");replacement.save(demoFile(c));demos=replacement;demoCount=demos.steps.size();planned="";}
 static final String EXPORT53="/storage/emulated/0/Download/像素助手记录/矿井示范.txt";
 static void transfer(Context c,boolean importing)throws Exception{
  if(importing){RootOps.Result r=RootOps.miningShell(c,"wc -c < "+RootOps.q(EXPORT53)+" || exit 4\n");String n=r.text.split("\n")[0].trim();if(!r.ok||!n.matches("[0-9]+")||Long.parseLong(n)>16000000)throw new IOException("导入文件不存在或超过16MB");File temp=new File(c.getFilesDir(),"mine-import53.txt");new FileOutputStream(temp).close();r=RootOps.miningShell(c,"cp "+RootOps.q(EXPORT53)+" "+RootOps.q(temp.getAbsolutePath())+" || exit 4\nchmod 666 "+RootOps.q(temp.getAbsolutePath())+" || exit 4\n");if(!r.ok)throw new IOException(r.text);MineReplay incoming=MineReplay.read(temp);MineReplay merged=MineReplay.decode(demos(c).encode());for(MineReplay.Group g:incoming.groups.values())if(!merged.groups.containsKey(g.id)){merged.addGroup(g.id,g.name);merged.group(g.id).enabled=g.enabled;}for(MineReplay.Step s:incoming.steps)merged.add(s);merged.save(demoFile(c));demos=merged;demoCount=merged.steps.size();planned="";temp.delete();}
  else{demos(c).save(demoFile(c));String events=RootOps.FLAGS+"/mine-demo-events.log",dest="/storage/emulated/0/Download/像素助手记录/矿井原始事件.log";RootOps.Result r=RootOps.miningShell(c,"mkdir -p "+RootOps.q("/storage/emulated/0/Download/像素助手记录")+" && cp "+RootOps.q(demoFile(c).getAbsolutePath())+" "+RootOps.q(EXPORT53)+" && chmod 644 "+RootOps.q(EXPORT53)+" || exit 4\nif [ -f "+RootOps.q(events)+" ]; then tail -c 33554432 "+RootOps.q(events)+" > "+RootOps.q(dest+".tmp")+" && chmod 644 "+RootOps.q(dest+".tmp")+" && mv -f "+RootOps.q(dest+".tmp")+" "+RootOps.q(dest)+" || exit 4; fi\n");if(!r.ok)throw new IOException(r.text);}
 }
 static long fullReadAt78;
 static String readCommand73(Context c,int mask){
  boolean mine=(mask&2097152)!=0,ref=(mask&4194304)!=0;long now78=System.nanoTime()/1000000L;boolean readFull78=fullReadAt78==0||now78-fullReadAt78>=200;if(mine&&readFull78)fullReadAt78=now78;StringBuilder b=new StringBuilder();
  if(mine)b.append("if [ -f ").append(RootOps.q(RootOps.FLAGS+"/feature2097152")).append(" ]; then cat ").append(RootOps.q(RootOps.FLAGS+"/mine-snapshot.txt")).append(" 2>/dev/null; fi\n");
  String[] marks={"__MINE_ALERT50__","__REFINE46__","__GRADES46__"},names={"mine-alert.txt","refine-alert.txt","refine-values.txt"};
  for(int j=0;j<marks.length;j++){b.append("printf '\\n").append(marks[j]).append("\\n'\n");if(j==0?mine:ref)b.append("cat ").append(RootOps.q(RootOps.FLAGS+"/"+names[j])).append(" 2>/dev/null\n");}
  if(mine&&mode(c)==1)b.append(eventRead(c));else b.append("printf '\\n__EVENTS53__\\n__EVENT_END53__\\n'\n");
  String[] suffix={"__FULL56__","__EXEC62__","__RECEIPT63__"},files={"mine-full56.txt","mine-execution62.txt","mine-receipt63.txt"};
  for(int j=0;j<suffix.length;j++){b.append("printf '\\n").append(suffix[j]).append("\\n'\n");if(mine&&(j!=0||readFull78))b.append("cat ").append(RootOps.q(RootOps.FLAGS+"/"+files[j])).append(" 2>/dev/null || true\n");}
  return b.toString();
 }
 static String plannerWrite77(MinePlanner.MapData m,MinePlanner.Plan p){String path=RootOps.FLAGS+"/mine-planner77.txt",value=System.currentTimeMillis()/1000+"\n"+(p.alert?"暂停":p.index>=0?"已规划":"等待")+"："+p.note+"\n快照="+m.nonce+"；深度="+m.depth+"；格子="+p.id+"；工具="+p.tool+"\n";return "printf '%s' "+RootOps.q(value)+" > "+RootOps.q(path+".tmp")+" && chmod 666 "+RootOps.q(path+".tmp")+" && mv -f "+RootOps.q(path+".tmp")+" "+RootOps.q(path)+" || true\n";}
 static void step(Context c,int mask)throws Exception{
  if(c.getSharedPreferences("mine_recorder53",0).getInt("schema56",0)==0)c.getSharedPreferences("mine_recorder53",0).edit().putLong("offset",0).putInt("schema56",1).commit();
  if((mask&2097152)==0){manualAlert="";lastMineAlert="";}if((mask&(2097152|4194304))==0)return;
  RootOps.Result r=RootOps.miningShell(c,readCommand73(c,mask));if(!r.ok)return;
  String[] receiptParts63=r.text.split("__RECEIPT63__",2);if(mode(c)==2&&receiptParts63.length>1)receipt63(c,receiptParts63[1].replaceAll("退出码: [0-9]+",""));
  String[] executionParts62=receiptParts63[0].split("__EXEC62__",2);execution62(executionParts62.length>1?executionParts62[1].replaceAll("退出码: [0-9]+",""):"");
  String[] fullParts=executionParts62[0].split("__FULL56__",2);if(fullParts.length==2){String full=fullParts[1].replaceAll("退出码: [0-9]+","").trim();if(!full.equals(fullRaw56)&&!full.isEmpty())try{full56=MinePlanner.parse(full);world56.clear();for(MinePlanner.Cell cell:full56.cells){world56.put(cell.id,cell);MinePlanner.Cell old=origin56.get(cell.id);if(old==null||old.ground==0&&cell.ground>0)origin56.put(cell.id,cell);}while(origin56.size()>4096)origin56.pollFirstEntry();fullRaw56=full;}catch(Exception ignored){}}
  String[] eventParts=fullParts[0].split("__EVENTS53__",2);if(eventParts.length==2&&mode(c)==1)try{ingest(c,eventParts[1]);demoError="";}catch(Exception e){demoError="记录未保存："+e.getMessage();note=demoError;if(!demoError.equals(lastMineAlert)){lastMineAlert=demoError;newAlert=demoError;}return;}
  if(mode(c)==1&&recovering56){note="正在恢复旧的道具记录，请稍候";return;}
  String[] parts=eventParts[0].split("__REFINE46__",2);if(parts.length>1){String[] grades=parts[1].split("__GRADES46__",2);if(grades.length>1)gradeRaw=grades[1].replaceAll("退出码: [0-9]+","").trim();String v=grades[0].replaceAll("退出码: [0-9]+","").trim();refine=v;if((mask&4194304)!=0&&!v.isEmpty()&&!v.equals(lastAlert)){lastAlert=v;newAlert=v.replaceFirst("^[0-9]+:","");}}
  if((mask&2097152)==0)return;if(mode(c)==3){note="录制与复刻已暂停；点击开始记录或按记录复刻继续";recorderStatus=note;return;}String[] mineParts=parts[0].split("__MINE_ALERT50__",2);String runtimeAlert=mineParts.length>1?mineParts[1].replaceAll("退出码: [0-9]+","").trim():"";if(!runtimeAlert.isEmpty()&&!runtimeAlert.equals(lastMineAlert)){lastMineAlert=runtimeAlert;newAlert=runtimeAlert.replaceFirst("^[0-9]+:","");manualAlert=newAlert;}String raw=mineParts[0].trim();if(raw.isEmpty()){note=execution62.isEmpty()?"等待游戏发布矿图；请确认已进入矿井":execution62;recorderStatus=note;return;}
  MinePlanner.MapData m;try{m=MinePlanner.parse(raw);}catch(Exception e){note="等待完整地图";return;}m.fastDescent=c.getSharedPreferences("floating",0).getInt("mine_fast51",1)!=0;m.allowTools=c.getSharedPreferences("floating",0).getInt("mine_tools51",1)!=0;m.fallbackPicks=c.getSharedPreferences("floating",0).getInt("mine_fallback57",1)!=0;latest=m;recorderStatus=mode(c)==1?"已连接矿图；确认动作累计保存 "+demoCount+"步":"矿图已连接";
  int mode=mode(c);if(mode==2&&!runtimeAlert.isEmpty()){manualAlert=runtimeAlert.replaceFirst("^[0-9]+:","");note=manualAlert;return;}if(mode==1){note="手动记录中 · "+groupTitle(c)+" · 已保存"+demoCount+"步；请直接在游戏里挖矿，录完点按记录复刻";return;}
  if(mode==2&&sent63&&MinePlanner.goldStop75(m,labels(c))==null){note="采矿操作已发送，等待匹配回包确认";return;}
  Map<Integer,Integer> labels=labels(c);Map<Integer,Integer> priorities=priorities(c);m.goldPriority58=MinePlanner.priority(MinePlanner.GOLD,priorities);String key=mode+raw+labels.toString()+priorities.toString()+m.fastDescent+m.allowTools+m.fallbackPicks+fullRaw56.hashCode();if(key.equals(planned)){if(manualAlert.isEmpty())note=executionNote62();return;}
  MinePlanner.Plan p=MinePlanner.goldStop75(m,labels);if(p==null)p=mode==2?MineRoute56.chooseRecorded70(replay(c),m,world56(m),origin56()):MinePlanner.choose(m,labels,priorities);
  if(mode==2&&p.alert){RootOps.miningShell(c,": > "+RootOps.q(RootOps.FLAGS+"/mine-plan.txt")+" || exit 4\n");note=p.note;
   if((p.note.contains("当前矿格状态没有对应录制步骤")||p.note.contains("未录过")||p.note.contains("未识别")||p.note.contains("首排相同")||p.note.contains("目标资源与当前矿格不同"))&&!stablePause57(p.note+":"+MineReplay.shape(m,true),System.nanoTime()/1000000L)){manualAlert="";note="核对矿图状态，请稍候";return;}
   RootOps.miningShell(c,plannerWrite77(m,p));manualAlert=p.note;if(!p.note.equals(lastMineAlert)){lastMineAlert=p.note;newAlert=p.note;mineAlert57=true;}planned=key;return;}clearPause57();String body="if [ -f "+RootOps.q(RootOps.FLAGS+"/feature2097152")+" ]; then printf '%s' "+RootOps.q(p.encode(m.nonce))+" > "+RootOps.q(RootOps.FLAGS+"/mine-plan.tmp")+" && chmod 666 "+RootOps.q(RootOps.FLAGS+"/mine-plan.tmp")+" && mv -f "+RootOps.q(RootOps.FLAGS+"/mine-plan.tmp")+" "+RootOps.q(RootOps.FLAGS+"/mine-plan.txt")+"; fi\n";
  RootOps.Result wrote=RootOps.miningShell(c,body+"plan_result77=$?\n"+plannerWrite77(m,p)+"exit \"$plan_result77\"\n");if(!wrote.ok){note="路线写入失败，未执行";return;}if(mode==2&&p.index>=0){expiredMap63=null;pendingMap63=m;pendingIndex63=p.index;pendingGrid63=p.id;pendingTool63=p.tool;sent63=false;}planned=key;planNote62=p.note;note=p.index>=0?"已规划，等待游戏执行："+p.note:p.note;if(!runtimeAlert.isEmpty()){manualAlert=runtimeAlert.replaceFirst("^[0-9]+:","");return;}
  manualAlert=p.alert?p.note:"";if(p.alert){String token=p.alertKey.isEmpty()?p.note:p.alertKey;if(!token.equals(lastMineAlert)){lastMineAlert=token;newAlert=p.note;}}else lastMineAlert="";
 }
 static void highlight(Context c,int resource)throws Exception{
  String b;if(resource>0)b="printf '%s' "+RootOps.q(String.valueOf(resource))+" > "+RootOps.q(RootOps.FLAGS+"/mine-mark.tmp")+" && chmod 666 "+RootOps.q(RootOps.FLAGS+"/mine-mark.tmp")+" && mv -f "+RootOps.q(RootOps.FLAGS+"/mine-mark.tmp")+" "+RootOps.q(RootOps.FLAGS+"/mine-mark.txt")+" || exit 4\n: > "+RootOps.q(RootOps.FLAGS+"/mine-plan.txt")+"\n";
  else b=": > "+RootOps.q(RootOps.FLAGS+"/mine-plan.txt")+" || exit 4\nrm -f "+RootOps.q(RootOps.FLAGS+"/mine-mark.txt")+"\n";
  RootOps.Result r=RootOps.miningShell(c,b);if(!r.ok)throw new java.io.IOException(r.text);planned="";
 }
 static void saveRefineRules76(Context c,int mode,int ss,int sss,boolean locked)throws Exception{
  if(mode<0||mode>2||ss<0||ss>5||sss<0||sss>5||mode==0&&ss==0&&sss==0)throw new IllegalArgumentException("数量模式至少开启一个数量条件（1至5条）");
  String value=mode+"\n"+ss+"\n"+sss+"\n"+(locked?1:0)+"\n";RootOps.Result r=RootOps.miningShell(c,RootOps.refineRulesWrite76(value));if(!r.ok)throw new IOException(r.text);
  if(!c.getSharedPreferences("floating",0).edit().putInt("refine_mode76",mode).putInt("refine_ss76",ss).putInt("refine_sss76",sss).putInt("refine_locked76",locked?1:0).commit())throw new IOException("暂停条件偏好保存失败");refine="";lastAlert="";
 }
 static String refineKeep71(Context c){return c.getSharedPreferences("floating",0).getString("refine_keep71","\n攻击\n速度\nAttack\nSpeed\n");}
 static void saveRefineKeep71(Context c,String value)throws Exception{
  if(value.length()>16000)throw new IOException("属性选择过多");String path=RootOps.FLAGS+"/refine-keep71.txt";
  RootOps.Result r=RootOps.miningShell(c,"printf '%s' "+RootOps.q(value)+" > "+RootOps.q(path+".tmp")+" && chmod 666 "+RootOps.q(path+".tmp")+" && mv -f "+RootOps.q(path+".tmp")+" "+RootOps.q(path)+" || exit 4\n");if(!r.ok)throw new IOException(r.text);
  c.getSharedPreferences("floating",0).edit().putString("refine_keep71",value).commit();refine="";lastAlert="";
 }
 static void saveGrades(Context c,int ss,int sss)throws Exception{if(ss<1||sss<=ss)throw new IllegalArgumentException("SSS必须高于SS，请核对所选条目");RootOps.Result r=RootOps.miningShell(c,"printf '%s' "+RootOps.q(ss+"\n"+sss+"\n")+" > "+RootOps.q(RootOps.FLAGS+"/refine-grade.tmp")+" && chmod 666 "+RootOps.q(RootOps.FLAGS+"/refine-grade.tmp")+" && mv -f "+RootOps.q(RootOps.FLAGS+"/refine-grade.tmp")+" "+RootOps.q(RootOps.FLAGS+"/refine-grade.txt")+"\n");if(!r.ok)throw new java.io.IOException(r.text);}
 static void mark(Context c,int resource,int kind){c.getSharedPreferences("mine_labels47",0).edit().putInt(String.valueOf(resource),kind).apply();planned="";}
 static String takeAlert(){String v=newAlert;newAlert="";mineAlert57=false;return v;}
}
