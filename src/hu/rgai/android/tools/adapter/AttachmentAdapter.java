package hu.rgai.android.tools.adapter;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import hu.rgai.android.beens.Attachment;
import hu.rgai.android.intent.beens.account.Account;
import hu.rgai.android.store.StoreHandler;
import hu.rgai.android.test.R;
import hu.rgai.android.tools.Utils;
import hu.rgai.android.workers.AttachmentDownloader;
import hu.uszeged.inf.rgai.messagelog.SimpleEmailMessageProvider;
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
  

  public AttachmentAdapter(Context context, List<Attachment> attachments, Account account, String messageId) {
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
      holder.fileName = (TextView) view.findViewById(R.id.file_name);
      holder.fileSize = (TextView) view.findViewById(R.id.file_size);
      holder.button = (ImageButton) view.findViewById(R.id.download);
      view.setTag(holder);
    } else {
      holder = (ViewHolder)view.getTag();
    }
    
    final Attachment attachment = (Attachment)this.getItem(position);
    final Handler progBarHandler = new Handler();
    
    holder.progressBar.setProgress(attachment.getProgress());
    holder.fileName.setText(attachment.getFileName());
    holder.fileSize.setText(Utils.getPrettyFileSize(attachment.getSize()));
    if (attachment.isInProgress() && !attachment.isDownloaded() && attachment.getAttachmentDownloader() != null) {
      attachment.getAttachmentDownloader().setProgressBarView(holder.progressBar);
      attachment.getAttachmentDownloader().setFileSizeView(holder.fileSize);
    }
    holder.button.setOnClickListener(new View.OnClickListener() {
      public void onClick(View arg0) {
        if (!attachment.isInProgress() && !attachment.isDownloaded()) {
          AttachmentDownloader ad = new AttachmentDownloader(attachment, progBarHandler,
                  mAccount, mMessageId, holder.progressBar, holder.fileSize, mContext);
          attachment.setAttachmentDownloader(ad);
          new Thread(ad).start();
        } else if (attachment.isDownloaded()) {
          Toast.makeText(mContext, "The file is in the Download folder.", Toast.LENGTH_LONG).show();
        }
      }
    });
    
    
    return view;
  }
  
  static class ViewHolder {
    ProgressBar progressBar;
    TextView fileName;
    TextView fileSize;
    ImageButton button;
  }
  
}