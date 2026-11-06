import ru.morcool.sender.CompletionWatch;
public class CompletionWatchTest {
 static int count;static void check(boolean ok,String text){count++;if(!ok)throw new AssertionError(text);}
 public static void main(String[] args){
  check(CompletionWatch.cleared(null,null,false),"null empty editor");
  check(CompletionWatch.cleared("", "Сообщение",false),"empty text with hint");
  check(CompletionWatch.cleared("Сообщение","Сообщение",true),"Android returns hint as text");
  check(CompletionWatch.cleared("Сообщение","Сообщение",false),"WhatsApp exposes hint without flag");
  check(CompletionWatch.cleared("Message","Message",true),"English placeholder");
  check(!CompletionWatch.cleared("Unsent draft","Сообщение",false),"draft must never count as clear");
  check(!CompletionWatch.cleared("Message",null,false),"do not hardcode placeholder words");
  check(CompletionWatch.cleared("\u200e \u200f",null,false),"format markers only");
  CompletionWatch w=new CompletionWatch();
  check(!w.observe(false,true,"Сообщение","Сообщение",true),"never advance before click");
  check(!w.observe(true,false,"Сообщение","Сообщение",true),"hidden editor not evidence");
  check(!w.observe(true,true,"Сообщение","Сообщение",true),"first sample waits");
  check(!w.observe(true,true,"stale draft","Сообщение",false),"stale text resets confirmation");
  check(!w.observe(true,true,"Сообщение","Сообщение",true),"first fresh clear again");
  check(w.observe(true,true,"Сообщение","Сообщение",true),"second clear schedules next row");
  for(int i=0;i<10;i++)check(!w.observe(true,true,"Сообщение","Сообщение",true),"schedule only once");
  w.reset();check(!w.observe(true,true,"",null,false),"next recipient resets");check(w.observe(true,true,"",null,false),"second recipient advances");
  // Regression scenario: a queue of three recipients all expose the hint after Send.
  int nextRows=0;for(int row=0;row<3;row++){w.reset();check(!w.observe(false,true,"draft","Сообщение",false),"draft before sending");check(!w.observe(true,true,"draft","Сообщение",false),"wait for field refresh");for(int frame=0;frame<5;frame++)if(w.observe(true,true,"Сообщение","Сообщение",true))nextRows++;}
  check(nextRows==3,"all three rows advance exactly once");
  w.reset();check(!w.observe(true,true,"Сообщение",null,false,true),"visible Send means label could be draft");
  check(!w.observe(true,true,"Сообщение",null,false,false),"WhatsApp unlabeled hint first sample");
  check(w.observe(true,true,"Сообщение",null,false,false),"WhatsApp unlabeled hint plus no Send confirms");
  check(!w.observe(true,true,"Сообщение",null,false,false),"unlabeled hint advances once");
  w.reset();check(!w.observe(false,true,"Сообщение",null,false,false),"unlabeled hint before click cannot advance");
  check(!w.observe(true,true,"unsent text",null,false,false),"missing Send alone is insufficient");
  int transitions=0;for(int row=0;row<3;row++){w.reset();check(!w.observe(false,true,"draft",null,false,true),"no early advance");for(int frame=0;frame<5;frame++)if(w.observe(true,true,"Сообщение",null,false,false))transitions++;}
  check(transitions==3,"three WhatsApp no-hint rows each schedule once");
  System.out.println("PASS: "+count+" post-send progression regression checks");
 }
}
