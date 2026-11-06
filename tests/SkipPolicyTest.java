import ru.morcool.sender.SkipPolicy;
public class SkipPolicyTest{
 static int count;static void check(boolean ok,String msg){count++;if(!ok)throw new AssertionError(msg);}
 public static void main(String[] args){
  check(SkipPolicy.unavailable("Номер телефона не зарегистрирован в WhatsApp"),"unregistered Russian");
  check(SkipPolicy.unavailable("Номер телефона недействителен"),"invalid Russian");
  check(SkipPolicy.unavailable("Не удалось найти номер телефона"),"missing number Russian");
  check(SkipPolicy.unavailable("Пользователь не найден"),"missing user Russian");
  check(SkipPolicy.unavailable("Phone number isn't on WhatsApp"),"unregistered English");
  check(SkipPolicy.unavailable("The phone number shared via URL is invalid"),"invalid English");
  check(SkipPolicy.unavailable("Phone number is not on WhatsApp"),"not on English");
  check(!SkipPolicy.unavailable("Нет подключения к интернету"),"network issue is not an unavailable number");
  check(!SkipPolicy.unavailable("Предоставьте доступ к контактам"),"permission dialog is not unavailable number");
  check(!SkipPolicy.unavailable("Подтвердите отправку"),"generic dialog is not unavailable number");
  check(SkipPolicy.skipTimeout(false,true),"missing chat skips before sending");
  check(!SkipPolicy.skipTimeout(true,true),"never classify uncertain sent message as not found");
  check(!SkipPolicy.skipTimeout(false,false),"do not skip whole queue if WhatsApp cannot launch");
  check(!SkipPolicy.skipTimeout(true,false),"missing window after click remains uncertain");
  System.out.println("PASS: "+count+" unavailable-number and timeout checks");
 }
}
