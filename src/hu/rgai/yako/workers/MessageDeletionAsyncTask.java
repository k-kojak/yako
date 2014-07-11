
package hu.rgai.yako.workers;

import android.util.Log;
import hu.rgai.yako.handlers.MessageDeleteHandler;
import hu.rgai.yako.messageproviders.MessageProvider;
import hu.rgai.yako.messageproviders.ThreadMessageProvider;

import java.util.logging.Level;
import java.util.logging.Logger;

public class MessageDeletionAsyncTask extends TimeoutAsyncTask<Void, Void, Boolean> {

  private MessageProvider mProvider = null;
  private long mMessageListRawIdToDelete;
  private String mFullSimpleMessageIdToDelete;
  private String mMessageId = null;
  private MessageDeleteHandler mHandler = null;
  private boolean mIsThreadAccountDelete = false;
  private boolean mDeleteAtMainList = false;
  
  public MessageDeletionAsyncTask(MessageProvider messageProvider, long messageListRawIdToDelete,
          String fullSimpleMessageIdToDelete, String msgId, MessageDeleteHandler handler,
          boolean isThreadAccountDelete, boolean deleteAtMainList) {
    
    super(handler);
    mProvider = messageProvider;
    mMessageListRawIdToDelete = messageListRawIdToDelete;
    mFullSimpleMessageIdToDelete = fullSimpleMessageIdToDelete;
    mMessageId = msgId;
    mHandler = handler;
    mIsThreadAccountDelete = isThreadAccountDelete;
    mDeleteAtMainList = deleteAtMainList;
  }
  
  
  @Override
  protected Boolean doInBackground(Void... params) {
    try {
      if (mIsThreadAccountDelete && mProvider instanceof ThreadMessageProvider) {
        ((ThreadMessageProvider)mProvider).deleteThread(mMessageId);
      } else {
        mProvider.deleteMessage(mMessageId);
      }
    } catch (Exception ex) {
      Log.d("rgai", "message delete exception", ex);
      return false;
    }
    return true;
  }


  @Override
  protected void onPostExecute(Boolean success) {
    if (mHandler != null) {
      if (success) {
        if (mDeleteAtMainList) {
          mHandler.onMainListDelete(mMessageListRawIdToDelete);
        } else {
          mHandler.onThreadListDelete(mMessageListRawIdToDelete, mFullSimpleMessageIdToDelete,
                  mProvider.getAccount().isInternetNeededForLoad());
        }
      } else {
        mHandler.toastMessage("Unable to delete message.");
      }
      mHandler.onComplete();
    }
  }

}
