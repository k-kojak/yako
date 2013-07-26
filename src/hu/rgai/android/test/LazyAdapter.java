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

    Map<String, Object> email = data.get(position);

    // Setting all values in listview
    subject.setText((String)email.get("subject"));
    from.setText((String)email.get("from"));
    if (!Boolean.valueOf((String)email.get("seen"))) {
      subject.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
      from.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
      icon.setImageResource(R.drawable.gmail_icon);
    } else {
      subject.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL);
      from.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL);
      icon.setImageResource(R.drawable.gmail_icon_seen);
    }
    date.setText((String)email.get("date"));
//        imageLoader.DisplayImage(song.get(CustomizedListView.KEY_THUMB_URL), thumb_image);
    return vi;
  }
}