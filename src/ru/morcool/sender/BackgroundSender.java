package ru.morcool.sender;
import android.app.*;import android.content.*;import android.os.*;import org.json.*;import java.util.concurrent.*;

/** User-started queue: no foreground messenger activity or accessibility clicks. */
public class BackgroundSender extends Service {
 public static volatile BackgroundSender instance;
 private final Handler main=new Handler(Looper.getMainLooper());private final Runnable nextRun=this::next;private final ExecutorService executor=Executors.newSingleThreadExecutor();
 private JSONObject task,row;private volatile boolean running,inFlight;private long generation;private PowerManager.WakeLock wake;
 public static boolean active(){return instance!=null&&(instance.running||instance.inFlight);}
 public static String taskId(){return active()?instance.task.optString("id"):"";}
 public IBinder onBind(Intent i){return null;}
 public void onCreate(){super.onCreate();Store.init(this);instance=this;wake=((PowerManager)getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"MorcoolSender:queue");}
 public static void launch(Context c,String taskId){Intent i=new Intent(c,BackgroundSender.class).putExtra("task_id",taskId);if(Build.VERSION.SDK_INT>=26)c.startForegroundService(i);else c.startService(i);}
 public int onStartCommand(Intent intent,int flags,int id){
  if(intent==null){stopSelf();return START_NOT_STICKY;}
  if(running||inFlight)return START_NOT_STICKY;
  task=Store.campaign(intent.getStringExtra("task_id"));if(task==null||!Channels.background(Store.channel(task))){stopSelf();return START_NOT_STICKY;}
  startForeground(41,notification("Подготовка фоновой очереди"));running=true;generation++;Store.put(task,"state","RUNNING");Store.save();next();return START_NOT_STICKY;
 }
 private void next(){
  if(!running)return;int idx=Store.next(task);if(idx<0){Store.put(task,"state","DONE");Store.save();finish("Очередь завершена");return;}
  if(!wake.isHeld())wake.acquire(10*60*1000L);
  row=task.optJSONArray("rows").optJSONObject(idx);String channel=Store.channel(task);long session=generation;
  update("Обрабатываем "+(idx+1)+" из "+task.optJSONArray("rows").length());
  if(Channels.bot(channel)){
   Store.state(row,"SENDING_API","Запрос к "+Channels.label(channel));inFlight=true;final String sendingTarget=Store.target(row),sendingText=row.optString("text");
   executor.execute(()->{BotTransport.Result result=BotTransport.send(this,channel,sendingTarget,sendingText);main.post(()->{if(session!=generation)return;inFlight=false;result(result);});});
  }else{
   Store.state(row,"OPENED","Готовим чат в веб-сессии");
   WebSessions.send(this,channel,Store.target(row),row.optString("name"),row.optString("text"),()->{if(session==generation)Store.state(row,"WEB_SENDING","Кнопка отправки нажимается в веб-сессии");},r->{if(session==generation)result(r);});
  }
 }
 private void result(BotTransport.Result result){
  if(row==null)return;
  if(result.state.equals("RETRY")){Store.state(row,"PENDING","Лимит сервиса: ожидание "+result.retrySeconds+" с");if(Store.setting("api_rate_limit_policy","wait").equals("pause")){pause("Сервис просит подождать "+result.retrySeconds+" с. Продолжите очередь позже.");return;}if(running){update("Ожидание лимита сервиса: "+result.retrySeconds+" с");main.postDelayed(nextRun,result.retrySeconds*1000L);}else finish("Очередь на паузе");return;}
  if(result.state.equals("PAUSE")){Store.state(row,"PENDING",result.detail);pause(result.detail);return;}
  if(result.state.equals("SKIPPED")&&Store.setting("unavailable_policy","skip").equals("pause")){Store.state(row,"PENDING",result.detail);pause("Получатель недоступен. Проверьте строку или пропустите её вручную.");return;}
  Store.state(row,result.state,result.detail);if(!result.messageId.isEmpty()){Store.put(row,"message_id",result.messageId);Store.save();}
  if(result.state.equals("UNKNOWN")){pause(result.detail);return;}
  if(running){int seconds;try{seconds=Integer.parseInt(Store.setting("delay","0"));}catch(Exception e){seconds=0;}main.postDelayed(nextRun,Math.max(0,seconds)*1000L);}else finish("Очередь на паузе");
 }
 public void pause(String reason){
  if(task==null)return;running=false;main.removeCallbacks(nextRun);WebSessions.cancel();
  if(row!=null && row.optString("state").equals("OPENED"))Store.state(row,"PENDING",reason);
  else if(row!=null&&row.optString("state").equals("WEB_SENDING"))Store.state(row,"UNKNOWN","Остановлено после начала отправки; проверьте чат");
  Store.put(task,"state","PAUSED");Store.save();update(reason);
  // A native HTTP request may already be in flight; its result is still recorded, but no next recipient is started.
  if(row==null||!row.optString("state").equals("SENDING_API"))finish(reason);
 }
 private void finish(String message){running=false;if(wake.isHeld())wake.release();update(message);if(Build.VERSION.SDK_INT>=24)stopForeground(STOP_FOREGROUND_DETACH);else stopForeground(false);stopSelf();}
 private void update(String text){try{getSystemService(NotificationManager.class).notify(41,notification(text));}catch(SecurityException ignored){/* Android may hide notifications after permission is revoked; queue remains stoppable in-app. */}}
 private Notification notification(String text){
  NotificationManager nm=getSystemService(NotificationManager.class);if(Build.VERSION.SDK_INT>=26)nm.createNotificationChannel(new NotificationChannel("background","Фоновая отправка",NotificationManager.IMPORTANCE_LOW));
  Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,"background"):new Notification.Builder(this);
  PendingIntent open=PendingIntent.getActivity(this,41,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  PendingIntent stop=PendingIntent.getBroadcast(this,42,new Intent(this,BackgroundStopReceiver.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  return b.setSmallIcon(android.R.drawable.ic_menu_send).setContentTitle("Morcool Sender · в фоне").setContentText(text).setContentIntent(open).setOngoing(running).setOnlyAlertOnce(true).addAction(new Notification.Action.Builder(null,"Остановить",stop).build()).build();
 }
 public void onTimeout(int startId,int type){running=false;generation++;WebSessions.cancel();checkpointInterrupted("Android завершил фоновую службу после начала запроса; проверьте чат");finish("Android ограничил время службы. Продолжите очередь из приложения.");}
 private void checkpointInterrupted(String reason){
  if(row!=null){String state=row.optString("state");if(java.util.Arrays.asList("SENDING_API","WEB_SENDING").contains(state))Store.state(row,"UNKNOWN",reason);else if(state.equals("OPENED"))Store.state(row,"PENDING","Подготовка чата прервалась до отправки");}
  if(task!=null&&!task.optString("state").equals("DONE")){Store.put(task,"state","PAUSED");Store.save();}
 }
 public void onDestroy(){main.removeCallbacksAndMessages(null);generation++;WebSessions.cancel();if((running||inFlight)&&task!=null)checkpointInterrupted("Служба завершилась после начала запроса; проверьте чат");running=false;inFlight=false;if(wake!=null&&wake.isHeld())wake.release();executor.shutdown();if(instance==this)instance=null;super.onDestroy();}
}
