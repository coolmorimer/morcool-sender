package ru.morcool.sender;

import android.content.*;import org.json.*;import java.util.*;

/** Atomic preferences snapshot, private to the app. Authentication secrets live separately. */
public final class Store {
    private static JSONObject db;private static Context app;
    public static synchronized void init(Context c){app=c.getApplicationContext();if(db==null){try{db=new JSONObject(app.getSharedPreferences("sender",0).getString("db","{}"));for(String k:new String[]{"contacts","campaigns","blocked","templates"})if(!db.has(k))put(db,k,new JSONArray());JSONArray tasks=list("campaigns");for(int i=0;i<tasks.length();i++){JSONObject t=tasks.optJSONObject(i);if(t.optString("state").equals("RUNNING"))put(t,"state","PAUSED");JSONArray rows=t.optJSONArray("rows");for(int j=0;rows!=null&&j<rows.length();j++){JSONObject r=rows.optJSONObject(j);if(Arrays.asList("OPENED","SENDING_API","WEB_SENDING").contains(r.optString("state")))put(r,"state","UNKNOWN");}}if(!db.optBoolean("instant_next_v1",false)){put(db,"delay","0");put(db,"instant_next_v1",true);}save();}catch(Exception e){throw new IllegalStateException("Не удалось прочитать базу",e);}}}
    public static void put(JSONObject o,String k,Object v){try{o.put(k,v);}catch(JSONException e){throw new IllegalArgumentException(e);}}
    public static JSONObject object(Object... kv){JSONObject o=new JSONObject();for(int i=0;i<kv.length;i+=2)put(o,(String)kv[i],kv[i+1]);return o;}
    public static JSONObject copy(JSONObject o){try{return new JSONObject(o.toString());}catch(Exception e){throw new IllegalArgumentException("Повреждены поля контакта");}}
    public static synchronized void replaceContact(JSONObject original,JSONObject replacement){String key=identityKey(replacement);JSONArray all=list("contacts");int found=-1;for(int i=0;i<all.length();i++){JSONObject c=all.optJSONObject(i);if(c==original){found=i;continue;}if(c.optString("group").equals(replacement.optString("group"))&&identityKey(c).equals(key))throw new IllegalArgumentException("Такой контакт уже есть в списке");}if(found<0)throw new IllegalArgumentException("Контакт уже удалён");put(replacement,"contact_id",original.optString("contact_id",id()));try{all.put(found,replacement);}catch(JSONException e){throw new IllegalArgumentException(e);}save();}
    public static synchronized void save(){if(!app.getSharedPreferences("sender",0).edit().putString("db",db.toString()).commit())throw new IllegalStateException("Не удалось сохранить данные");}
    public static synchronized JSONArray list(String k){return db.optJSONArray(k);}
    public static synchronized String setting(String k,String fallback){return db.optString(k,fallback);}
    public static synchronized int numberSetting(String key,int fallback,int minimum,int maximum){try{return Math.max(minimum,Math.min(maximum,Integer.parseInt(setting(key,""+fallback))));}catch(NumberFormatException e){return fallback;}}
    public static synchronized void set(String k,String v){put(db,k,v);save();}
    public static String id(){return UUID.randomUUID().toString();}
    public static synchronized JSONObject campaign(String id){JSONArray a=list("campaigns");for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o.optString("id").equals(id))return o;}return null;}
    private static String canonicalBlock(String value){int split=value.indexOf(':');if(split>0){String channel=value.substring(0,split);try{return channel+":"+Channels.normalize(channel,value.substring(split+1));}catch(IllegalArgumentException ignored){}}return value;}
    public static synchronized boolean blocked(String phone){String key=canonicalBlock(phone);JSONArray a=list("blocked");for(int i=0;i<a.length();i++)if(canonicalBlock(a.optString(i)).equals(key))return true;return false;}
    public static synchronized void block(String phone){if(!blocked(phone)){list("blocked").put(canonicalBlock(phone));save();}}
    public static synchronized void unblock(String phone){String key=canonicalBlock(phone);JSONArray a=list("blocked"),b=new JSONArray();for(int i=0;i<a.length();i++)if(!canonicalBlock(a.optString(i)).equals(key))b.put(a.optString(i));put(db,"blocked",b);save();}
    public static synchronized void removeContact(String phone,String group){JSONArray a=list("contacts"),b=new JSONArray();for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(!(o.optString("phone").equals(phone)&&o.optString("group").equals(group)))b.put(o);}put(db,"contacts",b);save();}
    public static String contactIdentity(JSONObject c){String p=c.optString("phone");if(!p.isEmpty())return p;JSONObject f=c.optJSONObject("fields");if(f!=null)for(String k:new String[]{"telegram","max","telegram_chat_id","max_chat_id","max_user_id"})if(!f.optString(k).trim().isEmpty())return k+":"+f.optString(k).trim();throw new IllegalArgumentException("У контакта нет адреса");}
    private static String identityKey(JSONObject c){String raw=contactIdentity(c);if(raw.startsWith("telegram:")){try{return "telegram:"+Channels.normalize("telegram_web",raw.substring(9));}catch(IllegalArgumentException ignored){}}if(raw.startsWith("telegram_chat_id:")){try{return "telegram_chat_id:"+Channels.normalize("telegram_bot",raw.substring(17));}catch(IllegalArgumentException ignored){}}return raw;}
    public static synchronized boolean addContact(JSONObject contact){String key=identityKey(contact);JSONArray a=list("contacts");for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(identityKey(o).equals(key)&&o.optString("group").equals(contact.optString("group")))return false;}if(!contact.has("contact_id"))put(contact,"contact_id",id());a.put(contact);return true;}
    public static synchronized void deleteContact(JSONObject c){JSONArray old=list("contacts"),next=new JSONArray();for(int i=0;i<old.length();i++){JSONObject o=old.optJSONObject(i);if(o!=c)next.put(o);}put(db,"contacts",next);save();}
    public static synchronized List<String> groups(){TreeSet<String> s=new TreeSet<>();JSONArray a=list("contacts");for(int i=0;i<a.length();i++)s.add(a.optJSONObject(i).optString("group","Контакты"));return new ArrayList<>(s);}
    public static synchronized JSONObject create(String title,String group,String template){return create(title,group,template,"whatsapp_app");}
    public static synchronized JSONObject create(String title,String group,String template,String channel){
        Channels.label(channel);
        JSONArray rows=new JSONArray(),a=list("contacts");Set<String> seen=new HashSet<>();
        int ready=0;
        for(int i=0;i<a.length();i++){JSONObject c=a.optJSONObject(i);String p=c.optString("phone");if(!group.equals(c.optString("group"))||(!p.isEmpty()&&blocked(p)))continue;
            Map<String,String> f=fields(c);String target="",state="PENDING",detail="";try{target=Channels.target(channel,f);}catch(IllegalArgumentException e){state="SKIPPED";detail=e.getMessage();}
            if(!target.isEmpty()&&(!seen.add(target)||blocked(channel+":"+target)))continue;
            String text=Core.render(template,f);if(text.length()>Channels.maxText(channel))throw new IllegalArgumentException("Сообщение длиннее лимита выбранного мессенджера");
            rows.put(object("phone",p,"target",target,"name",c.optString("name"),"text",text,"state",state,"detail",detail,"time",0));if(state.equals("PENDING"))ready++;
        }
        if(ready==0)throw new IllegalArgumentException("В группе нет адресов для "+Channels.label(channel)+". Добавьте колонку "+Channels.key(channel)+" и проверьте стоп-лист.");
        JSONObject task=object("id",id(),"title",title.isEmpty()?"Рассылка":title,"group",group,"channel",channel,"template",template,"rows",rows,"created",System.currentTimeMillis(),"state","READY");
        list("campaigns").put(task);save();return task;
    }
    public static Map<String,String> fields(JSONObject contact){Map<String,String> f=new HashMap<>();JSONObject x=contact.optJSONObject("fields");if(x!=null){Iterator<String> it=x.keys();while(it.hasNext()){String k=it.next();f.put(k,x.optString(k));}}f.put("name",contact.optString("name"));f.put("phone",contact.optString("phone"));return f;}
    public static String channel(JSONObject task){return task.optString("channel","whatsapp_app");}
    public static String target(JSONObject row){return row.optString("target",row.optString("phone"));}
    public static synchronized int next(JSONObject task){JSONArray a=task.optJSONArray("rows");for(int i=0;i<a.length();i++){JSONObject r=a.optJSONObject(i);if(r.optString("state").equals("PENDING")){String p=r.optString("phone");if((!p.isEmpty()&&blocked(p))||blocked(channel(task)+":"+target(r))){put(r,"state","SKIPPED");put(r,"detail","Стоп-лист");save();}else return i;}}return -1;}
    public static synchronized void state(JSONObject row,String state,String detail){put(row,"state",state);put(row,"detail",detail);put(row,"time",System.currentTimeMillis());save();}
    public static String label(String state){switch(state){case "PENDING":return "В очереди";case "OPENED":return "Чат открыт";case "SENDING_API":return "Запрос отправляется";case "WEB_SENDING":return "Отправляется через веб";case "ACCEPTED":return "Принято сервисом";case "CLICKED":return "Нажата отправка";case "MANUAL":return "Подтверждено вами";case "UNKNOWN":return "Проверьте вручную";case "SKIPPED":return "Пропущено";case "RUNNING":return "В работе";case "PAUSED":return "На паузе";case "READY":return "Готово к запуску";case "DONE":return "Очередь завершена";default:return state;}}
    public static synchronized String backup(){return db.toString();}
    public static synchronized void restore(String raw)throws Exception{
        JSONObject incoming=new JSONObject(raw);for(String k:new String[]{"contacts","campaigns","blocked","templates"})if(incoming.optJSONArray(k)==null)throw new IllegalArgumentException("Это не резервная копия Morcool Sender");
        JSONArray contacts=incoming.getJSONArray("contacts");for(int i=0;i<contacts.length();i++){JSONObject c=contacts.getJSONObject(i);if(!c.optString("phone").isEmpty())Core.phone(c.getString("phone"),"7");contactIdentity(c);if(!c.has("group"))throw new IllegalArgumentException("У контакта нет группы");}
        JSONArray campaigns=incoming.getJSONArray("campaigns");for(int i=0;i<campaigns.length();i++){JSONObject c=campaigns.getJSONObject(i);Channels.label(channel(c));if(c.optJSONArray("rows")==null)throw new IllegalArgumentException("Повреждена очередь");put(c,"state","PAUSED");JSONArray rows=c.getJSONArray("rows");for(int j=0;j<rows.length();j++){JSONObject r=rows.getJSONObject(j);if(!r.optString("state").equals("SKIPPED"))Channels.normalize(channel(c),target(r));if(!r.has("text"))throw new IllegalArgumentException("Нет текста сообщения");if(Arrays.asList("OPENED","SENDING_API","WEB_SENDING").contains(r.optString("state")))put(r,"state","UNKNOWN");}}
        JSONObject old=db;db=incoming;try{save();}catch(Exception e){db=old;throw e;}
    }
}
