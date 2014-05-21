package hu.rgai.android.test;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import hu.rgai.android.beens.Account;
import hu.rgai.android.beens.BitmapResult;
import hu.rgai.android.beens.EmailAccount;
import hu.rgai.android.beens.FullSimpleMessage;
import hu.rgai.android.beens.MessageListElement;
import hu.rgai.android.config.Settings;
import hu.rgai.android.messageproviders.MessageProvider;
import hu.rgai.android.services.MainService;
import hu.rgai.android.tools.ProfilePhotoProvider;
import hu.rgai.android.tools.Utils;
import hu.rgai.android.tools.view.AsyncImageLoadProvider;
import hu.rgai.android.tools.view.AsyncImageView;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class LazyAdapter extends BaseAdapter {

  private MainActivity activity;
  private static LayoutInflater inflater = null;
  private Account filterAcc = null;
  private final SimpleDateFormat sdf = new SimpleDateFormat();

  public LazyAdapter(MainActivity a) {
    activity = a;
    inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
  }

  public int getCount() {
    // the plus 1 item is because of the "last updated" row
    return MainService.getFilteredMessages(filterAcc).size() + 1;
  }
  
  public Object getItem(int position) {
    if (position == 0) {
      return null;
    } else {
      int i = 0;
      for (MessageListElement mlep : MainService.getFilteredMessages(filterAcc)) {
        if (i == position - 1) {
          return mlep;
        }
        i++;
      }
      return null;
    }
  }

  public long getItemId(int position) {
    return position;
  }

  public void setListFilter(Account filterAccount) {
    this.filterAcc = filterAccount;
  }
  
  public View getView(int position, View view, ViewGroup parent) {
    
    if (position == 0) {
      return getLastUpdatedRow(view, parent);
    } else {
    
      ViewHolder holder;
      if (view == null || view.getTag() == null) {
        view = inflater.inflate(R.layout.list_row, null);
        holder = new ViewHolder();

        holder.subject = (TextView) view.findViewById(R.id.subject);
        holder.from = (TextView) view.findViewById(R.id.from);
        holder.date = (TextView) view.findViewById(R.id.date);
        holder.icon = (AsyncImageView) view.findViewById(R.id.list_image);
        holder.msgType = (ImageView) view.findViewById(R.id.list_acc_type);
        holder.attachment = (ImageView) view.findViewById(R.id.attachment);
        holder.accountName = (TextView) view.findViewById(R.id.account_name);
        view.setTag(holder);
      } else {
        holder = (ViewHolder)view.getTag();
      }


      final MessageListElement message = (MessageListElement)getItem(position);

      // dealing with attachment display
      boolean hasAttachment = false;
      if (message.getFullMessage() != null && message.getFullMessage() instanceof FullSimpleMessage) {
        FullSimpleMessage fsmp = (FullSimpleMessage)message.getFullMessage();
        if (fsmp.getAttachments() != null && !fsmp.getAttachments().isEmpty()) {
          hasAttachment = true;
        } else {
          hasAttachment = false;
        }
      } else {
        hasAttachment = false;
      }
      if (hasAttachment) {
        holder.attachment.setVisibility(View.VISIBLE);
      } else {
        holder.attachment.setVisibility(View.GONE);
      }


      // Setting all values in listview
      // TODO: itt null pointer exceptionnel elszallunk olykor
      String subjectText = "?";
      if (message.getTitle() == null) {
        if (message.getSubTitle() != null) {
          subjectText = message.getSubTitle().replaceAll("\n", " ").replaceAll(" {2,}", " ");
        }
      } else {
        subjectText = message.getTitle().replaceAll("\n", " ").replaceAll(" {2,}", " ");
      }

      if (subjectText.length() > Settings.MAX_SNIPPET_LENGTH) {
        subjectText = subjectText.substring(0, Settings.MAX_SNIPPET_LENGTH) + "...";
      }
      if (message.getUnreadCount() > 0) {
        subjectText = "(" + message.getUnreadCount() + ") " + subjectText;
      }

      holder.subject.setText(subjectText);





      String fromText = "";
      if (message.getFrom() == null) {
        if (message.getRecipientsList() != null) {
          for (int i = 0; i < message.getRecipientsList().size(); i++) {
            if (i > 0) {
              fromText += ", ";
            }
            fromText += message.getRecipientsList().get(i).getName();
          }
        }
      } else {
        fromText = message.getFrom().getName();
      }
      holder.from.setText(fromText);



      if (!message.isSeen()) {
        holder.subject.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
        holder.from.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
      } else {
        holder.subject.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL);
        holder.from.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL);
      }



      if (message.getFrom() != null) {
        holder.icon.setImageBitmap(new AsyncImageLoadProvider() {
          public BitmapResult getBitmap(long id) {
            return ProfilePhotoProvider.getImageToUser(activity, id);
          }
          public boolean isBitmapLoaded(long id) {
            return ProfilePhotoProvider.isImageToUserInCache(id);
          }
          public Bitmap getDefaultBitmap(Context c) {
            return ProfilePhotoProvider.getDefaultBitmap(c);
          }
        }, message.getFrom().getContactId());
  //      img = ProfilePhotoProvider.getImageToUser(activity, message.getFrom().getContactId());
      } else {
        holder.icon.setImageBitmap(new AsyncImageLoadProvider() {
          public BitmapResult getBitmap(long id) {
            return ProfilePhotoProvider.getGroupChatPhoto(activity);
          }

          public boolean isBitmapLoaded(long id) {
            return ProfilePhotoProvider.isGroupChatPhotoLoaded();
          }
          public Bitmap getDefaultBitmap(Context c) {
            return ProfilePhotoProvider.getGroupChatPhoto(c).getBitmap();
          }
        }, 0);
      }




      if (message.getMessageType().equals(MessageProvider.Type.FACEBOOK)) {
        holder.msgType.setImageResource(R.drawable.ic_fb_messenger);
      } else if (message.getMessageType().equals(MessageProvider.Type.SMS)) {
        holder.msgType.setImageResource(R.drawable.ic_sms3);
      } else if (message.getAccount().getAccountType().equals(MessageProvider.Type.EMAIL)) {
        int resource = getSimpleMailIcon((EmailAccount)message.getAccount());
        holder.msgType.setImageResource(resource);
      } else if (message.getAccount().getAccountType().equals(MessageProvider.Type.GMAIL)) {
        holder.msgType.setImageResource(R.drawable.ic_gmail);
      }
      
      
      
      if (message.getAccount().isUnique()) {
        holder.accountName.setVisibility(View.GONE);
      } else {
        holder.accountName.setVisibility(View.VISIBLE);
        holder.accountName.setText(message.getAccount().getDisplayName());
      }
      

      holder.date.setText(message.getPrettyDate());
      return view;
    }
  }
  
  private View getLastUpdatedRow(View view, ViewGroup parent) {
    view = inflater.inflate(R.layout.main_list_last_mod_row, null);
    
    String niceText = activity.getResources().getString(R.string.last_full_update) + " ";
    
    Date d = new Date();
    Calendar c = new GregorianCalendar();
    c.setTime(d);
    c.set(Calendar.HOUR, 0);
    c.set(Calendar.MINUTE, 0);
    c.set(Calendar.SECOND, 0);
    c.set(Calendar.MILLISECOND, 0);
    Date today = c.getTime();
    
    c.set(Calendar.DAY_OF_MONTH, 1);
    c.set(Calendar.MONTH, 0);
    Date thisYear = c.getTime();
    
    
    if (MainService.last_message_update.before(thisYear)) {
      sdf.applyPattern("yyyy/MM/dd");
    } else if (MainService.last_message_update.after(today)) {
      niceText += activity.getResources().getString(R.string.today) + ", ";
      sdf.applyPattern("HH:mm");
    } else {
      sdf.applyPattern("MMM d");
    }
    
    niceText += sdf.format(MainService.last_message_update);
    
    ((TextView)view.findViewById(R.id.last_update)).setText(niceText);
    return view;
  }
  
  public static int getSimpleMailIcon(EmailAccount acc) {
    String dom = acc.getEmail().substring(acc.getEmail().indexOf("@") + 1);
    if (dom.indexOf(".") != -1) {
      dom = dom.substring(0, dom.indexOf("."));
    }
    
    return Settings.EmailUtils.getResourceIdToEmailDomain(dom);
  }
  
//  private class ImageLoaderWorker extends AsyncTask<Long, Void, Bitmap> {
//
//    private ImageView mImageView = null;
//    private Context mContext;
//
//    public ImageLoaderWorker(ImageView imageView, Context context) {
//      this.mImageView = imageView;
//      mContext = context;
//    }
//    
//    @Override
//    protected Bitmap doInBackground(Long... arg0) {
//      return ProfilePhotoProvider.getImageToUser(mContext, NO_SELECTION);
//    }
//    
//    @Override
//    protected void onPostExecute(Bitmap bitmap) {
//      mImageView.setImageBitmap(bitmap);
//    }
//    
//  }
  
  static class ViewHolder {
    TextView subject;
    TextView from;
    TextView date;
    AsyncImageView icon;
    ImageView msgType;
    ImageView attachment;
    TextView accountName;
  }
  
}