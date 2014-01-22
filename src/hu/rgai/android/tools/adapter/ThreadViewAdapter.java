package hu.rgai.android.tools.adapter;

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
import hu.rgai.android.intent.beens.MessageAtomParc;
import hu.rgai.android.intent.beens.account.AccountAndr;
import hu.rgai.android.store.StoreHandler;
import hu.rgai.android.test.R;
import hu.rgai.android.tools.ProfilePhotoProvider;

public class ThreadViewAdapter extends ArrayAdapter<MessageAtomParc> {

	private TextView countryName;
	private List<MessageAtomParc> messages = new ArrayList<MessageAtomParc>();
	private LinearLayout wrapper;
  private AccountAndr account = null;
  private Context context;

	@Override
	public void add(MessageAtomParc object) {
		messages.add(object);
		super.add(object);
	}

	public ThreadViewAdapter(Context context, int textViewResourceId, AccountAndr account) {
		super(context, textViewResourceId);
    this.account = account;
    this.context = context;
	}

  @Override
	public int getCount() {
		return this.messages.size();
	}

  @Override
	public MessageAtomParc getItem(int index) {
		return this.messages.get(index);
	}

  @Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;
    MessageAtomParc coment = getItem(position);
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
    
		countryName = (TextView) row.findViewById(R.id.comment);

		countryName.setText(coment.getContent());
    
		countryName.setBackgroundResource(!coment.isIsMe() ? R.drawable.bubble_yellow : R.drawable.bubble_green);
    RelativeLayout.LayoutParams wrapperParams = (RelativeLayout.LayoutParams)wrapper.getLayoutParams();
    if (coment.isIsMe()) {
      wrapperParams.addRule(RelativeLayout.ALIGN_RIGHT, R.id.wrapper);
    } else {
      wrapperParams.addRule(RelativeLayout.ALIGN_LEFT, R.id.wrapper);
    }
    wrapper.setLayoutParams(wrapperParams);
		wrapper.setGravity(!coment.isIsMe() ? Gravity.LEFT : Gravity.RIGHT);
    
    ImageView iv = (ImageView)row.findViewById(R.id.img);
    Bitmap img = null;
    if (coment.isIsMe()) {
      img = StoreHandler.getUserFbImage(context);
    } else {
      img = ProfilePhotoProvider.getImageToUser(context, coment.getFrom().getContactId());
    }
    if (img == null) {
      img = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_contact_picture);
    }
    iv.setImageBitmap(img);

    if (position - 1 >= 0) {
      MessageAtomParc prevMsg = getItem(position - 1);
      if (coment.getDate().getTime() - prevMsg.getDate().getTime() < 60000) {
        row.findViewById(R.id.hr).setVisibility(View.GONE);
      } else {
        row.findViewById(R.id.hr).setVisibility(View.VISIBLE);
        TextView ts = (TextView)row.findViewById(R.id.timestamp);
        ts.setText(coment.getDate().toString());
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