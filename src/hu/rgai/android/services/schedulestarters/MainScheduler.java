package hu.rgai.android.services.schedulestarters;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import hu.rgai.android.services.MainService;
import java.util.Calendar;


public class MainScheduler extends BroadcastReceiver {

  private static final int REPEAT_TIME = 60; // seconds
  
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
      if (intent.getExtras() != null && intent.getExtras().containsKey(MainService.IntentParams.TYPE)) {
        singleIntent.putExtra(MainService.IntentParams.TYPE, intent.getExtras().getString(MainService.IntentParams.TYPE));
      }
      if (intent.getExtras() != null && intent.getExtras().containsKey(MainService.IntentParams.LOAD_MORE)) {
        singleIntent.putExtra(MainService.IntentParams.LOAD_MORE, intent.getExtras().getBoolean(MainService.IntentParams.LOAD_MORE));
      }
      if (intent.getExtras() != null && intent.getExtras().containsKey(MainService.IntentParams.FORCE_QUERY)) {
        singleIntent.putExtra(MainService.IntentParams.FORCE_QUERY, intent.getExtras().getBoolean(MainService.IntentParams.FORCE_QUERY));
      }
      
      context.startService(singleIntent);
      
    }
  }
}
