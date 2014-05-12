package hu.rgai.android.tools.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;
import hu.rgai.android.beens.MessageRecipient;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class AutoCompleteRow extends RelativeLayout {

  private MessageRecipient user;
  
  public AutoCompleteRow(Context context) {
    super(context);
  }

  public AutoCompleteRow(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public AutoCompleteRow(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  public void setRecipient(MessageRecipient user) {
    this.user = user;
  }
  
  public MessageRecipient getRecipient() {
    return user;
  }

}
