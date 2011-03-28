package jm.android.jmxmpp;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * 
 * @author Justin Michalicek
 *
 * Class to hold data from asmack Message objects
 * since they are not parcelable and a parcelable class is needed
 * for attaching to Intent objects.  aidl interface still exists
 * but should no longer be needed
 */

public class JmMessage implements Parcelable {
	private String from;
	private String text;
	
	public JmMessage() {
	}
	
	public JmMessage(String from, String text) {
		this.from = from;
		this.text = text;
	}
	
	private JmMessage(Parcel p) {
		Bundle objectBundle = p.readBundle();
		from = objectBundle.getString("from");
		text = objectBundle.getString("text");
	}
	
	/**
	 * @return the from
	 */
	public String getFrom() {
		return from;
	}
	/**
	 * @param from the from to set
	 */
	public void setFrom(String from) {
		this.from = from;
	}
	/**
	 * @return the text
	 */
	public String getText() {
		return text;
	}
	/**
	 * @param text the text to set
	 */
	public void setText(String text) {
		this.text = text;
	}
	
	@Override
	public void writeToParcel(Parcel parcel, int arg1) {
		Bundle objectBundle = new Bundle();
		objectBundle.putString("from", from);
		objectBundle.putString("text", text);
		
		parcel.writeBundle(objectBundle);
	}
	
	public static final Parcelable.Creator<JmMessage> CREATOR
	= new Parcelable.Creator<JmMessage>() {
		public JmMessage createFromParcel(Parcel in) {
			return new JmMessage(in);
		}
		
		public JmMessage[] newArray(int size) {
			return new JmMessage[size];
		}
	};

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}
}
