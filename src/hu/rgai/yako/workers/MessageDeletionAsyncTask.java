
package hu.rgai.yako.workers;

import hu.rgai.yako.beens.FullSimpleMessage;
import hu.rgai.yako.beens.MessageListElement;
import hu.rgai.yako.handlers.MessageDeleteHandler;
import hu.rgai.yako.messageproviders.MessageProvider;
import hu.rgai.yako.messageproviders.ThreadMessageProvider;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MessageDeletionAsyncTask extends TimeoutAsyncTask<Void, Void, Boolean> {

  private MessageProvider mProvider = null;
  private MessageListElement mMessageToDelete = null;
  private FullSimpleMessage mFullSimpleMessage = null;
  private String mMessageId = null;
  private MessageDeleteHandler mHandler = null;
  private boolean mThreadDelete = false;
  private boolean mDeleteAtMainList = false;
  
  public MessageDeletionAsyncTask(MessageProvider messageProvider, MessageListElement messageToDelete,
          FullSimpleMessage fullSimpleMessage, String msgId, MessageDeleteHandler handler,
          boolean threadDelete, boolean deleteAtMainList) {
    
    super(handler);
    mProvider = messageProvider;
    mMessageToDelete = messageToDelete;
    mFullSimpleMessage = fullSimpleMessage;
    mMessageId = msgId;
    mHandler = handler;
    mThreadDelete = threadDelete;
    mDeleteAtMainList = deleteAtMainList;
  }
  
  
  @Override
  protected Boolean doInBackground(Void... params) {
    try {
      if (mThreadDelete && mProvider instanceof ThreadMessageProvider) {
        ((ThreadMessageProvider)mProvider).deleteThread(mMessageId);
      } else {
        mProvider.deleteMessage(mMessageId);
      }
    } catch (Exception ex) {
      Logger.getLogger(MessageDeletionAsyncTask.class.getName()).log(Level.SEVERE, null, ex);
      return false;
    }
    return true;
  }


  @Override
  protected void onPostExecute(Boolean success) {
    if (mHandler != null) {
      if (success) {
        if (mDeleteAtMainList) {
          mHandler.onMainListDelete(mMessageToDelete);
        } else {
          mHandler.onThreadListDelete(mMessageToDelete, mFullSimpleMessage);
        }
      } else {
        mHandler.toastMessage("Unable to delete message.");
      }
      mHandler.onComplete();
    }
  }

}
