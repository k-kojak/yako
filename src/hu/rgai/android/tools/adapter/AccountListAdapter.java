//TODO: comparator az account list-nek es legyen treeset-ben
//ALL-ra kattintva a filter list-ben toltsuk be ujra az uzeneteket


package hu.rgai.android.tools.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import hu.rgai.android.beens.Account;
import hu.rgai.android.beens.EmailAccount;
import hu.rgai.android.config.Settings;
import hu.rgai.android.messageproviders.MessageProvider;
import hu.rgai.android.test.R;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class AccountListAdapter extends BaseAdapter {

  private final List<Account> accounts;
  private static LayoutInflater inflater = null;
  
  public AccountListAdapter(Context context, TreeSet<Account> accounts ) {
    this.accounts = new LinkedList<Account>(accounts);
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

    Account account = accounts.get(position);

    // Setting all values in listview
    name.setText((String)account.getDisplayName());
    type.setText((String)account.getAccountType().toString());
    if (account.getAccountType().equals(MessageProvider.Type.FACEBOOK)) {
      icon.setImageResource(R.drawable.fb);
    } else if (account.getAccountType().equals(MessageProvider.Type.GMAIL)) {
      icon.setImageResource(R.drawable.gmail_icon);
    } else if (account.getAccountType().equals(MessageProvider.Type.SMS)) {
      icon.setImageResource(R.drawable.ic_sms3);
    } else if (account.getAccountType().equals(MessageProvider.Type.EMAIL)) {
      EmailAccount eacc = (EmailAccount)account;
      String dom = eacc.getEmail().substring(eacc.getEmail().indexOf("@") + 1);
      int dotIndex = dom.indexOf(".");
      if (dotIndex == -1) {
        dom = "";
      } else {
        dom = dom.substring(0, dotIndex);
      }
      icon.setImageResource(Settings.EmailUtils.getResourceIdToEmailDomain(dom));
    }
    
    return view;
  }

}
