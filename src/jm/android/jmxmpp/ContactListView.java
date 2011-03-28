package jm.android.jmxmpp;

/*TODO:
 * Main menu - add to roster, filter all/online only/groups
 * Context menu per entry - delete, start chat, view chat history... add to MUC?
 * Try to find a way to launch new ChatView instance for chat per user, but re-use
 * the existing instance for all chats with a particular user... HashMap<jid,intent> maybe?
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.packet.Presence;

import jm.android.jmxmpp.service.XmppConnectionService;
import jm.android.jmxmpp.service.XmppConnectionService.LocalBinder;

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
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.AdapterView.OnItemClickListener;

public class ContactListView extends ListActivity {
	
	XmppConnectionService mConnectionService = null;
	List<RosterEntry> mRosterList = new ArrayList<RosterEntry>();
 	List<HashMap<String,String>> mRosterDisplayList = new ArrayList<HashMap<String,String>>();
 	private SimpleAdapter mRosterAdapter = null;
	
 	private static final Comparator<RosterEntry> ROSTER_NAME_ORDER =
		new Comparator<RosterEntry>() {
			@Override
			public int compare(RosterEntry arg0, RosterEntry arg1) {
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
			LocalBinder binder = (LocalBinder)service;
			mConnectionService = binder.getService();
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
			i.putExtra("participant", this.mRosterList.get(position).getUser());
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
	private class UpdateRosterThread extends AsyncTask<Void,Void,Roster> {
		@Override
		protected Roster doInBackground(Void... arg0) {
			Roster testRoster = null;
			testRoster = mConnectionService.getRoster();
			Collection<RosterEntry> entries = testRoster.getEntries();
			//mRosterList = Arrays.asList((RosterEntry[])entries.toArray());
			
			/* It's much more compact to just extract the arraylist directly as above
			 * but that is resulting in errors, I believe due to a lack of thread safety
			 */
			Iterator<RosterEntry> i = entries.iterator();
			mRosterList.clear();
			while(i.hasNext()) {
				RosterEntry currentEntry = i.next();
				mRosterList.add(currentEntry);
			}
			
			if(mRosterList != null && mRosterList.size() > 0) {
				Collections.sort(mRosterList, ROSTER_NAME_ORDER);
			}
			return testRoster;
		}
		
		@Override
		protected void onPostExecute(Roster roster) {
			mRosterDisplayList.clear();

			// If we did not connect to the xmpp server first in our service
			// the mRosterList will be null
			// could also just check to make sure we're connected to xmpp
			// before starting this thread
			if(mRosterList != null) {
				Iterator<RosterEntry> i = mRosterList.iterator();
				while(i.hasNext()) {
					RosterEntry current = i.next();
					
					HashMap<String,String> entry = new HashMap<String,String>();
					
					String statusLine = "Offline";
					Presence presence = roster.getPresence(current.getUser());
					
					if(presence.getStatus() != null) {
						statusLine = (presence.getStatus().equals("") ? 
								"Online" : presence.getStatus());
					}
									
					if(presence.getMode() != null && 
							!presence.getMode().equals("")) {
						statusLine += " - " + presence.getMode();
					}
					entry.put("text1", (current.getName() != null) ?
							current.getName() : current.getUser());
					entry.put("text2", statusLine);
					mRosterDisplayList.add(entry);
				}
				mRosterAdapter.notifyDataSetChanged();
			}
		}
	}
}