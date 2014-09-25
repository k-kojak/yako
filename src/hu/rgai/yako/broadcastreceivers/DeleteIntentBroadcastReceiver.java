package hu.rgai.yako.broadcastreceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import hu.rgai.yako.beens.Account;
import hu.rgai.yako.beens.MessageListElement;
import hu.rgai.yako.intents.IntentStrings;
import hu.rgai.yako.sql.AccountDAO;
import hu.rgai.yako.sql.MessageListDAO;

import java.util.TreeMap;

/**
 * Created by kojak on 7/18/2014.
 */
public class DeleteIntentBroadcastReceiver extends BroadcastReceiver {

  public static final int DELETE_INTENT_REQ_CODE = 1;

  @Override
  public void onReceive(Context context, Intent intent) {
    if (intent.getAction() != null && intent.getAction().equals(IntentStrings.Actions.DELETE_INTENT)) {
      long rawId = intent.getLongExtra(IntentStrings.Params.MESSAGE_RAW_ID, -1);
      Log.d("rgai", "deleted message raw id: " + rawId);
      if (rawId != -1) {
        TreeMap<Long, Account> accountsLongKey = AccountDAO.getInstance(context).getIdToAccountsMap();
        MessageListElement mle = MessageListDAO.getInstance(context).getMessageByRawId(rawId, accountsLongKey);
        if (mle != null) {
          Log.d("rgai", mle.toString());
        }
      }
    }
  }
}
