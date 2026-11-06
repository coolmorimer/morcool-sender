package ru.morcool.sender;
import java.util.*;

public final class Channels {
 public static final String[] IDS={"whatsapp_app","whatsapp_web","telegram_web","max_web","telegram_bot","max_bot"};
 public static final String[] LABELS={"WhatsApp · приложение","WhatsApp · личный, веб","Telegram · личный, веб","MAX · личный, веб","Telegram · бот","MAX · бот"};
 public static String label(String id){for(int i=0;i<IDS.length;i++)if(IDS[i].equals(id))return LABELS[i];throw new IllegalArgumentException("Неизвестный способ отправки");}
 public static boolean web(String id){return id.endsWith("_web");}
 public static boolean bot(String id){return id.endsWith("_bot");}
 public static boolean background(String id){return web(id)||bot(id);}
 public static String home(String id){if(id.equals("whatsapp_web"))return "https://web.whatsapp.com/";if(id.equals("telegram_web"))return "https://web.telegram.org/k/";if(id.equals("max_web"))return "https://web.max.ru/";throw new IllegalArgumentException("Не веб-аккаунт");}
 public static String key(String id){if(id.startsWith("whatsapp"))return "phone";if(id.equals("telegram_web"))return "telegram";if(id.equals("max_web"))return "max";if(id.equals("telegram_bot"))return "telegram_chat_id";return "max_chat_id";}
 public static String normalize(String channel,String value){
  value=value==null?"":value.trim();label(channel);
  if(value.isEmpty())throw new IllegalArgumentException("Не указан адрес получателя");
  if(channel.startsWith("whatsapp"))return Core.phone(value,"7");
  if(channel.equals("telegram_bot")){if(value.matches("-?[1-9][0-9]{0,18}"))return value;if(value.matches("@[A-Za-z][A-Za-z0-9_]{3,63}"))return value.toLowerCase(Locale.ROOT);throw new IllegalArgumentException("Для Telegram-бота нужен Chat ID или @имя канала, а не телефон");}
  if(channel.equals("max_bot")){if(value.matches("(user:|chat:)?-?[1-9][0-9]{0,18}"))return value.startsWith("user:")||value.startsWith("chat:")?value:"chat:"+value;throw new IllegalArgumentException("Для MAX-бота нужен chat:ID или user:ID");}
  if(channel.equals("telegram_web")){
   value=value.replaceFirst("^https?://(?:www\\.)?(?:t\\.me|telegram\\.me)/","").replaceFirst("[?/#].*$","");
   if(value.startsWith("+"))return Core.phone(value,"7");
   value=value.replaceFirst("^@","");if(value.matches("[A-Za-z][A-Za-z0-9_]{3,63}"))return "@"+value.toLowerCase(Locale.ROOT);
   throw new IllegalArgumentException("Укажите Telegram @username, ссылку t.me или международный телефон");
  }
  if(value.startsWith("+"))return Core.phone(value,"7");
  if(value.matches("https://max\\.ru/(?:u/)?[A-Za-z0-9_\\-]{3,256}/?"))return value;
  if(value.matches("@?[A-Za-z0-9_\\-]{3,128}"))return value;
  throw new IllegalArgumentException("Укажите ссылку MAX, идентификатор или телефон с +");
 }
 public static String target(String channel,Map<String,String> f){
  String value=f.get(key(channel));
  if(channel.equals("max_bot") && f.containsKey("max_user_id") && !f.get("max_user_id").trim().isEmpty())value="user:"+f.get("max_user_id");
  if(channel.equals("telegram_web") && (value==null||value.isEmpty()))value=f.get("phone");
  return normalize(channel,value);
 }
 public static String limit(String id){if(id.equals("whatsapp_app"))return "Нужен открытый WhatsApp и разблокированный экран";if(web(id))return "Личный аккаунт через веб-вход. Совместимость требует проверки после входа";return "Работает в фоне. Нужен токен и ID доступного боту чата";}
 public static int maxText(String id){return id.startsWith("max")?4000:4096;}
 /** Local fixture endpoints are accepted only by QA builds; parse hosts instead of trusting a string prefix. */
 public static String loopbackBase(String value){try{java.net.URI u=new java.net.URI(value);String host=u.getHost(),path=u.getPath();if("http".equals(u.getScheme())&&("127.0.0.1".equals(host)||"10.0.2.2".equals(host))&&u.getPort()>0&&u.getPort()<=65535&&u.getUserInfo()==null&&u.getQuery()==null&&u.getFragment()==null&&(path==null||path.isEmpty()||path.equals("/")))return "http://"+host+":"+u.getPort();}catch(Exception ignored){}return "";}
}
