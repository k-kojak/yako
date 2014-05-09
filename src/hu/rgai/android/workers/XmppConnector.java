
package hu.rgai.android.workers;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import hu.rgai.android.beens.FacebookAccount;
import hu.rgai.android.messageproviders.FacebookMessageProvider;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class XmppConnector extends AsyncTask<String, Integer, Boolean> {

  private FacebookAccount fba = null;
  private Context context = null;
  
  public XmppConnector(FacebookAccount fba, Context context) {
    Log.d("rgai", "init xmpp connector");
    this.fba = fba;
    this.context = context;
  }
  
  @Override
  protected Boolean doInBackground(String... arg0) {
    FacebookMessageProvider.initConnection(fba, context);
    
    return true;
  }

}
