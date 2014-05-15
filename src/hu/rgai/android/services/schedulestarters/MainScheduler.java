package hu.rgai.android.services.schedulestarters;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import hu.rgai.android.beens.MainServiceExtraParams;
import hu.rgai.android.beens.MainServiceExtraParams.ParamStrings;
import hu.rgai.android.services.MainService;
import java.util.Calendar;


public class MainScheduler extends BroadcastReceiver {

  private static final int REPEAT_TIME = 60; // seconds
  
  @Override
  public void onReceive(Context context, Intent intent) {
    if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED") || intent.getAction().equals(Context.ALARM_SERVICE)) {
      AlarmManager service = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
      
      Intent repeatIntent = new Intent(context, MainService.class);
      PendingIntent pending = PendingIntent.getService(context, 0, repeatIntent, PendingIntent.FLAG_CANCEL_CURRENT);
      Calendar cal = Calendar.getInstance();
      cal.add(Calendar.SECOND, REPEAT_TIME);
      service.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), REPEAT_TIME * 1000l, pending);
      
      
      
      Intent singleIntent = new Intent(context, MainService.class);
      if (intent.getExtras() != null && intent.getExtras().containsKey(ParamStrings.EXTRA_PARAMS)) {
        singleIntent.putExtra(ParamStrings.EXTRA_PARAMS, intent.getExtras().getParcelable(ParamStrings.EXTRA_PARAMS));
      }
      context.startService(singleIntent);
      
    }
  }
}
