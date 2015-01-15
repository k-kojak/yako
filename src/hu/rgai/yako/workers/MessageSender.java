package hu.rgai.yako.workers;

import android.content.Context;
import android.widget.Toast;
import hu.rgai.android.test.R;
import hu.rgai.yako.beens.Account;
import hu.rgai.yako.beens.EmailAccount;
import hu.rgai.yako.beens.EmailMessageRecipient;
import hu.rgai.yako.beens.FacebookAccount;
import hu.rgai.yako.beens.FacebookMessageRecipient;
import hu.rgai.yako.beens.MessageRecipient;
import hu.rgai.yako.beens.SentMessageBroadcastDescriptor;
import hu.rgai.yako.handlers.TimeoutHandler;
import hu.rgai.yako.messageproviders.FacebookMessageProvider;
import hu.rgai.yako.messageproviders.MessageProvider;
import hu.rgai.yako.messageproviders.SimpleEmailMessageProvider;
import hu.rgai.yako.messageproviders.SmsMessageProvider;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 
 * @author Tamas Kojedzinszky
 */
public class MessageSender extends TimeoutAsyncTask<Void, String, Integer> {

  private final Context mContext;
  private final List<MessageRecipient> mRecipients;
  private final SentMessageBroadcastDescriptor mSentMessageData;
  private final Account mFromAccount;
  private final String mContent;
  private final String mSubject;
  private final MessageProvider.Type mRecipientsType;
  // private String recipients;
  

  public MessageSender(MessageProvider.Type recipientsType, List<MessageRecipient> recipients,
                       Account fromAccount, SentMessageBroadcastDescriptor sentMessageData,
                       TimeoutHandler timeoutHandler, String subject, String content, Context context) {
    
    super(timeoutHandler);

    this.mRecipientsType = recipientsType;
    this.mRecipients = recipients;
    this.mFromAccount = fromAccount;
    this.mSentMessageData = sentMessageData;
    this.mSubject = subject;
    this.mContent = content;
    this.mContext = context;
  }

  private boolean isValidContent() {
    if ( mRecipientsType.equals( MessageProvider.Type.FACEBOOK)
        || mRecipientsType.equals(MessageProvider.Type.SMS) ) {
      if ( mContent.length() == 0 )
        return false;
    }
    return true;
  }

  @Override
  protected Integer doInBackground(Void... params) {
    if ( mFromAccount != null && isValidContent() ) {
      MessageProvider mp = null;
//      Set<MessageRecipient> recipients = null;
      if (mRecipientsType.equals(MessageProvider.Type.FACEBOOK)) {
        mp = new FacebookMessageProvider((FacebookAccount) mFromAccount);
//        recipients = new HashSet<>();
//        recipients.add(new FacebookMessageRecipient(mRecipients.getData()));
      } else if (mRecipientsType.equals(MessageProvider.Type.EMAIL) || mRecipientsType.equals(MessageProvider.Type.GMAIL)) {
        publishProgress(mFromAccount.getDisplayName());
        mp = SimpleEmailMessageProvider.getInstance((EmailAccount) mFromAccount);
//        recipients = new HashSet<>();
//        recipients.add(new EmailMessageRecipient(mRecipients.getDisplayName(), mRecipients.getData()));
      } else if (mRecipientsType.equals(MessageProvider.Type.SMS)) {
        mp = new SmsMessageProvider(mContext);
//        recipients = new HashSet<>();
//        recipients.add(mRecipients);
      }
      if (mp != null && mRecipients != null) {
        Set<MessageRecipient> mr = new HashSet<>(mRecipients);
        mp.sendMessage(mContext, mSentMessageData, mr, mContent, mSubject);
        loggingSendMessage();
      }
    }
    return 0;
  }

  private void loggingSendMessage() {
    /*StringBuilder builder = new StringBuilder();
    builder.append(EventLogger.LOGGER_STRINGS.OTHER.SENDMESSAGE_STR);
    builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
    builder.append(mRecipient.getType());
    builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
    builder.append(mContent);
    builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
    builder.append(mRecipient.getContactId());
    builder.append(EventLogger.LOGGER_STRINGS.OTHER.SPACE_STR);
    builder.append(RSAENCODING.INSTANCE.encryptString(mRecipient.getData()));
    EventLogger.INSTANCE.writeToLogFile( LogFilePaths.FILE_TO_MESSAGES_PATH, builder.toString(), true);*/
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
    Toast.makeText(mContext, mContext.getString(R.string.sending_msg), Toast.LENGTH_SHORT).show();
  }

}
