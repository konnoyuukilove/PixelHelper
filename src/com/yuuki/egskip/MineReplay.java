package com.yuuki.egskip;
import java.io.*;
import java.util.*;
/** Map-state demonstrations, never timed or pixel-coordinate macros. */
public final class MineReplay {
 public static final int LIMIT=8000;int revision;
 public static class Step {public String id,name,group="";public boolean enabled=true;public int tool,rel;public MinePlanner.MapData before;}
 public static class Batch {public int consumed;public List<Step> steps=new ArrayList<Step>();}
 public static class Group {public String id,name;public boolean enabled=true;Group(String i,String n){id=i;name=n;}}
 public final Set<String> retiredGroups=new HashSet<String>();
 public final LinkedHashMap<String,Group> groups=new LinkedHashMap<String,Group>();
 public Group group(String id){return groups.get(id);}
 public int count(String id){int n=0;for(Step s:steps)if(s.group.equals(id))n++;return n;}
 public void addGroup(String id,String name){if(id==null||!id.matches("[0-9]{1,20}")||name==null||name.trim().isEmpty()||name.length()>120)throw new IllegalArgumentException("地图名称需1至120字");if(groups.size()>=500&&!groups.containsKey(id))throw new IllegalStateException("最多保存500个地图分组");retiredGroups.remove(id);groups.put(id,new Group(id,name.trim()));}
 static String legacyGroup(Step s){String[] parts=s.id.split(":");return parts[0].matches("[0-9]{1,20}")?parts[0]:"0";}
 public final List<Step> steps=new ArrayList<Step>();
 String replayGroup="";
 boolean acceptsGroup(String id){return replayGroup.isEmpty()||replayGroup.equals(id);}
 String sentNonce="",sentState="";long sentAt70;String playGroup70="";int playStart70=-1,playSource70=-1;
 public static boolean tool(int t){return t==0||t==1026||t==1028||t==1029;}
 public static String toolName(int t){return t==0?"免费拾取":t==1026?"镐子":t==1028?"地钻":"炸药";}
 public static int rel(MinePlanner.MapData m,MinePlanner.Cell c){return c.index-m.snap*6;}
 public static MinePlanner.Cell target(MinePlanner.MapData m,int rel){for(MinePlanner.Cell c:m.cells)if(rel(m,c)==rel)return c;return null;}
 public static String snapshot(MinePlanner.MapData m){StringBuilder b=new StringBuilder(m.nonce+"\n"+m.depth+","+m.snap+","+m.base+","+m.picks+","+m.lineTools+","+m.areaTools+"\n");for(MinePlanner.Cell c:m.cells)b.append(c.index).append(',').append(c.id).append(',').append(c.ground).append(',').append(c.res).append(',').append(c.unlocked).append('\n');return b.append("END46\n").toString();}
 public static String shape(MinePlanner.MapData m,boolean full){TreeMap<Integer,MinePlanner.Cell> cells=new TreeMap<Integer,MinePlanner.Cell>();for(MinePlanner.Cell c:m.cells){int r=rel(m,c);if(full||r>=0&&r<42)cells.put(r,c);}StringBuilder b=new StringBuilder();for(Map.Entry<Integer,MinePlanner.Cell> e:cells.entrySet()){MinePlanner.Cell c=e.getValue();b.append(e.getKey()).append(':').append(c.ground).append(',').append(c.res).append(',').append(c.unlocked).append(';');}return b.toString();}
 public static void validate(Step s){if(s.id==null||!s.id.matches("[0-9:-]{1,100}")||!tool(s.tool)||s.name==null||s.name.length()>120)throw new IllegalArgumentException("无效的记录字段");Set<Integer> indices=new HashSet<Integer>();for(MinePlanner.Cell c:s.before.cells){if(!indices.add(c.index)||c.ground<0||c.ground>65535||c.res<0||c.unlocked<0||c.unlocked>1)throw new IllegalArgumentException("无效的矿格");}MinePlanner.Cell t=target(s.before,s.rel);if(t==null||t.ground>2||(t.unlocked!=1&&s.tool!=1028&&s.tool!=1029)||s.rel< -36||s.rel>=72||s.tool==0&&(t.ground!=0||t.res==0))throw new IllegalArgumentException("目标不存在、未解锁或无法免费拾取");}
 public static Batch events(String text){Batch b=new Batch();int at=0;while(true){int end=text.indexOf("END53\n",at);if(end<0)break;String frame=text.substring(at,end);b.consumed=end+6;at=b.consumed;int nl=frame.indexOf('\n');if(nl<0)continue;String[] h=frame.substring(0,nl).split(",");try{if(h.length!=5||!h[0].equals("E53"))continue;Step s=new Step();s.id=h[1];s.name="示范 "+s.id;s.tool=Integer.parseInt(h[2]);int index=Integer.parseInt(h[3]),id=Integer.parseInt(h[4]);s.before=MinePlanner.parse(frame.substring(nl+1));s.rel=index-s.before.snap*6;MinePlanner.Cell t=target(s.before,s.rel);if(t==null&&(s.tool==1028||s.tool==1029)&&index>=0&&id>0&&s.rel>=-36&&s.rel<72){MinePlanner.Cell first=s.before.cells.get(0);if((id-1)/6-index/6==(first.id-1)/6-first.index/6&&(id-1)%6==index%6){t=new MinePlanner.Cell();t.index=index;t.id=id;t.unlocked=0;s.before.cells.add(t);}}if(t==null||t.id!=id)continue;validate(s);b.steps.add(s);}catch(Exception ignored){}}return b;}
 public boolean add(Step s){for(Step old:steps)if(old.id.equals(s.id))return false;if(steps.size()>=LIMIT)throw new IllegalStateException("记录已达8000步，请导出并清理");validate(s);if(s.group==null||s.group.isEmpty())s.group=legacyGroup(s);if(retiredGroups.contains(s.group))return false;if(!groups.containsKey(s.group))addGroup(s.group,"地图 "+(groups.size()+1));steps.add(s);revision++;return true;}
 public void clearSent70(){sentNonce="";sentState="";sentAt70=0;}
 public void reset(){clearSent70();playGroup70="";playStart70=-1;playSource70=-1;}
 public MinePlanner.Plan choose(MinePlanner.MapData m){
  String state=shape(m,true);if(!sentNonce.isEmpty()){if(sentNonce.equals(m.nonce))return new MinePlanner.Plan("等待上一笔采矿回包");if(sentState.equals(state)){reset();return pause("回包后矿图未变化，暂停补录，避免重复消耗");}reset();}
  String key=shape(m,false);Step match=null;for(int i=steps.size()-1;i>=0;i--){Step s=steps.get(i);if(!acceptsGroup(s.group)||!s.enabled||groups.containsKey(s.group)&&!groups.get(s.group).enabled||!key.equals(shape(s.before,false)))continue;if((s.tool==1028||s.tool==1029)&&!state.equals(shape(s.before,true)))continue;MinePlanner.Cell t=target(m,s.rel),old=target(s.before,s.rel);if(t==null||old==null||t.ground!=old.ground||t.res!=old.res||t.unlocked!=1)continue;match=s;break;}
  if(match==null)return pause("未录过的矿图，已暂停；手动操作补录后点“按记录复刻”");
  if((match.tool==1028||match.tool==1029)&&!m.allowTools)return pause("记录需要"+toolName(match.tool)+"，道具开关已关闭；请补录或修改设置");
  if(match.tool==1026&&m.picks<=0||match.tool==1028&&m.lineTools<=0||match.tool==1029&&m.areaTools<=0)return pause("记录所需"+toolName(match.tool)+"不足，暂停补录或等待补充");
  MinePlanner.Cell t=target(m,match.rel);MinePlanner.Plan p=new MinePlanner.Plan("复刻："+(groups.containsKey(match.group)?groups.get(match.group).name+" / ":"")+match.name+" · "+toolName(match.tool)+" · 第"+(Math.floorDiv(match.rel,6)+1)+"行"+(Math.floorMod(match.rel,6)+1)+"列");p.index=t.index;p.id=t.id;p.tool=match.tool;return p;
 }
 public void issued(MinePlanner.MapData m){sentNonce=m.nonce;sentState=shape(m,true);sentAt70=System.currentTimeMillis();}
 static MinePlanner.Plan pause(String s){MinePlanner.Plan p=new MinePlanner.Plan(s);p.alert=true;p.alertKey=s;return p;}
 static String hex(String s)throws IOException{byte[] b=s.getBytes("UTF-8");StringBuilder out=new StringBuilder();for(byte v:b)out.append(String.format(Locale.US,"%02x",v&255));return out.toString();}
 static String unhex(String s)throws IOException{if(s.length()>1000||s.length()%2!=0)throw new IOException("名称损坏");byte[] b=new byte[s.length()/2];try{for(int i=0;i<b.length;i++)b[i]=(byte)Integer.parseInt(s.substring(i*2,i*2+2),16);}catch(Exception e){throw new IOException("名称损坏");}return new String(b,"UTF-8");}
 public String encode()throws IOException{StringBuilder out=new StringBuilder("PIXEL_MINE_54\n");for(String id:retiredGroups)out.append("X54\t").append(id).append('\n');for(Group g:groups.values())out.append("G54\t").append(g.id).append('\t').append(g.enabled?1:0).append('\t').append(hex(g.name)).append('\n');for(Step s:steps){validate(s);if(!groups.containsKey(s.group))throw new IOException("记录缺少地图分组");out.append("D54\t").append(s.id).append('\t').append(s.enabled?1:0).append('\t').append(s.tool).append('\t').append(s.rel).append('\t').append(hex(s.name)).append('\t').append(s.group).append('\n').append(snapshot(s.before)).append("END53\n");}return out.toString();}
 public static MineReplay decode(String text)throws IOException{
  boolean old=text.startsWith("PIXEL_MINE_53\n");if(text.length()>16000000||!old&&!text.startsWith("PIXEL_MINE_54\n"))throw new IOException("不是矿井记录文件");MineReplay db=new MineReplay();int at=14;
  while(at<text.length()){int nl=text.indexOf('\n',at);if(nl<0)throw new IOException("记录不完整");String[] h=text.substring(at,nl).split("\t",-1);
   try{if(!old&&h[0].equals("X54")){if(h.length!=2||!h[1].matches("[0-9]{1,20}")||db.groups.containsKey(h[1]))throw new IllegalArgumentException();db.retiredGroups.add(h[1]);at=nl+1;continue;}if(!old&&h[0].equals("G54")){if(h.length!=4||!h[2].matches("[01]")||db.groups.containsKey(h[1]))throw new IllegalArgumentException();db.addGroup(h[1],unhex(h[3]));db.group(h[1]).enabled=h[2].equals("1");at=nl+1;continue;}
    int end=text.indexOf("END53\n",nl+1);if(end<0)throw new IllegalArgumentException("记录不完整");if(h.length!=(old?6:7)||!h[0].equals(old?"D53":"D54")||!h[2].matches("[01]"))throw new IllegalArgumentException();Step s=new Step();s.id=h[1];s.enabled=h[2].equals("1");s.tool=Integer.parseInt(h[3]);s.rel=Integer.parseInt(h[4]);s.name=unhex(h[5]);s.before=MinePlanner.parse(text.substring(nl+1,end));if(!old){s.group=h[6];if(!db.groups.containsKey(s.group))throw new IllegalArgumentException("缺少地图分组");}if(!db.add(s))throw new IllegalArgumentException("重复记录");at=end+6;
   }catch(Exception e){throw new IOException("无效记录："+e.getMessage());}
  }return db;
 }
 public static MineReplay read(File file)throws IOException{if(!file.exists())return new MineReplay();if(file.length()>16000000)throw new IOException("记录文件太大");ByteArrayOutputStream b=new ByteArrayOutputStream();try(InputStream in=new FileInputStream(file)){byte[] buf=new byte[16384];int n;while((n=in.read(buf))!=-1){b.write(buf,0,n);if(b.size()>16000000)throw new IOException("记录文件太大");}}return decode(new String(b.toByteArray(),"UTF-8"));}
 public void save(File file)throws IOException{byte[] data=encode().getBytes("UTF-8");if(data.length>16000000)throw new IOException("记录超过16MB，请导出并停用旧步骤");File tmp=new File(file+".tmp");try(FileOutputStream out=new FileOutputStream(tmp)){out.write(data);out.getFD().sync();}if(file.exists()){File bak=new File(file+".bak");copy(file,bak);}if(!tmp.renameTo(file))throw new IOException("记录保存失败");}
 static void copy(File a,File b)throws IOException{try(InputStream in=new FileInputStream(a);OutputStream out=new FileOutputStream(b)){byte[] buf=new byte[16384];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);}}
}
