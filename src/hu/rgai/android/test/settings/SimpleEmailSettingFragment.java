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
import com.actionbarsherlock.app.SherlockFragment;
import hu.rgai.android.intent.beens.account.AccountAndr;
import hu.rgai.android.intent.beens.account.EmailAccountAndr;
import hu.rgai.android.test.R;
import hu.uszeged.inf.rgai.messagelog.MessageProvider;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class SimpleEmailSettingFragment extends SherlockFragment implements SettingFragment, TextWatcher {

  private EditText email;
  private EditText pass;
  private EditText imap;
  private EditText smtp;
  private Spinner securityType;
  private Spinner messageAmount;
  private Map<String, String> domainMap;
  
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    
    domainMap = new HashMap<String, String>();
    domainMap.put("vipmail.hu", "indamail.hu");
    domainMap.put("csinibaba.hu", "indamail.hu");
    domainMap.put("totalcar.hu", "indamail.hu");
    domainMap.put("index.hu", "indamail.hu");
    domainMap.put("velvet.hu", "indamail.hu");
    domainMap.put("torzsasztal.hu", "indamail.hu");
    domainMap.put("lamer.hu", "indamail.hu");
    
    View v = inflater.inflate(R.layout.account_settings_simple_mail_layout, container, false);
    
    messageAmount = (Spinner)v.findViewById(R.id.initial_emails_num);
    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
            R.array.initial_emails_num, android.R.layout.simple_spinner_item);
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    messageAmount.setAdapter(adapter);
    
    securityType = (Spinner)v.findViewById(R.id.security_type);
    adapter = ArrayAdapter.createFromResource(getActivity(),
            R.array.security_types, android.R.layout.simple_spinner_item);
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    // Apply the adapter to the spinner
    securityType.setAdapter(adapter);
    
    email = (EditText)v.findViewById(R.id.email_address);
    pass = (EditText)v.findViewById(R.id.password);
    imap = (EditText)v.findViewById(R.id.imap_server);
    smtp = (EditText)v.findViewById(R.id.smtp_server);
    
    email.addTextChangedListener(this);
    
    imap.addTextChangedListener(new TextWatcher() {
      public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
      public void afterTextChanged(Editable arg0) {}

      public void onTextChanged(CharSequence text, int arg1, int arg2, int arg3) {
        AccountSettings.validateUriField(imap, text.toString());
      }
    });
    smtp.addTextChangedListener(new TextWatcher() {
      public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
      public void afterTextChanged(Editable arg0) {}

      public void onTextChanged(CharSequence text, int arg1, int arg2, int arg3) {
        AccountSettings.validateUriField(smtp, text.toString());
      }
    });
    
    Bundle b = this.getArguments();
    if (b != null) {
      email.setText(b.getString("name"));
      pass.setText(b.getString("pass"));
      imap.setText(b.getString("imap"));
      smtp.setText(b.getString("smtp"));
      securityType.setSelection(AccountSettings.getSpinnerPosition(securityType.getAdapter(), b.getBoolean("ssl") ? "SSL" : "None"));
      messageAmount.setSelection(AccountSettings.getSpinnerPosition(messageAmount.getAdapter(), b.getInt("num")));
    }
    
    return v;
    
  }
  
  private void autoFillImapSmtpField(CharSequence email) {
    String m = email.toString();
    int pos = m.indexOf("@");
    if (pos != -1) {
      String domain = m.substring(pos + 1).toLowerCase();
      if (domainMap.containsKey(domain)) {
        domain = domainMap.get(domain);
      }
      imap.setText("imap."+domain);
      smtp.setText("smtp."+domain);
    }
  }
  
  private boolean isSsl() {
    return ((String)securityType.getSelectedItem()).equals("SSL") ? true : false;
  }
  
  public void onTextChanged(CharSequence text, int arg1, int arg2, int arg3) {
    AccountSettings.validateEmailField(email, text.toString());
    autoFillImapSmtpField(text);
  }

  public void afterTextChanged(Editable e) {}
  public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
  
  public MessageProvider.Type getType() {
    return MessageProvider.Type.EMAIL;
  }

  public AccountAndr getAccount() {
    String m = email.getText().toString();
    String p = pass.getText().toString();
    String im = imap.getText().toString();
    String sm = smtp.getText().toString();
    boolean ssl = this.isSsl();
    int num = Integer.parseInt((String)messageAmount.getSelectedItem());
    return new EmailAccountAndr(m, p, im, sm, ssl, num);
  }
  
  
}
