package hu.rgai.android.test;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import hu.rgai.android.config.Settings;
import hu.rgai.android.intent.beens.MessageListElementParc;
import hu.rgai.android.tools.ProfilePhotoProvider;
import hu.uszeged.inf.rgai.messagelog.MessageProvider;
import java.util.List;

public class LazyAdapter extends BaseAdapter {

  private MainActivity activity;
  private List<MessageListElementParc> data;
  private static LayoutInflater inflater = null;
//    public ImageLoader imageLoader;

  public LazyAdapter(MainActivity a, List<MessageListElementParc> d) {
    activity = a;
    data = d;
    inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//        imageLoader=new ImageLoader(activity.getApplicationContext());
  }

  public int getCount() {
    return data.size();
  }

  public Object getItem(int position) {
    return data.get(position);
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

    MessageListElementParc message = data.get(position);

    // Setting all values in listview
    String subjectText = message.getTitle().replaceAll("\n", " ").replaceAll(" {2,}", " ");
    if (subjectText.length() > Settings.MAX_SNIPPET_LENGTH) {
      subjectText = subjectText.substring(0, Settings.MAX_SNIPPET_LENGTH) + "...";
    }
    if (message.getUnreadCount() > 0) {
      subjectText = "(" + message.getUnreadCount() + ") " + subjectText;
    }
    subject.setText(subjectText);
    from.setText(message.getFrom().getName());
    if (!message.isSeen()) {
      subject.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
      from.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
    } else {
      subject.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL);
      from.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL);
    }
    Bitmap img = ProfilePhotoProvider.getImageToUser(activity, message.getFrom().getContactId());
    icon.setImageBitmap(img);
    icon.setImageBitmap(img);
    
    date.setText(message.getFormattedDate());
//        imageLoader.DisplayImage(song.get(CustomizedListView.KEY_THUMB_URL), thumb_image);
    return vi;
  }
}