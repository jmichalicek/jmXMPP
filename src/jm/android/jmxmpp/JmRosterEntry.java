package jm.android.jmxmpp;

/** This class holds info to match up to 
 *  org.jivesoftware.smack.RosterEntry since it is not parcelable
 *  and so cannot be passed through AIDL
 */

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

public class JmRosterEntry implements Parcelable {
	String name;
	String user;
	String status;
	String type;
	
	public JmRosterEntry() {
		
	}
	
	public JmRosterEntry(String name, String user, String status, String type) {
		this.name = name;
		this.user = user;
		this.status = status;
		this.type = type;
	}
	
	private JmRosterEntry(Parcel p) {
		Bundle objectBundle = p.readBundle();
		name = objectBundle.getString("name");
		user = objectBundle.getString("user");
		status = objectBundle.getString("status");
		type = objectBundle.getString("type");

	}
	
	@Override
	public String toString() {
		String returnData = new String();
		if(name != null) {
			returnData += name + "\n";
		}
		if(user != null) {
			returnData += user;
		}
		return returnData;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel parcel, int arg1) {
		Bundle objectBundle = new Bundle();
		objectBundle.putString("name", name);
		objectBundle.putString("user", user);
		objectBundle.putString("status", status);
		objectBundle.putString("type", type);
		
		parcel.writeBundle(objectBundle);
	}
	
	public static final Parcelable.Creator<JmRosterEntry> CREATOR
	= new Parcelable.Creator<JmRosterEntry>() {
		public JmRosterEntry createFromParcel(Parcel in) {
			return new JmRosterEntry(in);
		}
		
		public JmRosterEntry[] newArray(int size) {
			return new JmRosterEntry[size];
		}
	};

}
