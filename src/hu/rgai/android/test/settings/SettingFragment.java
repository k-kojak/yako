package hu.rgai.android.test.settings;

import hu.rgai.android.intent.beens.account.AccountAndr;
import hu.uszeged.inf.rgai.messagelog.MessageProvider.Type;

/**
 *
 * @author Tamas Kojedzinszky
 */
public interface SettingFragment {
  public Type getType();
  public AccountAndr getAccount();
}
