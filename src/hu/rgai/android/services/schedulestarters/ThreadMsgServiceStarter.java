
package hu.rgai.android.services.schedulestarters;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import hu.rgai.android.config.Settings;
import hu.rgai.android.services.ThreadMsgService;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class ThreadMsgServiceStarter extends BroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent intent) {
    if (intent != null && intent.getAction() != null && intent.getAction().equals(Settings.Intents.THREAD_SERVICE_INTENT)) {
      Intent service = new Intent(context, ThreadMsgService.class);
      service.setAction(Settings.Intents.THREAD_SERVICE_INTENT);
      context.startService(service);
    }
  }
}
