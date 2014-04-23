package hu.rgai.android.view.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import hu.rgai.android.beens.ProgressAttachment;
import hu.rgai.android.intent.beens.FullSimpleMessageParc;
import hu.rgai.android.intent.beens.MessageListElementParc;
import hu.rgai.android.intent.beens.account.AccountAndr;
import hu.rgai.android.test.R;
import hu.rgai.android.tools.adapter.AttachmentAdapter;
import hu.uszeged.inf.rgai.messagelog.beans.Attachment;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class EmailAttachmentFragment extends Fragment {

  private View mView;
  private AccountAndr mAccount;
  private MessageListElementParc mMessage;
  private ListView mListView;
  private AttachmentAdapter mListAdapter;
  
  public EmailAttachmentFragment(AccountAndr account, MessageListElementParc message) {
    this.mAccount = account;
    this.mMessage = message;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    mView = inflater.inflate(R.layout.attachment_displayer, container, false);
    mListView = (ListView) mView.findViewById(R.id.list);
    mListAdapter = new AttachmentAdapter(this.getActivity(),
            ((FullSimpleMessageParc)mMessage.getFullMessage()).getAttachments(),
            mAccount, mMessage.getId());
    convertAttachments();
    
    mListView.setAdapter(mListAdapter);
    return mView;
  }
  
  private void convertAttachments() {
    FullSimpleMessageParc fsm = (FullSimpleMessageParc)mMessage.getFullMessage();
    for (int i = 0; i < fsm.getAttachments().size(); i++) {
      Attachment a = fsm.getAttachments().get(i);
      if (a instanceof ProgressAttachment) {
        Log.d("rgai", "SKIP PROGRESS BECAUSE ITS ALREADY A PROGRESS IZE..JUHUHUUHUHUHUH");
        continue;
      }
//      int progress = Math.random() > 0.5 ? 100 : 0;
      fsm.getAttachments().set(i, new ProgressAttachment(a.getFileName(), a.getSize()));
    }
  }
}
