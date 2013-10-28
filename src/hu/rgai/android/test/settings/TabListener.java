/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package hu.rgai.android.test.settings;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
//import com.actionbarsherlock.app.ActionBar;
//import com.actionbarsherlock.app.SherlockFragment;
import hu.rgai.android.intent.beens.account.AccountAndr;
import hu.rgai.android.intent.beens.account.EmailAccountAndr;
import hu.rgai.android.intent.beens.account.FacebookAccountAndr;
import hu.rgai.android.intent.beens.account.GmailAccountAndr;
import hu.uszeged.inf.rgai.messagelog.MessageProvider;
import java.io.Serializable;

/**
 *
 * @author Tamas Kojedzinszky
 * @deprecated
 */
public class TabListener<T >  {
  
//  protected SherlockFragment mFragment = null;

  AccountSettings activity;
  String tag;
  Class<T> classname;
  AccountAndr account;

  public TabListener(Activity activity, String tag, Class<T> classname, AccountAndr account) {
//    this.activity = (AccountSettings)activity;
    this.tag = tag;
    this.classname = classname;
    this.account = account;
  }
  
//  public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
//    if (mFragment == null) {
//      Bundle params = this.getBundle();
//      mFragment = (SherlockFragment)Fragment.instantiate(activity, classname.getName(), params);
//      ft.add(android.R.id.content, mFragment, tag);
//      activity.addAccount(mFragment);
//    } else {
//      ft.attach(mFragment);
//    }
//  }
  
//  public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
//    if (mFragment != null) {
//      ft.detach(mFragment);
//    }
//  }
  
//  public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
//  }
  
  private Bundle getBundle() {
    Bundle b = null;
    if (account != null) {
      b = new Bundle();
      if (account instanceof GmailAccountAndr) {
        GmailAccountAndr ga = (GmailAccountAndr)account;
        b.putString("name", ga.getEmail());
        b.putString("pass", ga.getPassword());
        b.putInt("num", ga.getMessageLimit());
      } else if (account instanceof FacebookAccountAndr) {
        FacebookAccountAndr fa = (FacebookAccountAndr)account;
        b.putString("name", fa.getUserName());
        b.putString("pass", fa.getPassword());
        b.putInt("num", fa.getMessageLimit());
      } else if (account instanceof EmailAccountAndr) {
        EmailAccountAndr ea = (EmailAccountAndr)account;
        b.putString("name", ea.getEmail());
        b.putString("pass", ea.getPassword());
        b.putString("imap", ea.getImapAddress());
        b.putString("smtp", ea.getSmtpAddress());
        b.putBoolean("ssl", ea.isSsl());
        b.putInt("num", ea.getMessageLimit());
      }
    }
    
    return b;
  }

}
