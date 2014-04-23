
package hu.rgai.android.workers;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import hu.rgai.android.intent.beens.account.FacebookAccountAndr;
import hu.rgai.android.messageproviders.FacebookMessageProvider;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class XmppConnector extends AsyncTask<String, Integer, Boolean> {

  private FacebookAccountAndr fba = null;
  private Context context = null;
  
  public XmppConnector(FacebookAccountAndr fba, Context context) {
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
