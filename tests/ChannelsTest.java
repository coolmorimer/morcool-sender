import ru.morcool.sender.Channels;
import java.util.*;
public class ChannelsTest {
 static int checks;
 static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
 static void rejects(Runnable r){try{r.run();throw new AssertionError("Expected rejection");}catch(IllegalArgumentException expected){checks++;}}
 public static void main(String[] args){
  check(Channels.normalize("whatsapp_app","8 (999) 123-45-67").equals("+79991234567"),"WhatsApp phone");
  check(Channels.normalize("telegram_web","https://t.me/demo_person?text=hello").equals("@demo_person"),"Telegram URL");
  check(Channels.normalize("telegram_web","+79991234567").equals("+79991234567"),"Telegram phone");
  check(Channels.normalize("telegram_web","@demo_person").equals("@demo_person"),"Telegram username");
  check(Channels.normalize("telegram_bot","-1001234567890").equals("-1001234567890"),"Telegram group ID");
  check(Channels.normalize("telegram_bot","@demo_channel").equals("@demo_channel"),"Telegram public channel");
  rejects(()->Channels.normalize("telegram_bot","+79991234567"));
  rejects(()->Channels.normalize("telegram_bot","https://evil.invalid"));
  check(Channels.normalize("max_bot","12345").equals("chat:12345"),"MAX default chat ID");
  check(Channels.normalize("max_bot","user:12345").equals("user:12345"),"MAX user ID");
  check(Channels.normalize("max_web","https://max.ru/u/demo123").equals("https://max.ru/u/demo123"),"MAX profile");
  rejects(()->Channels.normalize("max_web","https://max.ru.evil.invalid/u/demo123"));
  rejects(()->Channels.normalize("max_web","javascript:alert(1)"));
  rejects(()->Channels.normalize("max_bot","+79991234567"));
  Map<String,String> fields=new HashMap<>();fields.put("phone","+79991234567");fields.put("telegram_chat_id","7777");fields.put("max_user_id","5555");fields.put("max_chat_id","8888");
  check(Channels.target("telegram_bot",fields).equals("7777"),"never substitute phone for bot ID");
  check(Channels.target("telegram_web",fields).equals("+79991234567"),"phone fallback for personal Telegram");
  check(Channels.target("max_bot",fields).equals("user:5555"),"explicit MAX user ID");
  check(Channels.background("telegram_bot")&&!Channels.background("whatsapp_app"),"screen requirement stays truthful");
  check(Channels.maxText("max_bot")==4000,"MAX length cap");
  System.out.println("PASS: "+checks+" messenger address checks");
 }
}
