package hu.rgai.yako.adapters;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.*;
import hu.rgai.android.test.R;
import hu.rgai.yako.beens.Account;
import hu.rgai.yako.beens.Attachment;
import hu.rgai.yako.beens.EmailAccount;
import hu.rgai.yako.store.StoreHandler;
import hu.rgai.yako.tools.Utils;
import hu.rgai.yako.workers.AttachmentDownloader;

import java.io.File;
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
    
    view.setOnClickListener(new OnClickListener() {
    @Override
      public void onClick(View arg1) {
        if (attachment.isDownloaded()) {
          
          openAttachment(attachment);
          
        }
      }
    });
    

    
    return view;
  }

  private void openAttachment(final Attachment attachment) {
    
    File url = new File(StoreHandler.getEmailAttachmentDownloadLocation(), attachment.getFileName());
    Uri uri = Uri.fromFile(url);
    
    Intent intent = new Intent(Intent.ACTION_VIEW);
    // Check what kind of file you are trying to open, by comparing the url with extensions.
    // When the if condition is matched, plugin sets the correct intent (mime) type, 
    // so Android knew what application to use to open the file
    if (url.toString().contains(".doc") || url.toString().contains(".docx")) {
        // Word document
        intent.setDataAndType(uri, "application/msword");
    } else if(url.toString().contains(".pdf")) {
        // PDF file
        intent.setDataAndType(uri, "application/pdf");
    } else if(url.toString().contains(".ppt") || url.toString().contains(".pptx")) {
        // Powerpoint file
        intent.setDataAndType(uri, "application/vnd.ms-powerpoint");
    } else if(url.toString().contains(".xls") || url.toString().contains(".xlsx")) {
        // Excel file
        intent.setDataAndType(uri, "application/vnd.ms-excel");
    } else if(url.toString().contains(".zip") || url.toString().contains(".rar")) {
        // zip audio file
        intent.setDataAndType(uri, "application/zip");
    } else if(url.toString().contains(".rtf")) {
        // RTF file
        intent.setDataAndType(uri, "application/rtf");
    } else if(url.toString().contains(".wav") || url.toString().contains(".mp3")) {
        // WAV audio file
        intent.setDataAndType(uri, "audio/x-wav");
    } else if(url.toString().contains(".gif")) {
        // GIF file
        intent.setDataAndType(uri, "image/gif");
    } else if(url.toString().contains(".jpg") || url.toString().contains(".jpeg") || url.toString().contains(".png")) {
        // JPG file
        intent.setDataAndType(uri, "image/jpeg");
    } else if(url.toString().contains(".txt")) {
        // Text file
        intent.setDataAndType(uri, "text/plain");
    } else if(url.toString().contains(".3gp") || url.toString().contains(".mpg") || url.toString().contains(".mpeg") || url.toString().contains(".mpe") || url.toString().contains(".mp4") || url.toString().contains(".avi")) {
        // Video files
        intent.setDataAndType(uri, "video/*");
    } else {
        //if you want you can also define the intent type for any other file
        
        //additionally use else clause below, to manage other unknown extensions
        //in this case, Android will show all applications installed on the device
        //so you can choose which application to use
        intent.setDataAndType(uri, "*/*");
    }
    
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); 
    
    try {
      mContext.startActivity(intent);
    } catch (ActivityNotFoundException e) {
      Toast.makeText(mContext, "Can't open the file.", Toast.LENGTH_SHORT).show();
    }
    
  }

  static class ViewHolder {
    ProgressBar progressBar;
    TextView fileName;
    TextView fileSize;
    ImageButton button;
  }

}