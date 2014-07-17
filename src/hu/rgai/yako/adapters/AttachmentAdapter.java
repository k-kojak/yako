package hu.rgai.yako.adapters;

import android.content.Context;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import hu.rgai.android.test.R;
import hu.rgai.yako.beens.Account;
import hu.rgai.yako.beens.Attachment;
import hu.rgai.yako.beens.EmailAccount;
import hu.rgai.yako.tools.Utils;
import hu.rgai.yako.workers.AttachmentDownloader;

import java.util.HashMap;
import java.util.List;

public class AttachmentAdapter extends BaseAdapter {

//  private MainActivity activity;
  private static LayoutInflater inflater = null;
//  private AccountAndr filterAcc = null;
  private final Context mContext;
  private final List<Attachment> mAttachments;
  private final EmailAccount mAccount;
  private final String mMessageId;
  private static final HashMap<Long, AttachmentDownloader> downloaders = new HashMap<Long, AttachmentDownloader>();


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

    if (downloaders.containsKey(attachment.getRawId())) {
      AttachmentDownloader ad = downloaders.get(attachment.getRawId());
      if (ad.isRunning()) {
        ad.setProgressBarView(holder.progressBar);
        ad.setFileSizeView(holder.fileSize);
      }
    }

//    if (attachment.isInProgress() && !attachment.isDownloaded() && attachment.getAttachmentDownloader() != null) {
//      attachment.getAttachmentDownloader().setProgressBarView(holder.progressBar);
//      attachment.getAttachmentDownloader().setFileSizeView(holder.fileSize);
//    }
    holder.button.setOnClickListener(new View.OnClickListener() {
      public void onClick(View arg0) {
        if (!downloaders.containsKey(attachment.getRawId())) {
          if (!attachment.isDownloaded()) {
            AttachmentDownloader downloader = new AttachmentDownloader(attachment, progBarHandler, mAccount,
                    mMessageId, holder.progressBar, holder.fileSize, mContext);
            downloaders.put(attachment.getRawId(), downloader);
            new Thread(downloader).start();
          } else if (attachment.isDownloaded()) {
            Toast.makeText(mContext, "The file is in the Download folder.", Toast.LENGTH_LONG).show();
          }
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