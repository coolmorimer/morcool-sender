package ru.morcool.sender;
import android.app.*;import android.os.*;import android.view.*;import android.webkit.*;import android.widget.*;

/** Login stays on the messenger's own HTTPS page; the app never asks for a password or SMS code in its own form. */
public class WebAccountActivity extends Activity {
 WebView web;String channel;TextView status;
 public void onCreate(Bundle saved){super.onCreate(saved);Store.init(this);channel=getIntent().getStringExtra("channel");if(!Channels.web(channel)||BackgroundSender.active()){finish();return;}
  LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setBackgroundColor(0xfff6f8f5);root.setFitsSystemWindows(true);
  TextView heading=new TextView(this);heading.setText(Channels.label(channel));heading.setTextColor(0xff172d26);heading.setTextSize(21);heading.setPadding(20,18,20,6);root.addView(heading);
  status=new TextView(this);status.setText("Вход на официальной странице мессенджера. Веб-режим требует проверки после входа.");status.setTextColor(0xff52675e);status.setPadding(20,6,20,10);root.addView(status);
  web=WebSessions.get(this,channel);WebSessions.detach(web);root.addView(web,new LinearLayout.LayoutParams(-1,0,1));
  LinearLayout actions=new LinearLayout(this);Button close=new Button(this);close.setText("Готово");close.setAllCaps(false);close.setMinHeight(56);close.setOnClickListener(v->{WebSessions.probeLogin(this,channel,this::finish);});actions.addView(close,new LinearLayout.LayoutParams(0,-2,1));Button check=new Button(this);check.setText("Проверить вход");check.setAllCaps(false);check.setOnClickListener(v->WebSessions.probeLogin(this,channel,()->status.setText(Store.setting("session_"+channel,"Ожидаем вход"))));actions.addView(check,new LinearLayout.LayoutParams(0,-2,1));root.addView(actions);setContentView(root);
 }
 public void onDestroy(){if(web!=null)WebSessions.detach(web);super.onDestroy();}
}
