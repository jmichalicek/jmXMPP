package jm.android.jmxmpp;

import java.util.List;

import jm.android.jmxmpp.service.IXmppConnectionService;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class ContactListView extends ListActivity {
	IXmppConnectionService mConnectionService = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.roster_view);

		ListView rosterListView = (ListView)findViewById(android.R.id.list);
		rosterListView.setAdapter(
				new ArrayAdapter<JmRosterEntry>(this,android.R.layout.simple_list_item_1)
		);
		
		connectToXmppService();
	}

	public void fillRoster() {
		List<JmRosterEntry> rosterList = null;
		try {
			rosterList = mConnectionService.getRoster();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		ListView rosterListView = (ListView)findViewById(android.R.id.list);
		JmRosterEntry[] temp = new JmRosterEntry[rosterList.size()];
		rosterList.toArray(temp);

		rosterListView.setAdapter(				
				new ArrayAdapter<JmRosterEntry>(this,android.R.layout.simple_list_item_1,
						temp)
		);
	}

	public ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			mConnectionService = IXmppConnectionService.Stub.asInterface(service);
			if(mConnectionService != null) {
				fillRoster();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			mConnectionService = null;
		}
	};

	public void connectToXmppService() {
		bindService(new Intent("jm.android.jmxmpp.service.XmppConnectionService"),
				mConnection,Context.BIND_AUTO_CREATE);
	}
}