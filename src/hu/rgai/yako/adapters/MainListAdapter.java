package hu.rgai.yako.adapters;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import hu.rgai.android.test.R;
import hu.rgai.yako.YakoApp;
import hu.rgai.yako.beens.*;
import hu.rgai.yako.config.Settings;
import hu.rgai.yako.messageproviders.MessageProvider;
import hu.rgai.yako.sql.MessageListDAO;
import hu.rgai.yako.tools.ProfilePhotoProvider;
import hu.rgai.yako.view.extensions.AsyncImageView;

import java.util.*;

public class MainListAdapter extends CursorAdapter {


  private final YakoApp mYakoApp;
  private final Context mContext;
  private static LayoutInflater inflater = null;
  private TreeMap<Long, Account> mAccounts = null;
  private boolean mIsZonesActivated = false;
  private int mImportantDrawable = R.drawable.ic_important;
  private GpsZone mClosestZone;


  public MainListAdapter(YakoApp yakoApp, Context context, int importantDrawable, boolean isZonesActivated,
                         GpsZone closestZone, Cursor cursor, TreeMap<Long, Account> accounts) {
    super(context, cursor, false);
    mYakoApp = yakoApp;
    mContext = context;
    mImportantDrawable = importantDrawable;
    mIsZonesActivated = isZonesActivated;
    mClosestZone = closestZone;
    mAccounts = accounts;
    inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
  }


  public void bindView(View view, Context context, Cursor cursor) {
    

      ViewHolder holder = (ViewHolder)view.getTag();

      MessageListElement message = MessageListDAO.cursorToMessageListElement(cursor, mAccounts);

      // dealing with attachment display
//      boolean hasAttachment;
//      if (message.getFullMessage() != null && message.getFullMessage() instanceof FullSimpleMessage) {
//        FullSimpleMessage fsmp = (FullSimpleMessage)message.getFullMessage();
//        if (fsmp.getAttachments() != null && !fsmp.getAttachments().isEmpty()) {
//          hasAttachment = true;
//        } else {
//          hasAttachment = false;
//        }
//      } else {
//        hasAttachment = false;
//      }
      if (message.getAttachmentCount() != 0) {
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

      if (message.getUnreadCount() > 0) {
        subjectText = "(" + message.getUnreadCount() + ") " + subjectText;
      }

      holder.subject.setText(subjectText);

      if (message.isImportant() && mIsZonesActivated && mClosestZone != null) {
        holder.important.setVisibility(View.VISIBLE);
        holder.important.setImageDrawable(mContext.getResources().getDrawable(mImportantDrawable));
      } else {
        holder.important.setVisibility(View.GONE);
      }



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
        holder.icon.setImageBitmap(mYakoApp, new AsyncImageLoadProvider() {
          public BitmapResult getBitmap(Person p) {
            return ProfilePhotoProvider.getImageToUser(mContext, p);
          }
          public boolean isBitmapLoaded(Person p) {
            return ProfilePhotoProvider.isImageToUserInCache(p);
          }
          public Bitmap getDefaultBitmap(Context c) {
            return ProfilePhotoProvider.getDefaultBitmap(c);
          }
        }, message.getFrom());
  //      img = ProfilePhotoProvider.getImageToUser(activity, message.getFrom().getContactId());
      } else {
        holder.icon.setImageBitmap(mYakoApp, new AsyncImageLoadProvider() {
          public BitmapResult getBitmap(Person p) {
            return ProfilePhotoProvider.getGroupChatPhoto(mContext);
          }

          public boolean isBitmapLoaded(Person p) {
            return ProfilePhotoProvider.isGroupChatPhotoLoaded();
          }
          public Bitmap getDefaultBitmap(Context c) {
            return ProfilePhotoProvider.getGroupChatPhoto(c).getBitmap();
          }
        }, null);
      }

    YakoApp.printAsyncTasks(true);


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
//      return view;
//    }
  }

  @Override
  public View newView(Context context, Cursor cursor, ViewGroup parent) {
    View view = inflater.inflate(R.layout.list_row, parent, false);

    ViewHolder holder = new ViewHolder();

    holder.subject = (TextView) view.findViewById(R.id.subject);
    holder.from = (TextView) view.findViewById(R.id.from);
    holder.date = (TextView) view.findViewById(R.id.date);
    holder.icon = (AsyncImageView) view.findViewById(R.id.list_image);
    holder.msgType = (ImageView) view.findViewById(R.id.list_acc_type);
    holder.attachment = (ImageView) view.findViewById(R.id.attachment);
    holder.accountName = (TextView) view.findViewById(R.id.account_name);
    holder.important = (ImageView) view.findViewById(R.id.important);

    view.setTag(holder);

    return view;
  }

//  @Override
//  public void bindView(View view, Context context, Cursor cursor) {
//
//  }

//  private View getLastUpdatedRow(View view, ViewGroup parent) {
//    view = inflater.inflate(R.layout.main_list_last_mod_row, parent, false);
//
//    String niceText = mContext.getResources().getString(R.string.last_full_update) + " ";
//
//    Date d = new Date();
//    Calendar c = new GregorianCalendar();
//    c.setTime(d);
//    c.set(Calendar.HOUR, 0);
//    c.set(Calendar.MINUTE, 0);
//    c.set(Calendar.SECOND, 0);
//    c.set(Calendar.MILLISECOND, 0);
//    Date today = c.getTime();
//
//    c.set(Calendar.DAY_OF_MONTH, 1);
//    c.set(Calendar.MONTH, 0);
//    Date thisYear = c.getTime();
//
//
//
//    if (YakoApp.lastFullMessageUpdate == null) {
//
//    } else if (YakoApp.lastFullMessageUpdate.before(thisYear)) {
//      sdf.applyPattern("yyyy/MM/dd");
//    } else if (YakoApp.lastFullMessageUpdate.after(today)) {
//      niceText += mContext.getResources().getString(R.string.today) + ", ";
//      sdf.applyPattern("HH:mm");
//    } else {
//      sdf.applyPattern("MMM d");
//    }
//
//    if (YakoApp.lastFullMessageUpdate == null) {
//      niceText += "Never";
//    } else {
//      niceText += sdf.format(YakoApp.lastFullMessageUpdate);
//    }
//
//    ((TextView)view.findViewById(R.id.last_update)).setText(niceText);
//    return view;
//  }


  public void setAccounts(TreeMap<Long, Account> mAccounts) {
    this.mAccounts = mAccounts;
  }

  public void setZoneActivity(boolean isZonesActivated) {
    mIsZonesActivated = isZonesActivated;
  }

  public void setImportantDrawable(int drawable) {
    mImportantDrawable = drawable;
  }

  public void setClosestZone(GpsZone closestZone) {
    mClosestZone = closestZone;
  }

  public static int getSimpleMailIcon(EmailAccount acc) {
    String dom = acc.getEmail().substring(acc.getEmail().indexOf("@") + 1);
    if (dom.contains(".")) {
      dom = dom.substring(0, dom.indexOf("."));
    }
    
    return Settings.EmailUtils.getResourceIdToEmailDomain(dom);
  }
  
  
  static class ViewHolder {
    TextView subject;
    TextView from;
    TextView date;
    AsyncImageView icon;
    ImageView msgType;
    ImageView attachment;
    TextView accountName;
    ImageView important;
  }
  
}