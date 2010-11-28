package jm.android.jmxmpp;

/** This class holds info to match up to 
 *  org.jivesoftware.smack.RosterEntry since it is not parcelable
 *  and so cannot be passed through AIDL
 */

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

public class JmRosterEntry implements Parcelable {
	String mName;
	String mUser;
	String mStatus;
	String mType;
	String mClass;
	String mPresence;
	
	public JmRosterEntry() {
		
	}
	
	public JmRosterEntry(String name, String user, String status, String type, String presence) {
		this.mName = name;
		this.mUser = user;
		this.mStatus = status;
		this.mType = type;
		this.mPresence = presence;
	}
	
	private JmRosterEntry(Parcel p) {
		Bundle objectBundle = p.readBundle();
		mName = objectBundle.getString("name");
		mUser = objectBundle.getString("user");
		mStatus = objectBundle.getString("status");
		mType = objectBundle.getString("type");

	}
	
	@Override
	public String toString() {
		String returnData = new String();
		if(mUser != null) {
			returnData += mUser;
		}
		if(mName != null) {
			returnData += ": " + mName;
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
		objectBundle.putString("name", mName);
		objectBundle.putString("user", mUser);
		objectBundle.putString("status", mStatus);
		objectBundle.putString("type", mType);
		
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
