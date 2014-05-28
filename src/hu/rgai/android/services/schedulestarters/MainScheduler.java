package hu.rgai.android.services.schedulestarters;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import hu.rgai.android.config.Settings;
import hu.rgai.android.services.MainService;
import hu.rgai.android.tools.ParamStrings;
import java.util.Calendar;


public class MainScheduler extends BroadcastReceiver {

  
  @Override
  public void onReceive(Context context, Intent intent) {
    if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED") || intent.getAction().equals(Context.ALARM_SERVICE)) {
      AlarmManager service = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
      
      Intent repeatIntent = new Intent(context, MainService.class);
      PendingIntent pending = PendingIntent.getService(context, 0, repeatIntent, PendingIntent.FLAG_CANCEL_CURRENT);
      Calendar cal = Calendar.getInstance();
      cal.add(Calendar.SECOND, Settings.MESSAGE_LOAD_INTERVAL);
      service.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), Settings.MESSAGE_LOAD_INTERVAL * 1000l, pending);
      
      
      
      Intent singleIntent = new Intent(context, MainService.class);
      if (intent.getExtras() != null && intent.getExtras().containsKey(ParamStrings.EXTRA_PARAMS)) {
        singleIntent.putExtra(ParamStrings.EXTRA_PARAMS, intent.getExtras().getParcelable(ParamStrings.EXTRA_PARAMS));
      }
      context.startService(singleIntent);
      
    }
  }
}
