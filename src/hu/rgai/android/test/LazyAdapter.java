package hu.rgai.android.test;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import hu.uszeged.inf.rgai.messagelog.MessageProvider;
import hu.uszeged.inf.rgai.messagelog.beans.account.Account;
import java.util.List;
import java.util.Map;

public class LazyAdapter extends BaseAdapter {

  private MainActivity activity;
  private List<Map<String, Object>> data;
  private static LayoutInflater inflater = null;
//    public ImageLoader imageLoader;

  public LazyAdapter(MainActivity a, List<Map<String, Object>> d) {
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

    Map<String, Object> message = data.get(position);

    // Setting all values in listview
    subject.setText((String)message.get("subject"));
    from.setText((String)message.get("from"));
    if (!Boolean.valueOf((String)message.get("seen"))) {
      subject.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
      from.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
    } else {
      subject.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL);
      from.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL);
    }
    Account a = (Account)message.get("account");
    if (a.getAccountType().equals(MessageProvider.Type.FACEBOOK)) {
      icon.setImageResource(R.drawable.fb);
    } else {
      icon.setImageResource(R.drawable.gmail_icon);
    }
    date.setText((String)message.get("date"));
//        imageLoader.DisplayImage(song.get(CustomizedListView.KEY_THUMB_URL), thumb_image);
    return vi;
  }
}