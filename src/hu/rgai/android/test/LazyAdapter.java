package hu.rgai.android.test;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import hu.rgai.android.config.Settings;
import hu.rgai.android.intent.beens.FullSimpleMessageParc;
import hu.rgai.android.intent.beens.MessageListElementParc;
import hu.rgai.android.services.MainService;
import hu.rgai.android.tools.ProfilePhotoProvider;
import hu.uszeged.inf.rgai.messagelog.MessageProvider;
import hu.uszeged.inf.rgai.messagelog.beans.account.EmailAccount;
import java.util.List;

public class LazyAdapter extends BaseAdapter {

  private MainActivity activity;
//  private List<MessageListElementParc> data;
  private static LayoutInflater inflater = null;
//    public ImageLoader imageLoader;

  public LazyAdapter(MainActivity a) {
    activity = a;
//    data = d;
    inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//        imageLoader=new ImageLoader(activity.getApplicationContext());
  }

  public int getCount() {
    return MainService.messages.size();
  }

  public Object getItem(int position) {
    int i = 0;
    for (MessageListElementParc mlep : MainService.messages) {
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

  public View getView(int position, View convertView, ViewGroup parent) {
    View vi = convertView;
    if (convertView == null) {
      vi = inflater.inflate(R.layout.list_row, null);
    }

    TextView subject = (TextView) vi.findViewById(R.id.subject);
    TextView from = (TextView) vi.findViewById(R.id.from);
    TextView date = (TextView) vi.findViewById(R.id.date);
    ImageView icon = (ImageView) vi.findViewById(R.id.list_image);
    ImageView msgType = (ImageView) vi.findViewById(R.id.list_acc_type);
    ImageView attachment = (ImageView) vi.findViewById(R.id.attachment);

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
      attachment.setVisibility(View.VISIBLE);
    } else {
      attachment.setVisibility(View.GONE);
    }
    
    
    // Setting all values in listview
    // TODO: itt null pointer exceptionnel elszallunk olykor
    String subjectText = " ";
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
    
    subject.setText(subjectText);
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
    from.setText(fromText);
    if (!message.isSeen()) {
      subject.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
      from.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
    } else {
      subject.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL);
      from.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL);
    }
    Bitmap img;
    if (message.getFrom() != null) {
      img = ProfilePhotoProvider.getImageToUser(activity, message.getFrom().getContactId());
    } else {
      img = BitmapFactory.decodeResource(activity.getResources(), R.drawable.group_chat);
    }
    icon.setImageBitmap(img);
    if (message.getMessageType().equals(MessageProvider.Type.FACEBOOK)) {
      msgType.setImageResource(R.drawable.ic_fb_messenger);
    } else if (message.getMessageType().equals(MessageProvider.Type.SMS)) {
//      msgType.setImageResource(R.drawable.ic_phone);
      msgType.setImageResource(R.drawable.ic_sms3);
    } else if (message.getAccount().getAccountType().equals(MessageProvider.Type.EMAIL)) {
      int resource = getSimpleMailIcon((EmailAccount)message.getAccount());
      msgType.setImageResource(resource);
    } else if (message.getAccount().getAccountType().equals(MessageProvider.Type.GMAIL)) {
      msgType.setImageResource(R.drawable.ic_gmail);
//      Log.d("rgai", "PUTTING TO LIST -> " + message.getFrom());
    }
//    icon.setImageBitmap(img);
    
    date.setText(message.getPrettyDate());
//        imageLoader.DisplayImage(song.get(CustomizedListView.KEY_THUMB_URL), thumb_image);
    return vi;
  }
  
  public static int getSimpleMailIcon(EmailAccount acc) {
    String dom = acc.getEmail().substring(acc.getEmail().indexOf("@") + 1);
    dom = dom.substring(0, dom.indexOf("."));
    
    return Settings.EmailUtils.getResourceIdToEmailDomain(dom);
  }
  
//  private int getSimpleMailIcon(EmailAccount acc) {
//    String dom = acc.getEmail().substring(acc.getEmail().indexOf("@") + 1);
//    if (dom.contains("yahoo")) {
//      return R.drawable.ic_yahoo;
//    } else if (dom.contains("vipmail")) {
//      return R.drawable.ic_indamail;
//    } else if (dom.contains("citromail")) {
//      return R.drawable.ic_citromail;
//    } else if (dom.contains("outlook")) {
//      return R.drawable.ic_hotmail;
//    }
//    return R.drawable.ic_email;
//  }
}