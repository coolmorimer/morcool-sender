package ru.morcool.sender;
import android.content.*;import android.security.keystore.*;import android.util.Base64;import java.security.*;import javax.crypto.*;import javax.crypto.spec.GCMParameterSpec;

/** Tokens stay in Android Keystore-encrypted preferences, outside contact exports/backups. */
public final class Secrets {
 private static final String ALIAS="morcool_sender_bot_tokens";
 private static javax.crypto.SecretKey key()throws Exception{
  KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);
  if(!ks.containsAlias(ALIAS)){KeyGenerator g=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");g.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());g.generateKey();}
  return (javax.crypto.SecretKey)ks.getKey(ALIAS,null);
 }
 public static void set(Context c,String channel,String token){try{
  if(token.trim().isEmpty()){if(!c.getSharedPreferences("secrets",0).edit().remove(channel).commit())throw new Exception("delete failed");return;}
  Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key());String s=Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP)+":"+Base64.encodeToString(cipher.doFinal(token.trim().getBytes("UTF-8")),Base64.NO_WRAP);
  if(!c.getSharedPreferences("secrets",0).edit().putString(channel,s).commit())throw new Exception("save failed");
 }catch(Exception e){throw new IllegalStateException("Не удалось сохранить токен в защищённом хранилище");}}
 public static String get(Context c,String channel){String s=c.getSharedPreferences("secrets",0).getString(channel,"");if(s.isEmpty())return "";try{String[] a=s.split(":",2);Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(a[0],Base64.NO_WRAP)));return new String(cipher.doFinal(Base64.decode(a[1],Base64.NO_WRAP)),"UTF-8");}catch(Exception e){throw new IllegalStateException("Токен недоступен. Подключите бота заново.");}}
 public static boolean has(Context c,String channel){return !c.getSharedPreferences("secrets",0).getString(channel,"").isEmpty();}
}
