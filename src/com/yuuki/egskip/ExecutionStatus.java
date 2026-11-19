package com.yuuki.egskip;
import java.util.*;
import java.text.SimpleDateFormat;
final class ExecutionStatus {
 static Map<String,String> parse(String input){Map<String,String> m=new LinkedHashMap<>();for(String line:input.split("\n")){int i=line.indexOf('=');if(i>0&&!m.containsKey(line.substring(0,i)))m.put(line.substring(0,i),line.substring(i+1).trim());}return m;}
 static long number(Map<String,String> m,String k,long fallback){try{return Long.parseLong(m.get(k));}catch(Exception e){return fallback;}}
 static String value(Map<String,String> m,String k){String s=m.get(k);return s==null?"":s;}
 static String time(long seconds){if(seconds<=0)return "尚无";SimpleDateFormat f=new SimpleDateFormat("HH:mm:ss",Locale.CHINA);f.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));return f.format(new Date(seconds*1000L));}
 static String shortText(String s,int n){s=s.replace('\r',' ').replace('\n',' ');return s.length()>n?s.substring(0,n)+"…":s;}
 static String task(String pending,String status){
  if(pending.startsWith("mine_")){if(pending.equals("mine_snatch"))return "抢矿";if(pending.equals("mine_detail"))return "查询好友矿场";if(pending.equals("mine_self"))return "核对自身矿位";if(pending.equals("mine_friends"))return "查询好友矿列表";}
  if(pending.equals("mine_team78"))return "后台准备抢矿阵容";
  if(pending.equals("dispatch_data"))return "查询派遣";if(pending.equals("dispatch_send"))return "派遣上阵";if(pending.equals("dispatch_speed"))return "派遣加速";if(pending.equals("dispatch_claim"))return "领取派遣";if(pending.equals("dispatch_refresh"))return "刷新卷轴派遣";
  if(pending.equals("guild_base80"))return "查询公会与签到";if(pending.equals("guild_sign27"))return "公会签到";if(pending.equals("guild_data27"))return "查询公会砍价";if(pending.equals("guild_bargain27"))return "公会砍价";
  if(pending.equals("god_fight"))return "诸神试炼";if(pending.equals("world_fight"))return "异界入侵";
  String s=status;int i=s.indexOf('；');if(i>=0)s=s.substring(0,i);s=s.replace("日常自动：","");
  return s.length()==0?"处理任务":shortText(s,50);
 }
 static String render(String input){
  Map<String,String> m=parse(input);String daily=renderDaily(input);
  if(!value(m,"ore_enabled").equals("1"))return daily;
  String mining=value(m,"pid").length()==0?"矿井未运行｜游戏进程未启动":"矿井自动已开启｜保持矿井页面，执行详情见日志";
  return mining+"\n"+daily;
 }
 static String renderDaily(String input){
  Map<String,String> m=parse(input);long now=number(m,"now",0),mtime=number(m,"mtime",0),age=mtime>0?Math.max(0,now-mtime):Long.MAX_VALUE;
  String status=value(m,"status");String historical=status.length()>0?"\n最后记录："+shortText(task("",status),65):"";
  if(!value(m,"enabled").equals("1"))return "日常已停止｜开关已关闭"+historical;
  if(value(m,"pid").length()==0)return "日常未运行｜游戏进程未启动"+historical;
  if(mtime==0||status.length()==0)return "日常尚未启动｜等待游戏心跳"+historical;
  if(age>15)return "日常未运行｜心跳已停止 "+age+" 秒\n请回到游戏；若一直不恢复，检查更新是否已应用"+historical;
  long clock=number(m,"time",now);String pending=value(m,"pending"),park=value(m,"park");long sent=number(m,"sent",0);String headline;
  if(pending.isEmpty()&&!value(m,"dispatch_pending").isEmpty()){pending=value(m,"dispatch_pending");sent=number(m,"dispatch_sent",0);}
  if(pending.length()>0){headline="日常运行中｜等待回包："+task(pending,status)+(sent>0?" "+Math.max(0,clock-sent)+"秒":"");}
  else if(park.length()>0){sent=number(m,"park_sent",0);headline="日常运行中｜"+task(park,status)+"等待回包"+(sent>0?" "+Math.max(0,clock-sent)+"秒":"");}
  else if(number(m,"mine_retry",0)>clock){headline="日常运行中｜"+(value(m,"rpc42").equals("result")?"矿洞核对 / 继续补位 ":"抢矿待重试 ")+(number(m,"mine_retry",0)-clock)+"秒";}
  else if(status.contains("等待游戏")){headline="日常运行中｜等待进入主界面";}
  else headline="日常运行中｜持续处理 / 等待到期任务";
  if(value(m,"rpc42").equals("ui")&&(pending.equals("mine_snatch")||park.equals("mine_snatch")))headline="日常运行中｜抢矿："+(number(m,"mine_ui",0)==3?"一键上阵完成，等待确认":"进入原版布阵");
  StringBuilder out=new StringBuilder(headline);out.append("\n心跳：").append(time(mtime)).append("（").append(age).append("秒前） · 最近动作：").append(task(pending,status));
  String slots=value(m,"mine_slots");if(slots.length()>0){out.append("\n矿位 ").append(slots).append("/3");long retry=number(m,"mine_retry",0),due=number(m,"mine_due",0);if(retry>clock)out.append(" · 重试 ").append(time(retry));else if(due>clock)out.append(" · 复查 ").append(time(due));}
  if(value(m,"rpc42").equals("ui")){out.append("\n").append(!(pending.equals("mine_snatch")||park.equals("mine_snatch"))?"上次抢矿未确认，等待复查":number(m,"mine_ui",0)==3?"一键上阵已调用，准备原版确认":"已请求原版好友布阵，等待页面就绪");}
  else if(value(m,"rpc42").equals("error")){String error=value(m,"rpc_detail42");int start=error.indexOf("内容=");if(start>=0)error=error.substring(start+3);out.append("\n抢矿错误：").append(shortText(error,140));}
  else if(pending.equals("mine_snatch")||park.equals("mine_snatch")){out.append(value(m,"rpc42").equals("ack")?"\nRPC已返回，尚未确认占领结果":"\n正在等服务器响应，尚未确认占领");}
  long last=number(m,"last_send67",0);if(last>0)out.append("\n上次请求 ").append(time(last)).append(" · ").append(Math.max(0,clock-last)).append("秒前");
  if(pending.isEmpty()&&park.isEmpty()){
   String owner=value(m,"serial_owner67");long since=number(m,"serial_since67",0);if(!owner.isEmpty()&&since>0&&clock-since>=5)out.append("\n当前批次：").append(task(owner,owner.equals("cat_data")?"猫灵契约":owner)).append(" · ").append(clock-since).append("秒");
   long next=Long.MAX_VALUE;String label="";
   String[][] due={{"farm_due67","庄园成熟"},{"farm_scan67","庄园检查"},{"guild_base_due80","公会复查"},{"guild_due80","公会砍价"},{"cat_due67","猫灵"},{"dispatch_due67","派遣"},{"adventure_due67","冒险"},{"cycle_due67","奖励"},{"trial1_due67","冒险1"},{"trial2_due67","冒险2"},{"trial3_due67","冒险3"},{"trial4_due67","冒险4"}};
   for(String[] row:due){long t=number(m,row[0],0);if(t>clock&&t<next){next=t;label=row[1];}}
   if(next<Long.MAX_VALUE)out.append("\n最近定时到期：").append(label).append(" ").append(time(next));
  }
  return out.toString();
 }
}
