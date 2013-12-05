package hu.rgai.android.asynctasks;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import hu.rgai.android.config.Settings;
import hu.rgai.android.intent.beens.FullThreadMessageParc;
import hu.rgai.android.intent.beens.MessageAtomParc;
import hu.rgai.android.intent.beens.PersonAndr;
import hu.rgai.android.intent.beens.account.AccountAndr;
import hu.rgai.android.intent.beens.account.SmsAccountAndr;
import hu.rgai.android.services.ThreadMsgService;
import hu.rgai.android.test.ThreadDisplayer;
import hu.uszeged.inf.rgai.messagelog.MessageProvider;
import hu.uszeged.inf.rgai.messagelog.beans.fullmessage.FullThreadMessage;
import hu.uszeged.inf.rgai.messagelog.beans.fullmessage.MessageAtom;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;

public class ThreadContentGetter extends AsyncTask<String, Integer, FullThreadMessageParc> {
  
    private Context context;
    private Handler handler;
    private AccountAndr account;
    
    public ThreadContentGetter(Context context, Handler handler, AccountAndr account) {
      this.context = context;
      this.handler = handler;
      this.account = account;
      this.context = context;
    }
    
    @Override
    protected FullThreadMessageParc doInBackground(String... params) {
//      SharedPreferences sharedPref = getSharedPreferences(getString(R.string.settings_email_file_key), Context.MODE_PRIVATE);
//      String email = sharedPref.getString(getString(R.string.settings_saved_email), "");
//      String pass = sharedPref.getString(getString(R.string.settings_saved_pass), "");
//      String imap = sharedPref.getString(getString(R.string.settings_saved_imap), "");
//      MailProvider2 em = new MailProvider2(email, pass, imap, Pass.smtp);
      FullThreadMessageParc threadMessage = null;
      
      try {
        if (account == null) {
          Log.d("rgai", "account is NULL @ threadContentGetter");
        } else {
          Log.d("rgai", "Getting thread messages...");
          Class providerClass = Settings.getAccountTypeToMessageProvider().get(account.getAccountType());
          Class accountClass = Settings.getAccountTypeToAccountClass().get(account.getAccountType());
          Constructor constructor = null;

          if (providerClass == null) {
            throw new RuntimeException("Provider class is null, " + account.getAccountType() + " is not a valid TYPE.");
          }
          
          MessageProvider mp = null;
          if (account.getAccountType().equals(MessageProvider.Type.SMS)) {
        	  constructor = providerClass.getConstructor(Context.class);
        	  mp = (MessageProvider) constructor.newInstance(context);        	  
          } else {
        	  constructor = providerClass.getConstructor(accountClass);
	          mp = (MessageProvider) constructor.newInstance(account);
	          // force result to ThreadMessage, since this is a thread displayer
	          
          }
          threadMessage = new FullThreadMessageParc((FullThreadMessage)mp.getMessage(params[0]));
          Iterator<MessageAtom> iterator = threadMessage.getMessages().iterator();
          while (iterator.hasNext()) {
            MessageAtom ma = iterator.next();
            MessageAtomParc map = new MessageAtomParc(ma);
//            Log.d("rgai", "PERSON BEFORE repl (thread) -> " + ma.getFrom());
            map.setFrom(PersonAndr.searchPersonAndr(context, ma.getFrom()));
//            Log.d("rgai", "PERSON AFTER  repl (thread) -> " + map.getFrom());
            threadMessage.getMessagesParc().add(map);
            iterator.remove();
          }
        }

      // TODO: handle exceptions
      } catch (NoSuchMethodException ex) {
        Logger.getLogger(ThreadDisplayer.class.getName()).log(Level.SEVERE, null, ex);
      } catch (InstantiationException ex) {
        Logger.getLogger(ThreadDisplayer.class.getName()).log(Level.SEVERE, null, ex);
      } catch (IllegalAccessException ex) {
        Logger.getLogger(ThreadDisplayer.class.getName()).log(Level.SEVERE, null, ex);
      } catch (IllegalArgumentException ex) {
        Logger.getLogger(ThreadDisplayer.class.getName()).log(Level.SEVERE, null, ex);
      } catch (InvocationTargetException ex) {
        Logger.getLogger(ThreadDisplayer.class.getName()).log(Level.SEVERE, null, ex);
      } catch (NoSuchProviderException ex) {
        Logger.getLogger(ThreadDisplayer.class.getName()).log(Level.SEVERE, null, ex);
      } catch (MessagingException ex) {
        Logger.getLogger(ThreadDisplayer.class.getName()).log(Level.SEVERE, null, ex);
      } catch (IOException ex) {
        Logger.getLogger(ThreadDisplayer.class.getName()).log(Level.SEVERE, null, ex);
      }
        
//      try {
//        content = em.getMailContent2(params[0]);
//      } catch (IOException ex) {
//        Logger.getLogger(MyService.class.getName()).log(Level.SEVERE, null, ex);
//      } catch (MessagingException ex) {
//        Logger.getLogger(EmailDisplayer.class.getName()).log(Level.SEVERE, null, ex);
//      }
//
      return threadMessage;
//      return "";
    }

    @Override
    protected void onPostExecute(FullThreadMessageParc result) {
      Message msg = handler.obtainMessage();
      Bundle bundle = new Bundle();
      bundle.putParcelable("threadMessage", result);
      bundle.putInt("result", ThreadMsgService.OK);
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