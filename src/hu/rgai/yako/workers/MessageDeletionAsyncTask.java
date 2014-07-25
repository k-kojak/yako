
package hu.rgai.yako.workers;

import android.content.Context;
import android.util.Log;
import hu.rgai.yako.beens.MessageListElement;
import hu.rgai.yako.handlers.MessageDeleteHandler;
import hu.rgai.yako.messageproviders.MessageProvider;
import hu.rgai.yako.messageproviders.ThreadMessageProvider;
import hu.rgai.yako.tools.AndroidUtils;

import java.util.HashSet;
import java.util.LinkedList;
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
  private LinkedList<MessageListElement> mDeleteMessages;
  private Context mContext;
  
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
  
  public MessageDeletionAsyncTask( LinkedList<MessageListElement> deletemessages, String fullSimpleMessageIdToDelete, MessageDeleteHandler handler,
          boolean deleteAtMainList, Context context) {

    super(handler);
    mDeleteMessages = new LinkedList<MessageListElement>();
    mDeleteMessages.addAll(deletemessages);
    mFullSimpleMessageIdToDelete = fullSimpleMessageIdToDelete;
    mHandler = handler;
    mDeleteAtMainList = deleteAtMainList;
    mContext = context;
}  
  
  @Override
  protected Boolean doInBackground(Void... params) {
    try {      
      
      if(mFullSimpleMessageIdToDelete == null){
        
        for (MessageListElement mle : mDeleteMessages){
          
          mProvider= AndroidUtils.getMessageProviderInstanceByAccount(mle.getAccount(), mContext);
          
          if (mle.getAccount().isThreadAccount() && mProvider instanceof ThreadMessageProvider) {
            ((ThreadMessageProvider)mProvider).deleteThread(mle.getId());
          } else {
            mProvider.deleteMessage(mle.getId());
          }
          
        }
     
      }else{        
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
          for (MessageListElement mle : mDeleteMessages){
              
            mProvider= AndroidUtils.getMessageProviderInstanceByAccount(mle.getAccount(), mContext);
            mHandler.onMainListDelete(mle.getRawId());
          }
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
