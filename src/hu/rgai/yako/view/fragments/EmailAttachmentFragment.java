package hu.rgai.yako.view.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import hu.rgai.yako.beens.Account;
import hu.rgai.yako.beens.Attachment;
import hu.rgai.yako.beens.FullSimpleMessage;
import hu.rgai.yako.beens.MessageListElement;
import hu.rgai.yako.sql.AttachmentDAO;
import hu.rgai.yako.store.StoreHandler;
import hu.rgai.android.test.R;
import hu.rgai.yako.adapters.AttachmentAdapter;
import hu.rgai.yako.view.activities.EmailDisplayerActivity;
import java.io.File;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class EmailAttachmentFragment extends Fragment {

  private View mView;
  private Account mAccount;
  private FullSimpleMessage mFullMessage;
  private ListView mListView;
  private AttachmentAdapter mListAdapter;
  
  public static EmailAttachmentFragment newInstance() {
    EmailAttachmentFragment eaf = new EmailAttachmentFragment();
    return eaf;
  }

  
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    
    EmailDisplayerActivity eda = (EmailDisplayerActivity)getActivity();
    mAccount = eda.getAccount();
    mFullMessage = eda.getContent();
    String messageId = eda.getMessage().getId();
    
    
    mView = inflater.inflate(R.layout.attachment_displayer, container, false);
    mListView = (ListView) mView.findViewById(R.id.list);
    Log.d("rgai", "email attachment fragment oncreateview");
    checkAttachments();
    mListAdapter = new AttachmentAdapter(this.getActivity(), mFullMessage.getAttachments(), mAccount, messageId);

    mListView.setAdapter(mListAdapter);
    return mView;
  }
  
  private void checkAttachments() {
    File folder = StoreHandler.getEmailAttachmentDownloadLocation();
    for (int i = 0; i < mFullMessage.getAttachments().size(); i++) {
      Attachment a = mFullMessage.getAttachments().get(i);
      File f = new File(folder, a.getFileName());
      if (f.exists()) {
        a.setSize(f.length());
        a.setProgress(100);
      }/* else if (a.getAttachmentDownloader() != null && !a.getAttachmentDownloader().isRunning()) {
        a.setProgress(0);
        a.setInProgress(false);
      }*/
    }
  }
  
//  private void convertAttachments() {
//    FullSimpleMessage fsm = (FullSimpleMessage)mMessage.getFullMessage();
//    for (int i = 0; i < fsm.getAttachments().size(); i++) {
//      Attachment a = fsm.getAttachments().get(i);
//      if (a instanceof Attachment) {
//        continue;
//      }
////      int progress = Math.random() > 0.5 ? 100 : 0;
//      fsm.getAttachments().set(i, new Attachment(a.getFileName(), a.getSize()));
//    }
//  }
}
