
package hu.rgai.yako.workers;

import android.content.Context;
import hu.rgai.yako.messageproviders.MessageProvider;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class ActiveConnectionConnector extends TimeoutAsyncTask<String, Integer, Void> {

  private MessageProvider messageProvider = null;
  private Context context = null;
  
  public ActiveConnectionConnector(MessageProvider messageProvider, Context context) {
    super(null);
    this.messageProvider = messageProvider;
    this.context = context;
  }
  
  @Override
  protected Void doInBackground(String... arg0) {
    messageProvider.establishConnection(context);
    
    return null;
  }

}
