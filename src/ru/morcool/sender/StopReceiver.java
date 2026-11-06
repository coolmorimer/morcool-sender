package ru.morcool.sender;
import android.content.*;
public class StopReceiver extends BroadcastReceiver {public void onReceive(Context c,Intent i){if(SendAccess.instance!=null)SendAccess.instance.pause("Остановлено из уведомления");}}
