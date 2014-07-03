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
import hu.rgai.yako.beens.Account;
import hu.rgai.yako.beens.AsyncImageLoadProvider;
import hu.rgai.yako.beens.BitmapResult;
import hu.rgai.yako.beens.EmailAccount;
import hu.rgai.yako.beens.FullSimpleMessage;
import hu.rgai.yako.beens.MessageListElement;
import hu.rgai.yako.config.Settings;
import hu.rgai.yako.messageproviders.MessageProvider;
import hu.rgai.yako.sql.MessageListDAO;
import hu.rgai.yako.tools.ProfilePhotoProvider;
import hu.rgai.yako.view.extensions.AsyncImageView;

import java.util.*;

public class MainListAdapter extends CursorAdapter {



  private final Context mContext;
  private static LayoutInflater inflater = null;
  private TreeMap<Long, Account> mAccounts = null;



  public MainListAdapter(Context context, Cursor cursor, TreeMap<Long, Account> accounts) {
    super(context, cursor, false);
    mContext = context;
    inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    mAccounts = accounts;
  }

  // TODO: this should be a copy of the original array
//  public int getCount() {
//    // the plus 1 item is because of the "last updated" row
//    int messagesSize = YakoApp.getFilteredMessages(MainActivity.actSelectedFilter).size();
//    return messagesSize == 0 ? 0 : messagesSize + 1;
//  }
  
//  public Object getItem(int position) {
//    if (position == 0) {
//      return null;
//    } else {
//      int i = 0;
//      for (MessageListElement mlep : YakoApp.getFilteredMessages(MainActivity.actSelectedFilter)) {
//        if (i == position - 1) {
//          return mlep;
//        }
//        i++;
//      }
//      return null;
//    }
//  }

//  public long getItemId(int position) {
//    return position;
//  }

//  public void setListFilter(Account filterAccount) {
//    this.filterAcc = filterAccount;
//  }


  public void bindView(View view, Context context, Cursor cursor) {
    
//    if (position == 0) {
//      return getLastUpdatedRow(view, parent);
//    } else {
    
      ViewHolder holder = (ViewHolder)view.getTag();
//      if (view == null || view.getTag() == null) {
//        view = inflater.inflate(R.layout.list_row, null);
//        holder = new ViewHolder();
//
//        holder.subject = (TextView) view.findViewById(R.id.subject);
//        holder.from = (TextView) view.findViewById(R.id.from);
//        holder.date = (TextView) view.findViewById(R.id.date);
//        holder.icon = (AsyncImageView) view.findViewById(R.id.list_image);
//        holder.msgType = (ImageView) view.findViewById(R.id.list_acc_type);
//        holder.attachment = (ImageView) view.findViewById(R.id.attachment);
//        holder.accountName = (TextView) view.findViewById(R.id.account_name);
//        view.setTag(holder);
//      } else {
//        holder = (ViewHolder)view.getTag();
//      }


      MessageListElement message = MessageListDAO.cursorToMessageListElement(cursor, mAccounts);

      // dealing with attachment display
      boolean hasAttachment;
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
            return ProfilePhotoProvider.getImageToUser(mContext, id);
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
            return ProfilePhotoProvider.getGroupChatPhoto(mContext);
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
  }
  
}