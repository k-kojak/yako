package hu.rgai.android.intent.beens;

import android.os.Parcel;
import android.os.Parcelable;
import hu.uszeged.inf.rgai.messagelog.beans.Person;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class PersonAndr extends Person implements Parcelable {
  
  public static final Parcelable.Creator<PersonAndr> CREATOR = new Parcelable.Creator<PersonAndr>() {
    public PersonAndr createFromParcel(Parcel in) {
      return new PersonAndr(in);
    }

    public PersonAndr[] newArray(int size) {
      return new PersonAndr[size];
    }
  };
  
  public PersonAndr(Person per) {
    super(per.getId(), per.getName(), per.getEmails());
  }
  
  public PersonAndr(Parcel in) {
    this.id = in.readInt();
    this.name = in.readString();
    emails = new ArrayList<String>();
    in.readStringList(emails);
  }
  
  public PersonAndr(int id, String name, List<String> emails) {
    super(id, name, emails);
  }
  
  public PersonAndr(int id, String name, String email) {
    super(id, name, email);
  }

  public int describeContents() {
    return 0;
  }

  public void writeToParcel(Parcel out, int flags) {
    out.writeInt(this.id);
    out.writeString(this.name);
    out.writeStringList(this.emails);
  }
  
}