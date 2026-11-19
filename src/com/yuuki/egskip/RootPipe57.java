package com.yuuki.egskip;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
/** One framed command at a time; failures are never automatically replayed. */
final class RootPipe57 {
 static class Reply {final int code;final String text;Reply(int c,String t){code=c;text=t;}}
 final String[] command;final long timeout;Process process;BufferedWriter input;BufferedReader output;ExecutorService reader;
 RootPipe57(String[] c,long t){command=c;timeout=t;}
 synchronized Reply run(String script)throws Exception{
  if(process==null){process=new ProcessBuilder(command).redirectErrorStream(true).start();input=new BufferedWriter(new OutputStreamWriter(process.getOutputStream(),"UTF-8"));output=new BufferedReader(new InputStreamReader(process.getInputStream(),"UTF-8"));reader=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"mine-root-reader");t.setDaemon(true);return t;});}
  String token="EGH57_"+UUID.randomUUID().toString().replace("-","");final BufferedReader stream=output;
  Future<Reply> pending=reader.submit(()->{String line;StringBuilder text=new StringBuilder();boolean begun=false;while((line=stream.readLine())!=null){if(line.equals(token+"_BEGIN")){begun=true;continue;}if(!begun)continue;if(line.startsWith(token+"_END:"))return new Reply(Integer.parseInt(line.substring((token+"_END:").length())),text.toString());if(text.length()+line.length()>4194304)throw new IOException("Root输出超过限制");text.append(line).append('\n');}throw new EOFException("Root连接已断开");});
  try{input.write("printf '%s\\n' '"+token+"_BEGIN'\n(\nset -f\n"+script+"\n)\nprintf '\\n"+token+"_END:%s\\n' \"$?\"\n");input.flush();return pending.get(timeout,TimeUnit.MILLISECONDS);}catch(Exception e){pending.cancel(true);close();throw new IOException("Root连接中断或超时；未重发上一笔操作",e);}
 }
 synchronized void close(){if(process!=null){try{process.getOutputStream().close();}catch(IOException ignored){}process.destroy();}if(reader!=null)reader.shutdownNow();process=null;input=null;output=null;reader=null;}
}
