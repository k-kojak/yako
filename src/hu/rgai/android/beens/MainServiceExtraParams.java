
package hu.rgai.android.beens;

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
  
//  public static class ParamStrings {
//    public static final String EXTRA_PARAMS = "extra_params";
//    public static final String FROM_NOTIFIER = "from_notifier";
//    public static final String QUERY_LIMIT = "query_limit";
//    public static final String QUERY_OFFSET = "query_offset";
//    public static final String LOAD_MORE = "load_more";
//    public static final String TYPE = "type";
//    public static final String ACT_VIEWING_MESSAGE = "act_viewing_message";
//    public static final String FORCE_QUERY = "force_query";
//    public static final String RESULT = "result";
//    public static final String ERROR_MESSAGE = "error_message";
//  }
  
  private boolean mFromNotifier = false;
  private int mQueryLimit = -1;
  private int mQueryOffset = -1;
  private boolean mLoadMore = false;
  private Account mAccount = null;
  private MessageListElement mActViewingMessage = null;
  private boolean mForceQuery = true;
  private int mResult = -1;
  
  public MainServiceExtraParams() {}
  
  public MainServiceExtraParams(Parcel in) {
    mFromNotifier = in.readByte() == 1;
    mQueryLimit = in.readInt();
    mQueryOffset = in.readInt();
    mLoadMore = in.readByte() == 1;
    mAccount = in.readParcelable(Account.class.getClassLoader());
    mActViewingMessage = in.readParcelable(MessageListElement.class.getClassLoader());;
    mForceQuery = in.readByte() == 1;
    mResult = in.readInt();
  }
  
  public int describeContents() {
    return 0;
  }

  public void writeToParcel(Parcel dest, int flags) {
    dest.writeByte((byte)(mFromNotifier ? 1 : 0));
    dest.writeInt(mQueryLimit);
    dest.writeInt(mQueryOffset);
    dest.writeByte((byte)(mLoadMore ? 1 : 0));
    dest.writeParcelable(mAccount, flags);
    dest.writeParcelable(mActViewingMessage, flags);
    dest.writeByte((byte)(mForceQuery ? 1 : 0));
    dest.writeInt(mResult);
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

  public Account getAccount() {
    return mAccount;
  }

  public void setAccount(Account mTtype) {
    this.mAccount = mTtype;
  }

  public MessageListElement getActViewingMessage() {
    return mActViewingMessage;
  }

  public void setActViewingMessage(MessageListElement mActViewingMessage) {
    this.mActViewingMessage = mActViewingMessage;
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

  @Override
  public String toString() {
    return "MainServiceExtraParams{" + "mFromNotifier=" + mFromNotifier + ", mQueryLimit=" + mQueryLimit + ", mQueryOffset=" + mQueryOffset + ", mLoadMore=" + mLoadMore + ", mAccount=" + mAccount + ", mActViewingMessage=" + mActViewingMessage + ", mForceQuery=" + mForceQuery + ", mResult=" + mResult + '}';
  }

}
