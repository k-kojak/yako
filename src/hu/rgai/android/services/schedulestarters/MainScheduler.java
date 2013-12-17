package hu.rgai.android.services.schedulestarters;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import java.util.Calendar;


public class MainScheduler extends BroadcastReceiver {

  private static final long REPEAT_TIME = 25;
  
  @Override
  public void onReceive(Context context, Intent intent) {
    System.out.println("....ACTION -> " + intent.getAction());
    if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED") || intent.getAction().equals(Context.ALARM_SERVICE)) {
      Log.d("rgai", "MainScheduler onReceive");
      AlarmManager service = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
      Intent i = new Intent(context, MainServiceStarter.class);
      if (intent.getExtras() != null && intent.getExtras().containsKey("type")) {
        i.putExtra("type", intent.getExtras().getString("type"));
      }
      PendingIntent pending = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);
      Calendar cal = Calendar.getInstance();
  //    cal.add(Calendar.SECOND, 2);

      service.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), REPEAT_TIME * 1000, pending);
    }
  }
}
