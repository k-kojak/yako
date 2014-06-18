
package hu.rgai.yako.broadcastreceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import hu.rgai.yako.intents.IntentStrings;
import hu.rgai.yako.view.activities.ThreadDisplayerActivity;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class ThreadMessageSentBroadcastReceiver extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    if (intent.getAction().equals(IntentStrings.Actions.MESSAGE_SENT_BROADCAST)) {
      intent.setClass(context, ThreadDisplayerActivity.MessageSentResultReceiver.class);
      Log.d("rgai", "intent: " + intent);
      LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
  }

}
