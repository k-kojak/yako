
package hu.rgai.android.asynctasks;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;
import hu.rgai.android.intent.beens.RecipientItem;
import hu.rgai.android.intent.beens.account.AccountAndr;
import hu.rgai.android.messageproviders.FacebookMessageProvider;
import hu.rgai.android.messageproviders.SmsMessageProvider;
import hu.rgai.android.intent.beens.SmsMessageRecipientAndr;
import hu.rgai.android.intent.beens.account.SmsAccountAndr;
import hu.rgai.android.test.MessageReply;
import hu.uszeged.inf.rgai.messagelog.MessageProvider;
import hu.uszeged.inf.rgai.messagelog.SimpleEmailMessageProvider;
import hu.uszeged.inf.rgai.messagelog.beans.EmailMessageRecipient;
import hu.uszeged.inf.rgai.messagelog.beans.FacebookMessageRecipient;
import hu.uszeged.inf.rgai.messagelog.beans.MessageRecipient;
import hu.uszeged.inf.rgai.messagelog.beans.account.EmailAccount;
import hu.uszeged.inf.rgai.messagelog.beans.account.FacebookAccount;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;

/**
 *
 * @author Tamas Kojedzinszky
 */
  public class MessageSender extends AsyncTask<Integer, String, Boolean> {

    private Context context;
    private RecipientItem recipient;
    private Handler handler;
    private List<AccountAndr> accounts;
    private String content;
//    private String subject;
//    private String recipients;
    
    private String result = null;
    
    public MessageSender(RecipientItem recipient, List<AccountAndr> accounts, Handler handler, String content, Context context) {
      this.recipient = recipient;
      this.accounts = accounts;
      this.handler = handler;
      this.content = content;
      this.context = context;
//      this.subject = subject;
//      this.recipients = recipients;
    }
    
    @Override
    protected Boolean doInBackground(Integer... params) {
      AccountAndr acc = getAccountForType(recipient.getType());

      if (acc != null) {
        MessageProvider mp = null;
        Set<MessageRecipient> recipients = null;
        if (recipient.getType().equals(MessageProvider.Type.FACEBOOK)) {
          mp = new FacebookMessageProvider((FacebookAccount)acc);
          recipients = new HashSet<MessageRecipient>();
          recipients.add(new FacebookMessageRecipient(recipient.getData()));
          Log.d("rgai", "SENDING FACEBOOK MESSAGE");
        } else if (recipient.getType().equals(MessageProvider.Type.EMAIL) || recipient.getType().equals(MessageProvider.Type.GMAIL)) {
          publishProgress(acc.getDisplayName());
//          Toast.makeText(this.g, "Sending message with email: " + acc.getDisplayName(), Toast.LENGTH_LONG).show();
          mp = new SimpleEmailMessageProvider((EmailAccount)acc);
          recipients = new HashSet<MessageRecipient>();
          recipients.add(new EmailMessageRecipient(recipient.getDisplayName(), recipient.getData()));
        } else if (recipient.getType().equals(MessageProvider.Type.SMS)) {
          mp = new SmsMessageProvider(context);
          recipients = new HashSet<MessageRecipient>();
          recipients.add((MessageRecipient)recipient);
        }
        if (mp != null && recipients != null) {
          try {
            mp.sendMessage(recipients, content, content.substring(0, Math.min(content.length(), 10)));
          } catch (NoSuchProviderException ex) {
            Logger.getLogger(MessageReply.class.getName()).log(Level.SEVERE, null, ex);
          } catch (MessagingException ex) {
            Logger.getLogger(MessageReply.class.getName()).log(Level.SEVERE, null, ex);
          } catch (IOException ex) {
            Logger.getLogger(MessageReply.class.getName()).log(Level.SEVERE, null, ex);
          }
        }
      }
      return true;
    }
    
    // TODO: gmail != email
    private AccountAndr getAccountForType(MessageProvider.Type type) {
      boolean m = type.equals(MessageProvider.Type.EMAIL) || type.equals(MessageProvider.Type.GMAIL);
      for (AccountAndr acc : accounts) {
        if (m) {
          if (acc.getAccountType().equals(MessageProvider.Type.EMAIL) || acc.getAccountType().equals(MessageProvider.Type.GMAIL)) {
            return acc;
          }
        } else {
          if (acc.getAccountType().equals(type)) {
            return acc;
          }
        }
      }
      if (type.equals(MessageProvider.Type.SMS)) {
        return new SmsAccountAndr();
      }
      return null;
    }

    @Override
    protected void onPostExecute(Boolean success) {
      Message msg = handler.obtainMessage();
      Bundle bundle = new Bundle();
      bundle.putBoolean("success", success);
      msg.setData(bundle);
      handler.sendMessage(msg);
    }


    @Override
    protected void onProgressUpdate(String... values) {
      Toast.makeText(context, "Sending message with " + values[0], Toast.LENGTH_LONG).show();
    }

  }
