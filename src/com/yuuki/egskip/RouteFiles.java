package com.yuuki.egskip;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class RouteFiles extends Activity {
 private static final int LIMIT = 16_000_000;

 @Override public void onCreate(Bundle saved) {
  super.onCreate(saved);
  if(saved != null) return;
  boolean export = getIntent().hasExtra("group");
  Intent picker = new Intent(export ? Intent.ACTION_CREATE_DOCUMENT : Intent.ACTION_OPEN_DOCUMENT);
  picker.addCategory(Intent.CATEGORY_OPENABLE).setType(export ? "text/plain" : "*/*");
  if(export) picker.putExtra(Intent.EXTRA_TITLE,"矿井地图分组.txt");
  try { startActivityForResult(picker,1); }
  catch(android.content.ActivityNotFoundException e) { complete("系统文件选择器不可用"); }
 }

 @Override protected void onActivityResult(int request,int result,Intent data) {
  super.onActivityResult(request,result,data);
  if(request!=1 || result!=RESULT_OK || data==null || data.getData()==null){finish();return;}
  Uri uri=data.getData();
  String group=getIntent().getStringExtra("group");
  MainActivity.WORK.execute(()->{
   try {
    if(group==null) complete("已导入 "+load(uri)+" 个分组");
    else {save(uri,group);complete("地图文件已保存");}
   } catch(Exception e){complete("地图文件处理失败："+e.getMessage());}
  });
 }

 private void save(Uri uri,String group)throws Exception {
  MineReplay source=MiningControl.copy(this);
  MineReplay selected=new MineReplay();
  for(MineReplay.Group item:source.groups.values()){
   if(!group.isEmpty()&&!group.equals(item.id))continue;
   selected.addGroup(item.id,item.name);
   selected.group(item.id).enabled=item.enabled;
  }
  if(selected.groups.isEmpty()) throw new IOException("没有可导出的分组");
  for(MineReplay.Step step:source.steps)
   if(selected.groups.containsKey(step.group))selected.add(step);
  byte[] bytes=selected.encode().getBytes(StandardCharsets.UTF_8);
  try(OutputStream out=getContentResolver().openOutputStream(uri,"wt")){
   if(out==null)throw new IOException("无法写入所选位置");
   out.write(bytes);
  }
 }

 private int load(Uri uri)throws Exception {
  ByteArrayOutputStream bytes=new ByteArrayOutputStream();
  try(InputStream in=getContentResolver().openInputStream(uri)){
   if(in==null)throw new IOException("无法打开所选文件");
   byte[] buffer=new byte[8192];
   for(int n;(n=in.read(buffer))!=-1;){
    if(bytes.size()+n>LIMIT)throw new IOException("文件超过16MB");
    bytes.write(buffer,0,n);
   }
  }
  MineReplay incoming=MineReplay.decode(new String(bytes.toByteArray(),StandardCharsets.UTF_8));
  if(incoming.groups.isEmpty())throw new IOException("文件没有地图分组");
  MineReplay current=MiningControl.demos(this);
  MineReplay merged=MiningControl.copy(this);
  Map<String,String> ids=new HashMap<>();
  long clock=System.currentTimeMillis();
  for(MineReplay.Group group:incoming.groups.values()){
   String id;
   do{id=Long.toString(clock++);}while(merged.groups.containsKey(id)||merged.retiredGroups.contains(id));
   String name=group.name;
   merged.addGroup(id,name.substring(0,Math.min(name.length(),116))+"（导入）");
   ids.put(group.id,id);
  }
  int index=0;
  for(MineReplay.Step step:incoming.steps){
   step.group=ids.get(step.group);
   step.id=step.group+":0:"+(++index);
   merged.add(step);
  }
  merged.save(MiningControl.demoFile(this));
  current.groups.putAll(merged.groups);
  current.steps.addAll(merged.steps.subList(current.steps.size(),merged.steps.size()));
  current.revision++;
  MiningControl.demoCount=merged.steps.size();
  MiningControl.planned="";
  return ids.size();
 }

 private void complete(String message){
  runOnUiThread(()->{Toast.makeText(this,message,Toast.LENGTH_LONG).show();finish();});
 }
}
