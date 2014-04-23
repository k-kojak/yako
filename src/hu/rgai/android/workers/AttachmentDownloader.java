/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hu.rgai.android.workers;

import android.content.Context;
import android.os.Handler;
import android.widget.ProgressBar;
import android.widget.Toast;
import hu.rgai.android.beens.ProgressAttachment;
import hu.rgai.android.store.StoreHandler;
import hu.rgai.android.tools.adapter.AttachmentAdapter;
import hu.uszeged.inf.rgai.messagelog.SimpleEmailMessageProvider;
import hu.uszeged.inf.rgai.messagelog.beans.account.EmailAccount;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;

/**
 *
 * @author k
 */
public class AttachmentDownloader implements Runnable {

  private final ProgressAttachment mAttachment;
  private final Handler mHandler;
  private final EmailAccount mAccount;
  private final String mMessageId;
  private volatile WeakReference<ProgressBar> mProgressBar;
  private final Context mContext;
  
  public AttachmentDownloader(ProgressAttachment attachment, Handler handler, EmailAccount account,
          String messageId, ProgressBar progressBar, Context context) {
    this.mAttachment = attachment;
    this.mHandler = handler;
    this.mAccount = account;
    this.mMessageId = messageId;
    this.mProgressBar = new WeakReference<ProgressBar>(progressBar);
    this.mContext = context;
  }
  
  public void setProgressBar(ProgressBar progressBar) {
    this.mProgressBar = new WeakReference<ProgressBar>(progressBar);
  }
  
  public void run() {
    SimpleEmailMessageProvider semp = new SimpleEmailMessageProvider(mAccount);
    semp.setAttachmentProgressUpdateListener(new SimpleEmailMessageProvider.AttachmentProgressUpdate() {
      public void onProgressUpdate(int progress) {
        setProgressBarValue(progress);
      }
    });
    try {
      byte[] file = semp.getAttachmentOfMessage(mMessageId, mAttachment.getFileName());
      setProgressBarValue(100);
      StoreHandler.saveByteArrayToDownloadFolder(file, mAttachment.getFileName());
      mHandler.post(new Runnable() {
        public void run() {
          Toast.makeText(mContext, mAttachment.getFileName() + " saved in Downloads", Toast.LENGTH_SHORT).show();
        }
      });
    } catch (NoSuchProviderException ex) {
      Logger.getLogger(AttachmentAdapter.class.getName()).log(Level.SEVERE, null, ex);
    } catch (MessagingException ex) {
      Logger.getLogger(AttachmentAdapter.class.getName()).log(Level.SEVERE, null, ex);
    } catch (IOException ex) {
      Logger.getLogger(AttachmentAdapter.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
  
  private void setProgressBarValue(int value) {
    mAttachment.setProgress(value);
    if (mProgressBar != null && mProgressBar.get() != null) {
      mHandler.post(new Runnable() {
        public void run() {
          mProgressBar.get().setProgress(mAttachment.getProgress());
        }
      });
    }
  }

}
