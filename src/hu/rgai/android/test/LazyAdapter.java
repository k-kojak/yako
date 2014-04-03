package hu.rgai.android.test;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import hu.rgai.android.config.Settings;
import hu.rgai.android.intent.beens.FullSimpleMessageParc;
import hu.rgai.android.intent.beens.MessageListElementParc;
import hu.rgai.android.intent.beens.account.AccountAndr;
import hu.rgai.android.services.MainService;
import hu.rgai.android.tools.ProfilePhotoProvider;
import hu.uszeged.inf.rgai.messagelog.MessageProvider;
import hu.uszeged.inf.rgai.messagelog.beans.account.EmailAccount;

public class LazyAdapter extends BaseAdapter {

  private MainActivity activity;
  private static LayoutInflater inflater = null;
  private AccountAndr filterAcc = null;

  public LazyAdapter(MainActivity a) {
    activity = a;
    inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
  }

  public int getCount() {
    return MainService.getFilteredMessages(filterAcc).size();
  }

  public Object getItem(int position) {
    int i = 0;
    for (MessageListElementParc mlep : MainService.getFilteredMessages(filterAcc)) {
      if (i == position) {
        return mlep;
      }
      i++;
    }
    return null;
  }

  public long getItemId(int position) {
    return position;
  }

  public void setListFilter(AccountAndr filterAccount) {
    this.filterAcc = filterAccount;
  }
  
  public View getView(int position, View view, ViewGroup parent) {
    
    ViewHolder holder;
    if (view == null) {
      view = inflater.inflate(R.layout.list_row, null);
      holder = new ViewHolder();
      
      holder.subject = (TextView) view.findViewById(R.id.subject);
      holder.from = (TextView) view.findViewById(R.id.from);
      holder.date = (TextView) view.findViewById(R.id.date);
      holder.icon = (ImageView) view.findViewById(R.id.list_image);
      holder.msgType = (ImageView) view.findViewById(R.id.list_acc_type);
      holder.attachment = (ImageView) view.findViewById(R.id.attachment);
      view.setTag(holder);
    } else {
      holder = (ViewHolder)view.getTag();
    }
    

    MessageListElementParc message = (MessageListElementParc)this.getItem(position);
    
    // dealing with attachment display
    boolean hasAttachment = false;
    if (message.getFullMessage() != null && message.getFullMessage() instanceof FullSimpleMessageParc) {
      FullSimpleMessageParc fsmp = (FullSimpleMessageParc)message.getFullMessage();
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
    
    
    
    Bitmap img;
    if (message.getFrom() != null) {
      img = ProfilePhotoProvider.getImageToUser(activity, message.getFrom().getContactId());
    } else {
      img = ProfilePhotoProvider.getGroupChatPhoto(activity);
    }
    holder.icon.setImageBitmap(img);
    
    
    
    
    
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
    
    
    
    holder.date.setText(message.getPrettyDate());
    return view;
  }
  
  public static int getSimpleMailIcon(EmailAccount acc) {
    String dom = acc.getEmail().substring(acc.getEmail().indexOf("@") + 1);
    if (dom.indexOf(".") != -1) {
      dom = dom.substring(0, dom.indexOf("."));
    }
    
    return Settings.EmailUtils.getResourceIdToEmailDomain(dom);
  }
  
  static class ViewHolder {
    TextView subject;
    TextView from;
    TextView date;
    ImageView icon;
    ImageView msgType;
    ImageView attachment;
  }
  
}