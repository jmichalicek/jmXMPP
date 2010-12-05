package jm.android.jmxmpp;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import jm.android.jmxmpp.service.IXmppConnectionService;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class ContactListView extends ListActivity {
	IXmppConnectionService mConnectionService = null;
	ArrayAdapter<String> mRosterArrayAdapter = null;
 	List<JmRosterEntry> mRosterList = null; // parallel to ArrayAdapter
	
	private static final Comparator<JmRosterEntry> ROSTER_NAME_ORDER =
		new Comparator<JmRosterEntry>() {
			@Override
			public int compare(JmRosterEntry arg0, JmRosterEntry arg1) {
				String compare1 = (arg0.getName() != null) ? arg0.getName()
						: arg0.getUser();
				String compare2 = (arg1.getName() != null) ? arg1.getName()
						: arg1.getUser();
				return compare1.compareToIgnoreCase(compare2);
			}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.roster_view);
		mRosterArrayAdapter = new ArrayAdapter<String>(this,R.layout.roster_row);
		connectToXmppService();
		
		registerReceiver(rosterUpdatedReceiver,
				new IntentFilter("jm.android.jmxmpp.ROSTER_UPDATED"));
		
		ListView rosterListView = (ListView)findViewById(android.R.id.list);
		rosterListView.setOnItemClickListener(rosterItemClicked);
		
		setListAdapter(mRosterArrayAdapter);
	}

	private ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			mConnectionService = IXmppConnectionService.Stub.asInterface(service);
			
			//After logging in a ROSTER_UPDATED intent is received
			//but sometimes it comes before this activity is ready to receive
			//so manually update now to be safe
			if(mConnectionService != null) {
				UpdateRosterThread updater = new UpdateRosterThread();
				updater.execute();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			mConnectionService = null;
		}
	};

	private void connectToXmppService() {
		bindService(new Intent("jm.android.jmxmpp.service.XmppConnectionService"),
				mConnection,Context.BIND_AUTO_CREATE);
	}
	
	/**
	 * Launches intent for new chat activity with selected user
	 */
	private void startChat(int position) {
		Intent i = new Intent("jm.android.jmxmpp.START_CHAT");
		i.setClassName("jm.android.jmxmpp", "jm.android.jmxmpp.ChatView");
		i.putExtra("participant", this.mRosterList.get(position));
		startActivity(i);
	}
	
	//Listeners
	
	/* For now just use one receiver for all roster updates
	 * later more specific receivers/broadcasts may need implemented.
	 * Currently when any sort of roster update happens such as presence change
	 * the entire roster is rebuilt.
	 */
	BroadcastReceiver rosterUpdatedReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context arg0, Intent arg1) {
			if(mConnectionService != null) {
				UpdateRosterThread updater = new UpdateRosterThread();
				updater.execute();
			}
		}
	};
	
	OnItemClickListener rosterItemClicked = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> arg0, View view, int position,
				long id) {
			startChat(position);
		}
	};
	
	// Threads
	private class UpdateRosterThread extends AsyncTask<Void,Void,Void> {
		@Override
		protected Void doInBackground(Void... arg0) {
			try {
				mRosterList = mConnectionService.getRoster();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
			
			if(mRosterList != null && mRosterList.size() > 0) {
				Collections.sort(mRosterList, ROSTER_NAME_ORDER);
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Void a) {
			mRosterArrayAdapter.clear();

			Iterator<JmRosterEntry> i = mRosterList.iterator();
			while(i.hasNext()) {
				JmRosterEntry current = i.next();
				String rosterString = new String();
				
				if(current.getName() != null) {
					rosterString = current.getName();
				} else {
					rosterString = current.getUser();
				}
				
				String statusLine = null;
				statusLine = (current.getPresenceStatus() != null) ? 
						(!current.getPresenceStatus().equals("") ?
								current.getPresenceStatus() : "Online") : "Offline";
				
				if(current.getPresenceMode() != null) {
					statusLine += " - " + current.getPresenceMode();
				}
				
				rosterString += "\n" + statusLine;
				
				mRosterArrayAdapter.add(rosterString);
			}
		}
		
	}
}