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
import android.widget.LinearLayout;
import android.widget.TextView;
import hu.rgai.android.intent.beens.FullMessageParc;
import hu.rgai.android.intent.beens.MessageAtomParc;
import hu.rgai.android.intent.beens.MessageListElementParc;
import hu.rgai.android.intent.beens.account.AccountAndr;
import hu.rgai.android.test.R;
import hu.uszeged.inf.rgai.messagelog.beans.fullmessage.MessageAtom;

public class ThreadViewAdapter extends ArrayAdapter<MessageAtom> {

	private TextView countryName;
	private List<MessageAtom> messages = new ArrayList<MessageAtom>();
	private LinearLayout wrapper;
  private AccountAndr account = null;

	@Override
	public void add(MessageAtom object) {
		messages.add(object);
		super.add(object);
	}

	public ThreadViewAdapter(Context context, int textViewResourceId, AccountAndr account) {
		super(context, textViewResourceId);
    this.account = account;
	}

  @Override
	public int getCount() {
		return this.messages.size();
	}

  @Override
	public MessageAtom getItem(int index) {
		return this.messages.get(index);
	}

  @Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;
		if (row == null) {
			LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			row = inflater.inflate(R.layout.threadview_list_item, parent, false);
		}

		wrapper = (LinearLayout) row.findViewById(R.id.wrapper);

		MessageAtom coment = getItem(position);

		countryName = (TextView) row.findViewById(R.id.comment);

		countryName.setText(coment.getContent());
    
		countryName.setBackgroundResource(coment.isIsMe() ? R.drawable.bubble_yellow : R.drawable.bubble_green);
		wrapper.setGravity(coment.isIsMe() ? Gravity.LEFT : Gravity.RIGHT);

		return row;
	}

	public Bitmap decodeToBitmap(byte[] decodedByte) {
		return BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.length);
	}

}