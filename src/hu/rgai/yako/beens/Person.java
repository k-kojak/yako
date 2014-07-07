package hu.rgai.yako.beens;

import hu.rgai.yako.config.Settings;
import hu.rgai.yako.messageproviders.MessageProvider;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.provider.ContactsContract.PhoneLookup;

/**
 * 
 * @author Tamas Kojedzinszky
 */
public final class Person implements Parcelable {

  protected String id;
  protected String name;
  protected String secondaryName;
  protected MessageProvider.Type type;
  private final long contactId;

  private static Map<String, Person> storedPerson = null;

  private Map<MessageProvider.Type, Set<String>> idMap = null;

  public static final Parcelable.Creator<Person> CREATOR = new Parcelable.Creator<Person>() {
    @Override
    public Person createFromParcel(Parcel in) {
      return new Person(in);
    }

    @Override
    public Person[] newArray(int size) {
      return new Person[size];
    }
  };

  public Person(String id, String name, MessageProvider.Type type) {
    this(-1, id, name, type);
  }

  public Person(long contactId, String id, String name, MessageProvider.Type type) {
    this.contactId = contactId;
    this.id = id;
    this.name = name;
    this.type = type;
  }

  /**
   * The data which identifies the person. Although this is not enough to be
   * unique, the type of the Person should be used with this data to be sure
   * that the person and it's id is unique. The id holds something like: phone
   * number, email address or Facebook user ID, etc.
   * 
   * @return the id of the user
   */
  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getSecondaryName() {
    return secondaryName;
  }

  public MessageProvider.Type getType() {
    return type;
  }

  public void setType(MessageProvider.Type type) {
    this.type = type;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setSecondaryName(String secondaryName) {
    this.secondaryName = secondaryName;
  }

  public void addId(MessageProvider.Type type, String id) {
    if (id == null) {
      throw new RuntimeException("Person id cannot be null!");
    }
    if (idMap == null) {
      idMap = new EnumMap<MessageProvider.Type, Set<String>>(MessageProvider.Type.class);
    }
    if (!idMap.containsKey(type)) {
      idMap.put(type, new HashSet<String>());
    }
    idMap.get(type).add(id);
  }

  public String getOneId(MessageProvider.Type type) {
    Set<String> l = this.getIds(type);
    if (l != null && !l.isEmpty()) {
      Iterator<String> i = l.iterator();
      return i.next();
    }
    return null;
  }

  public Set<String> getIds(MessageProvider.Type type) {
    if (idMap != null && idMap.containsKey(type)) {
      return idMap.get(type);
    }
    return null;
  }

  public long getContactId() {
    return contactId;
  }

  public Person(Parcel in) {
    this.id = in.readString();
    this.name = in.readString();
    this.contactId = in.readLong();
    int length = in.readInt();
    String[] types = new String[length];
    in.readStringArray(types);
    idMap = new EnumMap<MessageProvider.Type, Set<String>>(MessageProvider.Type.class);
    for (String t : types) {
      List<String> valList = new LinkedList<String>();
      in.readStringList(valList);
      idMap.put(MessageProvider.Type.valueOf(t), new HashSet<String>(valList));
    }
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel out, int flags) {
    String[] typesStr = new String[0];
    if (idMap != null && !idMap.isEmpty()) {
      Set<MessageProvider.Type> types = idMap.keySet();
      typesStr = new String[types.size()];
      int i = 0;
      for (MessageProvider.Type t : types) {
        typesStr[i++] = t.toString();
      }
    }

    out.writeString(id);
    out.writeString(name);
    out.writeLong(this.contactId);
    out.writeInt(typesStr.length);
    out.writeStringArray(typesStr);
    for (String t : typesStr) {
      ArrayList<String> al = new ArrayList<String>(idMap.get(MessageProvider.Type.valueOf(t)));
      out.writeStringList(al);
    }
  }

  @Override
  public String toString() {
    return "Person{" + "contactId=" + contactId + ", id = " + id + ", name=" + name + ", type=" + type + '}';
  }

  public static Person searchPersonAndr(Context context, Person p) {
    if (p == null) {
      return null;
    }
    String key = p.getType().toString() + "_" + p.getId();
    if (storedPerson == null) {
      storedPerson = new HashMap<String, Person>();
    }
    if (storedPerson.containsKey(key)) {
      return storedPerson.get(key);
    } else {

      long rawContactId = getUid(context, p.getType(), p.getId(), p.getName());

      key = p.getType().toString() + "_" + rawContactId;
      if (storedPerson.containsKey(key)) {
        return storedPerson.get(key);
      } else {
        Person pa = null;
        if (rawContactId != -1) {
          // if dealing with sms, than p.getName() contains the phone number, so
          // that is the user id for sending message
          if (p.getType().equals(MessageProvider.Type.SMS)) {
            pa = getUserData(context, rawContactId, p.getName());
            pa.setType(MessageProvider.Type.SMS);
            // if not using sms, than p.getId() can be used for communication
            // (fb id, email addr, etc.)
          } else {
            pa = getUserData(context, rawContactId, p.getId());
            pa.setType(p.getType());

          }
          // Log.d("rgai", "STORING IN PERSON MAP -> " + key + ", " + pa);
          storedPerson.put(key, pa);
        }
        // user is not in contact list
        else {
          if (p.getType().equals(MessageProvider.Type.SMS)) {
            pa = new Person(-1, p.getName(), p.getName(), MessageProvider.Type.SMS);
          } else {
            pa = new Person(-1, p.getId(), p.getName(), p.getType());

            if (p.getType().equals(MessageProvider.Type.FACEBOOK)) {
              pa.setSecondaryName(p.getSecondaryName());
            }

          }
        }
        return pa;
      }
    }
  }

  private static Person getUserData(Context context, long rawContactId, String userAddrId) {
    Person pa = null;

    // selecting name
    Cursor cursor = context.getContentResolver().query(ContactsContract.Data.CONTENT_URI, new String[] { Settings.CONTACT_DISPLAY_NAME }, ContactsContract.Data.RAW_CONTACT_ID + " = ?",
        new String[] { rawContactId + "" }, null);
    if (cursor.getCount() > 0) {
      cursor.moveToNext();
      pa = new Person(rawContactId, userAddrId, cursor.getString(0), MessageProvider.Type.SMS);
    }
    cursor.close();
    if (pa != null) {
      // selection phone numbers
      cursor = context.getContentResolver().query(ContactsContract.Data.CONTENT_URI, new String[] { ContactsContract.CommonDataKinds.Phone.DATA },

          ContactsContract.Data.RAW_CONTACT_ID + " = ? " + " AND " + ContactsContract.Data.MIMETYPE + " = ?",

          new String[] { rawContactId + "", ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE }, null);
      while (cursor.moveToNext()) {
        pa.addId(MessageProvider.Type.SMS, cursor.getColumnName(0));
      }
      cursor.close();

      // selection emails
      cursor = context.getContentResolver().query(ContactsContract.Data.CONTENT_URI, new String[] { ContactsContract.CommonDataKinds.Email.DATA },

          ContactsContract.Data.RAW_CONTACT_ID + " = ? " + " AND " + ContactsContract.Data.MIMETYPE + " = ?",

          new String[] { rawContactId + "", ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE }, null);
      while (cursor.moveToNext()) {
        pa.addId(MessageProvider.Type.EMAIL, cursor.getColumnName(0));
      }
      cursor.close();

      // selection facebook
      cursor = context.getContentResolver().query(
          ContactsContract.Data.CONTENT_URI,
          new String[] { ContactsContract.CommonDataKinds.Email.DATA1 },

          ContactsContract.Data.RAW_CONTACT_ID + " = ? " + " AND " + ContactsContract.Data.MIMETYPE + " = ? " + " AND " + ContactsContract.Data.DATA2 + " = ? " + " AND " + ContactsContract.Data.DATA5
              + " = ? " + " AND " + ContactsContract.Data.DATA6 + " = ? ",

          new String[] { rawContactId + "", ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE, ContactsContract.CommonDataKinds.Im.TYPE_OTHER + "",
              ContactsContract.CommonDataKinds.Im.PROTOCOL_CUSTOM + "", Settings.Contacts.DataKinds.Facebook.CUSTOM_NAME }, null);
      while (cursor.moveToNext()) {
        pa.addId(MessageProvider.Type.FACEBOOK, cursor.getColumnName(0));
      }
      cursor.close();
    }

    return pa;
  }

  private static long getUid(Context context, MessageProvider.Type type, String id) {
    return getUid(context, type, id, null);
  }

  private static long getUid(Context context, MessageProvider.Type type, String id, String id2) {
    long uid = -1;

    String selection = "";
    String[] selectionArgs = null;
    if (type.equals(MessageProvider.Type.SMS)) {
      return getUidForSms(context, id2);
    } else if (type.equals(MessageProvider.Type.EMAIL) || type.equals(MessageProvider.Type.GMAIL)) {
      selection = ContactsContract.Data.MIMETYPE + " = ? " + " AND " + ContactsContract.CommonDataKinds.Email.DATA + " = ? ";
      selectionArgs = new String[] { ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE, id };
    } else if (type.equals(MessageProvider.Type.FACEBOOK)) {
      selection = ContactsContract.Data.MIMETYPE + " = ? " + " AND " + ContactsContract.Data.DATA2 + " = ? " + " AND " + ContactsContract.Data.DATA5 + " = ? " + " AND " + ContactsContract.Data.DATA6
          + " = ? " + " AND " + ContactsContract.Data.DATA10 + " = ? ";
      selectionArgs = new String[] { ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE, ContactsContract.CommonDataKinds.Im.TYPE_OTHER + "",
          ContactsContract.CommonDataKinds.Im.PROTOCOL_CUSTOM + "", Settings.Contacts.DataKinds.Facebook.CUSTOM_NAME, id };
    }

    Cursor cursor = context.getContentResolver().query(
        ContactsContract.Data.CONTENT_URI,
        new String[] { ContactsContract.Data.RAW_CONTACT_ID },
        selection,
        selectionArgs,
        null);
    if (cursor != null) {
      if (cursor.getCount() > 0) {
        cursor.moveToFirst();
        uid = cursor.getLong(0);
      }
      cursor.close();
    }

    return uid;
  }

  private static long getUidForSms(Context context, String number) {
    long uid = -1;
    long contactId = -1;

    ContentResolver contentResolver = context.getContentResolver();
    Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
    String[] projection = new String[] { PhoneLookup._ID };
    Cursor cursor = contentResolver.query(uri, projection, null, null, null);
    if (cursor != null) {
      while (cursor.moveToNext()) {
        contactId = cursor.getLong(cursor.getColumnIndexOrThrow(PhoneLookup._ID));
      }
      cursor.close();
    }

    // second query
    if (contactId != -1) {
      String[] contactProjection = new String[] {
          ContactsContract.Data.RAW_CONTACT_ID
      };
      Cursor contactCursor = contentResolver.query(ContactsContract.Data.CONTENT_URI,
          contactProjection,
          ContactsContract.Data.CONTACT_ID + " = ? ",
          new String[] { contactId + "" },
          null);
      if (contactCursor != null) {
        while (contactCursor.moveToNext()) {
          uid = contactCursor.getLong(contactCursor.getColumnIndexOrThrow(ContactsContract.Data.RAW_CONTACT_ID));
          break;
        }
        contactCursor.close();
      }
    }

    return uid;
  }

  @Override
  public int hashCode() {
    return Long.valueOf(contactId).hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Person))
      return false;
    if (obj == this)
      return true;
    return (this.contactId == ((Person) obj).contactId) && (this.id.equals(((Person) obj).id)) && (this.name.equals(((Person) obj).name)) && (this.type == ((Person) obj).type);
  }
}