
package hu.rgai.android.tools.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import hu.rgai.android.intent.beens.account.AccountAndr;
import hu.rgai.android.test.MainActivity;
import hu.rgai.android.test.R;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class AccountListAdapter extends BaseAdapter {

//  private Context context;
  private List<AccountAndr> accounts;
  private static LayoutInflater inflater = null;
  
  public AccountListAdapter(Context context, List<AccountAndr> accounts ) {
//    this.context = context;
    this.accounts = accounts;
    inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
  }
  
  public int getCount() {
    return accounts.size();
  }

  public Object getItem(int position) {
    return accounts.get(position);
  }

  public long getItemId(int position) {
    return position;
  }

  public View getView(int position, View view, ViewGroup parent) {
    if (view == null) {
      view = inflater.inflate(R.layout.account_list_item, null);
    }

    TextView name = (TextView) view.findViewById(R.id.name);
    TextView type = (TextView) view.findViewById(R.id.type);
    ImageView icon = (ImageView) view.findViewById(R.id.img);

    AccountAndr account = accounts.get(position);

    // Setting all values in listview
    name.setText((String)account.getDisplayName());
    type.setText((String)account.getAccountType().toString());
    icon.setImageResource(R.drawable.gmail_icon);
    
    return view;
  }

}
