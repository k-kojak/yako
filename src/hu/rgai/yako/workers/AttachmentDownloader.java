/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hu.rgai.yako.workers;

import android.content.Context;
import android.os.Handler;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import hu.rgai.yako.beens.Attachment;
import hu.rgai.yako.beens.EmailAccount;
import hu.rgai.yako.messageproviders.SimpleEmailMessageProvider;
import hu.rgai.yako.store.StoreHandler;
import hu.rgai.yako.tools.Utils;
import java.io.Serializable;
import java.lang.ref.WeakReference;

/**
 *
 * @author k
 */
public class AttachmentDownloader implements Runnable, Serializable {

  private final Attachment mAttachment;
  private final Handler mHandler;
  private final EmailAccount mAccount;
  private final String mMessageId;
  private volatile WeakReference<ProgressBar> mProgressBar;
  private final Context mContext;
  private WeakReference<TextView> mFileSize;
  private volatile boolean running = false;
  
  public AttachmentDownloader(Attachment attachment, Handler handler, EmailAccount account,
          String messageId, ProgressBar progressBar, TextView fileSize, Context context) {
    this.mAttachment = attachment;
    this.mHandler = handler;
    this.mAccount = account;
    this.mMessageId = messageId;
    this.mProgressBar = new WeakReference<ProgressBar>(progressBar);
    this.mFileSize = new WeakReference<TextView>(fileSize);
    this.mContext = context;
  }
  
  public void setProgressBarView(ProgressBar progressBar) {
    this.mProgressBar = new WeakReference<ProgressBar>(progressBar);
  }
  
  public void setFileSizeView(TextView fileSize) {
    this.mFileSize = new WeakReference<TextView>(fileSize);
  }
  
  public void run() {
    running = true;
    mAttachment.setInProgress(true);
    mProgressBar.get().setIndeterminate(true);
    SimpleEmailMessageProvider semp = SimpleEmailMessageProvider.getInstance(mAccount);
    semp.setAttachmentProgressUpdateListener(new SimpleEmailMessageProvider.AttachmentProgressUpdate() {
      public void onProgressUpdate(int progress) {
        setProgressBarValue(progress, false);
      }
    });
    try {
      byte[] file = semp.getAttachmentOfMessage(mMessageId, mAttachment.getFileName());
      setProgressBarValue(100, false);
      if (StoreHandler.saveByteArrayToDownloadFolder(mContext, file, mAttachment.getFileName())) {
        mAttachment.setSize(file.length);
        mHandler.post(new Runnable() {
          public void run() {
            if (mFileSize != null && mFileSize.get() != null) {
              mFileSize.get().setText(Utils.getPrettyFileSize(mAttachment.getSize()));
            }
            Toast.makeText(mContext, mAttachment.getFileName() + " saved in Downloads", Toast.LENGTH_SHORT).show();
          }
        });
      } else {
        mHandler.post(new Runnable() {
          public void run() {
            Toast.makeText(mContext, "Failed to save document: " + mAttachment.getFileName(), Toast.LENGTH_LONG).show();
          }
        });
      }
    } catch (Exception ex) {
      ex.printStackTrace();
      setProgressBarValue(0, true);
      mHandler.post(new Runnable() {
        public void run() {
          Toast.makeText(mContext, "Failed to download file: " + mAttachment.getFileName(), Toast.LENGTH_LONG).show();
        }
      });
      
    }
    running = false;
  }
  
  public boolean isRunning() {
    return running;
  }
  
  private void setProgressBarValue(int value, boolean reset) {
    mAttachment.setProgress(value);
    if (reset) {
      mAttachment.setInProgress(false);
    }
    if (mProgressBar != null && mProgressBar.get() != null) {
      mProgressBar.get().setIndeterminate(false);
      mHandler.post(new Runnable() {
        public void run() {
          mProgressBar.get().setProgress(mAttachment.getProgress());
        }
      });
    }
  }

}
