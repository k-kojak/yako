//package hu.rgai.android.test;
//
//
//import android.content.Context;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.ArrayAdapter;
//import android.widget.ImageView;
//import android.widget.TextView;
//import hu.rgai.android.intent.beens.EmailListElement;
//
//public class EmailListItemAdapter extends ArrayAdapter<String> {
//  private final Context context;
//  private final EmailListElement[] values;
//
//  public EmailListItemAdapter(Context context, EmailListElement[] values) {
//    super(context, R.layout.emaillist_rowlayout, values);
//    this.context = context;
//    this.values = values;
//  }
//
//  @Override
//  public View getView(int position, View convertView, ViewGroup parent) {
//    LayoutInflater inflater = (LayoutInflater) context
//        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//    View rowView = inflater.inflate(R.layout.emaillist_rowlayout, parent, false);
//    TextView subject = (TextView) rowView.findViewById(R.id.subject);
//    TextView from = (ImageView) rowView.findViewById(R.id.from);
//    subject.setText(values[position]);
//    // Change the icon for Windows and iPhone
//    String s = values[position];
//    if (s.startsWith("Windows7") || s.startsWith("iPhone")
//        || s.startsWith("Solaris")) {
//      imageView.setImageResource(R.drawable.no);
//    } else {
//      imageView.setImageResource(R.drawable.ok);
//    }
//
//    return rowView;
//  }
//} 