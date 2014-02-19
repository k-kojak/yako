
package hu.rgai.android.asynctasks;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import hu.rgai.android.intent.beens.FullSimpleMessageParc;
import hu.rgai.android.intent.beens.account.AccountAndr;
import hu.rgai.android.test.EmailDisplayer;
import hu.uszeged.inf.rgai.messagelog.MessageProvider;
import hu.uszeged.inf.rgai.messagelog.SimpleEmailMessageProvider;
import hu.uszeged.inf.rgai.messagelog.beans.account.EmailAccount;
import hu.uszeged.inf.rgai.messagelog.beans.account.GmailAccount;
import hu.uszeged.inf.rgai.messagelog.beans.fullmessage.FullSimpleMessage;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class EmailContentGetter extends AsyncTask<String, Integer, FullSimpleMessageParc> {

    Handler handler;
    AccountAndr account;
    
    public EmailContentGetter(Handler handler, AccountAndr account) {
      this.handler = handler;
      this.account = account;
    }
    
    @Override
    protected FullSimpleMessageParc doInBackground(String... params) {
      FullSimpleMessageParc fsm = null;
      
      try {
        if (account.getAccountType().equals(MessageProvider.Type.EMAIL)) {
          SimpleEmailMessageProvider semp = new SimpleEmailMessageProvider((EmailAccount)account);
          fsm = new FullSimpleMessageParc((FullSimpleMessage)semp.getMessage(params[0]));
        } else if (account.getAccountType().equals(MessageProvider.Type.GMAIL)) {
          SimpleEmailMessageProvider semp = new SimpleEmailMessageProvider((GmailAccount)account);
          fsm = new FullSimpleMessageParc((FullSimpleMessage)semp.getMessage(params[0]));
        }
      } catch (NoSuchProviderException ex) {
        Logger.getLogger(EmailDisplayer.class.getName()).log(Level.SEVERE, null, ex);
      } catch (MessagingException ex) {
        Logger.getLogger(EmailDisplayer.class.getName()).log(Level.SEVERE, null, ex);
      } catch (IOException ex) {
        Logger.getLogger(EmailDisplayer.class.getName()).log(Level.SEVERE, null, ex);
      }

      return fsm;
    }

    @Override
    protected void onPostExecute(FullSimpleMessageParc result) {
      Message msg = handler.obtainMessage();
      Bundle bundle = new Bundle();
      bundle.putParcelable("content", result);
      msg.setData(bundle);
      handler.sendMessage(msg);
    }

  }
