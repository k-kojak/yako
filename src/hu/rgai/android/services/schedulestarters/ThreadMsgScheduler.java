
package hu.rgai.android.services.schedulestarters;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import hu.rgai.android.config.Settings;
import java.util.Calendar;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class ThreadMsgScheduler extends BroadcastReceiver {

  private static final int REPEAT_TIME = 8;
  
  @Override
  public void onReceive(Context context, Intent intent) {
    Log.d("rgai", "# ON RECEIVE -> " + this.getClass().getName() + " intent action -> " + intent.getAction());
    if (intent.getAction().equals(Settings.Alarms.THREAD_MSG_ALARM)) {
      Log.d("rgai", "ThreadDisplayScheduler onReceive");
      AlarmManager service = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
      Intent i = new Intent(context, ThreadMsgServiceStarter.class);
      i.setAction(Settings.Intents.THREAD_SERVICE_INTENT);
      PendingIntent pending = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);
      Calendar cal = Calendar.getInstance();
//      cal.add(Calendar.SECOND, REPEAT_TIME);

      service.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), REPEAT_TIME * 1000, pending);
    }
    
  }

}
