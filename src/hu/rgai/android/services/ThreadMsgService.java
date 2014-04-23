
package hu.rgai.android.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import hu.rgai.android.workers.ThreadContentGetter;
import hu.rgai.android.config.Settings;
import hu.rgai.android.intent.beens.FullThreadMessageParc;
import hu.rgai.android.intent.beens.account.AccountAndr;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class ThreadMsgService extends Service {

  private final IBinder mBinder = new MyBinder();
  private Handler handler = null;
  private AccountAndr account = null;
  private String threadId = null;
  
  public static final int OK = 0;
  public static final int NO_INTERNET_ACCESS = 1;
  
  @Override
  public void onCreate() {
    handler = new MyHandler(this);
  }
  
  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
//    if (intent != null && intent.getAction() != null && intent.getAction().equals(Settings.Intents.THREAD_SERVICE_INTENT)) {
//      if (isNetworkAvailable()) {
//        Log.d("rgai", "# ON START COMMAND ThreadMsgService");
//        ThreadContentGetter myThread = new ThreadContentGetter(this, handler, account);
//        myThread.execute(threadId);
//  //      myThread = new LongOperation(handler);
//  //      myThread.execute();
//      } else {
//        Message msg = handler.obtainMessage();
//        Bundle bundle = new Bundle();
//        bundle.putInt("result", NO_INTERNET_ACCESS);
//        msg.setData(bundle);
//        handler.sendMessage(msg);
//      }
//    }
    
    return Service.START_STICKY;
  }
  
  public void setAccount(AccountAndr account) {
    this.account = account;
  }
  
  public void setThreadId(String threadId) {
    this.threadId = threadId;
  }
  
  private boolean isNetworkAvailable() {
    ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
    return activeNetworkInfo != null && activeNetworkInfo.isConnected();
  }
  
  @Override
  public IBinder onBind(Intent intent) {
//    Log.d("rgai", "# BIND ThreadMsgService");
    account = intent.getParcelableExtra("account");
    threadId = intent.getStringExtra("threadId");
    return mBinder;
  }

  public class MyBinder extends Binder {
    public ThreadMsgService getService() {
      return ThreadMsgService.this;
    }
  }
  
  private class MyHandler extends Handler {
    
    private Context context;
//    
    public MyHandler(Context context) {
      this.context = context;
    }
    
    @Override
    public void handleMessage(Message msg) {
//      Log.d("rgai", "message arrived");
      Bundle bundle = msg.getData();
      if (bundle != null) {
        if (bundle.get("result") != null) {
          
          Intent intent = new Intent(Settings.Intents.THREAD_SERVICE_INTENT);
          intent.putExtra("result", bundle.getInt("result"));
          intent.putExtra("threadMessage", bundle.getParcelable("threadMessage"));
          
          sendBroadcast(intent);
        }
      }
    }
  }

}
