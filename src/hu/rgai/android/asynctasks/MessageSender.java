
package hu.rgai.android.asynctasks;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import hu.rgai.android.eventlogger.EventLogger;
import hu.rgai.android.eventlogger.rsa.RSAENCODING;
import hu.rgai.android.intent.beens.RecipientItem;
import hu.rgai.android.intent.beens.account.AccountAndr;
import hu.rgai.android.messageproviders.FacebookMessageProvider;
import hu.rgai.android.messageproviders.SmsMessageProvider;
import hu.rgai.android.intent.beens.SmsMessageRecipientAndr;
import hu.rgai.android.test.MessageReply;
import hu.uszeged.inf.rgai.messagelog.MessageProvider;
import hu.uszeged.inf.rgai.messagelog.SimpleEmailMessageProvider;
import hu.uszeged.inf.rgai.messagelog.beans.EmailMessageRecipient;
import hu.uszeged.inf.rgai.messagelog.beans.FacebookMessageRecipient;
import hu.uszeged.inf.rgai.messagelog.beans.MessageRecipient;
import hu.uszeged.inf.rgai.messagelog.beans.account.EmailAccount;
import hu.uszeged.inf.rgai.messagelog.beans.account.FacebookAccount;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;

/**
 *
 * @author Tamas Kojedzinszky
 */
  public class MessageSender extends AsyncTask<Integer, Integer, Boolean> {

    private static final String SENDMESSAGE_STR = "sendmessage";
    private static final String SPACE_STR = " ";
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
          mp = new SimpleEmailMessageProvider((EmailAccount)acc);
          recipients = new HashSet<MessageRecipient>();
          recipients.add(new EmailMessageRecipient(recipient.getDisplayName(), recipient.getData()));
        } else if (recipient.getType().equals(MessageProvider.Type.SMS)) {
          mp = new SmsMessageProvider(context);
          recipients = new HashSet<MessageRecipient>();
          recipients.add((MessageRecipient)recipient);
        }
        if (mp != null && recipients != null) {
          while (true) {
            try {
              mp.sendMessage(recipients, content, content.substring(0, Math.min(content.length(), 10)));
            } catch (NoSuchProviderException ex) {
              Logger.getLogger(MessageReply.class.getName()).log(Level.SEVERE, null, ex);
              break;
            } catch (MessagingException ex) {
              Logger.getLogger(MessageReply.class.getName()).log(Level.SEVERE, null, ex);
              break;
            } catch (IOException ex) {
              Logger.getLogger(MessageReply.class.getName()).log(Level.SEVERE, null, ex);
              break;
            }
            
            loggingSendMessage();
            
            break;
          }
        }
      }
      return true;
    }

    private void loggingSendMessage() {
      StringBuilder builder = new StringBuilder();
      builder.append( SENDMESSAGE_STR);
      builder.append( SPACE_STR );
      builder.append( recipient.getType() );
      builder.append( SPACE_STR );
      builder.append( content );
      builder.append( SPACE_STR );
      builder.append( recipient.getContactId());
      builder.append( SPACE_STR );
      try {
        builder.append( RSAENCODING.INSTANCE.encodingString(recipient.getData()));
        EventLogger.INSTANCE.writeToLogFile( builder.toString(), true);
      } catch (InvalidKeyException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (IllegalBlockSizeException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (NoSuchAlgorithmException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (NoSuchPaddingException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (ClassNotFoundException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (BadPaddingException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      
      
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


//    @Override
//    protected void onProgressUpdate(Integer... values) {
//      Log.d(Constants.LOG, "onProgressUpdate");
//      Message msg = handler.obtainMessage();
//      Bundle bundle = new Bundle();
//
//      bundle.putInt("progress", values[0]);
//      msg.setData(bundle);
//      handler.sendMessage(msg);
//    }

  }
