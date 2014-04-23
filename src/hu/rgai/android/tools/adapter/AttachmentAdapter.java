package hu.rgai.android.tools.adapter;

import android.content.Context;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import hu.rgai.android.beens.ProgressAttachment;
import hu.rgai.android.intent.beens.account.AccountAndr;
import hu.rgai.android.store.StoreHandler;
import hu.rgai.android.test.R;
import hu.uszeged.inf.rgai.messagelog.SimpleEmailMessageProvider;
import hu.uszeged.inf.rgai.messagelog.beans.Attachment;
import hu.uszeged.inf.rgai.messagelog.beans.account.EmailAccount;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;

public class AttachmentAdapter extends BaseAdapter {

//  private MainActivity activity;
  private static LayoutInflater inflater = null;
//  private AccountAndr filterAcc = null;
  private Context mContext;
  private List<Attachment> mAttachments;
  private EmailAccount mAccount;
  private String mMessageId;
  

  public AttachmentAdapter(Context context, List<Attachment> attachments, AccountAndr account, String messageId) {
    this.mContext = context;
    this.mAttachments = attachments;
    this.mAccount = (EmailAccount)account;
    this.mMessageId = messageId;
    inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    
  }

  public int getCount() {
    return mAttachments.size();
  }

  public Object getItem(int position) {
    return mAttachments.get(position);
  }

  public long getItemId(int position) {
    return position;
  }

  public View getView(int position, View view, ViewGroup parent) {
    final ViewHolder holder;
    
    if (view == null) {
      view = inflater.inflate(R.layout.attachment_list_item, null);
      holder = new ViewHolder();
      
      holder.progressBar = (ProgressBar) view.findViewById(R.id.progressbar);
      holder.textView = (TextView) view.findViewById(R.id.file_name);
      holder.button = (ImageButton) view.findViewById(R.id.download);
      view.setTag(holder);
    } else {
      holder = (ViewHolder)view.getTag();
    }
    
    final ProgressAttachment attachment = (ProgressAttachment)this.getItem(position);
    final Handler progBarHandler = new Handler();
    
    holder.progressBar.setProgress(attachment.getProgress());
    holder.textView.setText(attachment.getFileName());
    holder.button.setOnClickListener(new View.OnClickListener() {
      public void onClick(View arg0) {
        if (!attachment.isInProgress() && !attachment.isDownloaded()) {
          attachment.setInProgress(true);
          new Thread(new Runnable() {
            public void run() {
              SimpleEmailMessageProvider semp = new SimpleEmailMessageProvider(mAccount);
              semp.setAttachmentProgressUpdateListener(new SimpleEmailMessageProvider.AttachmentProgressUpdate() {
                public void onProgressUpdate(int progress) {
                  attachment.setProgress(progress);
                  progBarHandler.post(new Runnable() {
                    public void run() {
                      holder.progressBar.setProgress(attachment.getProgress());
                    }
                  });
                }
              });
              try {
                byte[] file = semp.getAttachmentOfMessage(mMessageId, attachment.getFileName());
                attachment.setProgress(100);
                holder.progressBar.setProgress(100);
                StoreHandler.saveByteArrayToDownloadFolder(file, attachment.getFileName());
                progBarHandler.post(new Runnable() {
                  public void run() {
                    Toast.makeText(mContext, attachment.getFileName() + " saved in Downloads", Toast.LENGTH_SHORT).show();
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
          }).start();
        }

      }
    });
    
    
    return view;
  }
  
  static class ViewHolder {
    ProgressBar progressBar;
    TextView textView;
    ImageButton button;
  }
  
}