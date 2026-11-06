package ru.morcool.sender;
import android.content.Context;import java.net.*;import java.io.*;import java.nio.charset.StandardCharsets;import org.json.*;

public final class BotTransport {
 public static final class Result {
  public final String state,detail,messageId;public final int retrySeconds;
  public Result(String s,String d,String m,int r){state=s;detail=d;messageId=m;retrySeconds=r;}
 }
 public static Result send(Context context,String channel,String target,String text){
  String path;JSONObject body=new JSONObject();
  try{String token=Secrets.get(context,channel);if(token.isEmpty())return new Result("PAUSE","Подключите бота в разделе «Аккаунты»","",0);if(token.matches("(?s).*[\\s/?#].*"))return new Result("PAUSE","Токен содержит недопустимые символы. Скопируйте его заново.","",0);target=Channels.normalize(channel,target);Store.put(body,"text",text);
   if(channel.equals("telegram_bot")){path="https://api.telegram.org/bot"+token+"/sendMessage";Store.put(body,"chat_id",target);}
   else if(channel.equals("max_bot")){String[] p=target.split(":",2);path="https://platform-api2.max.ru/messages?"+(p[0].equals("user")?"user_id=":"chat_id=")+URLEncoder.encode(p[1],"UTF-8");}
   else return new Result("PAUSE","Неверный API-провайдер","",0);
   if(BuildFlags.QA){String override=Channels.loopbackBase(Store.setting("qa_api_base",""));if(!override.isEmpty())path=override+"/"+channel+(new URL(path).getQuery()==null?"":"?"+new URL(path).getQuery());}
   return request(context,path,channel,token,body);
  }catch(Exception e){return new Result("PAUSE","Проверьте адрес получателя и параметры подключения","",0);}
 }
 static Result request(Context context,String path,String channel,String token,JSONObject body){
  HttpURLConnection con=null;boolean submitted=false;
  try{
   con=(HttpURLConnection)new URL(path).openConnection();if(channel.equals("max_bot")&&con instanceof javax.net.ssl.HttpsURLConnection&&new URL(path).getHost().equals("platform-api2.max.ru"))((javax.net.ssl.HttpsURLConnection)con).setSSLSocketFactory(MaxTrust.socketFactory(context));con.setRequestMethod("POST");con.setConnectTimeout(15000);con.setReadTimeout(20000);con.setInstanceFollowRedirects(false);con.setDoOutput(true);con.setRequestProperty("Content-Type","application/json; charset=utf-8");if(channel.equals("max_bot"))con.setRequestProperty("Authorization",token);
   byte[] payload=body.toString().getBytes(StandardCharsets.UTF_8);con.setFixedLengthStreamingMode(payload.length);con.connect();submitted=true;try(OutputStream out=con.getOutputStream()){out.write(payload);}
   int code=con.getResponseCode();InputStream input=code>=400?con.getErrorStream():con.getInputStream();String raw=read(input);JSONObject response;try{response=new JSONObject(raw);}catch(Exception e){if(code>=400)return interpret(channel,code,new JSONObject(),con.getHeaderField("Retry-After"));return new Result("UNKNOWN","Сервер вернул нечитаемый результат. Проверьте чат перед повтором.","",0);}
   return interpret(channel,code,response,con.getHeaderField("Retry-After"));
  }catch(Exception e){return new Result(submitted?"UNKNOWN":"PAUSE",submitted?"Ответ не получен после начала запроса. Проверьте чат перед повтором.":"Не удалось подключиться. Проверьте интернет или доверенный сертификат сервиса.","",0);}
  finally{if(con!=null)con.disconnect();}
 }
 static String read(InputStream in)throws Exception{if(in==null)return "{}";try(InputStream input=in;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[4096];int n;while((n=input.read(b))!=-1){if(out.size()+n>1024*1024)throw new IOException("response too large");out.write(b,0,n);}return new String(out.toByteArray(),StandardCharsets.UTF_8);}}
 public static Result interpret(String channel,int http,JSONObject r,String retry){
  if(http==429){int seconds=30;try{seconds=Integer.parseInt(retry);}catch(Exception ignored){}JSONObject params=r.optJSONObject("parameters");if(params!=null)seconds=params.optInt("retry_after",seconds);return new Result("RETRY","Сервис просит подождать", "",Math.max(1,Math.min(86400,seconds)));}
  if(http==401)return new Result("PAUSE","Токен бота недействителен. Обновите его в «Аккаунтах».","",0);
  if(http==403)return new Result("SKIPPED","Бот не имеет доступа к получателю или заблокирован","",0);
  if(http==400||http==404)return new Result("SKIPPED","Чат не найден или запрос отклонён сервисом","",0);
  if(http>=500)return new Result("UNKNOWN","Сбой сервиса после запроса. Проверьте результат в чате.","",0);
  if(http<200||http>=300)return new Result("PAUSE","Сервис отклонил запрос: HTTP "+http,"",0);
  if(channel.equals("telegram_bot")){if(!r.optBoolean("ok")){int error=r.optInt("error_code",500);return interpret(channel,error<400?500:error,r,retry);}JSONObject message=r.optJSONObject("result");if(message!=null && message.has("message_id"))return new Result("ACCEPTED","Telegram принял сообщение. Доставка и прочтение не проверены.",message.optString("message_id"),0);}
  else {JSONObject message=r.optJSONObject("message");JSONObject content=message==null?null:message.optJSONObject("body");if(content!=null&&!content.optString("mid").isEmpty())return new Result("ACCEPTED","MAX принял сообщение. Доставка и прочтение не проверены.",content.optString("mid"),0);}
  return new Result("UNKNOWN","Нет подтверждения сервиса. Проверьте чат перед повтором.","",0);
 }
}
