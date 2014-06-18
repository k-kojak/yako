
package hu.rgai.yako.broadcastreceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import hu.rgai.yako.intents.IntentStrings;

/**
 *
 * @author Tamas Kojedzinszky
 * 
 */
public class MessageSentBroadcastReceiver extends BroadcastReceiver {

  public static final int MESSAGE_SENT_FAILED = 0;
  public static final int MESSAGE_SENT_SUCCESS = 1;
  public static final int MESSAGE_DELIVERED = 2;
  public static final int MESSAGE_SEEN = 3;
  
  @Override
  public void onReceive(Context context, Intent intent) {
    int sentResultType = intent.getIntExtra(IntentStrings.Params.MESSAGE_SENT_RESULT_TYPE, -1);
    if (intent.getAction().equals(IntentStrings.Actions.MESSAGE_SENT_BROADCAST)) {
      if (intent.getParcelableExtra(IntentStrings.Params.MESSAGE_SENT_HANDLER_INTENT) == null) {
        // TODO: later here we can add some message store logic if sent wasn't successfull for later resend
      } else {
        Intent i = (Intent) intent.getParcelableExtra(IntentStrings.Params.MESSAGE_SENT_HANDLER_INTENT);
        i.putExtra(IntentStrings.Params.MESSAGE_SENT_RESULT_TYPE, sentResultType);
        context.sendBroadcast(i);
      }
    }
  }
}
