
package hu.rgai.yako.beens;

import java.util.*;

import android.os.Parcel;
import android.os.Parcelable;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class MainServiceExtraParams implements Parcelable {

  public static final Parcelable.Creator<MainServiceExtraParams> CREATOR = new Parcelable.Creator<MainServiceExtraParams>() {
    public MainServiceExtraParams createFromParcel(Parcel in) {
      return new MainServiceExtraParams(in);
    }

    public MainServiceExtraParams[] newArray(int size) {
      return new MainServiceExtraParams[size];
    }
  };
  
  private boolean mFromNotifier = false;
  private int mQueryLimit = -1;
  private int mQueryOffset = -1;
  private boolean mLoadMore = false;
  private List<Account> mAccounts = new LinkedList<>();
  private boolean mForceQuery = false;
  private int mResult = -1;
  private boolean mMessagesRemovedAtServer = false;
  private boolean mSplittedMessageSecondPart = false;
  private boolean mNewMessageArrivedRequest = false;
  private TreeMap<String, MessageListElement> mSplittedMessages = new TreeMap<>();

  public MainServiceExtraParams() {}
  
  public MainServiceExtraParams(Parcel in) {
    mFromNotifier = in.readByte() == 1;
    mQueryLimit = in.readInt();
    mQueryOffset = in.readInt();
    mLoadMore = in.readByte() == 1;
    in.readList(mAccounts,Account.class.getClassLoader());
    mForceQuery = in.readByte() == 1;
    mResult = in.readInt();
    mMessagesRemovedAtServer = in.readByte() == 1;
    mSplittedMessageSecondPart = in.readByte() == 1;
    mNewMessageArrivedRequest = in.readByte() == 1;

    int splittedLength = in.readInt();
    for (int i = 0; i < splittedLength; i++) {
      mSplittedMessages.put(in.readString(), (MessageListElement)in.readParcelable(MessageListElement.class.getClassLoader()));
    }
  }
  
  public int describeContents() {
    return 0;
  }

  public void writeToParcel(Parcel dest, int flags) {
    dest.writeByte((byte)(mFromNotifier ? 1 : 0));
    dest.writeInt(mQueryLimit);
    dest.writeInt(mQueryOffset);
    dest.writeByte((byte) (mLoadMore ? 1 : 0));
    dest.writeList(mAccounts);
    dest.writeByte((byte) (mForceQuery ? 1 : 0));
    dest.writeInt(mResult);
    dest.writeByte((byte) (mMessagesRemovedAtServer ? 1 : 0));
    dest.writeByte((byte) (mSplittedMessageSecondPart ? 1 : 0));
    dest.writeByte((byte) (mNewMessageArrivedRequest ? 1 : 0));

    dest.writeInt(mSplittedMessages.size());
    for (Map.Entry<String, MessageListElement> e : mSplittedMessages.entrySet()) {
      dest.writeString(e.getKey());
      dest.writeParcelable(e.getValue(), flags);
    }
  }

  public boolean isFromNotifier() {
    return mFromNotifier;
  }

  public void setFromNotifier(boolean mFromNotifier) {
    this.mFromNotifier = mFromNotifier;
  }

  public int getQueryLimit() {
    return mQueryLimit;
  }

  public void setQueryLimit(int mQueryLimit) {
    this.mQueryLimit = mQueryLimit;
  }

  public int getQueryOffset() {
    return mQueryOffset;
  }

  public void setQueryOffset(int mQueryOffset) {
    this.mQueryOffset = mQueryOffset;
  }

  public boolean isLoadMore() {
    return mLoadMore;
  }

  public void setLoadMore(boolean mLoadMore) {
    this.mLoadMore = mLoadMore;
  }
  
  public boolean isAccountsEmpty() {
    return mAccounts.isEmpty();
  }
  
  public boolean accountsContains(Account acc) {
    return mAccounts.contains(acc);
  }
  
  public List<Account> getAccounts() {
    return Collections.unmodifiableList(mAccounts) ;
  }

  public void addAccount(Account acc) {
    this.mAccounts.add(acc);
  }

  public void setOnNewMessageArrived(boolean newMessageArrivedRequest) {
    mNewMessageArrivedRequest = newMessageArrivedRequest;
  }

  public boolean isNewMessageArrivedRequest() {
    return mNewMessageArrivedRequest;
  }

  public void setAccount(Account account) {
    mAccounts.clear();
    mAccounts.add(account);
  }
  
  public void setAccounts(List<Account> accounts) {
    this.mAccounts = accounts;
  }

  public boolean isForceQuery() {
    return mForceQuery;
  }

  public void setForceQuery(boolean mForceQuery) {
    this.mForceQuery = mForceQuery;
  }

  public int getResult() {
    return mResult;
  }

  public void setResult(int mResult) {
    this.mResult = mResult;
  }

  public boolean isMessagesRemovedAtServer() {
    return mMessagesRemovedAtServer;
  }

  public void setMessagesRemovedAtServer(boolean mMessagesRemovedAtServer) {
    this.mMessagesRemovedAtServer = mMessagesRemovedAtServer;
  }

  public void setSplittedMessageSecondPart(boolean splittedMessageSecondPart) {
    mSplittedMessageSecondPart = splittedMessageSecondPart;
  }

  public void setSplittedMessages(TreeMap<String, MessageListElement> splittedMessages) {
    mSplittedMessages = splittedMessages;
  }

  public TreeMap<String, MessageListElement> getSplittedMessages() {
    return mSplittedMessages;
  }

  public boolean isSplittedMessageSecondPart() {
    return mSplittedMessageSecondPart;
  }

  @Override
  public String toString() {
    String ToString = "MainServiceExtraParams{" + "mFromNotifier=" + mFromNotifier + ", mQueryLimit=" + mQueryLimit + ", mQueryOffset=" + mQueryOffset + ", mLoadMore=" + mLoadMore + ", mAccounts=";
    for (int i = 0; i < mAccounts.size(); i++) {
      ToString += mAccounts.get(i) + ", ";
    }
    ToString += "mForceQuery=" + mForceQuery + ", mResult=" + mResult + '}';
    return  ToString;
  }


}
