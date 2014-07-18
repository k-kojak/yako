package hu.rgai.yako.broadcastreceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import hu.rgai.yako.intents.IntentStrings;

/**
 * Created by kojak on 7/18/2014.
 */
public class DeleteIntentBroadcastReceiver extends BroadcastReceiver {

  public static final int DELETE_INTENT_REQ_CODE = 1;

  @Override
  public void onReceive(Context context, Intent intent) {
    if (intent.getAction() != null && intent.getAction().equals(IntentStrings.Actions.DELETE_INTENT)) {
      Log.d("rgai", "deleted message id: " + intent.getStringExtra(IntentStrings.Params.MESSAGE_ID));
    }
  }
}
