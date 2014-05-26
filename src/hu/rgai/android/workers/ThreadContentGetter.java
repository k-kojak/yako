package hu.rgai.android.workers;

import android.content.Context;
import android.util.Log;
import hu.rgai.android.beens.FullSimpleMessage;
import hu.rgai.android.beens.FullThreadMessage;
import hu.rgai.android.config.Settings;
import hu.rgai.android.beens.Person;
import hu.rgai.android.beens.Account;
import hu.rgai.android.handlers.ThreadContentGetterHandler;
import hu.rgai.android.messageproviders.MessageProvider;
import hu.rgai.android.messageproviders.ThreadMessageProvider;
import hu.rgai.android.test.ThreadDisplayer;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;

public class ThreadContentGetter extends TimeoutAsyncTask<String, Integer, FullThreadMessage> {
  
    private final Context context;
    private final ThreadContentGetterHandler mHandler;
    private final Account account;
    private int offset = 0;
    private boolean scrollBottomAfterLoad = false;
    
    public ThreadContentGetter(Context context, ThreadContentGetterHandler handler, Account account, boolean scrollBottomAfterLoad) {
      super(handler);
      
      this.context = context;
      this.mHandler = handler;
      this.account = account;
      this.scrollBottomAfterLoad = scrollBottomAfterLoad;
    }
    
    public void setOffset(int offset) {
      this.offset = offset;
    }
    
    @Override
    protected FullThreadMessage doInBackground(String... params) {
      
      FullThreadMessage threadMessage = null;
      try {
        if (account == null) {
          Log.d("rgai", "account is NULL @ threadContentGetter");
        } else {
//          Log.d("rgai", "Getting thread messages...");
          Class providerClass = Settings.getAccountTypeToMessageProvider().get(account.getAccountType());
          Class accountClass = Settings.getAccountTypeToAccountClass().get(account.getAccountType());
          Constructor constructor = null;

          if (providerClass == null) {
            throw new RuntimeException("Provider class is null, " + account.getAccountType() + " is not a valid TYPE.");
          }
          
          ThreadMessageProvider mp = null;
          if (account.getAccountType().equals(MessageProvider.Type.SMS)) {
        	  constructor = providerClass.getConstructor(Context.class);
        	  mp = (ThreadMessageProvider) constructor.newInstance(context);
          } else {
        	  constructor = providerClass.getConstructor(accountClass);
	          mp = (ThreadMessageProvider) constructor.newInstance(account);
	          
          }
          // cast result to ThreadMessage, since this is a thread displayer
          threadMessage = (FullThreadMessage)mp.getMessage(params[0], offset, 20);
          for (FullSimpleMessage fsm : threadMessage.getMessages()) {
            fsm.setFrom(Person.searchPersonAndr(context, fsm.getFrom()));
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
        
      return threadMessage;
    }

    @Override
    protected void onPostExecute(FullThreadMessage result) {
      mHandler.onComplete(true, result, scrollBottomAfterLoad);
    }
    
  }