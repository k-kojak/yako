
package hu.rgai.android.workers;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import hu.rgai.android.messageproviders.MessageProvider;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class ActiveConnectionConnector extends AsyncTask<String, Integer, Boolean> {

  private MessageProvider messageProvider = null;
  private Context context = null;
  
  public ActiveConnectionConnector(MessageProvider messageProvider, Context context) {
    this.messageProvider = messageProvider;
    this.context = context;
  }
  
  @Override
  protected Boolean doInBackground(String... arg0) {
    messageProvider.establishConnection(context);
    
    return true;
  }

}
