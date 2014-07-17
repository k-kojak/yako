package hu.rgai.yako.workers;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;
import hu.rgai.yako.beens.Account;
import hu.rgai.yako.beens.EmailAccount;
import hu.rgai.yako.beens.EmailMessageRecipient;
import hu.rgai.yako.beens.FacebookAccount;
import hu.rgai.yako.beens.FacebookMessageRecipient;
import hu.rgai.yako.beens.MessageRecipient;
import hu.rgai.yako.beens.SentMessageBroadcastDescriptor;
import hu.rgai.yako.eventlogger.EventLogger;
import hu.rgai.yako.eventlogger.rsa.RSAENCODING;
import hu.rgai.yako.handlers.TimeoutHandler;
import hu.rgai.yako.messageproviders.FacebookMessageProvider;
import hu.rgai.yako.messageproviders.MessageProvider;
import hu.rgai.yako.messageproviders.SimpleEmailMessageProvider;
import hu.rgai.yako.messageproviders.SmsMessageProvider;
import java.util.HashSet;
import java.util.Set;

/**
 * 
 * @author Tamas Kojedzinszky
 */
public class MessageSender extends TimeoutAsyncTask<Void, String, Integer> {

  private final Context mContext;
  private final MessageRecipient mRecipient;
  private final SentMessageBroadcastDescriptor mSentMessageData;
  private final Account mFromAccount;
  private final String mContent;
  private final String mSubject;
  // private String recipients;
  

  public MessageSender(MessageRecipient recipient, Account fromAccount, SentMessageBroadcastDescriptor sentMessageData,
          TimeoutHandler timeoutHandler, String subject, String content, Context context) {
    
    super(timeoutHandler);
    
    this.mRecipient = recipient;
    this.mFromAccount = fromAccount;
    this.mSentMessageData = sentMessageData;
    this.mSubject = subject;
    this.mContent = content;
    this.mContext = context;
  }

  private boolean isValidContent() {
    if ( mRecipient.getType().equals( MessageProvider.Type.FACEBOOK)
        || mRecipient.getType().equals( MessageProvider.Type.SMS) ) {
      if ( mContent.length() == 0 )
        return false;
    }
    return true;
  }

  @Override
  protected Integer doInBackground(Void... params) {
    if ( mFromAccount != null && isValidContent() ) {
      MessageProvider mp = null;
      Set<MessageRecipient> recipients = null;
      if (mRecipient.getType().equals(MessageProvider.Type.FACEBOOK)) {
        mp = new FacebookMessageProvider((FacebookAccount) mFromAccount);
        recipients = new HashSet<MessageRecipient>();
        recipients.add(new FacebookMessageRecipient(mRecipient.getData()));
      } else if (mRecipient.getType().equals(MessageProvider.Type.EMAIL) || mRecipient.getType().equals(MessageProvider.Type.GMAIL)) {
        publishProgress(mFromAccount.getDisplayName());
        mp = SimpleEmailMessageProvider.getInstance((EmailAccount) mFromAccount);
        recipients = new HashSet<MessageRecipient>();
        recipients.add(new EmailMessageRecipient(mRecipient.getDisplayName(), mRecipient.getData()));
      } else if (mRecipient.getType().equals(MessageProvider.Type.SMS)) {
        mp = new SmsMessageProvider(mContext);
        recipients = new HashSet<MessageRecipient>();
        recipients.add((MessageRecipient) mRecipient);
      }
      if (mp != null && recipients != null) {
        mp.sendMessage(mContext, mSentMessageData, recipients, mContent, mSubject);
        loggingSendMessage();
      }
    }
    return 0;
  }

  private void loggingSendMessage() {
    StringBuilder builder = new StringBuilder();
    builder.append(EventLogger.LOGGER_STRINGS.OTHER.SENDMESSAGE_STR);
    builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
    builder.append(mRecipient.getType());
    builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
    builder.append(mContent);
    builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
    builder.append(mRecipient.getContactId());
    builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
    builder.append(RSAENCODING.INSTANCE.encodingString(mRecipient.getData()));
    EventLogger.INSTANCE.writeToLogFile(builder.toString(), true);
  }

  @Override
  protected void onPostExecute(Integer resultCode) {
//    if (mHandler != null) {
//      if (resultCode == SENT) {
//        mHandler.success(mRecipient.getDisplayName());
//      } else {
//        mHandler.fail(mRecipient.getDisplayName());
//      }
//    }
  }

  @Override
  protected void onProgressUpdate(String... values) {
    Toast.makeText(mContext, "Sending message ...", Toast.LENGTH_SHORT).show();
  }

}
