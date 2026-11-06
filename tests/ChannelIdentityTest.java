import ru.morcool.sender.Channels;
import java.util.*;

public final class ChannelIdentityTest {
 static int cases;
 static void check(boolean ok,String why){cases++;if(!ok)throw new AssertionError(why);}
 static void rejects(String channel,String value){try{Channels.normalize(channel,value);throw new AssertionError("Must reject "+channel);}catch(IllegalArgumentException expected){cases++;}}
 public static void main(String[] args){
  check(Channels.normalize("telegram_web","@DEMO_Person").equals("@demo_person"),"Telegram usernames canonicalize case");
  check(Channels.normalize("telegram_bot","@DEMO_Channel").equals("@demo_channel"),"Telegram bot channels canonicalize case");
  check(Channels.normalize("telegram_web","https://t.me/DEMO_Person").equals("@demo_person"),"Telegram URL normalization");
  check(Channels.normalize("max_web","https://max.ru/u/AbCd").equals("https://max.ru/u/AbCd"),"MAX profile token case is preserved");
  check(!Channels.normalize("max_web","https://max.ru/u/AbCd").equals(Channels.normalize("max_web","https://max.ru/u/abcd")),"Distinct MAX tokens remain distinct");
  rejects("telegram_bot","+70000000001");rejects("max_web","https://max.ru.evil.example/u/AbCd");
  check(Channels.loopbackBase("http://127.0.0.1:8879/").equals("http://127.0.0.1:8879"),"localhost fixture");
  check(Channels.loopbackBase("http://10.0.2.2:8879").equals("http://10.0.2.2:8879"),"emulator fixture");
  check(Channels.loopbackBase("http://127.0.0.1:8879@evil.example").isEmpty(),"userinfo trick rejected");
  check(Channels.loopbackBase("http://127.0.0.1:8879/?target=evil").isEmpty(),"query rejected");
  check(Channels.loopbackBase("http://127.0.0.1:8879/path").isEmpty(),"path rejected");
  check(Channels.loopbackBase("https://127.0.0.1:8879").isEmpty(),"unsupported fixture protocol rejected");
  check(Channels.loopbackBase("http://127.0.0.1:0").isEmpty(),"invalid port rejected");
  check(Channels.loopbackBase("http://127.0.0.1:65536").isEmpty(),"out of range port rejected");
  System.out.println("PASS: "+cases+" channel identity and QA endpoint checks");
 }
}
