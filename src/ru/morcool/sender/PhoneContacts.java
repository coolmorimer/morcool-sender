package ru.morcool.sender;
import android.content.*;import android.database.Cursor;import android.provider.ContactsContract;import java.util.*;
public final class PhoneContacts {
 public static class Contact {public final String name,phone;Contact(String n,String p){name=n;phone=p;}}
 public static List<Contact> read(Context context){TreeMap<String,Contact> byPhone=new TreeMap<>();
  try(Cursor c=context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,ContactsContract.CommonDataKinds.Phone.NUMBER},null,null,ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" ASC")){
   while(c!=null&&c.moveToNext()){try{String phone=Core.phone(c.getString(1),Store.setting("country","7"));if(!byPhone.containsKey(phone))byPhone.put(phone,new Contact(c.getString(0)==null?"":c.getString(0),phone));}catch(IllegalArgumentException ignored){}}
  }return new ArrayList<>(byPhone.values());
 }
}
