/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package hu.rgai.android.test.settings;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
//import com.actionbarsherlock.app.SherlockFragment;
import hu.rgai.android.intent.beens.account.FacebookAccountAndr;
import hu.rgai.android.test.R;
import hu.uszeged.inf.rgai.messagelog.MessageProvider;

/**
 *
 * @author Tamas Kojedzinszky
 * @deprecated 
 */
public class FacebookSettingFragment  implements SettingFragment, TextWatcher {

  private EditText email;
  private EditText pass;
  private Spinner messageAmount;
  
//  @Override
//  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//    
//    View v = inflater.inflate(R.layout.account_settings_facebook_layout, container, false);
//    
//    messageAmount = (Spinner)v.findViewById(R.id.initial_items_num);
//    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
//            R.array.initial_emails_num, android.R.layout.simple_spinner_item);
//    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//    // Apply the adapter to the spinner
//    messageAmount.setAdapter(adapter);
//    
//    email = (EditText)v.findViewById(R.id.email_address);
//    email.addTextChangedListener(this);
//    pass = (EditText)v.findViewById(R.id.password);
//    
//    
//    Bundle b = this.getArguments();
//    if (b != null) {
//      email.setText(b.getString("name"));
//      pass.setText(b.getString("pass"));
//      messageAmount.setSelection(AccountSettings.getSpinnerPosition(messageAmount.getAdapter(), b.getInt("num")));
//    }
//    
//    return v;
//  }
  
  public void onTextChanged(CharSequence text, int arg1, int arg2, int arg3) {
    AccountSettings.validateEmailField(email, text.toString());
  }

  public void afterTextChanged(Editable e) {}
  public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}

  public MessageProvider.Type getType() {
    return MessageProvider.Type.FACEBOOK;
  }
  
  public FacebookAccountAndr getAccount() {
    String m = email.getText().toString();
    String p = pass.getText().toString();
    int num = Integer.parseInt((String)messageAmount.getSelectedItem());
    return new FacebookAccountAndr(num, m, p);
  }
  
}
