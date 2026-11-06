package ru.morcool.sender;
import android.content.*;
public class BackgroundStopReceiver extends BroadcastReceiver {public void onReceive(Context c,Intent i){if(BackgroundSender.instance!=null)BackgroundSender.instance.pause("Остановлено пользователем");}}
