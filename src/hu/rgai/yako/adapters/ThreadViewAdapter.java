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

  public static final int MY_MESSAGE = 0;
  public static final int FRIEND_MESSAGE = 1;
  public static final int MESSAGE_TYPE_COUNT = 2;
	private List<FullSimpleMessage> messages = new ArrayList<FullSimpleMessage>();
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
  public void clear() {
    if (messages != null) {
      messages.clear();
    }
  }

  @Override
	public int getCount() {
		return this.messages.size();
	}

  @Override
	public FullSimpleMessage getItem(int index) {
		return this.messages.get(index);
	}
  
  public void removeItem(int index) {
    this.messages.remove(index);
  }
  
  @Override
  public int getViewTypeCount() {
   return MESSAGE_TYPE_COUNT;
  }
  
  @Override
  public int getItemViewType(int position) {
    FullSimpleMessage item = getItem(position);
    if (item.isIsMe()) {
      return MY_MESSAGE;
    } else {
      return FRIEND_MESSAGE; 
    } 
  }

  @Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;
    FullSimpleMessage coment = getItem(position);
    LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if (row == null) {
      if (coment.isIsMe()) {
        row = inflater.inflate(R.layout.threadview_list_item_me, parent, false);
      } else {
        row = inflater.inflate(R.layout.threadview_list_item_friend, parent, false);
      }
		}
    
		TextView msgBubble = (TextView) row.findViewById(R.id.comment);
    TextView time = (TextView) row.findViewById(R.id.timestamp);
    ImageView iv = (ImageView)row.findViewById(R.id.img);
    View padding = row.findViewById(R.id.padding);

		msgBubble.setText(coment.getContent().getContent().toString());
    time.setText(Utils.getSimplifiedTime(context, coment.getDate()));

    FullSimpleMessage prevMsg = null;
    if (position - 1 >= 0) {
      prevMsg = getItem(position - 1);
    }

    if (iv != null) {
      if (!coment.isIsMe()) {
        showImageOfUser(coment, msgBubble, iv);
      } else {
        iv.setVisibility(View.GONE);
      }
    }

    if (prevMsg == null || prevMsg.isIsMe() != coment.isIsMe()) {
      padding.setVisibility(View.VISIBLE);
    } else {
      padding.setVisibility(View.GONE);
    }

		return row;
	}
  
  private void showImageOfUser(FullSimpleMessage coment, TextView msgBubble, ImageView iv) {

    Bitmap img = ProfilePhotoProvider.getImageToUser(context, coment.getFrom()).getBitmap();
    if (img == null) {
      img = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_contact_picture);
    }
    iv.setImageBitmap(img);

    iv.setVisibility(View.VISIBLE);
  } 

	public Bitmap decodeToBitmap(byte[] decodedByte) {
		return BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.length);
	}

}