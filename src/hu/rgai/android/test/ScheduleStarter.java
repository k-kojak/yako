package hu.rgai.android.test;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import java.util.Calendar;

public class ScheduleStarter extends BroadcastReceiver {

  private static final long REPEAT_TIME = 25;
  
  @Override
  public void onReceive(Context context, Intent intent) {
    System.out.println("....ACTION -> " + intent.getAction());
    if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED") || intent.getAction().equals(Context.ALARM_SERVICE)) {
      Log.d("rgai", "ScheduleStarter onReceive");
      AlarmManager service = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
      Intent i = new Intent(context, StartService.class);
      PendingIntent pending = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);
      Calendar cal = Calendar.getInstance();
  //    cal.add(Calendar.SECOND, 2);

      service.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), REPEAT_TIME * 1000, pending);
    }
  }
}
