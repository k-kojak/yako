package hu.rgai.android.services.schedulestarters;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import hu.rgai.android.services.MainService;
import java.util.Calendar;


public class MainScheduler extends BroadcastReceiver {

  private static final int REPEAT_TIME = 300; // seconds
  
  @Override
  public void onReceive(Context context, Intent intent) {
    if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED") || intent.getAction().equals(Context.ALARM_SERVICE)) {
      AlarmManager service = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
      
      Intent repeatIntent = new Intent(context, MainServiceStarter.class);
      PendingIntent pending = PendingIntent.getBroadcast(context, 0, repeatIntent, PendingIntent.FLAG_CANCEL_CURRENT);
      Calendar cal = Calendar.getInstance();
      cal.add(Calendar.SECOND, REPEAT_TIME);
      service.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), REPEAT_TIME * 1000l, pending);
      
      
      
      Intent singleIntent = new Intent(context, MainService.class);
      // TODO: it should copy all bundles automatically
      if (intent.getExtras() != null && intent.getExtras().containsKey("type")) {
        singleIntent.putExtra("type", intent.getExtras().getString("type"));
      }
      if (intent.getExtras() != null && intent.getExtras().containsKey("load_more")) {
        singleIntent.putExtra("load_more", intent.getExtras().getBoolean("load_more"));
      }
      if (intent.getExtras() != null && intent.getExtras().containsKey("force_query")) {
        singleIntent.putExtra("force_query", intent.getExtras().getBoolean("force_query"));
      }
      
      context.startService(singleIntent);
      
    }
  }
}
