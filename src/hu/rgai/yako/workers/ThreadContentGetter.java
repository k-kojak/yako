package hu.rgai.yako.workers;

import android.content.Context;
import android.util.Log;
import hu.rgai.yako.beens.FullSimpleMessage;
import hu.rgai.yako.beens.FullThreadMessage;
import hu.rgai.yako.config.Settings;
import hu.rgai.yako.beens.Person;
import hu.rgai.yako.beens.Account;
import hu.rgai.yako.handlers.ThreadContentGetterHandler;
import hu.rgai.yako.messageproviders.MessageProvider;
import hu.rgai.yako.messageproviders.ThreadMessageProvider;
import hu.rgai.yako.view.activities.ThreadDisplayerActivity;
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
          Log.d("rgai", "instance is NULL @ threadContentGetter");
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
          threadMessage = (FullThreadMessage)mp.getMessage(params[0], offset, Settings.MESSAGE_QUERY_LIMIT);
          for (FullSimpleMessage fsm : threadMessage.getMessages()) {
            fsm.setFrom(Person.searchPersonAndr(context, fsm.getFrom()));
          }
        }

      // TODO: handle exceptions
      } catch (NoSuchMethodException ex) {
        Log.d("rgai", "", ex);
      } catch (InstantiationException ex) {
        Log.d("rgai", "", ex);
      } catch (IllegalAccessException ex) {
        Log.d("rgai", "", ex);
      } catch (IllegalArgumentException ex) {
        Log.d("rgai", "", ex);
      } catch (InvocationTargetException ex) {
        Log.d("rgai", "", ex);
      } catch (NoSuchProviderException ex) {
        Log.d("rgai", "", ex);
      } catch (MessagingException ex) {
        Log.d("rgai", "", ex);
      } catch (IOException ex) {
        Log.d("rgai", "", ex);
      }
        
      return threadMessage;
    }

    @Override
    protected void onPostExecute(FullThreadMessage result) {
      mHandler.onComplete(account.isInternetNeededForLoad(), true, result, scrollBottomAfterLoad);
    }
    
  }