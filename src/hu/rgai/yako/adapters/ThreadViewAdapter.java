package hu.rgai.yako.adapters;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import hu.rgai.yako.beens.FullSimpleMessage;
import hu.rgai.yako.beens.Account;
import hu.rgai.yako.store.StoreHandler;
import hu.rgai.android.test.R;
import hu.rgai.yako.tools.ProfilePhotoProvider;
import hu.rgai.yako.tools.Utils;

public class ThreadViewAdapter extends ArrayAdapter<FullSimpleMessage> {

//	private TextView msgBubble;
	private List<FullSimpleMessage> messages = new ArrayList<FullSimpleMessage>();
	private LinearLayout wrapper;
  private Context context;

	@Override
	public void add(FullSimpleMessage object) {
		messages.add(object);
		super.add(object);
	}

	public ThreadViewAdapter(Context context, int textViewResourceId) {
		super(context, textViewResourceId);
    this.context = context;
	}

  @Override
	public int getCount() {
		return this.messages.size();
	}

  @Override
	public FullSimpleMessage getItem(int index) {
		return this.messages.get(index);
	}

  @Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;
    FullSimpleMessage coment = getItem(position);
    LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if (row == null) {
      if (coment.isIsMe()) {
        row = inflater.inflate(R.layout.threadview_list_item, parent, false);
      } else {
        // TODO: display different view when showing partner's message
        row = inflater.inflate(R.layout.threadview_list_item, parent, false);
      }
		}
    
		wrapper = (LinearLayout) row.findViewById(R.id.content_wrap);

		
//    Bitmap img = ProfilePhotoProvider.getImageToUser(context, account.getAccountType(), coment.getFrom().getId());
//    Bitmap meImg = StoreHandler.getUserFbImage(context);
    
		TextView msgBubble = (TextView) row.findViewById(R.id.comment);

		msgBubble.setText(coment.getContent().getContent().toString());
    
		msgBubble.setBackgroundResource(!coment.isIsMe() ? R.drawable.bubble_yellow : R.drawable.bubble_green);
    
//    RelativeLayout.LayoutParams wrapperParams = (RelativeLayout.LayoutParams)wrapper.getLayoutParams();
//    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
//              LinearLayout.LayoutParams.WRAP_CONTENT,
//              LinearLayout.LayoutParams.WRAP_CONTENT);
//    if (coment.isIsMe()) {
//      params.setMargins(75, 10, 10, 10);
//      wrapperParams.addRule(RelativeLayout.ALIGN_RIGHT, R.id.wrapper);
//    } else {
//      params.setMargins(10, 30, 75, 10);
//      wrapperParams.addRule(RelativeLayout.ALIGN_LEFT, R.id.wrapper);
//    }
//    msgBubble.setLayoutParams(params);
    
//    wrapper.setLayoutParams(wrapperParams);
		wrapper.setGravity(!coment.isIsMe() ? Gravity.LEFT : Gravity.RIGHT);
    
    ImageView iv = (ImageView)row.findViewById(R.id.img);
//    Bitmap img;
//    if (coment.isIsMe()) {
//      img = StoreHandler.getUserFbImage(context);
//    } else {
//      img = ProfilePhotoProvider.getImageToUser(context, coment.getFrom().getContactId());
//    }
//    if (img == null) {
//      img = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_contact_picture);
//    }
//    iv.setImageBitmap(img);

    FullSimpleMessage prevMsg = null;
    if (position - 1 >= 0) {
      prevMsg = getItem(position - 1);
    }
    boolean smallTopPadding;
    if (coment.isIsMe()) {
      iv.setVisibility(View.GONE);
      smallTopPadding = true;
    } else {
//      iv.setVisibility(View.VISIBLE);
      if (prevMsg != null && !prevMsg.isIsMe()) {
        iv.setVisibility(View.GONE);
        smallTopPadding = true;
      } else {
        iv.setVisibility(View.VISIBLE);
        Bitmap img = ProfilePhotoProvider.getImageToUser(context, coment.getFrom().getContactId()).getBitmap();
        if (img == null) {
          img = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_contact_picture);
        }
        iv.setImageBitmap(img);
        smallTopPadding = false;
      }
    }
    
    RelativeLayout.LayoutParams wrapperParams = (RelativeLayout.LayoutParams)wrapper.getLayoutParams();
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
              LinearLayout.LayoutParams.WRAP_CONTENT,
              LinearLayout.LayoutParams.WRAP_CONTENT);
    if (coment.isIsMe()) {
      params.setMargins(75, smallTopPadding ? 10 : 30, 10, 5);
      wrapperParams.addRule(RelativeLayout.ALIGN_RIGHT, R.id.wrapper);
    } else {
      params.setMargins(10, smallTopPadding ? 0 : 30, 75, 5);
      wrapperParams.addRule(RelativeLayout.ALIGN_LEFT, R.id.wrapper);
    }
    msgBubble.setLayoutParams(params);
    wrapper.setLayoutParams(wrapperParams);
    
    if (prevMsg != null) {
      // dealing with timestamps
      if (coment.getDate().getTime() - prevMsg.getDate().getTime() < 60000) {
        row.findViewById(R.id.hr).setVisibility(View.GONE);
      } else {
        row.findViewById(R.id.hr).setVisibility(View.VISIBLE);
        TextView ts = (TextView)row.findViewById(R.id.timestamp);
        ts.setText(Utils.getSimplifiedTime(coment.getDate()));
      }
    }
    
    
//    TextView ts = (TextView)row.findViewById(R.id.timestamp);
//    ts.setText("kecske");
    
		return row;
	}

	public Bitmap decodeToBitmap(byte[] decodedByte) {
		return BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.length);
	}

}