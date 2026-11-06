package ru.morcool.sender;

import android.accessibilityservice.*;import android.app.*;import android.content.*;import android.net.Uri;import android.os.*;import android.view.accessibility.*;import org.json.*;import java.util.*;

/** Explicitly armed session only. One click per row; uncertain rows are never retried automatically. */
public class SendAccess extends AccessibilityService {
    public static volatile SendAccess instance;
    public static boolean enabled(Context context){
        String list=android.provider.Settings.Secure.getString(context.getContentResolver(),android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        ComponentName own=new ComponentName(context,SendAccess.class);
        if(list!=null)for(String value:list.split(":"))if(own.equals(ComponentName.unflattenFromString(value)))return true;
        return false;
    }
    public static AccessState.State connectionState(Context context){return AccessState.resolve(enabled(context),instance!=null);}
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final CompletionWatch completion=new CompletionWatch();
    private JSONObject task,row;private String pkg,expected;private boolean running,clicked,opening,confirmed,sawWhatsapp;private long deadline;private int delay=0;
    public static boolean active(){return instance!=null&&instance.running;}
    public static String taskId(){return active()?instance.task.optString("id"):"";}
    protected void onServiceConnected(){super.onServiceConnected();Store.init(this);instance=this;}
    public void onAccessibilityEvent(AccessibilityEvent event){} // Poll only while a user-started session is active.
    public void onInterrupt(){pause("Служба прервана");}
    public void onDestroy(){pause("Служба отключена");instance=null;super.onDestroy();}
    public void start(JSONObject t,String packageName,int seconds){
        if(running)throw new IllegalStateException("Сначала остановите текущую рассылку");
        task=t;row=null;confirmed=true;pkg=packageName;delay=Math.max(0,seconds);running=true;Store.put(task,"state","RUNNING");Store.save();advance();
    }
    private void advance(){
        if(!running)return;
        if(((KeyguardManager)getSystemService(KEYGUARD_SERVICE)).isKeyguardLocked()){pause("Экран заблокирован");return;}
        int idx=Store.next(task);if(idx<0){Store.put(task,"state","DONE");Store.save();running=false;notifyStatus("Очередь завершена. Статус доставки смотрите в WhatsApp.");return;}
        row=task.optJSONArray("rows").optJSONObject(idx);expected=row.optString("text");clicked=false;confirmed=false;sawWhatsapp=false;completion.reset();opening=true;deadline=SystemClock.elapsedRealtime()+15000;
        android.util.Log.i("MorcoolQueue","Opening queue row "+idx);
        Store.state(row,"OPENED","Ожидается чат WhatsApp");notifyStatus("Открываем "+row.optString("phone")+" · остановить можно здесь");
        try{startActivity(chatIntent(pkg,row.optString("phone"),expected));opening=false;handler.postDelayed(tick,1000);}catch(Exception e){pause("WhatsApp не открылся: "+e.getMessage());}
    }
    public static Intent chatIntent(String pkg,String phone,String text){return new Intent(Intent.ACTION_VIEW,Uri.parse("https://wa.me/"+phone.replace("+","")+"?text="+Uri.encode(text))).setPackage(pkg).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);}
    private final Runnable tick=new Runnable(){public void run(){
        if(!running||opening)return;
        if(((KeyguardManager)getSystemService(KEYGUARD_SERVICE)).isKeyguardLocked()){pause("Экран заблокирован");return;}
        AccessibilityNodeInfo root=conversationRoot(pkg);
        if(root!=null && pkg.contentEquals(root.getPackageName())) {
            sawWhatsapp=true;
            if(!clicked && unavailableDialog(root)){skipCurrent("Номер отсутствует в WhatsApp или недействителен",true);return;}
            List<AccessibilityNodeInfo> editors=root.findAccessibilityNodeInfosByViewId(pkg+":id/entry");
            if(editors.size()==1){AccessibilityNodeInfo editor=editors.get(0);if(!editor.refresh()){if(SystemClock.elapsedRealtime()>deadline){handleTimeout();return;}handler.postDelayed(this,500);return;}String actual=editor.getText()==null?"":editor.getText().toString();
                if(!clicked && actual.equals(expected) && editor.isEditable() && matchesRecipient(root)) {
                    List<AccessibilityNodeInfo> sends=root.findAccessibilityNodeInfosByViewId(pkg+":id/send");
                    if(sends.size()==1 && sends.get(0).isEnabled() && sends.get(0).isClickable() && sends.get(0).isVisibleToUser()) {
                        // Commit uncertain status before touching WhatsApp: process death cannot create a duplicate retry.
                        Store.state(row,"UNKNOWN","Отправка начата; при сбое проверьте чат");clicked=true;
                        if(!sends.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK)){pause("WhatsApp не принял нажатие; проверьте чат");return;}
                        android.util.Log.i("MorcoolQueue","Send clicked; awaiting cleared editor");
                        deadline=SystemClock.elapsedRealtime()+15000;
                    }
                }else if(matchesRecipient(root) && completion.observe(clicked,editor.isVisibleToUser(),actual,nodeHint(editor),nodeShowsHint(editor),sendAvailable(root,pkg))){
                    Store.state(row,"CLICKED","Кнопка нажата, поле очищено. Доставка не проверена.");confirmed=true;
                    android.util.Log.i("MorcoolQueue","Editor clear confirmed; next row scheduled in "+delay+"s");
                    notifyStatus(delay==0?"Открываем следующего получателя":"Следующий получатель через "+delay+" с");handler.postDelayed(()->{if(running)advance();},delay*1000L);return;
                }
            }
        }
        if(SystemClock.elapsedRealtime()>deadline){handleTimeout();return;}
        handler.postDelayed(this,500);
    }};
    private void handleTimeout(){
        if(SkipPolicy.skipTimeout(clicked,sawWhatsapp))skipCurrent("Не удалось найти или подготовить чат за 15 секунд",false);
        else pause(clicked?"Не удалось подтвердить очистку поля. Проверьте чат.":"WhatsApp не открылся. Проверьте приложение и разрешение фонового открытия.");
    }
    private void skipCurrent(String reason,boolean dismiss){
        if(!running)return;
        if(clicked){pause("Результат отправки неизвестен; проверьте чат");return;}
        Store.state(row,"SKIPPED",reason);confirmed=true;
        android.util.Log.i("MorcoolQueue","Row skipped before Send; advancing");
        if(dismiss)performGlobalAction(GLOBAL_ACTION_BACK);
        notifyStatus("Пропущено: "+reason+". Переходим дальше.");
        handler.postDelayed(()->{if(running)advance();},dismiss?400:0);
    }
    private boolean unavailableDialog(AccessibilityNodeInfo root){
        for(String id:new String[]{"android:id/message",pkg+":id/dialog_message",pkg+":id/alert_dialog_message"}){
            for(AccessibilityNodeInfo n:root.findAccessibilityNodeInfosByViewId(id))if(n.isVisibleToUser() && SkipPolicy.unavailable(n.getText()==null?"":n.getText().toString()))return true;
        }
        // Custom WhatsApp dialogs: inspect visible labels only when the editor is absent and a dialog button exists.
        if(!root.findAccessibilityNodeInfosByViewId(pkg+":id/entry").isEmpty())return false;
        boolean dialog=!root.findAccessibilityNodeInfosByViewId("android:id/button1").isEmpty()||!root.findAccessibilityNodeInfosByViewId("android:id/button2").isEmpty();
        if(!dialog)return false;
        ArrayDeque<AccessibilityNodeInfo> todo=new ArrayDeque<>();todo.add(root);int visited=0;
        while(!todo.isEmpty()&&visited++<300){AccessibilityNodeInfo n=todo.removeFirst();if(n.isVisibleToUser()&&n.getText()!=null&&SkipPolicy.unavailable(n.getText().toString()))return true;for(int i=0;i<n.getChildCount();i++){AccessibilityNodeInfo child=n.getChild(i);if(child!=null)todo.add(child);}}
        return false;
    }
    private CharSequence nodeHint(AccessibilityNodeInfo n){return Build.VERSION.SDK_INT>=26?n.getHintText():null;}
    private boolean nodeShowsHint(AccessibilityNodeInfo n){return Build.VERSION.SDK_INT>=26&&n.isShowingHintText();}
    private boolean sendAvailable(AccessibilityNodeInfo root,String target){for(AccessibilityNodeInfo button:root.findAccessibilityNodeInfosByViewId(target+":id/send")){button.refresh();if(button.isVisibleToUser()&&button.isEnabled())return true;}return false;}
    private AccessibilityNodeInfo conversationRoot(String packageName){
        AccessibilityNodeInfo root=getRootInActiveWindow();
        if(root!=null && packageName.contentEquals(root.getPackageName())){root.refresh();return root;}
        for(AccessibilityWindowInfo window:getWindows()){
            if(window.getType()!=AccessibilityWindowInfo.TYPE_APPLICATION || !(window.isFocused()||window.isActive()))continue;
            AccessibilityNodeInfo candidate=window.getRoot();
            if(candidate!=null && packageName.contentEquals(candidate.getPackageName())){candidate.refresh();return candidate;}
        }
        return null;
    }
    /** Read-only ADB diagnostics. Never prints message text, contact names, or numbers; never starts a session. */
    protected void dump(java.io.FileDescriptor fd,java.io.PrintWriter out,String[] args){
        out.println("MorcoolQueue connected=true running="+running+" clicked="+clicked+" confirmed="+confirmed+" configuredDelaySeconds="+Store.setting("delay","0"));
        String target=pkg==null?Store.setting("whatsapp","com.whatsapp"):pkg;
        AccessibilityNodeInfo root=conversationRoot(target);out.println("whatsappWindow="+(root!=null));
        if(root!=null){List<AccessibilityNodeInfo> editors=root.findAccessibilityNodeInfosByViewId(target+":id/entry");out.println("editorCount="+editors.size()+" sendAvailable="+sendAvailable(root,target));for(AccessibilityNodeInfo e:editors){e.refresh();CharSequence t=e.getText(),h=nodeHint(e);out.println("editorVisible="+e.isVisibleToUser()+" textLength="+(t==null?0:t.length())+" hintLength="+(h==null?0:h.length())+" showingHint="+nodeShowsHint(e)+" cleared="+CompletionWatch.cleared(t,h,nodeShowsHint(e))+" knownPlaceholder="+CompletionWatch.placeholder(t));}}
    }
    private boolean matchesRecipient(AccessibilityNodeInfo root){
        List<AccessibilityNodeInfo> headers=root.findAccessibilityNodeInfosByViewId(pkg+":id/conversation_contact_name");
        if(headers.size()!=1 || headers.get(0).getText()==null)return false;
        String title=headers.get(0).getText().toString().trim();
        String phone=row.optString("phone").replaceAll("[^0-9]","");
        if(title.replaceAll("[^0-9]","").equals(phone))return true;
        // Saved contacts use the phone's address-book display name. Only allow an exact imported name.
        return !row.optString("name").trim().isEmpty() && title.equals(row.optString("name").trim());
    }
    public void pause(String reason){
        handler.removeCallbacksAndMessages(null);
        if(running){if(row!=null&&!confirmed)Store.state(row,"UNKNOWN",reason);Store.put(task,"state","PAUSED");Store.save();running=false;notifyStatus("Пауза: "+reason);}
    }
    private void notifyStatus(String text){
        NotificationManager nm=getSystemService(NotificationManager.class);if(Build.VERSION.SDK_INT>=26)nm.createNotificationChannel(new NotificationChannel("queue","Очередь отправки",NotificationManager.IMPORTANCE_LOW));
        PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent stop=PendingIntent.getBroadcast(this,1,new Intent(this,StopReceiver.class),PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b=(Build.VERSION.SDK_INT>=26?new Notification.Builder(this,"queue"):new Notification.Builder(this)).setSmallIcon(android.R.drawable.ic_menu_send).setContentTitle("Morcool Sender").setContentText(text).setStyle(new Notification.BigTextStyle().bigText(text)).setContentIntent(open).setOngoing(running).setOnlyAlertOnce(true);
        if(running)b.addAction(new Notification.Action.Builder(null,"Остановить",stop).build());
        try{nm.notify(20,b.build());}catch(SecurityException ignored){}
    }
}
