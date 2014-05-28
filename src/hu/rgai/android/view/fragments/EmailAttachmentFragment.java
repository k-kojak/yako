package hu.rgai.android.view.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import hu.rgai.android.beens.Attachment;
import hu.rgai.android.beens.FullSimpleMessage;
import hu.rgai.android.beens.MessageListElement;
import hu.rgai.android.beens.Account;
import hu.rgai.android.store.StoreHandler;
import hu.rgai.android.test.R;
import hu.rgai.android.tools.adapter.AttachmentAdapter;
import hu.rgai.android.view.activities.EmailDisplayerActivity;
import java.io.File;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class EmailAttachmentFragment extends Fragment {

  private View mView;
  private Account mAccount;
  private MessageListElement mMessage;
  private ListView mListView;
  private AttachmentAdapter mListAdapter;
  
  public static EmailAttachmentFragment newInstance() {
    EmailAttachmentFragment eaf = new EmailAttachmentFragment();
    return eaf;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState); //To change body of generated methods, choose Tools | Templates.
    EmailDisplayerActivity eda = (EmailDisplayerActivity)getActivity();
    mAccount = eda.getAccount();
    mMessage = eda.getMessage();
  }

  
  
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    mView = inflater.inflate(R.layout.attachment_displayer, container, false);
    mListView = (ListView) mView.findViewById(R.id.list);
//    convertAttachments();
    checkAttachments();
    mListAdapter = new AttachmentAdapter(this.getActivity(),
            ((FullSimpleMessage)mMessage.getFullMessage()).getAttachments(),
            mAccount, mMessage.getId());
    
    
    mListView.setAdapter(mListAdapter);
    return mView;
  }
  
  private void checkAttachments() {
    FullSimpleMessage fsm = (FullSimpleMessage)mMessage.getFullMessage();
    File folder = StoreHandler.getEmailAttachmentDownloadLocation();
    for (int i = 0; i < fsm.getAttachments().size(); i++) {
      Attachment a = (Attachment)fsm.getAttachments().get(i);
      File f = new File(folder, a.getFileName());
      if (f.exists()) {
        a.setSize(f.length());
        a.setProgress(100);
      } else if (a.getAttachmentDownloader() != null && !a.getAttachmentDownloader().isRunning()) {
        a.setProgress(0);
        a.setInProgress(false);
      }
    }
  }
  
  private void convertAttachments() {
    FullSimpleMessage fsm = (FullSimpleMessage)mMessage.getFullMessage();
    for (int i = 0; i < fsm.getAttachments().size(); i++) {
      Attachment a = fsm.getAttachments().get(i);
      if (a instanceof Attachment) {
        continue;
      }
//      int progress = Math.random() > 0.5 ? 100 : 0;
      fsm.getAttachments().set(i, new Attachment(a.getFileName(), a.getSize()));
    }
  }
}
