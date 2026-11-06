package ru.morcool.sender;
import android.content.*;import android.net.*;import android.net.http.SslError;import android.os.*;import android.view.*;import android.webkit.*;import java.util.*;import java.io.*;import org.json.*;

/** Personal web clients live inside the app process; queue operation never opens an Android messenger activity. */
public final class WebSessions {
 public interface Callback {void done(BotTransport.Result result);}
 private static final Map<String,WebView> sessions=new HashMap<>();private static final Handler main=new Handler(Looper.getMainLooper());private static String driver;private static Run current;
 private static class Run {String nonce,channel,target,name,text;WebView view;Callback callback;Runnable committed,watchdog;long deadline;boolean begun,clicked,cancelled;}
 static class Client extends WebViewClient {
  final Context context;final String channel;
  Client(Context c,String ch){context=c.getApplicationContext();channel=ch;}
  public boolean shouldOverrideUrlLoading(WebView v,String url){return !allowedFor(channel,url);}
  public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){return !allowedFor(channel,r.getUrl().toString());}
  public void onReceivedSslError(WebView v,SslErrorHandler h,SslError e){h.cancel();Store.set("session_"+channel,"Требуется доверенный сертификат");}
  public void onPageFinished(WebView v,String url){CookieManager.getInstance().flush();probeLogin(context,channel,null);}
 }
 static class Client26 extends Client {
  Client26(Context c,String ch){super(c,ch);}
  public boolean onRenderProcessGone(WebView v,RenderProcessGoneDetail detail){sessions.remove(channel);Store.set("session_"+channel,"Веб-сессия завершилась. Откройте аккаунт заново.");if(current!=null&&current.view==v)finish(new BotTransport.Result(current.clicked?"UNKNOWN":"PAUSE","Веб-процесс завершился; проверьте результат перед повтором","",0));v.destroy();return true;}
 }
 static String source(Context c){if(driver!=null)return driver;try(InputStream in=c.getAssets().open("web-driver.js");ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);driver=new String(out.toByteArray(),"UTF-8");return driver;}catch(Exception e){throw new IllegalStateException("Не найден модуль веб-сессии");}}
 public static WebView get(Context context,String channel){
  if(sessions.containsKey(channel))return sessions.get(channel);
  Channels.home(channel);WebView view=new WebView(context.getApplicationContext());WebSettings s=view.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setDatabaseEnabled(true);s.setAllowFileAccess(false);s.setAllowContentAccess(false);s.setAllowFileAccessFromFileURLs(false);s.setAllowUniversalAccessFromFileURLs(false);s.setJavaScriptCanOpenWindowsAutomatically(false);s.setSupportMultipleWindows(false);s.setLoadWithOverviewMode(true);s.setUseWideViewPort(true);s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
  s.setUserAgentString("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36");
  CookieManager.getInstance().setAcceptCookie(true);CookieManager.getInstance().setAcceptThirdPartyCookies(view,true);
  view.setWebViewClient(Build.VERSION.SDK_INT>=26?new Client26(context,channel):new Client(context,channel));
  view.setWebChromeClient(new WebChromeClient(){public void onPermissionRequest(PermissionRequest request){request.deny();}});
  view.setLayoutParams(new ViewGroup.LayoutParams(1280,900));view.measure(View.MeasureSpec.makeMeasureSpec(1280,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(900,View.MeasureSpec.EXACTLY));view.layout(0,0,1280,900);
  sessions.put(channel,view);String home=Channels.home(channel);if(BuildFlags.QA){String test=Channels.loopbackBase(Store.setting("qa_web_base",""));if(!test.isEmpty())home=test+"/"+channel;}view.loadUrl(home);return view;
 }
 static boolean allowed(String raw){try{Uri u=Uri.parse(raw);String host=u.getHost();if(BuildFlags.QA&&"http".equals(u.getScheme())&&("10.0.2.2".equals(host)||"127.0.0.1".equals(host)))return true;if(!"https".equals(u.getScheme())||host==null)return false;return host.equals("web.whatsapp.com")||host.endsWith(".whatsapp.com")||host.equals("telegram.org")||host.endsWith(".telegram.org")||host.equals("t.me")||host.equals("max.ru")||host.endsWith(".max.ru");}catch(Exception e){return false;}}
 static boolean allowedFor(String channel,String raw){if(!allowed(raw))return false;Uri u=Uri.parse(raw);String host=u.getHost();if(BuildFlags.QA&&"http".equals(u.getScheme())&&("10.0.2.2".equals(host)||"127.0.0.1".equals(host)))return true;if(channel.equals("whatsapp_web"))return host.equals("whatsapp.com")||host.endsWith(".whatsapp.com");if(channel.equals("telegram_web"))return host.equals("telegram.org")||host.endsWith(".telegram.org")||host.equals("t.me");if(channel.equals("max_web"))return host.equals("max.ru")||host.endsWith(".max.ru");return false;}
 public static void detach(WebView view){if(view.getParent() instanceof ViewGroup)((ViewGroup)view.getParent()).removeView(view);view.measure(View.MeasureSpec.makeMeasureSpec(1280,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(900,View.MeasureSpec.EXACTLY));view.layout(0,0,1280,900);CookieManager.getInstance().flush();}
 public static void probeLogin(Context c,String channel,Runnable after){WebView v=sessions.get(channel);if(v==null||!allowedFor(channel,v.getUrl())){if(after!=null)after.run();return;}try{v.evaluateJavascript(source(c)+";JSON.stringify(window.__morcoolDriver.authState())",value->{try{JSONObject r=parse(value);Store.set("session_"+channel,r.optBoolean("connected")?"Веб-вход выполнен":"Откройте аккаунт и завершите вход");}catch(Exception ignored){}if(after!=null)after.run();});}catch(RuntimeException e){if(after!=null)after.run();}}
 static JSONObject parse(String raw)throws Exception{Object value=new JSONTokener(raw==null?"null":raw).nextValue();if(value instanceof String)return new JSONObject((String)value);if(value instanceof JSONObject)return (JSONObject)value;return new JSONObject();}
 public static void send(Context c,String channel,String target,String name,String text,Runnable onCommit,Callback callback){
  if(current!=null){callback.done(new BotTransport.Result("PAUSE","Другая веб-отправка ещё выполняется","",0));return;}
  Run run=new Run();run.nonce=Store.id();run.channel=channel;run.target=target;run.name=name;run.text=text;run.committed=onCommit;run.callback=callback;run.deadline=SystemClock.elapsedRealtime()+Store.numberSetting("web_prepare_timeout_seconds",60,15,180)*1000L;
  try{run.target=Channels.normalize(channel,target);run.view=get(c,channel);detach(run.view);}catch(RuntimeException e){callback.done(new BotTransport.Result("PAUSE","Не удалось открыть веб-сессию. Проверьте адрес и Android System WebView.","",0));return;}
  current=run;run.watchdog=new Runnable(){public void run(){if(current!=run||run.cancelled)return;if(SystemClock.elapsedRealtime()>run.deadline){timedOut(run);return;}main.postDelayed(this,500);}};main.postDelayed(run.watchdog,500);
  if(channel.equals("whatsapp_web"))run.view.loadUrl("https://web.whatsapp.com/send?phone="+run.target.replace("+","")+"&text="+Uri.encode(text));
  main.postDelayed(new Runnable(){public void run(){poll(c,run,this);}},800);
 }
 static void poll(Context c,Run run,Runnable repeat){
  if(current!=run||run.cancelled)return;
  if(SystemClock.elapsedRealtime()>run.deadline){timedOut(run);return;}
  if(!allowedFor(run.channel,run.view.getUrl())){main.postDelayed(repeat,500);return;}
  String command=source(c)+";JSON.stringify(window.__morcoolDriver."+(run.begun?"probe("+JSONObject.quote(run.nonce)+")":"begin("+JSONObject.quote(run.nonce)+","+JSONObject.quote(run.channel)+","+JSONObject.quote(run.target)+","+JSONObject.quote(run.name)+","+JSONObject.quote(run.text)+")")+")";
  try{run.view.evaluateJavascript(command,raw->{if(current!=run||run.cancelled)return;try{JSONObject r=parse(raw);String kind=r.optString("kind");if(!run.begun)run.begun=true;
   if(kind.equals("DONE")&&run.clicked){finish(new BotTransport.Result("CLICKED","Веб-клиент очистил поле после нажатия. Доставка не проверена.","",0));return;}
   if(kind.equals("AUTH")){finish(new BotTransport.Result("PAUSE","Завершите вход в личный аккаунт","",0));return;}
   if(kind.equals("MISSING")||kind.equals("AMBIGUOUS")){finish(new BotTransport.Result(run.clicked?"UNKNOWN":"SKIPPED",run.clicked?"Экран изменился после начала отправки. Проверьте чат.":r.optString("reason","Адресат не найден"),"",0));return;}
   if(kind.equals("LOST")&&run.clicked){finish(new BotTransport.Result("UNKNOWN","Страница обновилась после начала отправки. Проверьте чат.","",0));return;}
   if(kind.equals("LOST")){run.begun=false;}
   if(kind.equals("READY")&&!run.clicked){run.committed.run();run.clicked=true;run.deadline=SystemClock.elapsedRealtime()+20000;run.view.evaluateJavascript("JSON.stringify(window.__morcoolDriver.commit("+JSONObject.quote(run.nonce)+"))",commit->{if(current!=run)return;try{String k=parse(commit).optString("kind");if(k.equals("NOT_READY")){finish(new BotTransport.Result("PAUSE","Чат или текст изменились до нажатия. Запустите после проверки.","",0));return;}if(k.equals("LOST")){finish(new BotTransport.Result("UNKNOWN","Страница обновилась в момент отправки","",0));return;}}catch(Exception ignored){}main.postDelayed(repeat,500);});return;}
  }catch(Exception e){failed(run);return;}main.postDelayed(repeat,500);});}catch(RuntimeException e){failed(run);}
 }
 static void timedOut(Run run){if(current!=run)return;finish(new BotTransport.Result(run.clicked?"UNKNOWN":"PAUSE",run.clicked?"Не удалось подтвердить отправку через веб. Проверьте чат.":"Веб-клиент не подготовил адресата. Проверьте вход, адрес и совместимость.","",0));}
 static void failed(Run run){if(current!=run)return;finish(new BotTransport.Result(run.clicked?"UNKNOWN":"PAUSE",run.clicked?"Веб-сессия прервалась после начала отправки. Проверьте чат.":"Веб-сессия недоступна. Откройте аккаунт заново.","",0));}
 static void finish(BotTransport.Result result){Run run=current;if(run==null)return;current=null;run.cancelled=true;if(run.watchdog!=null)main.removeCallbacks(run.watchdog);CookieManager.getInstance().flush();run.callback.done(result);}
 public static void cancel(){Run run=current;if(run!=null){current=null;run.cancelled=true;if(run.watchdog!=null)main.removeCallbacks(run.watchdog);}}
 public static void clearAll(){if(BackgroundSender.active())throw new IllegalStateException("Сначала остановите очередь");for(WebView v:sessions.values())v.destroy();sessions.clear();CookieManager.getInstance().removeAllCookies(null);CookieManager.getInstance().flush();WebStorage.getInstance().deleteAllData();for(String c:new String[]{"whatsapp_web","telegram_web","max_web"})Store.set("session_"+c,"");}
}
