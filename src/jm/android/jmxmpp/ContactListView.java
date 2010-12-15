package jm.android.jmxmpp;

/*TODO:
 * Main menu - add to roster, filter all/online only/groups
 * Context menu per entry - delete, start chat, view chat history... add to MUC?
 * Try to find a way to launch new ChatView instance for chat per user, but re-use
 * the existing instance for all chats with a particular user... HashMap<jid,intent> maybe?
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.AdapterView.OnItemClickListener;

public class ContactListView extends ListActivity {
	
	IXmppConnectionService mConnectionService = null;
 	List<JmRosterEntry> mRosterList = null; // parallel to ArrayAdapter
 	List<HashMap<String,String>> mRosterDisplayList = new ArrayList<HashMap<String,String>>();
 	private SimpleAdapter mRosterAdapter = null;
	
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
		
		mRosterAdapter = new SimpleAdapter(this,mRosterDisplayList,
				android.R.layout.simple_list_item_2,
				new String[] {"text1","text2"},
				new int[] {android.R.id.text1,android.R.id.text2}
		);
		
		connectToXmppService();
		
		registerReceiver(rosterUpdatedReceiver,
				new IntentFilter("jm.android.jmxmpp.ROSTER_UPDATED"));
		
		ListView rosterListView = (ListView)findViewById(android.R.id.list);
		rosterListView.setOnItemClickListener(rosterItemClicked);
		
		setListAdapter(mRosterAdapter);
		
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
			mRosterDisplayList.clear();

			Iterator<JmRosterEntry> i = mRosterList.iterator();
			while(i.hasNext()) {
				JmRosterEntry current = i.next();
				
				String statusLine = null;
				statusLine = (current.getPresenceStatus() != null) ? 
						(!current.getPresenceStatus().equals("") ?
								current.getPresenceStatus() : "Online") : "Offline";
				
				if(current.getPresenceMode() != null) {
					statusLine += " - " + current.getPresenceMode();
				}
				
				HashMap<String,String> entry = new HashMap<String,String>();
				entry.put("text1", (current.getName() != null) ?
						current.getName() : current.getUser());
				entry.put("text2", statusLine);
				mRosterDisplayList.add(entry);
				mRosterAdapter.notifyDataSetChanged();
			}
		}
		
	}
}